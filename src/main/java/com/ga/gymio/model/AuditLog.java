package com.ga.gymio.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Records user-related actions performed within the Gymio application.
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

    public enum Action {
        SIGNUP,
        VERIFY_EMAIL,

        RESET_PASSWORD,
        CHANGE_PASSWORD,

        CREATE_PROFILE,
        UPDATE_PROFILE,

        CREATE_CLASS,
        UPDATE_CLASS,
        CANCEL_CLASS,
        DELETE_CLASS,

        BOOK_CLASS,
        CANCEL_BOOKING,
        DELETE_BOOKING,

        UPDATE_USER,

        COMPLETE_CLASS,
        COMPLETE_BOOKING,

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
        EXPIRE_MEMBERSHIP
    }
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Action action;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
}