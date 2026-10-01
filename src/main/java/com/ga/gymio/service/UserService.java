package com.ga.gymio.service;

import com.ga.gymio.authentication.JWTUtils;
import com.ga.gymio.authentication.MyUserDetails;
import com.ga.gymio.dto.request.RegisterRequest;
import com.ga.gymio.exception.*;
import com.ga.gymio.model.User;
import com.ga.gymio.dto.request.LoginRequest;
import com.ga.gymio.dto.response.LoginResponse;
import com.ga.gymio.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JWTUtils jwtUtils;
    private final AuthenticationManager authenticationManager;
    private MyUserDetails myUserDetails;
    private final EmailService emailService;

    public void createUser(RegisterRequest request){
        String email = request.getEmail().trim().toLowerCase();

        if(!userRepository.existsByEmail(email) ){
            User user = new User();

            user.setEmail(email);

            user.setPassword(passwordEncoder.encode(request.getPassword()));

            user.setEmailVerified(false);

            user.setStatus(User.Status.ACTIVE);

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
        throw new InformationExistsException("User with email: "+ user.getEmail() +" already exists");
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

    public void resendVerificationEmail(String email) {

        User user = userRepository.findByEmail(email);

        if (user == null) {
            throw new InformationNotFoundException(
                    "User not found"
            );
        }

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

    public LoginResponse loginUser(LoginRequest loginRequest){
        try{
            Authentication authentication = authenticationManager
                    .authenticate(new UsernamePasswordAuthenticationToken(loginRequest.getEmail(),
                            loginRequest.getPassword()));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            myUserDetails = (MyUserDetails) authentication.getPrincipal();
            final String JWT = jwtUtils.generateJwtToken(myUserDetails);
            return new LoginResponse(JWT);
        } catch (AuthenticationException e) {
            throw new InvalidCredentialsException( "Invalid email or password");
        }
    }
}
