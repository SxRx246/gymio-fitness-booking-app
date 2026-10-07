package com.ga.gymio.service;

import com.ga.gymio.authentication.JWTUtils;
import com.ga.gymio.authentication.MyUserDetails;
import com.ga.gymio.dto.request.*;
import com.ga.gymio.exception.*;
import com.ga.gymio.model.User;
import com.ga.gymio.dto.response.LoginResponse;
import com.ga.gymio.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JWTUtils jwtUtils;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;

    public void createUser(RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmail(email)) {
            throw new InformationExistsException("An account with this email address already exists.");
        }

        User user = new User();

        user.setEmail(email);

        user.setPassword(passwordEncoder.encode(request.getPassword()));

        user.setEmailVerified(false);

        user.setStatus(User.Status.ACTIVE);

        user.setRole(User.Role.CUSTOMER);

        String token = UUID.randomUUID().toString();

        user.setVerificationToken(token);

        user.setVerificationTokenExpiresAt(
                LocalDateTime.now().plusHours(24)
        );

        user.setVerificationEmailSentAt(
                LocalDateTime.now()
        );

        User savedUser = userRepository.save(user);

        emailService.sendVerificationEmail(
                savedUser.getEmail(),
                token
        );
    }

    public void verifyEmail(String token) {

        User user = userRepository
                .findByVerificationToken(token)
                .orElseThrow(() ->
                        new InvalidTokenException(
                                "Invalid verification token"
                        )
                );

        if (user.isEmailVerified()) {
            throw new InformationExistsException(
                    "Email is already verified"
            );
        }

        if (user.getVerificationTokenExpiresAt()
                .isBefore(LocalDateTime.now())) {

            throw new InvalidTokenException(
                    "Verification token has expired"
            );
        }

        user.setEmailVerified(true);

        user.setVerificationToken(null);

        user.setVerificationTokenExpiresAt(null);

        userRepository.save(user);
    }

    public void resendVerificationEmail(String email) throws UsernameNotFoundException {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new UsernameNotFoundException("User not found")
                );

        if (user.isEmailVerified()) {
            throw new InformationExistsException(
                    "Email is already verified"
            );
        }

        LocalDateTime now = LocalDateTime.now();

        if (user.getVerificationEmailSentAt() != null) {

            LocalDateTime nextAllowedTime =
                    user.getVerificationEmailSentAt()
                            .plusMinutes(1);

            if (now.isBefore(nextAllowedTime)) {

                throw new TooManyRequestsException(
                        "Please wait before requesting another verification email."
                );
            }
        }

        String newToken = UUID.randomUUID().toString();

        user.setVerificationToken(newToken);

        user.setVerificationTokenExpiresAt(
                now.plusHours(24)
        );

        user.setVerificationEmailSentAt(now);

        userRepository.save(user);

        emailService.sendVerificationEmail(
                user.getEmail(),
                newToken
        );
    }

    public LoginResponse loginUser(LoginRequest loginRequest) {
        try {
            Authentication authentication = authenticationManager
                    .authenticate(new UsernamePasswordAuthenticationToken(loginRequest.getEmail(),
                            loginRequest.getPassword()));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            MyUserDetails myUserDetails = (MyUserDetails) authentication.getPrincipal();
            final String JWT = jwtUtils.generateJwtToken(myUserDetails);
            return new LoginResponse(JWT);
        } catch (AuthenticationException e) {
            throw new InvalidCredentialsException("Invalid email or password");
        }
    }


    private String generateResetToken() {

        SecureRandom secureRandom = new SecureRandom();

        byte[] tokenBytes = new byte[32];

        secureRandom.nextBytes(tokenBytes);

        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }

    public void forgotPassword(ForgotPasswordRequest request) {

        String email = request.getEmail()
                .trim()
                .toLowerCase();

        Optional<User> optionalUser =
                userRepository.findByEmail(email);

        if (optionalUser.isEmpty()) {
            return;
        }

        User user = optionalUser.get();

        String token = generateResetToken();

        user.setPasswordResetToken(token);

        user.setPasswordResetTokenExpiresAt(
                LocalDateTime.now().plusMinutes(15)
        );

        userRepository.save(user);

        emailService.sendPasswordResetEmail(
                user.getEmail(),
                token
        );
    }

    public void resetPassword(ResetPasswordRequest request) {

        User user = userRepository
                .findByPasswordResetToken(request.getToken())
                .orElseThrow(() ->
                        new InvalidTokenException(
                                "Invalid password reset token"
                        )
                );

        if (user.getPasswordResetTokenExpiresAt() == null
                || user.getPasswordResetTokenExpiresAt()
                .isBefore(LocalDateTime.now())) {

            throw new InvalidTokenException(
                    "Password reset token has expired"
            );
        }

        user.setPassword(
                passwordEncoder.encode(request.getNewPassword())
        );

        user.setPasswordResetToken(null);
        user.setPasswordResetTokenExpiresAt(null);

        userRepository.save(user);
    }

    public void changePassword(ChangePasswordRequest request) {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        MyUserDetails myUserDetails =
                (MyUserDetails) authentication.getPrincipal();

        User currentUser = myUserDetails.getUser();

        if (!passwordEncoder.matches(
                request.getCurrentPassword(),
                currentUser.getPassword())) {

            throw new IllegalArgumentException(
                    "Current password is incorrect"
            );
        }

        if (passwordEncoder.matches(
                request.getNewPassword(),
                currentUser.getPassword())) {

            throw new IllegalArgumentException(
                    "New password must be different from your current password"
            );
        }

        currentUser.setPassword(
                passwordEncoder.encode(request.getNewPassword())
        );

        userRepository.save(currentUser);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public void updateUser(Long id, AdminUserUpdateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() ->
                        new InformationNotFoundException(
                                "User with id " + id + " not found")
                );

        if (request.getRole() != null) {
            user.setRole(request.getRole());
        }

        if (request.getStatus() != null) {
            user.setStatus(request.getStatus());
        }

        userRepository.save(user);
    }

}
