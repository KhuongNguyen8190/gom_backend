package com.gom.badminton.repository;

import com.gom.badminton.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByPhoneNumberOrderByBookingDateDesc(String phoneNumber);

    List<Booking> findByBookingDateAndCourtNumber(LocalDate bookingDate, Integer courtNumber);

    // API Lấy toàn bộ lịch ra sân của cả 2 sân trong một ngày cụ thể
    List<Booking> findByBookingDate(LocalDate bookingDate);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.bookingDate = :date AND b.courtNumber = :courtNumber " +
            "AND b.paymentStatus IN ('PAID', 'ADMIN_ADDED')")
    long countActiveSlots(@Param("date") LocalDate date, @Param("courtNumber") Integer courtNumber);

    // Hàm đếm tổng số lượng người quen Admin đã thêm để tạo số thứ tự ADMIN_1, ADMIN_2...
    long countByIsAdminAddedTrue();

    Optional<Booking> findFirstByPhoneNumberAndPaymentStatusOrderByCreatedAtDesc(String phoneNumber, String paymentStatus);

    Optional<Booking> findByBookingCode(String bookingCode);

    // Hàm kiểm tra xem mã đặt sân đã tồn tại trong DB chưa
    boolean existsByBookingCode(String bookingCode);
}