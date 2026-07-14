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

    @GetMapping
    public ResponseEntity<List<Booking>> getAdminSchedules(
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam("courtNumber") Integer courtNumber) {
        List<Booking> raw = bookingRepository.findByBookingDateAndCourtNumber(date, courtNumber);
        return ResponseEntity.ok(bookingService.checkAndExpireBookings(raw));
    }

    @GetMapping("/today")
    public ResponseEntity<List<Booking>> getTodaySchedules() {
        List<Booking> raw = bookingRepository.findByBookingDate(LocalDate.now());
        return ResponseEntity.ok(bookingService.checkAndExpireBookings(raw));
    }

    @PostMapping("/force-add")
    public ResponseEntity<?> adminForceAddPlayer(@RequestBody Booking request) {
        long activeCount = bookingRepository.countActiveSlots(request.getBookingDate(), request.getCourtNumber());
        if (activeCount >= 8) {
            return ResponseEntity.badRequest().body("Sân này đã lấp đầy tối đa 8 người!");
        }

        boolean isMale = "MALE".equalsIgnoreCase(request.getGender());
        BigDecimal price = isMale ? new BigDecimal("60000") : new BigDecimal("50000");

        Booking booking = new Booking();
        booking.setFullName(request.getFullName().trim());
        booking.setPhoneNumber(request.getPhoneNumber().trim());
        booking.setGender(request.getGender().toUpperCase());
        booking.setBookingDate(request.getBookingDate());
        booking.setCourtNumber(request.getCourtNumber());
        booking.setSessionTime("05:30 - 07:00");
        booking.setTotalPrice(price);
        booking.setDepositAmount(BigDecimal.ZERO);
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