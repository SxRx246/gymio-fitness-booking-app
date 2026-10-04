package com.ga.gymio.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

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

    @JsonIgnore
    private String verificationToken;

    @JsonIgnore
    private LocalDateTime verificationTokenExpiresAt;

    @JsonIgnore
    private LocalDateTime verificationEmailSentAt;

    private boolean emailVerified;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
