package com.ga.gymio.config;

import com.ga.gymio.model.FitnessClass;
import com.ga.gymio.model.User;
import com.ga.gymio.model.UserProfile;
import com.ga.gymio.repository.FitnessClassRepository;
import com.ga.gymio.repository.UserProfileRepository;
import com.ga.gymio.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Configuration
@RequiredArgsConstructor
public class DatabaseSeeder {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final FitnessClassRepository fitnessClassRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    CommandLineRunner seedDatabase() {
        return args -> {

            if (userRepository.count() > 0) {
                return;
            }

//            User

            User admin = createUser(
                    "admin@gymio.com",
                    "Admin123!",
                    User.Role.ADMIN
            );

            User trainer1 = createUser(
                    "trainer1@gymio.com",
                    "Trainer123!",
                    User.Role.TRAINER
            );

            User trainer2 = createUser(
                    "trainer2@gymio.com",
                    "Trainer123!",
                    User.Role.TRAINER
            );

            User customer = createUser(
                    "customer@gymio.com",
                    "Customer123!",
                    User.Role.CUSTOMER
            );

//            profiles

            createProfile(
                    admin,
                    "Gymio",
                    "Admin",
                    "+97330000001",
                    LocalDate.of(1995, 1, 1)
            );

            createProfile(
                    trainer1,
                    "Ahmed",
                    "Ali",
                    "+97330000002",
                    LocalDate.of(1998, 5, 10)
            );

            createProfile(
                    trainer2,
                    "Fatima",
                    "Hassan",
                    "+97330000003",
                    LocalDate.of(1997, 8, 20)
            );

            createProfile(
                    customer,
                    "Sara",
                    "Customer",
                    "+97330000004",
                    LocalDate.of(2000, 3, 15)
            );

//            Fitness classes

            LocalDateTime tomorrow =
                    LocalDateTime.now().plusDays(1);

            createFitnessClass(
                    "Morning HIIT",
                    "High intensity interval training.",
                    FitnessClass.Type.HIIT,
                    FitnessClass.Level.MEDIUM,
                    tomorrow.withHour(9).withMinute(0),
                    tomorrow.withHour(10).withMinute(0),
                    20,
                    trainer1
            );

            createFitnessClass(
                    "Morning Yoga",
                    "A relaxing yoga session for all levels.",
                    FitnessClass.Type.FULLBODY,
                    FitnessClass.Level.BEGINNER,
                    tomorrow.withHour(11).withMinute(0),
                    tomorrow.withHour(12).withMinute(0),
                    15,
                    trainer1
            );

            createFitnessClass(
                    "Strength Training",
                    "Strength and resistance training.",
                    FitnessClass.Type.STRENGTHTRAINING,
                    FitnessClass.Level.ADVANCED,
                    tomorrow.withHour(17).withMinute(0),
                    tomorrow.withHour(18).withMinute(0),
                    10,
                    trainer2
            );

        };
    }

    private User createUser(
            String email,
            String password,
            User.Role role) {

        User user = new User();

        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role);
        user.setStatus(User.Status.ACTIVE);
        user.setEmailVerified(true);

        return userRepository.save(user);
    }

    private void createProfile(
            User user,
            String firstName,
            String lastName,
            String phoneNumber,
            LocalDate dateOfBirth) {

        UserProfile profile = new UserProfile();

        profile.setUser(user);
        profile.setFirstName(firstName);
        profile.setLastName(lastName);
        profile.setPhoneNumber(phoneNumber);
        profile.setDateOfBirth(dateOfBirth);

        userProfileRepository.save(profile);
    }

    private FitnessClass createFitnessClass(
            String name,
            String description,
            FitnessClass.Type type,
            FitnessClass.Level level,
            LocalDateTime startTime,
            LocalDateTime endTime,
            Integer capacity,
            User trainer) {

        FitnessClass fitnessClass = new FitnessClass();

        fitnessClass.setName(name);
        fitnessClass.setDescription(description);
        fitnessClass.setType(type);
        fitnessClass.setLevel(level);
        fitnessClass.setStartTime(startTime);
        fitnessClass.setEndTime(endTime);
        fitnessClass.setCapacity(capacity);
        fitnessClass.setStatus(FitnessClass.Status.SCHEDULED);
        fitnessClass.setTrainer(trainer);

        return fitnessClassRepository.save(fitnessClass);
    }
}