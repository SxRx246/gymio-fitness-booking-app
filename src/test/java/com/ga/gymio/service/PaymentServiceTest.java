package com.ga.gymio.service;

import com.ga.gymio.authentication.MyUserDetails;
import com.ga.gymio.model.MembershipPlan;
import com.ga.gymio.model.Payment;
import com.ga.gymio.model.User;
import com.ga.gymio.repository.MembershipPlanRepository;
import com.ga.gymio.repository.PaymentRepository;
import com.ga.gymio.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

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

        userDetails = mock(MyUserDetails.class);
        authentication = mock(Authentication.class);

        when(userDetails.getUser()).thenReturn(customer);
        when(authentication.getPrincipal()).thenReturn(userDetails);

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

        when(paymentRepository.findById(100L))
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

        when(paymentRepository.findById(100L))
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
    void confirmDemoPayment_shouldRejectAnotherCustomersPayment() {
        User anotherCustomer = new User();
        anotherCustomer.setId(2L);

        Payment payment = new Payment();
        payment.setId(100L);
        payment.setCustomer(anotherCustomer);
        payment.setStatus(Payment.Status.PENDING);
        payment.setProviderName("GYMIO_DEMO");

        when(paymentRepository.findById(100L))
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
    void confirmDemoPayment_shouldRejectNonDemoPayment() {
        Payment payment = new Payment();
        payment.setId(100L);
        payment.setCustomer(customer);
        payment.setStatus(Payment.Status.PENDING);
        payment.setProviderName("OTHER_PROVIDER");

        when(paymentRepository.findById(100L))
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
    void createMembershipPayment_shouldNotActivateMembershipBeforeConfirmation() {
        when(membershipPlanRepository.findById(10L))
                .thenReturn(Optional.of(plan));

        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        paymentService.createMembershipPayment(
                10L,
                Payment.PaymentMethod.values()[0]
        );

        assertEquals(
                User.MembershipStatus.EXPIRED,
                customer.getMembershipStatus()
        );

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
                        Payment.PaymentMethod.values()[0]
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
    void createMembershipPayment_shouldRejectNonCustomer() {
        customer.setRole(User.Role.ADMIN);

        assertThrows(
                IllegalStateException.class,
                () -> paymentService.createMembershipPayment(
                        10L,
                        Payment.PaymentMethod.values()[0]
                )
        );

        verify(membershipPlanRepository, never()).findById(any());
        verify(paymentRepository, never()).save(any(Payment.class));
        verify(userRepository, never()).save(any(User.class));
    }


}
