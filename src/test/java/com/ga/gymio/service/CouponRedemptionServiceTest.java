package com.ga.gymio.service;

import com.ga.gymio.model.Coupon;
import com.ga.gymio.model.CouponRedemption;
import com.ga.gymio.model.Payment;
import com.ga.gymio.model.User;
import com.ga.gymio.repository.CouponRedemptionRepository;
import com.ga.gymio.repository.CouponRepository;
import com.ga.gymio.repository.PaymentRepository;
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

    private User customer;
    private Coupon coupon;
    private Payment payment;

    @BeforeEach
    void setUp() {
        customer = new User();
        customer.setId(1L);
        customer.setRole(User.Role.CUSTOMER);

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
        payment.setCustomer(customer);
        payment.setCoupon(coupon);
        payment.setStatus(Payment.Status.COMPLETED);
    }

    private void mockPaymentAndCoupon() {
        when(paymentRepository.findById(100L))
                .thenReturn(Optional.of(payment));

        when(couponRepository.findById(10L))
                .thenReturn(Optional.of(coupon));

        when(couponRedemptionRepository
                .existsByCoupon_IdAndCustomer_Id(10L, 1L))
                .thenReturn(false);
    }

    @Test
    void redeemCoupon_shouldSaveRedemptionAndIncrementUsageCount() {
        mockPaymentAndCoupon();

        when(couponRedemptionRepository.save(any(CouponRedemption.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CouponRedemption result =
                couponRedemptionService
                        .redeemCouponAfterSuccessfulPayment(100L);

        assertNotNull(result);
        assertSame(coupon, result.getCoupon());
        assertSame(customer, result.getCustomer());
        assertEquals(1, coupon.getUsedCount());

        verify(couponRepository).save(coupon);
        verify(couponRedemptionRepository)
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
        payment.setCustomer(null);

        when(paymentRepository.findById(100L))
                .thenReturn(Optional.of(payment));

        assertThrows(IllegalStateException.class,
                () -> couponRedemptionService
                        .redeemCouponAfterSuccessfulPayment(100L));

        verifyNoInteractions(couponRepository, couponRedemptionRepository);
    }

    @Test
    void redeemCoupon_shouldRejectAlreadyRedeemedCoupon() {
        when(paymentRepository.findById(100L))
                .thenReturn(Optional.of(payment));

        when(couponRepository.findById(10L))
                .thenReturn(Optional.of(coupon));

        when(couponRedemptionRepository
                .existsByCoupon_IdAndCustomer_Id(10L, 1L))
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

        // The helper configures the coupon and payment but does not
        // change the coupon's usage count.
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

        when(couponRepository.findById(10L))
                .thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> couponRedemptionService
                        .redeemCouponAfterSuccessfulPayment(100L));

        verify(couponRedemptionRepository, never())
                .save(any(CouponRedemption.class));
    }
}
