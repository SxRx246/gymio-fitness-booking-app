package com.ga.gymio.dto.request;

import com.ga.gymio.model.FitnessClass;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class FitnessClassRequest {

    @NotBlank(message = "Class name is required")
    @Size(max = 100, message = "Class name cannot exceed 100 characters")
    private String name;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    @NotNull(message = "Class type is required")
    private FitnessClass.Type type;

    @NotNull(message = "Class level is required")
    private FitnessClass.Level level;

    @NotNull(message = "Start time is required")
    @Future(message = "Start time must be in the future")
    private LocalDateTime startTime;

    @NotNull(message = "End time is required")
    private LocalDateTime endTime;

    @NotNull(message = "Capacity is required")
    @Positive(message = "Capacity must be greater than 0")
    private Integer capacity;

    @NotBlank(message = "Trainer email is required")
    private String trainerEmail;
}