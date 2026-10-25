package com.ga.gymio.controller;

import com.ga.gymio.dto.response.PaymentResponse;
import com.ga.gymio.model.Payment;
import com.ga.gymio.service.PaymentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/membership-plans/{planId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponse createMembershipPayment(
            @PathVariable Long planId,
            @Valid @RequestBody CreatePaymentRequest request) {

        return paymentService.createMembershipPayment(
                planId,
                request.paymentMethod()
        );
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('CUSTOMER')")
    public List<PaymentResponse> getMyPayments() {
        return paymentService.getMyPayments();
    }

    public record CreatePaymentRequest(
            @NotNull(message = "Payment method is required.")
            Payment.PaymentMethod paymentMethod) {
    }
}
