package com.ga.gymio.repository;

import com.ga.gymio.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findAllByCustomer_IdOrderByCreatedAtDesc(Long customerId);

    List<Payment> findAllByBooking_Id(Long bookingId);

    Optional<Payment> findByProviderPaymentId(String providerPaymentId);

    boolean existsByBooking_IdAndStatus(Long bookingId, Payment.Status status);

    boolean existsByCustomer_IdAndMembershipPlan_IdAndPaymentTypeAndStatus(Long customerId, Long membershipPlanId, Payment.PaymentType paymentType, Payment.Status status);
}