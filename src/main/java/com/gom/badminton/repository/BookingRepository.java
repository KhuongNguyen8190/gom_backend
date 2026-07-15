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

    // Lọc danh sách người chơi cố định theo ngày (Không chia sân)
    List<Booking> findByBookingDate(LocalDate bookingDate);

    // Tìm các đơn PENDING trùng ngày để dọn dẹp
    List<Booking> findByPhoneNumberAndBookingDateAndPaymentStatus(
            String phoneNumber, LocalDate bookingDate, String paymentStatus);

    // Đếm tổng số slot đã khóa chỗ thành công trong ngày (Tối đa 16 người)
    @Query("SELECT COUNT(b) FROM Booking b WHERE b.bookingDate = :date " +
            "AND b.paymentStatus IN ('PAID', 'ADMIN_ADDED')")
    long countActiveSlots(@Param("date") LocalDate date);

    long countByIsAdminAddedTrue();

    Optional<Booking> findByBookingCode(String bookingCode);

    boolean existsByBookingCode(String bookingCode);
}