package com.ga.gymio.controller;

import com.ga.gymio.dto.response.PaymentResponse;
import com.ga.gymio.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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
