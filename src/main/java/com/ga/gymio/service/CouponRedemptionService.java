package com.ga.gymio.service;

import com.ga.gymio.model.Coupon;
import com.ga.gymio.model.CouponRedemption;
import com.ga.gymio.model.Payment;
import com.ga.gymio.repository.CouponRedemptionRepository;
import com.ga.gymio.repository.CouponRepository;
import com.ga.gymio.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Manages coupon redemptions and retrieves redemption history for customers.
 * Ensures redemptions are recorded only after successful payment and that
 * coupon usage limits and per-customer redemption restrictions are enforced.
 */
@Service
@RequiredArgsConstructor
public class CouponRedemptionService {

    private final CouponRedemptionRepository couponRedemptionRepository;
    private final CouponRepository couponRepository;
    private final PaymentRepository paymentRepository;

    /**
     * Records a coupon redemption after verifying that the associated payment
     * has completed successfully.
     * Locks the coupon record while checking and updating its usage count to
     * help prevent concurrent redemptions from exceeding the usage limit.
     *
     * @param paymentId the ID of the payment associated with the redemption
     * @return the saved coupon redemption
     * @throws IllegalArgumentException if the payment or coupon cannot be found
     * @throws IllegalStateException if the payment is unsuccessful, has no
     *                                associated customer or coupon, the customer
     *                                has already redeemed the coupon, or the
     *                                coupon has reached its usage limit
     */
    @Transactional
    public CouponRedemption redeemCouponAfterSuccessfulPayment(
            Long paymentId) {

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Payment not found."));

        if (payment.getStatus() != Payment.Status.COMPLETED) {
            throw new IllegalStateException(
                    "Coupon redemption requires a successful payment.");
        }

        if (payment.getCustomer() == null) {
            throw new IllegalStateException(
                    "The payment has no associated customer.");
        }

        Coupon paidCoupon = payment.getCoupon();

        if (paidCoupon == null) {
            throw new IllegalStateException(
                    "No coupon was applied to this payment.");
        }

        Long couponId = paidCoupon.getId();
        Long customerId = payment.getCustomer().getId();

        Coupon coupon = couponRepository.findByIdAndActiveTrue(couponId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Coupon not found or is inactive."
                        )
                );

        if (couponRedemptionRepository
                .existsByCoupon_IdAndCustomer_Id(couponId, customerId)) {
            throw new IllegalStateException(
                    "This customer has already redeemed this coupon.");
        }

        if (coupon.getUsedCount() >= coupon.getUsageLimit()) {
            throw new IllegalStateException(
                    "The coupon has reached its usage limit.");
        }

        LocalDateTime now = LocalDateTime.now();

        if (!coupon.isActive()) {
            throw new IllegalStateException("This coupon is inactive.");
        }

        if (coupon.getStartDate() == null
                || now.isBefore(coupon.getStartDate())) {
            throw new IllegalStateException("This coupon is not available yet.");
        }

        if (coupon.getExpiresAt() == null
                || !now.isBefore(coupon.getExpiresAt())) {
            throw new IllegalStateException("This coupon has expired.");
        }

        CouponRedemption redemption = new CouponRedemption();
        redemption.setCoupon(coupon);
        redemption.setCustomer(payment.getCustomer());

        coupon.setUsedCount(coupon.getUsedCount() + 1);
        couponRepository.save(coupon);

        return couponRedemptionRepository.save(redemption);
    }

    /**
     * Retrieves a customer's coupon redemption history, ordered from newest
     * to oldest.
     *
     * @param customerId the ID of the customer whose redemptions are requested
     * @return a list of the customer's coupon redemptions
     */
    @Transactional(readOnly = true)
    public List<CouponRedemption> getCustomerRedemptions(
            Long customerId) {

        return couponRedemptionRepository
                .findAllByCustomer_IdOrderByRedeemedAtDesc(customerId);
    }
}