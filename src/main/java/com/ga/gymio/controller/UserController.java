package com.ga.gymio.controller;

import com.ga.gymio.dto.request.RegisterRequest;
import com.ga.gymio.model.User;
import com.ga.gymio.dto.request.LoginRequest;
import com.ga.gymio.dto.response.LoginResponse;
import com.ga.gymio.service.UserService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<LoginResponse> loginUser(@RequestBody LoginRequest loginRequest) {
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

}