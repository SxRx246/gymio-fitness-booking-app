package com.ga.membership_payment_service.controller;

import com.ga.membership_payment_service.dto.request.PaymentRequest;
import com.ga.membership_payment_service.dto.response.PaymentResponse;
import com.ga.membership_payment_service.service.PaymentService;
import jakarta.validation.Valid;
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
            @Valid @RequestBody PaymentRequest request) {

        return paymentService.createMembershipPayment(
                planId,
                request.getPaymentMethod(),
                request.getCouponId()
        );
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('CUSTOMER')")
    public List<PaymentResponse> getMyPayments() {
        return paymentService.getMyPayments();
    }

}
