package com.ga.user_service.config;

import com.ga.user_service.model.User;
import com.ga.user_service.model.UserProfile;
import com.ga.user_service.repository.UserProfileRepository;
import com.ga.user_service.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;

@Configuration
@Profile("docker")
public class UserDataSeeder {

    /**
     * Creates demo users and their profiles when the Docker profile is active.
     *
     * @param userRepository repository for user accounts
     * @param userProfileRepository repository for user profiles
     * @param passwordEncoder encoder used to securely hash passwords
     * @return a runner that inserts the demo data
     */
    @Bean
    public CommandLineRunner seedUsers(
            UserRepository userRepository,
            UserProfileRepository userProfileRepository,
            PasswordEncoder passwordEncoder) {

        return args -> {
            createDemoAccount(
                    userRepository,
                    userProfileRepository,
                    passwordEncoder,
                    "admin@gymio.com",
                    "Admin123!",
                    User.Role.ADMIN,
                    "Gymio",
                    "Admin",
                    "+97330000001",
                    LocalDate.of(1990, 1, 1)
            );

            createDemoAccount(
                    userRepository,
                    userProfileRepository,
                    passwordEncoder,
                    "trainer@gymio.com",
                    "Trainer123!",
                    User.Role.TRAINER,
                    "Ahmed",
                    "Trainer",
                    "+97330000002",
                    LocalDate.of(1995, 5, 15)
            );

            createDemoAccount(
                    userRepository,
                    userProfileRepository,
                    passwordEncoder,
                    "customer@gymio.com",
                    "Customer123!",
                    User.Role.CUSTOMER,
                    "Sara",
                    "Customer",
                    "+97330000003",
                    LocalDate.of(2000, 8, 20)
            );
        };
    }

    /**
     * Creates a demo user if it does not exist and ensures that the user
     * has an associated profile.
     *
     * @param userRepository repository for user accounts
     * @param userProfileRepository repository for user profiles
     * @param passwordEncoder encoder used to hash the password
     * @param email user's email address
     * @param rawPassword user's plain-text demo password
     * @param role user's role
     * @param firstName profile first name
     * @param lastName profile last name
     * @param phoneNumber profile phone number
     * @param dateOfBirth profile date of birth
     */
    private void createDemoAccount(
            UserRepository userRepository,
            UserProfileRepository userProfileRepository,
            PasswordEncoder passwordEncoder,
            String email,
            String rawPassword,
            User.Role role,
            String firstName,
            String lastName,
            String phoneNumber,
            LocalDate dateOfBirth) {

        User user = userRepository.findByEmail(email)
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setEmail(email);
                    newUser.setPassword(passwordEncoder.encode(rawPassword));
                    newUser.setRole(role);
                    newUser.setStatus(User.Status.ACTIVE);
                    newUser.setEmailVerified(true);

                    return userRepository.save(newUser);
                });

        if (!userProfileRepository.existsByUserId(user.getId())) {
            UserProfile profile = new UserProfile();
            profile.setFirstName(firstName);
            profile.setLastName(lastName);
            profile.setPhoneNumber(phoneNumber);
            profile.setDateOfBirth(dateOfBirth);
            profile.setUser(user);

            userProfileRepository.save(profile);
        }
    }
}
