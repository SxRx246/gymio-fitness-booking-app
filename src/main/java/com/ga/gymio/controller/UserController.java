package com.ga.gymio.controller;

import com.ga.gymio.dto.request.*;
import com.ga.gymio.model.User;
import com.ga.gymio.dto.response.LoginResponse;
import com.ga.gymio.service.UserService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@AllArgsConstructor
@RequestMapping(path = "/auth/users")
public class UserController {
    private UserService userService;

    @PostMapping("/register")
    public ResponseEntity<String> createUser(
            @Valid @RequestBody RegisterRequest registerRequest) {

        userService.createUser(registerRequest);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body("Registration successful. Please check your email to verify your account.");
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> loginUser(@Valid @RequestBody LoginRequest loginRequest) {
        System.out.println("Controller Calling Login() ==>");
        return ResponseEntity.ok(userService.loginUser(loginRequest));
    }

    @GetMapping("/verify")
    public ResponseEntity<String> verifyEmail(@RequestParam String token) {
        System.out.println("Controller Calling verifyEmail() ==>");
        userService.verifyEmail(token);
        return ResponseEntity.ok("Email verified successfully. You can now log in.");
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<String> resendVerificationEmail(
            @RequestParam String email) {
        System.out.println("Controller Calling resendVerificationEmail() ==>");
        userService.resendVerificationEmail(email);

        return ResponseEntity.ok(
                "A new verification email has been sent."
        );
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {

        userService.forgotPassword(request);

        return ResponseEntity.ok(
                "If an account exists with this email, a password reset link has been sent."
        );
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {

        userService.resetPassword(request);

        return ResponseEntity.ok(
                "Password has been reset successfully. You can now log in."
        );
    }

    @PutMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> changePassword(
            @Valid @RequestBody ChangePasswordRequest request) {

        userService.changePassword(request);

        return ResponseEntity.ok(
                "Password changed successfully."
        );
    }




}