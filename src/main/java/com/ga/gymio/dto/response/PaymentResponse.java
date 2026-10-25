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
    private String paymentReference;
    private BigDecimal amount;
    private String currency;
    private Payment.Status status;
    private Payment.PaymentMethod paymentMethod;
    private LocalDateTime createdAt;
}
