package com.gom.badminton.controller;

import com.gom.badminton.entity.Booking;
import com.gom.badminton.dto.WebhookRequest;
import com.gom.badminton.repository.BookingRepository;
import com.gom.badminton.service.BookingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/bookings")
@CrossOrigin(origins = "*")
public class BookingController {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private BookingService bookingService;

    // Chuỗi mã bảo mật tự đặt để cấu hình trùng khớp bên phía cổng SePay
    private static final String SEPAY_API_KEY = "GOM_SUPER_SECRET_TOKEN_12345";

    @PostMapping
    public ResponseEntity<?> createBooking(@RequestBody Booking request) {
        try {
            Booking result = bookingService.createCustomerBooking(request);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/lookup")
    public ResponseEntity<?> lookupByPhone(@RequestParam String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Vui lòng nhập số điện thoại.");
        }

        List<Booking> rawList = bookingRepository.findByPhoneNumberOrderByBookingDateDesc(phone.trim());
        List<Booking> verifiedList = bookingService.checkAndExpireBookings(rawList);

        if (verifiedList.isEmpty()) {
            return ResponseEntity.status(404).body("Không tìm thấy dữ liệu.");
        }

        List<Booking> masked = verifiedList.stream().map(b -> {
            Booking m = new Booking();
            m.setId(b.getId());
            m.setBookingCode(b.getBookingCode());
            m.setFullName(b.getFullName().substring(0, Math.min(b.getFullName().length(), 2)) + "***");
            m.setPhoneNumber(b.getPhoneNumber().substring(0, 4) + "***" + b.getPhoneNumber().substring(b.getPhoneNumber().length() - 3));
            m.setGender(b.getGender());
            m.setBookingDate(b.getBookingDate());
            m.setCourtNumber(b.getCourtNumber());
            m.setSessionTime(b.getSessionTime());
            m.setTotalPrice(b.getTotalPrice());
            m.setDepositAmount(b.getDepositAmount());
            m.setPaymentStatus(b.getPaymentStatus());
            return m;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(masked);
    }

    @GetMapping("/status/{code}")
    public ResponseEntity<?> getStatusByCode(@PathVariable String code) {
        Optional<Booking> opt = bookingRepository.findByBookingCode(code);
        if (opt.isEmpty()) {
            return ResponseEntity.ok().body("{\"paymentStatus\":\"EXPIRED_OR_DELETED\"}");
        }

        Booking b = opt.get();
        if ("PENDING".equals(b.getPaymentStatus()) && b.getCreatedAt().plusMinutes(5).isBefore(LocalDateTime.now())) {
            bookingRepository.delete(b);
            return ResponseEntity.ok().body("{\"paymentStatus\":\"EXPIRED_OR_DELETED\"}");
        }
        return ResponseEntity.ok(b);
    }

    // API tiếp nhận cổng Webhook tự động với cơ chế kiểm thử Header Security
    @PostMapping("/webhook")
    public ResponseEntity<?> handleBankWebhook(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody WebhookRequest request) {

        // Xác thực xem chuỗi Token bảo mật gửi từ SePay có trùng khớp không
        if (authHeader == null || !authHeader.contains(SEPAY_API_KEY)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Cảnh báo: Yêu cầu truy cập trái phép!");
        }

        try {
            bookingService.processPaymentWebhook(request.getContent(), request.getTransferAmount());
            return ResponseEntity.ok("Xử lý webhook khớp lệnh thành công.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}