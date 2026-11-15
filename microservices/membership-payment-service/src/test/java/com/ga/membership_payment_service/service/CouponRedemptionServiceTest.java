package com.ga.membership_payment_service.service;

import com.ga.membership_payment_service.model.Coupon;
import com.ga.membership_payment_service.model.CouponRedemption;
import com.ga.membership_payment_service.model.Payment;
import com.ga.membership_payment_service.repository.CouponRedemptionRepository;
import com.ga.membership_payment_service.repository.CouponRepository;
import com.ga.membership_payment_service.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CouponRedemptionServiceTest {

    @Mock
    private CouponRedemptionRepository couponRedemptionRepository;

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private CouponRedemptionService couponRedemptionService;

    private Coupon coupon;
    private Payment payment;

    /**
     * Initializes the coupon and payment used by the tests.
     */
    @BeforeEach
    void setUp() {
        coupon = new Coupon();
        coupon.setId(10L);
        coupon.setCode("TEST10");
        coupon.setActive(true);
        coupon.setUsageLimit(5);
        coupon.setUsedCount(0);
        coupon.setStartDate(LocalDateTime.now().minusDays(1));
        coupon.setExpiresAt(LocalDateTime.now().plusDays(1));
        coupon.setDiscountType(Coupon.DiscountType.PERCENTAGE);

        payment = new Payment();
        payment.setId(100L);
        payment.setCustomerId(1L);
        payment.setCoupon(coupon);
        payment.setStatus(Payment.Status.COMPLETED);
    }

    /**
     * Configures the repositories for a valid payment and coupon.
     */
    private void mockPaymentAndCoupon() {
        when(paymentRepository.findById(100L))
                .thenReturn(Optional.of(payment));

        when(couponRepository.findByIdAndActiveTrue(10L))
                .thenReturn(Optional.of(coupon));
    }

    @Test
    void redeemCoupon_shouldSaveRedemptionAndIncrementUsageCount() {
        mockPaymentAndCoupon();

        when(couponRedemptionRepository.save(any(CouponRedemption.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CouponRedemption result =
                couponRedemptionService.redeemCouponAfterSuccessfulPayment(100L);

        assertNotNull(result);
        assertSame(coupon, result.getCoupon());
        assertEquals(1L, result.getCustomerId());
        assertSame(payment, result.getPayment());
        assertEquals(1, coupon.getUsedCount());

        verify(couponRepository).save(coupon);
        verify(couponRedemptionRepository)
                .save(any(CouponRedemption.class));
    }

    @Test
    void redeemCoupon_shouldReturnExistingRedemption() {
        CouponRedemption existingRedemption = new CouponRedemption();

        when(paymentRepository.findById(100L))
                .thenReturn(Optional.of(payment));

        when(couponRedemptionRepository.findByPayment_Id(100L))
                .thenReturn(Optional.of(existingRedemption));

        CouponRedemption result =
                couponRedemptionService.redeemCouponAfterSuccessfulPayment(100L);

        assertSame(existingRedemption, result);

        verify(couponRepository, never()).save(any(Coupon.class));
        verify(couponRedemptionRepository, never())
                .save(any(CouponRedemption.class));
    }

    @Test
    void redeemCoupon_shouldRejectPendingPayment() {
        payment.setStatus(Payment.Status.PENDING);

        when(paymentRepository.findById(100L))
                .thenReturn(Optional.of(payment));

        assertThrows(IllegalStateException.class,
                () -> couponRedemptionService
                        .redeemCouponAfterSuccessfulPayment(100L));

        verifyNoInteractions(couponRepository, couponRedemptionRepository);
    }

    @Test
    void redeemCoupon_shouldRejectFailedPayment() {
        payment.setStatus(Payment.Status.FAILED);

        when(paymentRepository.findById(100L))
                .thenReturn(Optional.of(payment));

        assertThrows(IllegalStateException.class,
                () -> couponRedemptionService
                        .redeemCouponAfterSuccessfulPayment(100L));

        verifyNoInteractions(couponRepository, couponRedemptionRepository);
    }

    @Test
    void redeemCoupon_shouldRejectMissingPayment() {
        when(paymentRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> couponRedemptionService
                        .redeemCouponAfterSuccessfulPayment(999L));

        verifyNoInteractions(couponRepository, couponRedemptionRepository);
    }

    @Test
    void redeemCoupon_shouldRejectPaymentWithoutCustomer() {
        payment.setCustomerId(null);

        when(paymentRepository.findById(100L))
                .thenReturn(Optional.of(payment));

        assertThrows(IllegalStateException.class,
                () -> couponRedemptionService
                        .redeemCouponAfterSuccessfulPayment(100L));

        verify(couponRedemptionRepository).findByPayment_Id(100L);
        verify(couponRepository, never()).save(any(Coupon.class));
        verify(couponRedemptionRepository, never())
                .save(any(CouponRedemption.class));
    }

    @Test
    void redeemCoupon_shouldRejectPaymentWithoutCoupon() {
        payment.setCoupon(null);

        when(paymentRepository.findById(100L))
                .thenReturn(Optional.of(payment));

        assertThrows(IllegalStateException.class,
                () -> couponRedemptionService
                        .redeemCouponAfterSuccessfulPayment(100L));

        verify(couponRepository, never()).save(any(Coupon.class));
        verify(couponRedemptionRepository, never())
                .save(any(CouponRedemption.class));
    }

    @Test
    void redeemCoupon_shouldRejectAlreadyRedeemedCoupon() {
        when(paymentRepository.findById(100L))
                .thenReturn(Optional.of(payment));

        when(couponRepository.findByIdAndActiveTrue(10L))
                .thenReturn(Optional.of(coupon));

        when(couponRedemptionRepository
                .existsByCoupon_IdAndCustomerId(10L, 1L))
                .thenReturn(true);

        assertThrows(IllegalStateException.class,
                () -> couponRedemptionService
                        .redeemCouponAfterSuccessfulPayment(100L));

        assertEquals(0, coupon.getUsedCount());

        verify(couponRepository, never()).save(any(Coupon.class));
        verify(couponRedemptionRepository, never())
                .save(any(CouponRedemption.class));
    }

    @Test
    void redeemCoupon_shouldRejectCouponAtUsageLimit() {
        coupon.setUsedCount(5);

        mockPaymentAndCoupon();

        assertThrows(IllegalStateException.class,
                () -> couponRedemptionService
                        .redeemCouponAfterSuccessfulPayment(100L));

        assertEquals(5, coupon.getUsedCount());

        verify(couponRepository, never()).save(any(Coupon.class));
        verify(couponRedemptionRepository, never())
                .save(any(CouponRedemption.class));
    }

    @Test
    void redeemCoupon_shouldRejectInactiveCoupon() {
        coupon.setActive(false);

        when(paymentRepository.findById(100L))
                .thenReturn(Optional.of(payment));

        when(couponRepository.findByIdAndActiveTrue(10L))
                .thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> couponRedemptionService
                        .redeemCouponAfterSuccessfulPayment(100L));

        assertEquals(0, coupon.getUsedCount());

        verify(couponRepository, never()).save(any(Coupon.class));
        verify(couponRedemptionRepository, never())
                .save(any(CouponRedemption.class));
    }

    @Test
    void redeemCoupon_shouldRejectCouponNotYetValid() {
        coupon.setStartDate(LocalDateTime.now().plusDays(1));

        mockPaymentAndCoupon();

        assertThrows(IllegalStateException.class,
                () -> couponRedemptionService
                        .redeemCouponAfterSuccessfulPayment(100L));

        assertEquals(0, coupon.getUsedCount());

        verify(couponRepository, never()).save(any(Coupon.class));
        verify(couponRedemptionRepository, never())
                .save(any(CouponRedemption.class));
    }

    @Test
    void redeemCoupon_shouldRejectExpiredCoupon() {
        coupon.setExpiresAt(LocalDateTime.now().minusDays(1));

        mockPaymentAndCoupon();

        assertThrows(IllegalStateException.class,
                () -> couponRedemptionService
                        .redeemCouponAfterSuccessfulPayment(100L));

        assertEquals(0, coupon.getUsedCount());

        verify(couponRepository, never()).save(any(Coupon.class));
        verify(couponRedemptionRepository, never())
                .save(any(CouponRedemption.class));
    }

    @Test
    void redeemCoupon_shouldRejectMissingCoupon() {
        when(paymentRepository.findById(100L))
                .thenReturn(Optional.of(payment));

        when(couponRepository.findByIdAndActiveTrue(10L))
                .thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> couponRedemptionService
                        .redeemCouponAfterSuccessfulPayment(100L));

        verify(couponRepository, never()).save(any(Coupon.class));
        verify(couponRedemptionRepository, never())
                .save(any(CouponRedemption.class));
    }
}
