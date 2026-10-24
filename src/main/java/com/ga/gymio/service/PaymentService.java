package com.ga.gymio.service;

import com.ga.gymio.authentication.MyUserDetails;
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

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final MembershipPlanRepository membershipPlanRepository;
    private final UserRepository userRepository;

    @Transactional
    public Payment createMembershipPayment(
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

        return paymentRepository.save(payment);
    }
}
