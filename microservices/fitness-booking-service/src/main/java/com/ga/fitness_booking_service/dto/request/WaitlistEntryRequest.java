package com.ga.fitness_booking_service.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class WaitlistEntryRequest {

    @NotNull(message = "Fitness class ID is required")
    @Positive(message = "Fitness class ID must be positive")
    private Long fitnessClassId;

}
