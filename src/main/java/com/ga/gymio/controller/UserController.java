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

/**
 * Provides REST endpoints for user authentication and account management.
 *
 * Supports registration, login, email verification, password recovery,
 * password reset, and authenticated password changes.
 * Rate limiting is applied to registration, login, and password recovery.
 */
@RestController
@AllArgsConstructor
@Slf4j
@RequestMapping(path = "/auth/users")
public class UserController {
    private UserService userService;
    private RateLimiterService rateLimiterService;

    /**
     * Registers a new user and initiates the email verification process.
     *
     * Checks the request's IP address against the rate limiter before
     * creating the user account.
     *
     * @param registerRequest the validated registration details
     * @param request the HTTP request used to obtain the client's IP address
     * @return a response with HTTP 201 Created and a verification message
     * @throws TooManyRequestsException if the registration rate limit is exceeded
     */
    @PostMapping("/register")
    public ResponseEntity<String> createUser(
            @Valid @RequestBody RegisterRequest registerRequest, HttpServletRequest request) {

        String ipAddress = request.getRemoteAddr();

        log.info("Registration request received for email: {}",
                registerRequest.getEmail());

        if (!rateLimiterService.isAllowed(ipAddress)) {
            log.warn("Registration rate limit exceeded for IP: {}", ipAddress);

            throw new TooManyRequestsException(
                    "Too many registration attempts. Please try again later."
            );
        }

        userService.createUser(registerRequest);

        log.info("Registration completed for email: {}",
                registerRequest.getEmail());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body("Registration successful. Please check your email to verify your account.");
    }

    /**
     * Authenticates a user using the supplied login credentials.
     *
     * Applies rate limiting based on the client's IP address before
     * processing the login request.
     *
     * @param loginRequest the validated login credentials
     * @param request the HTTP request used to obtain the client's IP address
     * @return a response containing the login result, such as authentication
     *         tokens and user details
     * @throws TooManyRequestsException if the login rate limit is exceeded
     */
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

        LoginResponse response = userService.loginUser(loginRequest);

        log.info("Login request processed successfully for IP: {}",
                ipAddress);

        return ResponseEntity.ok(response);
    }

    /**
     * Verifies a user's email address using a verification token.
     *
     * @param token the email verification token
     * @return a response confirming successful email verification
     */
    @GetMapping("/verify")
    public ResponseEntity<String> verifyEmail(@RequestParam String token) {
        log.info("Email verification request received");

        userService.verifyEmail(token);

        log.info("Email verification completed successfully");

        return ResponseEntity.ok("Email verified successfully. You can now log in.");
    }

    /**
     * Requests another email verification message for the specified email.
     *
     * @param email the email address associated with the account
     * @return a response indicating that a verification email was requested
     */
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

    /**
     * Requests a password reset for the supplied email address.
     *
     * Applies rate limiting based on the client's IP address. The response
     * does not reveal whether an account exists for the supplied email.
     *
     * @param request the validated password recovery details
     * @param httpRequest the HTTP request used to obtain the client's IP address
     * @return a generic response indicating that the request was processed
     * @throws TooManyRequestsException if the password recovery rate limit is exceeded
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request, HttpServletRequest httpRequest) {

        String ipAddress = httpRequest.getRemoteAddr();

        log.info("Password reset request received for email: {}",
                request.getEmail());

        if (!rateLimiterService.isAllowed(ipAddress)) {
            log.warn("Password reset rate limit exceeded for IP: {}", ipAddress);

            throw new TooManyRequestsException(
                    "Too many password recovery attempts. Please try again later."
            );
        }

        userService.forgotPassword(request);

        log.info("Password reset request processed for email: {}",
                request.getEmail());

        return ResponseEntity.ok(
                "If an account exists with this email, a password reset link has been sent."
        );
    }

    /**
     * Resets a user's password using the supplied reset details.
     *
     * @param request the validated password reset details, including the
     *                required reset token and new password
     * @return a response confirming that the password was reset
     */
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

    /**
     * Changes the password of the currently authenticated user.
     *
     * Access is restricted to authenticated users. The service processes
     * the supplied password change details.
     *
     * @param request the validated password change details
     * @return a response confirming that the password was changed
     */
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