package com.ga.gymio.dto.response;

import com.ga.gymio.model.Payment;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class PaymentResponse {
    private Long id;
    private BigDecimal amount;
    private String currency;
    private Payment.Status status;
    private Payment.PaymentType paymentType;
    private Payment.PaymentMethod paymentMethod;
    private String providerName;
    private Long bookingId;
    private Long membershipPlanId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
