package com.ga.gymio.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;



@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "fitness_classes")
public class FitnessClass{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String description;

    public enum Type {
        FULLBODY,
        UPPERBODY,
        LOWERBODY,
        GLUTESANDCORE,
        HIIT,
        STRENGTHTRAINING,
        ZUMBA
    }
    @Enumerated(EnumType.STRING)
    private Type type;

    public enum Level {
        BEGINNER,
        MEDIUM,
        ADVANCED
    }
    @Enumerated(EnumType.STRING)
    private Level level;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Integer capacity;

    public enum Status {
        SCHEDULED,
        IN_PROGRESS,
        COMPLETED,
        CANCELLED
    }
    @Enumerated(EnumType.STRING)
    private Status status;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @ManyToOne
    @JoinColumn(name = "trainer_id")
    private User trainer;
}
