package com.ga.membership_payment_service.service;

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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private CouponRedemptionService couponRedemptionService;

    @Mock
    private CouponRedemptionRepository couponRedemptionRepository;

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private MembershipPlanRepository membershipPlanRepository;

    @Mock
    private UserMembershipRepository userMembershipRepository;

    @InjectMocks
    private PaymentService paymentService;

    private MembershipPlan plan;

    @BeforeEach
    void setUp() {
        plan = new MembershipPlan();
        plan.setId(10L);
        plan.setActive(true);
        plan.setDurationByDays(30);
        plan.setPrice(new BigDecimal("25.00"));
    }

    /**
     * Configures an authenticated customer in the security context.
     */
    private void setCustomerAuthentication() {
        Authentication authentication =
                new UsernamePasswordAuthenticationToken(
                        1L,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
                );

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    /**
     * Configures an authenticated administrator in the security context.
     */
    private void setAdminAuthentication() {
        Authentication authentication =
                new UsernamePasswordAuthenticationToken(
                        1L,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
                );

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    /**
     * Creates a valid coupon for testing.
     *
     * @param discountType the coupon discount type
     * @param discountValue the discount value
     * @return a valid coupon
     */
    private Coupon createCoupon(
            Coupon.DiscountType discountType,
            BigDecimal discountValue) {

        Coupon coupon = new Coupon();
        coupon.setId(20L);
        coupon.setActive(true);
        coupon.setStartDate(LocalDateTime.now().minusDays(1));
        coupon.setExpiresAt(LocalDateTime.now().plusDays(7));
        coupon.setUsageLimit(100);
        coupon.setUsedCount(0);
        coupon.setDiscountType(discountType);
        coupon.setDiscountValue(discountValue);

        return coupon;
    }

    /**
     * Creates a pending demo membership payment for testing.
     *
     * @param paymentId the payment ID
     * @param customerId the customer ID
     * @return a pending membership payment
     */
    private Payment createMembershipPayment(
            Long paymentId,
            Long customerId) {

        Payment payment = new Payment();
        payment.setId(paymentId);
        payment.setCustomerId(customerId);
        payment.setMembershipPlan(plan);
        payment.setPaymentType(Payment.PaymentType.MEMBERSHIP);
        payment.setStatus(Payment.Status.PENDING);
        payment.setProviderName("GYMIO_DEMO");

        return payment;
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void confirmDemoPayment_shouldActivateMembershipAfterSuccessfulPayment() {
        setCustomerAuthentication();

        Payment payment = createMembershipPayment(100L, 1L);

        when(paymentRepository.findByIdAndStatus(
                100L, Payment.Status.PENDING))
                .thenReturn(Optional.of(payment));

        when(paymentRepository.save(payment)).thenReturn(payment);

        paymentService.confirmDemoPayment(100L);

        assertEquals(Payment.Status.COMPLETED, payment.getStatus());

        verify(userMembershipRepository).save(argThat(membership ->
                membership.getUserId().equals(1L)
                        && membership.getMembershipPlan() == plan
                        && membership.getMembershipStatus()
                        == UserMembership.MembershipStatus.ACTIVE
                        && membership.getMembershipStartDate()
                        .equals(LocalDate.now())
                        && membership.getMembershipEndDate()
                        .equals(LocalDate.now().plusDays(30))
        ));

        verify(paymentRepository).save(payment);

        verify(couponRedemptionService, never())
                .redeemCouponAfterSuccessfulPayment(anyLong());
    }

    @Test
    void confirmDemoPayment_shouldRejectAlreadyCompletedPayment() {
        setCustomerAuthentication();

        when(paymentRepository.findByIdAndStatus(
                100L, Payment.Status.PENDING))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> paymentService.confirmDemoPayment(100L)
        );

        verify(userMembershipRepository, never()).save(any());
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void confirmDemoPayment_shouldRejectAnotherCustomersPayment() {
        setCustomerAuthentication();

        Payment payment = createMembershipPayment(100L, 2L);

        when(paymentRepository.findByIdAndStatus(
                100L, Payment.Status.PENDING))
                .thenReturn(Optional.of(payment));

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> paymentService.confirmDemoPayment(100L)
        );

        assertEquals(
                "You cannot confirm another customer's payment.",
                exception.getMessage()
        );

        assertEquals(Payment.Status.PENDING, payment.getStatus());

        verify(paymentRepository, never()).save(any(Payment.class));
        verify(userMembershipRepository, never()).save(any());
    }

    @Test
    void confirmDemoPayment_shouldRejectNonDemoPayment() {
        setCustomerAuthentication();

        Payment payment = createMembershipPayment(100L, 1L);
        payment.setProviderName("OTHER_PROVIDER");

        when(paymentRepository.findByIdAndStatus(
                100L, Payment.Status.PENDING))
                .thenReturn(Optional.of(payment));

        assertThrows(
                IllegalStateException.class,
                () -> paymentService.confirmDemoPayment(100L)
        );

        verify(userMembershipRepository, never()).save(any());
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void confirmDemoPayment_shouldRejectUnauthenticatedUser() {
        SecurityContextHolder.clearContext();

        assertThrows(
                ForbiddenException.class,
                () -> paymentService.confirmDemoPayment(100L)
        );

        verify(paymentRepository, never())
                .findByIdAndStatus(anyLong(), any(Payment.Status.class));

        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void createMembershipPayment_shouldNotActivateMembershipBeforeConfirmation() {
        setCustomerAuthentication();

        when(membershipPlanRepository.findById(10L))
                .thenReturn(Optional.of(plan));

        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        paymentService.createMembershipPayment(
                10L,
                Payment.PaymentMethod.values()[0],
                null
        );

        verify(paymentRepository).save(argThat(payment ->
                payment.getCustomerId().equals(1L)
                        && payment.getStatus() == Payment.Status.PENDING
                        && payment.getPaymentType()
                        == Payment.PaymentType.MEMBERSHIP
                        && payment.getMembershipPlan() == plan
                        && "BHD".equals(payment.getCurrency())
                        && payment.getAmount().compareTo(plan.getPrice()) == 0
        ));

        verify(userMembershipRepository, never()).save(any());
    }

    @Test
    void createMembershipPayment_shouldRejectInactivePlan() {
        setCustomerAuthentication();

        plan.setActive(false);

        when(membershipPlanRepository.findById(10L))
                .thenReturn(Optional.of(plan));

        assertThrows(
                IllegalStateException.class,
                () -> paymentService.createMembershipPayment(
                        10L,
                        Payment.PaymentMethod.values()[0],
                        null
                )
        );

        verify(paymentRepository, never()).save(any(Payment.class));
        verify(userMembershipRepository, never()).save(any());
    }

    @Test
    void createMembershipPayment_shouldRejectNonCustomer() {
        setAdminAuthentication();

        assertThrows(
                ForbiddenException.class,
                () -> paymentService.createMembershipPayment(
                        10L,
                        Payment.PaymentMethod.values()[0],
                        null
                )
        );

        verify(membershipPlanRepository, never()).findById(any());
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void createMembershipPayment_shouldRejectNonexistentPlan() {
        setCustomerAuthentication();

        when(membershipPlanRepository.findById(10L))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> paymentService.createMembershipPayment(
                        10L,
                        Payment.PaymentMethod.values()[0],
                        null
                )
        );

        verify(paymentRepository, never()).save(any(Payment.class));
        verify(userMembershipRepository, never()).save(any());
    }

    @Test
    void createMembershipPayment_shouldRejectUnauthenticatedUser() {
        SecurityContextHolder.clearContext();

        assertThrows(
                ForbiddenException.class,
                () -> paymentService.createMembershipPayment(
                        10L,
                        Payment.PaymentMethod.values()[0],
                        null
                )
        );

        verify(membershipPlanRepository, never()).findById(any());
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void createMembershipPayment_shouldCreatePendingPayment() {
        setCustomerAuthentication();

        when(membershipPlanRepository.findById(10L))
                .thenReturn(Optional.of(plan));

        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> {
                    Payment payment = invocation.getArgument(0);
                    payment.setId(100L);
                    return payment;
                });

        var response = paymentService.createMembershipPayment(
                10L,
                Payment.PaymentMethod.values()[0],
                null
        );

        assertEquals(100L, response.getId());

        verify(paymentRepository).save(argThat(payment ->
                payment.getCustomerId().equals(1L)
                        && payment.getStatus() == Payment.Status.PENDING
                        && payment.getPaymentType()
                        == Payment.PaymentType.MEMBERSHIP
                        && payment.getMembershipPlan() == plan
        ));

        verify(userMembershipRepository, never()).save(any());
    }

    @Test
    void getMyPayments_shouldRejectUnauthenticatedUser() {
        SecurityContextHolder.clearContext();

        assertThrows(
                ForbiddenException.class,
                () -> paymentService.getMyPayments()
        );

        verify(paymentRepository, never())
                .findAllByCustomerIdOrderByCreatedAtDesc(anyLong());
    }

    @Test
    void getMyPayments_shouldRejectNonCustomer() {
        setAdminAuthentication();

        assertThrows(
                ForbiddenException.class,
                () -> paymentService.getMyPayments()
        );

        verify(paymentRepository, never())
                .findAllByCustomerIdOrderByCreatedAtDesc(anyLong());
    }

    @Test
    void getMyPayments_shouldReturnCustomerPaymentHistory() {
        setCustomerAuthentication();

        Payment payment = createMembershipPayment(100L, 1L);
        payment.setStatus(Payment.Status.COMPLETED);
        payment.setPaymentMethod(Payment.PaymentMethod.values()[0]);

        when(paymentRepository.findAllByCustomerIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(payment));

        var result = paymentService.getMyPayments();

        assertEquals(1, result.size());
        assertEquals(100L, result.get(0).getId());
        assertEquals(Payment.Status.COMPLETED, result.get(0).getStatus());

        verify(paymentRepository)
                .findAllByCustomerIdOrderByCreatedAtDesc(1L);
    }

    @Test
    void confirmDemoPayment_shouldRejectMissingPayment() {
        setCustomerAuthentication();

        when(paymentRepository.findByIdAndStatus(
                999L, Payment.Status.PENDING))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> paymentService.confirmDemoPayment(999L)
        );

        verify(paymentRepository, never()).save(any(Payment.class));
        verify(userMembershipRepository, never()).save(any());
    }

    @Test
    void confirmDemoPayment_shouldRejectNonCustomer() {
        setAdminAuthentication();

        assertThrows(
                ForbiddenException.class,
                () -> paymentService.confirmDemoPayment(100L)
        );

        verify(paymentRepository, never())
                .findByIdAndStatus(anyLong(), any(Payment.Status.class));

        verify(paymentRepository, never()).save(any(Payment.class));
        verify(userMembershipRepository, never()).save(any());
    }

    @Test
    void createMembershipPayment_shouldApplyPercentageDiscount() {
        setCustomerAuthentication();

        Coupon coupon = createCoupon(
                Coupon.DiscountType.PERCENTAGE,
                new BigDecimal("20")
        );

        when(membershipPlanRepository.findById(10L))
                .thenReturn(Optional.of(plan));

        when(couponRepository.findByIdAndActiveTrue(20L))
                .thenReturn(Optional.of(coupon));

        when(couponRedemptionRepository
                .existsByCoupon_IdAndCustomerId(20L, 1L))
                .thenReturn(false);

        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        paymentService.createMembershipPayment(
                10L,
                Payment.PaymentMethod.values()[0],
                20L
        );

        verify(paymentRepository).save(argThat(payment ->
                payment.getStatus() == Payment.Status.PENDING
                        && payment.getCoupon() == coupon
                        && payment.getAmount().compareTo(
                        new BigDecimal("20.000")) == 0
        ));

        verify(couponRedemptionRepository, never()).save(any());
        verify(couponRedemptionService, never())
                .redeemCouponAfterSuccessfulPayment(anyLong());
    }

    @Test
    void createMembershipPayment_shouldApplyFixedDiscount() {
        setCustomerAuthentication();

        Coupon coupon = createCoupon(
                Coupon.DiscountType.FIXED_AMOUNT,
                new BigDecimal("5")
        );

        when(membershipPlanRepository.findById(10L))
                .thenReturn(Optional.of(plan));

        when(couponRepository.findByIdAndActiveTrue(20L))
                .thenReturn(Optional.of(coupon));

        when(couponRedemptionRepository
                .existsByCoupon_IdAndCustomerId(20L, 1L))
                .thenReturn(false);

        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        paymentService.createMembershipPayment(
                10L,
                Payment.PaymentMethod.values()[0],
                20L
        );

        verify(paymentRepository).save(argThat(payment ->
                payment.getAmount().compareTo(
                        new BigDecimal("20.000")) == 0
                        && payment.getCoupon() == coupon
                        && payment.getStatus() == Payment.Status.PENDING
        ));

        verify(couponRedemptionRepository, never()).save(any());
    }

    @Test
    void createMembershipPayment_shouldRejectMissingOrInactiveCoupon() {
        setCustomerAuthentication();

        when(membershipPlanRepository.findById(10L))
                .thenReturn(Optional.of(plan));

        when(couponRepository.findByIdAndActiveTrue(20L))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> paymentService.createMembershipPayment(
                        10L,
                        Payment.PaymentMethod.values()[0],
                        20L
                )
        );

        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void confirmDemoPayment_shouldRedeemCouponAndActivateMembership() {
        setCustomerAuthentication();

        Coupon coupon = createCoupon(
                Coupon.DiscountType.PERCENTAGE,
                new BigDecimal("20")
        );

        Payment payment = createMembershipPayment(100L, 1L);
        payment.setCoupon(coupon);

        when(paymentRepository.findByIdAndStatus(
                100L, Payment.Status.PENDING))
                .thenReturn(Optional.of(payment));

        when(paymentRepository.save(payment)).thenReturn(payment);

        paymentService.confirmDemoPayment(100L);

        assertEquals(Payment.Status.COMPLETED, payment.getStatus());

        verify(couponRedemptionService)
                .redeemCouponAfterSuccessfulPayment(100L);

        verify(userMembershipRepository).save(argThat(membership ->
                membership.getUserId().equals(1L)
                        && membership.getMembershipStatus()
                        == UserMembership.MembershipStatus.ACTIVE
        ));

        verify(paymentRepository).save(payment);
    }

    @Test
    void confirmDemoPayment_shouldNotSavePaymentOrActivateMembershipWhenCouponRedemptionFails() {
        setCustomerAuthentication();

        Coupon coupon = createCoupon(
                Coupon.DiscountType.PERCENTAGE,
                new BigDecimal("20")
        );

        Payment payment = createMembershipPayment(100L, 1L);
        payment.setCoupon(coupon);

        when(paymentRepository.findByIdAndStatus(
                100L, Payment.Status.PENDING))
                .thenReturn(Optional.of(payment));

        doThrow(new IllegalStateException("Coupon redemption failed"))
                .when(couponRedemptionService)
                .redeemCouponAfterSuccessfulPayment(100L);

        assertThrows(
                IllegalStateException.class,
                () -> paymentService.confirmDemoPayment(100L)
        );

        verify(couponRedemptionService)
                .redeemCouponAfterSuccessfulPayment(100L);

        verify(userMembershipRepository, never()).save(any());
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void confirmDemoPayment_shouldRejectMembershipPaymentWithoutPlan() {
        setCustomerAuthentication();

        Payment payment = createMembershipPayment(100L, 1L);
        payment.setMembershipPlan(null);

        when(paymentRepository.findByIdAndStatus(
                100L, Payment.Status.PENDING))
                .thenReturn(Optional.of(payment));

        assertThrows(
                IllegalStateException.class,
                () -> paymentService.confirmDemoPayment(100L)
        );

        verify(userMembershipRepository, never()).save(any());
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void confirmDemoPayment_shouldNotActivateMembershipForBookingPayment() {
        setCustomerAuthentication();

        Payment payment = new Payment();
        payment.setId(101L);
        payment.setCustomerId(1L);
        payment.setPaymentType(Payment.PaymentType.BOOKING);
        payment.setStatus(Payment.Status.PENDING);
        payment.setProviderName("GYMIO_DEMO");

        when(paymentRepository.findByIdAndStatus(
                101L, Payment.Status.PENDING))
                .thenReturn(Optional.of(payment));

        when(paymentRepository.save(payment)).thenReturn(payment);

        paymentService.confirmDemoPayment(101L);

        assertEquals(Payment.Status.COMPLETED, payment.getStatus());

        verify(paymentRepository).save(payment);
        verify(userMembershipRepository, never()).save(any());

        verify(couponRedemptionService, never())
                .redeemCouponAfterSuccessfulPayment(anyLong());
    }

    @Test
    void createMembershipPayment_shouldRejectCouponThatHasNotStarted() {
        setCustomerAuthentication();

        Coupon coupon = createCoupon(
                Coupon.DiscountType.PERCENTAGE,
                new BigDecimal("20")
        );
        coupon.setStartDate(LocalDateTime.now().plusDays(1));

        when(membershipPlanRepository.findById(10L))
                .thenReturn(Optional.of(plan));

        when(couponRepository.findByIdAndActiveTrue(20L))
                .thenReturn(Optional.of(coupon));

        assertThrows(
                IllegalStateException.class,
                () -> paymentService.createMembershipPayment(
                        10L,
                        Payment.PaymentMethod.values()[0],
                        20L
                )
        );

        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void createMembershipPayment_shouldRejectExpiredCoupon() {
        setCustomerAuthentication();

        Coupon coupon = createCoupon(
                Coupon.DiscountType.PERCENTAGE,
                new BigDecimal("20")
        );
        coupon.setExpiresAt(LocalDateTime.now().minusDays(1));

        when(membershipPlanRepository.findById(10L))
                .thenReturn(Optional.of(plan));

        when(couponRepository.findByIdAndActiveTrue(20L))
                .thenReturn(Optional.of(coupon));

        assertThrows(
                IllegalStateException.class,
                () -> paymentService.createMembershipPayment(
                        10L,
                        Payment.PaymentMethod.values()[0],
                        20L
                )
        );

        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void createMembershipPayment_shouldRejectCouponAtUsageLimit() {
        setCustomerAuthentication();

        Coupon coupon = createCoupon(
                Coupon.DiscountType.PERCENTAGE,
                new BigDecimal("20")
        );
        coupon.setUsageLimit(5);
        coupon.setUsedCount(5);

        when(membershipPlanRepository.findById(10L))
                .thenReturn(Optional.of(plan));

        when(couponRepository.findByIdAndActiveTrue(20L))
                .thenReturn(Optional.of(coupon));

        assertThrows(
                IllegalStateException.class,
                () -> paymentService.createMembershipPayment(
                        10L,
                        Payment.PaymentMethod.values()[0],
                        20L
                )
        );

        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void createMembershipPayment_shouldRejectPreviouslyRedeemedCoupon() {
        setCustomerAuthentication();

        Coupon coupon = createCoupon(
                Coupon.DiscountType.PERCENTAGE,
                new BigDecimal("20")
        );

        when(membershipPlanRepository.findById(10L))
                .thenReturn(Optional.of(plan));

        when(couponRepository.findByIdAndActiveTrue(20L))
                .thenReturn(Optional.of(coupon));

        when(couponRedemptionRepository
                .existsByCoupon_IdAndCustomerId(20L, 1L))
                .thenReturn(true);

        assertThrows(
                IllegalStateException.class,
                () -> paymentService.createMembershipPayment(
                        10L,
                        Payment.PaymentMethod.values()[0],
                        20L
                )
        );

        verify(paymentRepository, never()).save(any(Payment.class));
    }
}
