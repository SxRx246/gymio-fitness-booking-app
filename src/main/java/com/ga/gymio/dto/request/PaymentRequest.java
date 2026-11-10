package com.ga.gymio.dto.request;

import com.ga.gymio.model.Payment;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PaymentRequest {
    @NotNull(message = "Payment type is required")
    private Payment.PaymentType paymentType;

    @NotNull(message = "Payment method is required")
    private Payment.PaymentMethod paymentMethod;

    @Positive(message = "Booking ID must be positive")
    private Long bookingId;

    @Positive(message = "Membership plan ID must be positive")
    private Long membershipPlanId;

    @Positive(message = "Coupon ID must be positive.")
    private Long couponId;

}
