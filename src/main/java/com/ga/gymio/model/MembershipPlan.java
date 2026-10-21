package com.ga.gymio.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents a gym membership plan, including its name, duration,
 * price, and creation and update timestamps.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "membership_plans")
public class MembershipPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Membership plan name is required")
    @Column(nullable = false, unique = true)
    private String name;

    @NotNull(message = "Membership duration is required")
    @Min(value = 1, message = "Membership duration must be at least one day")
    @Column(nullable = false)
    private Integer durationByDays;

    @NotNull(message = "Membership price is required")
    @DecimalMin(value = "0.00", message = "Membership price cannot be negative")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

}
