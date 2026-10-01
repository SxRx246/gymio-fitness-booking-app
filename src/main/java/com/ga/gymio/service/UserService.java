package com.ga.gymio.service;

import com.ga.gymio.authentication.JWTUtils;
import com.ga.gymio.authentication.MyUserDetails;
import com.ga.gymio.exception.InformationExistsException;
import com.ga.gymio.model.User;
import com.ga.gymio.model.request.LoginRequest;
import com.ga.gymio.model.response.LoginResponse;
import com.ga.gymio.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
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

    public User createUser(User user){
        if(!userRepository.existsByEmail(user.getEmail()) ){
            user.setPassword(passwordEncoder.encode(user.getPassword()));

            user.setEmailVerified(false);

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

            return savedUser;
        }
        throw new InformationExistsException("User with email: "+ user.getEmail() +" already exists");
    }

    public ResponseEntity<?> loginUser(LoginRequest loginRequest){
        try{
            Authentication authentication = authenticationManager
                    .authenticate(new UsernamePasswordAuthenticationToken(loginRequest.getEmail(),
                            loginRequest.getPassword()));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            myUserDetails = (MyUserDetails) authentication.getPrincipal();
            final String JWT = jwtUtils.generateJwtToken(myUserDetails);
            return ResponseEntity.ok(new LoginResponse(JWT));
        }
        catch (Exception e){
            return ResponseEntity.ok(new LoginResponse("Error: user name or email is incorrect."));
        }
    }
}
