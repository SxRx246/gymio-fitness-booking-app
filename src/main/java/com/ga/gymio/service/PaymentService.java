package com.ga.gymio.service;

import com.ga.gymio.authentication.MyUserDetails;
import com.ga.gymio.dto.response.PaymentResponse;
import com.ga.gymio.model.Coupon;
import com.ga.gymio.model.MembershipPlan;
import com.ga.gymio.model.Payment;
import com.ga.gymio.model.User;
import com.ga.gymio.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final MembershipPlanRepository membershipPlanRepository;
    private final UserRepository userRepository;
    private final CouponRepository couponRepository;
    private final CouponRedemptionRepository couponRedemptionRepository;

    private final CouponRedemptionService couponRedemptionService;

    @Transactional
    public PaymentResponse createMembershipPayment(
            Long membershipPlanId,
            Payment.PaymentMethod paymentMethod,
            Long couponId) {

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

        BigDecimal amount = plan.getPrice();
        Coupon coupon = null;

        if (couponId != null) {
            coupon = couponRepository.findByIdAndActiveTrue(couponId)
                    .orElseThrow(() ->
                            new IllegalArgumentException(
                                    "Coupon not found or is inactive."));

            LocalDateTime now = LocalDateTime.now();

            if (coupon.getStartDate() == null
                    || now.isBefore(coupon.getStartDate())) {
                throw new IllegalStateException(
                        "This coupon is not available yet.");
            }

            if (coupon.getExpiresAt() == null
                    || !now.isBefore(coupon.getExpiresAt())) {
                throw new IllegalStateException("This coupon has expired.");
            }

            if (coupon.getUsedCount() >= coupon.getUsageLimit()) {
                throw new IllegalStateException(
                        "This coupon has reached its usage limit.");
            }

            if (couponRedemptionRepository
                    .existsByCoupon_IdAndCustomer_Id(
                            couponId, customer.getId())) {
                throw new IllegalStateException(
                        "You have already redeemed this coupon.");
            }

            BigDecimal discount;

            if (coupon.getDiscountType() == Coupon.DiscountType.PERCENTAGE) {
                discount = amount
                        .multiply(coupon.getDiscountValue())
                        .divide(BigDecimal.valueOf(100), 3, RoundingMode.HALF_UP);
            } else {
                discount = coupon.getDiscountValue().min(amount);
            }

            amount = amount.subtract(discount)
                    .max(BigDecimal.ZERO)
                    .setScale(3, RoundingMode.HALF_UP);
        }

        Payment payment = new Payment();
        payment.setCustomer(customer);
        payment.setMembershipPlan(plan);
        payment.setPaymentType(Payment.PaymentType.MEMBERSHIP);
        payment.setPaymentMethod(paymentMethod);
        payment.setAmount(amount);
        payment.setCurrency("BHD");
        payment.setStatus(Payment.Status.PENDING);

        payment.setProviderName("GYMIO_DEMO");
        payment.setProviderPaymentId(
                "DEMO-" + UUID.randomUUID());
        payment.setCoupon(coupon);

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

        Payment payment = paymentRepository.findByIdAndStatus(
                paymentId,
                Payment.Status.PENDING
        ).orElseThrow(() ->
                new IllegalArgumentException(
                        "Payment not found or is not pending."
                )
        );

        if (!payment.getCustomer().getId().equals(customer.getId())) {
            throw new IllegalStateException(
                    "You cannot confirm another customer's payment.");
        }

        if (!"GYMIO_DEMO".equals(payment.getProviderName())) {
            throw new IllegalStateException(
                    "This is not a demo payment.");
        }

        payment.setStatus(Payment.Status.COMPLETED);

        if (payment.getCoupon() != null) {
            couponRedemptionService
                    .redeemCouponAfterSuccessfulPayment(payment.getId());
        }

        if (payment.getPaymentType() == Payment.PaymentType.MEMBERSHIP) {
            MembershipPlan plan = payment.getMembershipPlan();

            if (plan == null) {
                throw new IllegalStateException(
                        "The payment has no associated membership plan.");
            }

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
