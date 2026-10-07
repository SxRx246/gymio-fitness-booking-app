package com.ga.gymio.controller;

import com.ga.gymio.dto.request.*;
import com.ga.gymio.exception.TooManyRequestsException;
import com.ga.gymio.service.RateLimiterService;
import jakarta.servlet.http.HttpServletRequest;
import com.ga.gymio.dto.response.LoginResponse;
import com.ga.gymio.service.UserService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@AllArgsConstructor
@Slf4j
@RequestMapping(path = "/auth/users")
public class UserController {
    private UserService userService;
    private RateLimiterService rateLimiterService;

    @PostMapping("/register")
    public ResponseEntity<String> createUser(
            @Valid @RequestBody RegisterRequest registerRequest) {

        log.info("Registration request received for email: {}",
                registerRequest.getEmail());

        userService.createUser(registerRequest);

        log.info("Registration completed for email: {}",
                registerRequest.getEmail());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body("Registration successful. Please check your email to verify your account.");
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> loginUser(
            @Valid @RequestBody LoginRequest loginRequest,
            HttpServletRequest request) {

        String ipAddress = request.getRemoteAddr();

        log.info("Login request received from IP: {}", ipAddress);

        if (!rateLimiterService.isAllowed(ipAddress)) {
            throw new TooManyRequestsException(
                    "Too many login attempts. Please try again later."
            );
        }

        log.info("Login request processed successfully for IP: {}",
                ipAddress);

        return ResponseEntity.ok(
                userService.loginUser(loginRequest)
        );
    }

    @GetMapping("/verify")
    public ResponseEntity<String> verifyEmail(@RequestParam String token) {
        log.info("Email verification request received");

        userService.verifyEmail(token);

        log.info("Email verification completed successfully");

        return ResponseEntity.ok("Email verified successfully. You can now log in.");
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<String> resendVerificationEmail(
            @RequestParam String email) {
        log.info("Verification email resend requested for email: {}",
                email);

        userService.resendVerificationEmail(email);

        log.info("Verification email resent successfully for email: {}",
                email);

        return ResponseEntity.ok(
                "A new verification email has been sent."
        );
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {

        log.info("Password reset request received for email: {}",
                request.getEmail());

        userService.forgotPassword(request);

        log.info("Password reset request processed for email: {}",
                request.getEmail());

        return ResponseEntity.ok(
                "If an account exists with this email, a password reset link has been sent."
        );
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {

        log.info("Password reset confirmation received");

        userService.resetPassword(request);

        log.info("Password reset completed successfully");

        return ResponseEntity.ok(
                "Password has been reset successfully. You can now log in."
        );
    }

    @PutMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> changePassword(
            @Valid @RequestBody ChangePasswordRequest request) {

        log.info("Password change request received");

        userService.changePassword(request);

        log.info("Password changed successfully");

        return ResponseEntity.ok(
                "Password changed successfully."
        );
    }




}