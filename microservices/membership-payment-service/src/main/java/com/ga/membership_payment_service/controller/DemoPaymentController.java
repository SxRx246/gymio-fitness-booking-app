package com.ga.membership_payment_service.controller;

import com.ga.membership_payment_service.dto.response.PaymentResponse;
import com.ga.membership_payment_service.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Profile("dev")
public class DemoPaymentController {

    private final PaymentService paymentService;

    @PostMapping("/{paymentId}/demo-confirmation")
    @PreAuthorize("hasRole('CUSTOMER')")
    public PaymentResponse confirmDemoPayment(
            @PathVariable Long paymentId) {

        return paymentService.confirmDemoPayment(paymentId);
    }
}
