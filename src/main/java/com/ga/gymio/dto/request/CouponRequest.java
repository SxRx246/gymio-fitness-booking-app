package com.ga.gymio.dto.request;

import com.ga.gymio.model.Coupon;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class CouponRequest {

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    @NotBlank(message = "Coupon code is required")
    @Size(max = 50, message = "Coupon code cannot exceed 50 characters")
    private String code;

    public enum DiscountType {
        PERCENTAGE,
        FIXED_AMOUNT
    }

    @NotNull(message = "Discount type is required")
    private Coupon.DiscountType discountType;

    @NotNull(message = "Discount value is required")
    @DecimalMin(
            value = "0.01",
            message = "Discount value must be greater than zero"
    )
    private BigDecimal discountValue;

    @NotNull(message = "Usage limit is required")
    @Min(value = 1, message = "Usage limit must be at least one")
    private Integer usageLimit;

    @NotNull(message = "Start date is required")
    private LocalDateTime startDate;

    @NotNull(message = "Expiration date is required")
    @Future(message = "Expiration date must be in the future")
    private LocalDateTime expiresAt;
}
