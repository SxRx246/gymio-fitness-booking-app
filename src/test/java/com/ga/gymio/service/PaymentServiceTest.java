package com.ga.gymio.service;

import com.ga.gymio.authentication.MyUserDetails;
import com.ga.gymio.model.Coupon;
import com.ga.gymio.model.MembershipPlan;
import com.ga.gymio.model.Payment;
import com.ga.gymio.model.User;
import com.ga.gymio.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
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
    private UserRepository userRepository;

    @InjectMocks
    private PaymentService paymentService;

    private User customer;
    private MembershipPlan plan;
    private MyUserDetails userDetails;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        customer = new User();
        customer.setId(1L);
        customer.setRole(User.Role.CUSTOMER);
        customer.setMembershipStatus(User.MembershipStatus.EXPIRED);

        plan = new MembershipPlan();
        plan.setId(10L);
        plan.setActive(true);
        plan.setDurationByDays(30);
        plan.setPrice(new BigDecimal("25.00"));

        userDetails = mock(MyUserDetails.class);
        authentication = mock(Authentication.class);

        lenient().when(userDetails.getUser()).thenReturn(customer);
        lenient().when(authentication.getPrincipal()).thenReturn(userDetails);

        SecurityContextHolder.getContext()
                .setAuthentication(authentication);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void confirmDemoPayment_shouldActivateMembershipAfterSuccessfulPayment() {
        Payment payment = new Payment();
        payment.setId(100L);
        payment.setCustomer(customer);
        payment.setMembershipPlan(plan);
        payment.setPaymentType(Payment.PaymentType.MEMBERSHIP);
        payment.setStatus(Payment.Status.PENDING);
        payment.setProviderName("GYMIO_DEMO");

        when(paymentRepository.findByIdAndStatus(
                100L, Payment.Status.PENDING))
                .thenReturn(Optional.of(payment));
        when(paymentRepository.save(payment))
                .thenReturn(payment);

        paymentService.confirmDemoPayment(100L);

        assertEquals(Payment.Status.COMPLETED, payment.getStatus());
        assertEquals(User.MembershipStatus.ACTIVE,
                customer.getMembershipStatus());
        assertEquals(plan, customer.getMembershipPlan());
        assertEquals(LocalDate.now(), customer.getMembershipStartDate());
        assertEquals(
                LocalDate.now().plusDays(30),
                customer.getMembershipEndDate()
        );

        verify(userRepository).save(customer);
        verify(paymentRepository).save(payment);
        verify(couponRedemptionService, never())
                .redeemCouponAfterSuccessfulPayment(anyLong());
    }

    @Test
    void confirmDemoPayment_shouldRejectAlreadyCompletedPayment() {
        Payment payment = new Payment();
        payment.setId(100L);
        payment.setCustomer(customer);
        payment.setMembershipPlan(plan);
        payment.setPaymentType(Payment.PaymentType.MEMBERSHIP);
        payment.setStatus(Payment.Status.COMPLETED);
        payment.setProviderName("GYMIO_DEMO");

        when(paymentRepository.findByIdAndStatus(
                100L, Payment.Status.PENDING))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> paymentService.confirmDemoPayment(100L)
        );

        assertEquals(User.MembershipStatus.EXPIRED,
                customer.getMembershipStatus());

        verify(userRepository, never()).save(any(User.class));
        verify(paymentRepository, never()).save(any(Payment.class));
    }


    @Test
    void confirmDemoPayment_shouldRejectAnotherCustomersPayment() {
        User anotherCustomer = new User();
        anotherCustomer.setId(2L);

        Payment payment = new Payment();
        payment.setId(100L);
        payment.setCustomer(anotherCustomer);
        payment.setStatus(Payment.Status.PENDING);
        payment.setProviderName("GYMIO_DEMO");

        when(paymentRepository.findByIdAndStatus(
                100L, Payment.Status.PENDING))
                .thenReturn(Optional.of(payment));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> paymentService.confirmDemoPayment(100L)
        );

        assertEquals(
                "You cannot confirm another customer's payment.",
                exception.getMessage()
        );

        assertEquals(Payment.Status.PENDING, payment.getStatus());

        assertEquals(User.MembershipStatus.EXPIRED,
                customer.getMembershipStatus());
        assertNull(customer.getMembershipPlan());
        assertNull(customer.getMembershipStartDate());
        assertNull(customer.getMembershipEndDate());

        verify(paymentRepository, never()).save(any(Payment.class));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void confirmDemoPayment_shouldRejectNonDemoPayment() {
        Payment payment = new Payment();
        payment.setId(100L);
        payment.setCustomer(customer);
        payment.setStatus(Payment.Status.PENDING);
        payment.setProviderName("OTHER_PROVIDER");

        when(paymentRepository.findByIdAndStatus(
                100L, Payment.Status.PENDING))
                .thenReturn(Optional.of(payment));

        assertThrows(
                IllegalStateException.class,
                () -> paymentService.confirmDemoPayment(100L)
        );

        assertEquals(User.MembershipStatus.EXPIRED,
                customer.getMembershipStatus());

        verify(userRepository, never()).save(any(User.class));
        verify(paymentRepository, never()).save(any(Payment.class));
    }


    @Test
    void confirmDemoPayment_shouldRejectUnauthenticatedUser() {
        SecurityContextHolder.clearContext();

        assertThrows(
                IllegalStateException.class,
                () -> paymentService.confirmDemoPayment(100L)
        );

        verify(paymentRepository, never())
                .findByIdAndStatus(anyLong(), any(Payment.Status.class));
        verify(paymentRepository, never()).save(any(Payment.class));
        verify(userRepository, never()).save(any(User.class));
    }


    @Test
    void createMembershipPayment_shouldNotActivateMembershipBeforeConfirmation() {
        when(membershipPlanRepository.findById(10L))
                .thenReturn(Optional.of(plan));

        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        paymentService.createMembershipPayment(
                10L,
                Payment.PaymentMethod.values()[0],
                null
        );

        assertEquals(
                User.MembershipStatus.EXPIRED,
                customer.getMembershipStatus()
        );

        assertNull(customer.getMembershipPlan());
        assertNull(customer.getMembershipStartDate());
        assertNull(customer.getMembershipEndDate());

        verify(userRepository, never()).save(any(User.class));

        verify(paymentRepository).save(argThat(payment ->
                payment.getStatus() == Payment.Status.PENDING
                        && payment.getPaymentType() == Payment.PaymentType.MEMBERSHIP
        ));
    }


    @Test
    void createMembershipPayment_shouldRejectInactivePlan() {
        plan.setActive(false);

        when(membershipPlanRepository.findById(10L))
                .thenReturn(Optional.of(plan));

        assertThrows(
                IllegalStateException.class,
                () -> paymentService.createMembershipPayment(
                        10L,
                        Payment.PaymentMethod.values()[0],
                        null
                ));

        verify(paymentRepository, never()).save(any(Payment.class));
        verify(userRepository, never()).save(any(User.class));

        assertEquals(
                User.MembershipStatus.EXPIRED,
                customer.getMembershipStatus()
        );
    }


    @Test
    void createMembershipPayment_shouldRejectNonCustomer() {
        customer.setRole(User.Role.ADMIN);

        assertThrows(
                IllegalStateException.class,
                () -> paymentService.createMembershipPayment(
                        10L,
                        Payment.PaymentMethod.values()[0],
                        null
                )
        );

        verify(membershipPlanRepository, never()).findById(any());
        verify(paymentRepository, never()).save(any(Payment.class));
        verify(userRepository, never()).save(any(User.class));
    }


    @Test
    void createMembershipPayment_shouldRejectNonexistentPlan() {
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
        verify(userRepository, never()).save(any(User.class));

        assertEquals(
                User.MembershipStatus.EXPIRED,
                customer.getMembershipStatus()
        );
    }

    @Test
    void createMembershipPayment_shouldRejectUnauthenticatedUser() {
        SecurityContextHolder.clearContext();

        assertThrows(
                IllegalStateException.class,
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
        when(membershipPlanRepository.findById(10L))
                .thenReturn(Optional.of(plan));

        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> {
                    Payment payment = invocation.getArgument(0);
                    payment.setId(100L);
                    return payment;
                });

        paymentService.createMembershipPayment(
                10L,
                Payment.PaymentMethod.values()[0],
                null
        );

        verify(paymentRepository).save(argThat(payment ->
                payment.getStatus() == Payment.Status.PENDING
                        && payment.getPaymentType()
                        == Payment.PaymentType.MEMBERSHIP
                        && payment.getCustomer() == customer
                        && payment.getMembershipPlan() == plan
                        && "BHD".equals(payment.getCurrency())
                        && Objects.equals(payment.getAmount(), plan.getPrice())
        ));

        verify(userRepository, never()).save(any(User.class));

        assertEquals(
                User.MembershipStatus.EXPIRED,
                customer.getMembershipStatus()
        );
    }

    @Test
    void getMyPayments_shouldRejectUnauthenticatedUser() {
        SecurityContextHolder.clearContext();

        assertThrows(
                IllegalStateException.class,
                () -> paymentService.getMyPayments()
        );

        verify(paymentRepository, never())
                .findAllByCustomer_IdOrderByCreatedAtDesc(any());
    }

    @Test
    void getMyPayments_shouldRejectNonCustomer() {
        customer.setRole(User.Role.ADMIN);

        assertThrows(
                IllegalStateException.class,
                () -> paymentService.getMyPayments()
        );

        verify(paymentRepository, never())
                .findAllByCustomer_IdOrderByCreatedAtDesc(any());
    }


    @Test
    void getMyPayments_shouldReturnCustomerPaymentHistory() {
        Payment payment = new Payment();
        payment.setId(100L);
        payment.setCustomer(customer);
        payment.setStatus(Payment.Status.COMPLETED);
        payment.setPaymentMethod(Payment.PaymentMethod.values()[0]);

        when(paymentRepository.findAllByCustomer_IdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(payment));

        var result = paymentService.getMyPayments();

        assertEquals(1, result.size());
        assertEquals(100L, result.get(0).getId());
        assertEquals(Payment.Status.COMPLETED, result.get(0).getStatus());

        verify(paymentRepository)
                .findAllByCustomer_IdOrderByCreatedAtDesc(1L);
    }


    @Test
    void confirmDemoPayment_shouldRejectMissingPayment() {
        when(paymentRepository.findByIdAndStatus(
                999L, Payment.Status.PENDING))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> paymentService.confirmDemoPayment(999L)
        );

        verify(paymentRepository, never()).save(any(Payment.class));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void confirmDemoPayment_shouldRejectNonCustomer() {
        customer.setRole(User.Role.ADMIN);

        assertThrows(
                IllegalStateException.class,
                () -> paymentService.confirmDemoPayment(100L)
        );

        verify(paymentRepository, never())
                .findByIdAndStatus(anyLong(), any(Payment.Status.class));
        verify(paymentRepository, never()).save(any(Payment.class));
        verify(userRepository, never()).save(any(User.class));
    }

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

    @Test
    void createMembershipPayment_shouldApplyPercentageDiscount() {
        Coupon coupon = createCoupon(
                Coupon.DiscountType.PERCENTAGE,
                new BigDecimal("20")
        );

        when(membershipPlanRepository.findById(10L))
                .thenReturn(Optional.of(plan));

        when(couponRepository.findByIdAndActiveTrue(20L))
                .thenReturn(Optional.of(coupon));

        when(couponRedemptionRepository
                .existsByCoupon_IdAndCustomer_Id(20L, 1L))
                .thenReturn(false);

        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> {
                    Payment payment = invocation.getArgument(0);
                    payment.setId(100L);
                    return payment;
                });

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

        assertEquals(
                User.MembershipStatus.EXPIRED,
                customer.getMembershipStatus()
        );

        verify(couponRedemptionRepository, never())
                .save(any());

        verify(couponRedemptionService, never())
                .redeemCouponAfterSuccessfulPayment(anyLong());
    }

    @Test
    void createMembershipPayment_shouldApplyFixedDiscount() {
        Coupon coupon = createCoupon(
                Coupon.DiscountType.FIXED_AMOUNT,
                new BigDecimal("5")
        );

        when(membershipPlanRepository.findById(10L))
                .thenReturn(Optional.of(plan));

        when(couponRepository.findByIdAndActiveTrue(20L))
                .thenReturn(Optional.of(coupon));

        when(couponRedemptionRepository
                .existsByCoupon_IdAndCustomer_Id(20L, 1L))
                .thenReturn(false);

        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> {
                    Payment payment = invocation.getArgument(0);
                    payment.setId(101L);
                    return payment;
                });

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

        verify(couponRedemptionRepository, never())
                .save(any());
    }

    @Test
    void createMembershipPayment_shouldRejectMissingOrInactiveCoupon() {
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

        verify(couponRedemptionRepository, never()).save(any());
    }


    @Test
    void confirmDemoPayment_shouldRedeemCouponAndActivateMembership() {
        Coupon coupon = createCoupon(
                Coupon.DiscountType.PERCENTAGE,
                new BigDecimal("20")
        );

        Payment payment = new Payment();
        payment.setId(100L);
        payment.setCustomer(customer);
        payment.setMembershipPlan(plan);
        payment.setPaymentType(Payment.PaymentType.MEMBERSHIP);
        payment.setStatus(Payment.Status.PENDING);
        payment.setProviderName("GYMIO_DEMO");
        payment.setCoupon(coupon);

        when(paymentRepository.findByIdAndStatus(
                100L, Payment.Status.PENDING
        )).thenReturn(Optional.of(payment));

        when(paymentRepository.save(payment)).thenReturn(payment);

        paymentService.confirmDemoPayment(100L);

        assertEquals(Payment.Status.COMPLETED, payment.getStatus());
        assertEquals(User.MembershipStatus.ACTIVE,
                customer.getMembershipStatus());
        assertEquals(plan, customer.getMembershipPlan());

        verify(couponRedemptionService)
                .redeemCouponAfterSuccessfulPayment(100L);
        verify(userRepository).save(customer);
        verify(paymentRepository).save(payment);
    }


    @Test
    void confirmDemoPayment_shouldNotSavePaymentOrActivateMembershipWhenCouponRedemptionFails() {
        Coupon coupon = createCoupon(
                Coupon.DiscountType.PERCENTAGE,
                new BigDecimal("20")
        );

        Payment payment = new Payment();
        payment.setId(100L);
        payment.setCustomer(customer);
        payment.setMembershipPlan(plan);
        payment.setPaymentType(Payment.PaymentType.MEMBERSHIP);
        payment.setStatus(Payment.Status.PENDING);
        payment.setProviderName("GYMIO_DEMO");
        payment.setCoupon(coupon);

        when(paymentRepository.findByIdAndStatus(
                100L, Payment.Status.PENDING
        )).thenReturn(Optional.of(payment));

        doThrow(new IllegalStateException("Coupon redemption failed"))
                .when(couponRedemptionService)
                .redeemCouponAfterSuccessfulPayment(100L);

        assertThrows(
                IllegalStateException.class,
                () -> paymentService.confirmDemoPayment(100L)
        );

        verify(couponRedemptionService)
                .redeemCouponAfterSuccessfulPayment(100L);
        verify(userRepository, never()).save(any(User.class));
        verify(paymentRepository, never()).save(any(Payment.class));

        assertEquals(User.MembershipStatus.EXPIRED,
                customer.getMembershipStatus());
        assertNull(customer.getMembershipPlan());
        assertNull(customer.getMembershipStartDate());
        assertNull(customer.getMembershipEndDate());
    }


}
