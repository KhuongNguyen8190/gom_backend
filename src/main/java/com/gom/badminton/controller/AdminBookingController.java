package com.gom.badminton.controller;

import com.gom.badminton.entity.Booking;
import com.gom.badminton.repository.BookingRepository;
import com.gom.badminton.service.BookingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/admin/bookings")
@CrossOrigin(origins = "*")
public class AdminBookingController {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private BookingService bookingService;

    // ĐÃ SỬA: Xóa bỏ hoàn toàn courtNumber, chỉ tìm theo Ngày
    @GetMapping
    public ResponseEntity<List<Booking>> getAdminSchedules(
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<Booking> raw = bookingRepository.findByBookingDate(date);
        return ResponseEntity.ok(bookingService.checkAndExpireBookings(raw));
    }

    @GetMapping("/today")
    public ResponseEntity<List<Booking>> getTodaySchedules() {
        List<Booking> raw = bookingRepository.findByBookingDate(LocalDate.now());
        return ResponseEntity.ok(bookingService.checkAndExpireBookings(raw));
    }

    // ĐÃ SỬA: Bỏ courtNumber, nâng sĩ số lên 16 người
    @PostMapping("/force-add")
    public ResponseEntity<?> adminForceAddPlayer(@RequestBody Booking request) {
        long activeCount = bookingRepository.countActiveSlots(request.getBookingDate());
        if (activeCount >= 16) {
            return ResponseEntity.badRequest().body("Lịch chơi vào ngày này đã đầy (Tối đa 16 người)!");
        }

        boolean isMale = "MALE".equalsIgnoreCase(request.getGender());
        BigDecimal price = isMale ? new BigDecimal("60000") : new BigDecimal("50000");

        Booking booking = new Booking();
        booking.setFullName(request.getFullName().trim());
        booking.setPhoneNumber(request.getPhoneNumber().trim());
        booking.setGender(request.getGender().toUpperCase());
        booking.setBookingDate(request.getBookingDate());
        // Bỏ dòng booking.setCourtNumber(...)
        booking.setSessionTime("05:30 - 07:00");
        booking.setTotalPrice(price);
        booking.setDepositAmount(BigDecimal.ZERO); // Miễn cọc
        booking.setPaymentStatus("ADMIN_ADDED");
        booking.setIsAdminAdded(true);
        booking.setCreatedAt(LocalDateTime.now());

        long adminCount = bookingRepository.countByIsAdminAddedTrue() + 1;
        String code = "ADMIN_" + adminCount;
        while(bookingRepository.existsByBookingCode(code)) {
            adminCount++;
            code = "ADMIN_" + adminCount;
        }
        booking.setBookingCode(code);

        return ResponseEntity.ok(bookingRepository.save(booking));
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<?> cancelBooking(@PathVariable Long id) {
        return bookingRepository.findById(id).map(b -> {
            b.setPaymentStatus("CANCELLED");
            bookingRepository.save(b);
            return ResponseEntity.ok("Success");
        }).orElse(ResponseEntity.status(404).build());
    }
}