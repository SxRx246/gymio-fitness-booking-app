package com.ga.gymio.service;

import com.ga.gymio.authentication.MyUserDetails;
import com.ga.gymio.dto.response.PaymentResponse;
import com.ga.gymio.model.MembershipPlan;
import com.ga.gymio.model.Payment;
import com.ga.gymio.model.User;
import com.ga.gymio.repository.MembershipPlanRepository;
import com.ga.gymio.repository.PaymentRepository;
import com.ga.gymio.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final MembershipPlanRepository membershipPlanRepository;
    private final UserRepository userRepository;

    @Transactional
    public PaymentResponse createMembershipPayment(
            Long membershipPlanId,
            Payment.PaymentMethod paymentMethod) {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !(authentication.getPrincipal()
                instanceof MyUserDetails userDetails)) {
            throw new IllegalStateException(
                    "An authenticated customer is required.");
        }

        User customer = userDetails.getUser();

        if (customer.getRole() != User.Role.CUSTOMER) {
            throw new IllegalStateException(
                    "Only customers can purchase memberships.");
        }

        MembershipPlan plan = membershipPlanRepository
                .findById(membershipPlanId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Membership plan not found."));

        if (!plan.isActive()) {
            throw new IllegalStateException(
                    "This membership plan is not available.");
        }

        Payment payment = new Payment();
        payment.setCustomer(customer);
        payment.setMembershipPlan(plan);
        payment.setPaymentType(Payment.PaymentType.MEMBERSHIP);
        payment.setPaymentMethod(paymentMethod);
        payment.setAmount(plan.getPrice());
        payment.setCurrency("BHD");
        payment.setStatus(Payment.Status.PENDING);

        payment.setProviderName("GYMIO_DEMO");
        payment.setProviderPaymentId(
                "DEMO-" + UUID.randomUUID());

        Payment savedPayment = paymentRepository.save(payment);

        return new PaymentResponse(
                savedPayment.getId(),
                savedPayment.getProviderPaymentId(),
                savedPayment.getAmount(),
                savedPayment.getCurrency(),
                savedPayment.getStatus(),
                savedPayment.getPaymentMethod(),
                savedPayment.getCreatedAt()
        );
    }


    @Transactional(readOnly = true)
    public List<PaymentResponse> getMyPayments() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !(authentication.getPrincipal()
                instanceof MyUserDetails userDetails)) {
            throw new IllegalStateException(
                    "An authenticated customer is required.");
        }

        User customer = userDetails.getUser();

        if (customer.getRole() != User.Role.CUSTOMER) {
            throw new IllegalStateException(
                    "Only customers can view their payment history.");
        }

        return paymentRepository
                .findAllByCustomer_IdOrderByCreatedAtDesc(customer.getId())
                .stream()
                .map(payment -> new PaymentResponse(
                        payment.getId(),
                        payment.getProviderPaymentId(),
                        payment.getAmount(),
                        payment.getCurrency(),
                        payment.getStatus(),
                        payment.getPaymentMethod(),
                        payment.getCreatedAt()
                ))
                .toList();
    }

    @Transactional
    public PaymentResponse confirmDemoPayment(Long paymentId) {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !(authentication.getPrincipal()
                instanceof MyUserDetails userDetails)) {
            throw new IllegalStateException(
                    "An authenticated customer is required.");
        }

        User customer = userDetails.getUser();

        if (customer.getRole() != User.Role.CUSTOMER) {
            throw new IllegalStateException(
                    "Only customers can confirm demo payments.");
        }

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Payment not found."));

        if (!payment.getCustomer().getId().equals(customer.getId())) {
            throw new IllegalStateException(
                    "You cannot confirm another customer's payment.");
        }

        if (!"GYMIO_DEMO".equals(payment.getProviderName())) {
            throw new IllegalStateException(
                    "This is not a demo payment.");
        }

        if (payment.getStatus() != Payment.Status.PENDING) {
            throw new IllegalStateException(
                    "Only pending payments can be confirmed.");
        }

        payment.setStatus(Payment.Status.COMPLETED);

        if (payment.getPaymentType() == Payment.PaymentType.MEMBERSHIP) {
            MembershipPlan plan = payment.getMembershipPlan();

            customer.setMembershipPlan(plan);
            customer.setMembershipStatus(User.MembershipStatus.ACTIVE);

            LocalDate startDate = LocalDate.now();

            customer.setMembershipStartDate(startDate);
            customer.setMembershipEndDate(
                    startDate.plusDays(plan.getDurationByDays())
            );

            userRepository.save(customer);
        }

        Payment savedPayment = paymentRepository.save(payment);

        return new PaymentResponse(
                savedPayment.getId(),
                savedPayment.getProviderPaymentId(),
                savedPayment.getAmount(),
                savedPayment.getCurrency(),
                savedPayment.getStatus(),
                savedPayment.getPaymentMethod(),
                savedPayment.getCreatedAt()
        );
    }

}
