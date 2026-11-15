package com.ga.membership_payment_service.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Records membership, payment, and coupon-related actions
 * performed within the Gymio application.
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Represents the types of actions recorded by this service.
     */
    public enum Action {
        CREATE_MEMBERSHIP_PLAN,
        UPDATE_MEMBERSHIP_PLAN,
        DEACTIVATE_MEMBERSHIP_PLAN,
        ACTIVATE_MEMBERSHIP_PLAN,

        ASSIGN_MEMBERSHIP,
        CHANGE_MEMBERSHIP,
        FREEZE_MEMBERSHIP,
        RESUME_MEMBERSHIP,
        CANCEL_MEMBERSHIP,
        RENEW_MEMBERSHIP,
        EXPIRE_MEMBERSHIP,

        CREATE_PAYMENT,
        PROCESS_PAYMENT,
        COMPLETE_PAYMENT,
        FAIL_PAYMENT,
        REFUND_PAYMENT,

        CREATE_COUPON,
        UPDATE_COUPON,
        DEACTIVATE_COUPON,
        REDEEM_COUPON
    }

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Action action;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * ID of the user who performed the action.
     * References the user in the User Service.
     */
    @Column(name = "user_id")
    private Long userId;
}
