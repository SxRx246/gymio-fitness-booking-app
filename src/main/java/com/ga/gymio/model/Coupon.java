package com.ga.gymio.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents a discount coupon that customers can use
 * when paying for bookings or membership plans.
 */
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "coupons")
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 500)
    private String description;

    @NotBlank(message = "Coupon code is required")
    @Column(nullable = false, unique = true, length = 50)
    private String code;

    public enum DiscountType {
        PERCENTAGE,
        FIXED_AMOUNT
    }

    @NotNull(message = "Discount type is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DiscountType discountType;

    @NotNull(message = "Discount value is required")
    @DecimalMin(
            value = "0.01",
            message = "Discount value must be greater than zero"
    )
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal discountValue;

    @NotNull(message = "Usage limit is required")
    @Min(value = 1, message = "Usage limit must be at least one")
    @Column(nullable = false)
    private Integer usageLimit;

    @NotNull(message = "Used count is required")
    @Min(value = 0, message = "Used count cannot be negative")
    @Column(nullable = false)
    private Integer usedCount = 0;

    @NotNull(message = "Start date is required")
    @Column(nullable = false)
    private LocalDateTime startDate;

    @NotNull(message = "Expiration date is required")
    @Future(message = "Expiration date must be in the future")
    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}