package com.gom.badminton.service;

import com.gom.badminton.entity.Booking;
import com.gom.badminton.repository.BookingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class BookingService {

    @Autowired
    private BookingRepository bookingRepository;

    @Transactional
    public Booking createCustomerBooking(Booking request) {
        if (request.getBookingDate().getDayOfWeek() == DayOfWeek.MONDAY) {
            throw new IllegalArgumentException("Hệ thống đóng cửa vào Thứ 2.");
        }

        long activeCount = bookingRepository.countActiveSlots(request.getBookingDate(), request.getCourtNumber());
        if (activeCount >= 8) {
            throw new IllegalStateException("Sân số " + request.getCourtNumber() + " vào ngày này đã đầy!");
        }

        boolean isMale = "MALE".equalsIgnoreCase(request.getGender());
        BigDecimal total = isMale ? new BigDecimal("60000") : new BigDecimal("50000");

        // Cấu hình cọc test đồng bộ với giao diện
        BigDecimal deposit = isMale ? new BigDecimal("3000") : new BigDecimal("2000");

        Booking booking = new Booking();
        booking.setFullName(request.getFullName().trim());
        booking.setPhoneNumber(request.getPhoneNumber().trim());
        booking.setGender(request.getGender().toUpperCase());
        booking.setBookingDate(request.getBookingDate());
        booking.setCourtNumber(request.getCourtNumber());
        booking.setSessionTime("05:30 - 07:00");
        booking.setTotalPrice(total);
        booking.setDepositAmount(deposit);
        booking.setPaymentStatus("PENDING");
        booking.setIsAdminAdded(false);
        booking.setCreatedAt(LocalDateTime.now());

        String dayStr = getDayString(request.getBookingDate().getDayOfWeek());
        String phone = request.getPhoneNumber().trim();
        String last3Digits = phone.substring(phone.length() - Math.min(phone.length(), 3));

        String prefix = "CN".equals(dayStr) ? "" : "T";
        String code = prefix + dayStr + "S" + request.getCourtNumber() + last3Digits;

        while (bookingRepository.existsByBookingCode(code.toUpperCase())) {
            code = "T" + code;
        }

        booking.setBookingCode(code.toUpperCase());
        return bookingRepository.save(booking);
    }

    @Transactional
    public List<Booking> checkAndExpireBookings(List<Booking> list) {
        LocalDateTime now = LocalDateTime.now();
        List<Booking> expiredBookings = list.stream()
                .filter(b -> "PENDING".equals(b.getPaymentStatus()) && b.getCreatedAt().plusMinutes(5).isBefore(now))
                .collect(Collectors.toList());

        if (!expiredBookings.isEmpty()) {
            bookingRepository.deleteAll(expiredBookings);
            list.removeAll(expiredBookings);
        }
        return list;
    }

    @Transactional
    public void processPaymentWebhook(String content, BigDecimal amount) {
        if (content == null || !content.toUpperCase().contains("GOM")) {
            return;
        }

        // Bóc tách mã hoàn hảo không dính rác ngân hàng
        String afterGom = content.toUpperCase().replaceAll(".*GOM\\s*", "").trim();
        String bookingCode = afterGom.split("\\s+")[0];

        Optional<Booking> opt = bookingRepository.findByBookingCode(bookingCode);

        if (opt.isPresent()) {
            Booking b = opt.get();
            if ("PENDING".equals(b.getPaymentStatus())) {
                if (b.getCreatedAt().plusMinutes(5).isBefore(LocalDateTime.now())) {
                    bookingRepository.delete(b);
                    return;
                }
                if (amount.compareTo(b.getDepositAmount()) >= 0) {
                    b.setPaymentStatus("PAID");
                    bookingRepository.save(b);
                }
            }
        }
    }

    private String getDayString(DayOfWeek dayOfWeek) {
        switch (dayOfWeek) {
            case TUESDAY: return "3";
            case WEDNESDAY: return "4";
            case THURSDAY: return "5";
            case FRIDAY: return "6";
            case SATURDAY: return "7";
            case SUNDAY: return "CN";
            default: return "2";
        }
    }
}