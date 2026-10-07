package com.ga.gymio.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ForgotPasswordRequest {

    @NotBlank(message = "Please enter your email address")
    @Email(message = "Please enter a valid email address")
    @Size(max = 254, message = "Email address is too long")
    private String email;
}

