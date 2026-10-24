package com.ga.gymio.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class MembershipPlanRequest {

    @NotBlank(message = "Membership plan name is required")
    private String name;

    @NotNull(message = "Membership duration is required")
    @Min(value = 1, message = "Membership duration must be at least one day")
    private Integer durationByDays;

    @NotNull(message = "Membership price is required")
    @DecimalMin(value = "0.00", inclusive = false, message = "Membership price must be greater than zero")
    private BigDecimal price;

}
