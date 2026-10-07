package com.ga.gymio.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

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
        COMPLETE_BOOKING
    }
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Action action;

    @Lob
    @Column(nullable = false)
    private String description;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
}