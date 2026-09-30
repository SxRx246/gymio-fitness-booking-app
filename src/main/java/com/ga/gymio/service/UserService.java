package com.ga.gymio.service;

import com.ga.gymio.exception.InformationExistsException;
import com.ga.gymio.model.User;
import com.ga.gymio.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public User createUser(User user){
        if(
//                user.isEmailVerified() &&
                        !userRepository.existsByEmail(user.getEmail()) ){
            user.setPassword(passwordEncoder.encode(user.getPassword()));
            return userRepository.save(user);
        }
        throw new InformationExistsException("User with email: "+ user.getEmail() +" already exists");
    }
}
