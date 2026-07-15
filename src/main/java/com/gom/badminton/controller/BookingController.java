package com.gom.badminton.controller;

import com.gom.badminton.entity.Booking;
import com.gom.badminton.dto.WebhookRequest;
import com.gom.badminton.repository.BookingRepository;
import com.gom.badminton.service.BookingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
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

    @PostMapping("/admin-add")
    public ResponseEntity<?> adminAddPlayer(@RequestBody Booking request) {
        try {
            Booking result = bookingService.adminForceAddPlayer(request);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/admin/schedules")
    public ResponseEntity<?> getAdminSchedules(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<Booking> data = bookingRepository.findByBookingDate(date);
        return ResponseEntity.ok(data);
    }

    @GetMapping("/admin/today")
    public ResponseEntity<?> getTodaySchedules() {
        List<Booking> data = bookingService.getTodaySchedules();
        return ResponseEntity.ok(data);
    }

    @DeleteMapping("/admin/cancel/{id}")
    public ResponseEntity<?> cancelBookingByAdmin(@PathVariable Long id) {
        try {
            bookingService.cancelBookingByAdmin(id);
            return ResponseEntity.ok("{\"message\":\"Trục xuất thành viên thành công.\"}");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    // ==========================================
    // LOGIC TRA CỨU MỚI: Ẩn lịch sử ngày hôm qua
    // ==========================================
    @GetMapping("/lookup")
    public ResponseEntity<?> lookupByPhone(@RequestParam String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Vui lòng nhập số điện thoại.");
        }

        // Lấy ngày giờ hiện tại của hệ thống (Ví dụ: 16/07/2026)
        LocalDate today = LocalDate.now();

        // Chỉ lấy những đơn hàng có Date >= Today
        List<Booking> rawList = bookingRepository.findByPhoneNumberAndBookingDateGreaterThanEqualOrderByBookingDateDesc(phone.trim(), today);

        List<Booking> verifiedList = bookingService.checkAndExpireBookings(rawList);

        if (verifiedList.isEmpty()) {
            return ResponseEntity.status(404).body("Không có lịch chơi nào trong hôm nay hoặc sắp tới.");
        }

        List<Booking> masked = verifiedList.stream().map(b -> {
            Booking m = new Booking();
            m.setId(b.getId());
            m.setBookingCode(b.getBookingCode());
            m.setFullName(b.getFullName().substring(0, Math.min(b.getFullName().length(), 2)) + "***");
            m.setPhoneNumber(b.getPhoneNumber().substring(0, 4) + "***" + b.getPhoneNumber().substring(b.getPhoneNumber().length() - 3));
            m.setGender(b.getGender());
            m.setBookingDate(b.getBookingDate());
            m.setSessionTime(b.getSessionTime());
            m.setTotalPrice(b.getTotalPrice());
            m.setDepositAmount(b.getDepositAmount());
            m.setPaymentStatus(b.getPaymentStatus());
            m.setCreatedAt(b.getCreatedAt());
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

    @PostMapping("/webhook")
    public ResponseEntity<?> handleBankWebhook(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody WebhookRequest request) {

        if (authHeader == null || !authHeader.contains(SEPAY_API_KEY)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Cảnh báo bảo mật!");
        }

        try {
            bookingService.processPaymentWebhook(request.getContent(), request.getTransferAmount());
            return ResponseEntity.ok("Xử lý Webhook thành công.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}