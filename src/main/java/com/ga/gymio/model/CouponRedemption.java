package com.ga.gymio.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Represents a customer's redemption of a coupon,
 * including the coupon used, the customer, and the redemption time.
 */
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "coupon_redemptions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_coupon_redemption_customer",
                        columnNames = {"coupon_id", "customer_id"}
                )
        }
)
public class CouponRedemption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "coupon_id", nullable = false)
    private Coupon coupon;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime redeemedAt;
}