package com.ga.gymio.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Represents a Gymio user account, including authentication details,
 * roles, account status, and email verification and password recovery data.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @JsonIgnore
    private String password;

    public enum Role {
        ADMIN,
        CUSTOMER,
        TRAINER
    }
    @Enumerated(EnumType.STRING)
    private Role role;

    public enum Status {
        ACTIVE,
        INACTIVE,
        SUSPENDED
    }
    @Enumerated(EnumType.STRING)
    private Status status;

    public enum MembershipStatus {
        PENDING,
        ACTIVE,
        EXPIRED,
        CANCELLED
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "membership_plan_id")
    private MembershipPlan membershipPlan;

    private LocalDate membershipStartDate;

    private LocalDate membershipEndDate;

    @Enumerated(EnumType.STRING)
    private MembershipStatus membershipStatus;

    @JsonIgnore
    private String verificationToken;

    @JsonIgnore
    private LocalDateTime verificationTokenExpiresAt;

    @JsonIgnore
    private LocalDateTime verificationEmailSentAt;

    private boolean emailVerified;

    @JsonIgnore
    private String passwordResetToken;

    @JsonIgnore
    private LocalDateTime passwordResetTokenExpiresAt;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
