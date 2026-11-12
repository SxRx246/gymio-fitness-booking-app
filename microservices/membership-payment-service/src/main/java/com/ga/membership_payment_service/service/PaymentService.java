package com.ga.membership_payment_service.service;

import com.ga.membership_payment_service.dto.response.PaymentResponse;
import com.ga.membership_payment_service.exception.ForbiddenException;
import com.ga.membership_payment_service.model.Coupon;
import com.ga.membership_payment_service.model.MembershipPlan;
import com.ga.membership_payment_service.model.Payment;
import com.ga.membership_payment_service.model.UserMembership;
import com.ga.membership_payment_service.repository.CouponRedemptionRepository;
import com.ga.membership_payment_service.repository.CouponRepository;
import com.ga.membership_payment_service.repository.MembershipPlanRepository;
import com.ga.membership_payment_service.repository.PaymentRepository;
import com.ga.membership_payment_service.repository.UserMembershipRepository;
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

/**
 * Manages membership payments, payment confirmation, and payment history.
 */
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final MembershipPlanRepository membershipPlanRepository;
    private final CouponRepository couponRepository;
    private final CouponRedemptionRepository couponRedemptionRepository;
    private final CouponRedemptionService couponRedemptionService;
    private final UserMembershipRepository userMembershipRepository;

    /**
     * Retrieves the authenticated customer's ID after verifying their role.
     *
     * @return the authenticated customer's ID
     * @throws ForbiddenException if the user is not an authenticated customer
     */
    private Long getCurrentCustomerId() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof Long customerId)) {
            throw new ForbiddenException(
                    "An authenticated customer is required.");
        }

        boolean isCustomer = authentication.getAuthorities().stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals("ROLE_CUSTOMER"));

        if (!isCustomer) {
            throw new ForbiddenException(
                    "Only customers can perform this operation.");
        }

        return customerId;
    }

    /**
     * Creates a membership payment after validating the membership plan
     * and any applied coupon.
     *
     * @param membershipPlanId the ID of the membership plan
     * @param paymentMethod the selected payment method
     * @param couponId the optional coupon ID
     * @return the created payment response
     */
    @Transactional
    public PaymentResponse createMembershipPayment(
            Long membershipPlanId,
            Payment.PaymentMethod paymentMethod,
            Long couponId) {

        Long customerId = getCurrentCustomerId();

        MembershipPlan plan = membershipPlanRepository
                .findById(membershipPlanId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
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
                throw new IllegalStateException(
                        "This coupon has expired.");
            }

            if (coupon.getUsedCount() >= coupon.getUsageLimit()) {
                throw new IllegalStateException(
                        "This coupon has reached its usage limit.");
            }

            if (couponRedemptionRepository
                    .existsByCoupon_IdAndCustomerId(
                            couponId, customerId)) {
                throw new IllegalStateException(
                        "You have already redeemed this coupon.");
            }

            BigDecimal discount;

            if (coupon.getDiscountType()
                    == Coupon.DiscountType.PERCENTAGE) {

                discount = amount
                        .multiply(coupon.getDiscountValue())
                        .divide(
                                BigDecimal.valueOf(100),
                                3,
                                RoundingMode.HALF_UP
                        );

            } else {
                discount = coupon.getDiscountValue().min(amount);
            }

            amount = amount.subtract(discount)
                    .max(BigDecimal.ZERO)
                    .setScale(3, RoundingMode.HALF_UP);
        }

        Payment payment = new Payment();

        payment.setCustomerId(customerId);
        payment.setMembershipPlan(plan);
        payment.setPaymentType(Payment.PaymentType.MEMBERSHIP);
        payment.setPaymentMethod(paymentMethod);
        payment.setAmount(amount);
        payment.setCurrency("BHD");
        payment.setStatus(Payment.Status.PENDING);
        payment.setProviderName("GYMIO_DEMO");
        payment.setProviderPaymentId("DEMO-" + UUID.randomUUID());
        payment.setCoupon(coupon);

        Payment savedPayment = paymentRepository.save(payment);

        return mapToResponse(savedPayment);
    }

    /**
     * Retrieves the authenticated customer's payment history.
     *
     * @return the customer's payments ordered from newest to oldest
     */
    @Transactional(readOnly = true)
    public List<PaymentResponse> getMyPayments() {

        Long customerId = getCurrentCustomerId();

        return paymentRepository
                .findAllByCustomerIdOrderByCreatedAtDesc(customerId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    /**
     * Confirms a pending demo payment belonging to the authenticated customer.
     * If the payment is for a membership, a new user membership is created.
     *
     * @param paymentId the ID of the payment to confirm
     * @return the confirmed payment response
     */
    @Transactional
    public PaymentResponse confirmDemoPayment(Long paymentId) {

        Long customerId = getCurrentCustomerId();

        Payment payment = paymentRepository.findByIdAndStatus(
                        paymentId,
                        Payment.Status.PENDING)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Payment not found or is not pending."));

        if (!payment.getCustomerId().equals(customerId)) {
            throw new ForbiddenException(
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

        if (payment.getPaymentType()
                == Payment.PaymentType.MEMBERSHIP) {

            MembershipPlan plan = payment.getMembershipPlan();

            if (plan == null) {
                throw new IllegalStateException(
                        "The payment has no associated membership plan.");
            }

            LocalDate startDate = LocalDate.now();

            UserMembership userMembership = new UserMembership();
            userMembership.setUserId(customerId);
            userMembership.setMembershipPlan(plan);
            userMembership.setMembershipStatus(UserMembership.MembershipStatus.ACTIVE);
            userMembership.setMembershipStartDate(startDate);
            userMembership.setMembershipEndDate(
                    startDate.plusDays(plan.getDurationByDays()));

            userMembershipRepository.save(userMembership);
        }

        Payment savedPayment = paymentRepository.save(payment);

        return mapToResponse(savedPayment);
    }

    /**
     * Converts a payment entity into its response DTO.
     *
     * @param payment the payment entity
     * @return the payment response
     */
    private PaymentResponse mapToResponse(Payment payment) {

        return new PaymentResponse(
                payment.getId(),
                payment.getProviderPaymentId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getStatus(),
                payment.getPaymentMethod(),
                payment.getCreatedAt()
        );
    }
}
