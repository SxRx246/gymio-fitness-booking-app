package com.ga.fitness_booking_service.model;

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
        CREATE_CLASS,
        UPDATE_CLASS,
        CANCEL_CLASS,
        DELETE_CLASS,
        BOOK_CLASS,
        CANCEL_BOOKING,
        DELETE_BOOKING,
        COMPLETE_CLASS,
        COMPLETE_BOOKING,
        JOIN_WAITLIST,
        LEAVE_WAITLIST,
        PROMOTE_WAITLIST
    }
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Action action;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "user_id")
    private Long userId;
}