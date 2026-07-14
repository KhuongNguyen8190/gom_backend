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

    List<Booking> findByBookingDate(LocalDate bookingDate);

    // Lệnh truy vấn TÌM ĐƠN PENDING CŨ để xóa
    List<Booking> findByPhoneNumberAndBookingDateAndCourtNumberAndPaymentStatus(
            String phoneNumber, LocalDate bookingDate, Integer courtNumber, String paymentStatus);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.bookingDate = :date AND b.courtNumber = :courtNumber " +
            "AND b.paymentStatus IN ('PAID', 'ADMIN_ADDED')")
    long countActiveSlots(@Param("date") LocalDate date, @Param("courtNumber") Integer courtNumber);

    long countByIsAdminAddedTrue();

    Optional<Booking> findFirstByPhoneNumberAndPaymentStatusOrderByCreatedAtDesc(String phoneNumber, String paymentStatus);

    Optional<Booking> findByBookingCode(String bookingCode);

    boolean existsByBookingCode(String bookingCode);
}