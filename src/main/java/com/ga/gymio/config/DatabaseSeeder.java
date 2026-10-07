package com.ga.gymio.config;

import com.ga.gymio.model.AuditLog;
import com.ga.gymio.model.Booking;
import com.ga.gymio.model.FitnessClass;
import com.ga.gymio.model.User;
import com.ga.gymio.model.UserProfile;
import com.ga.gymio.repository.AuditLogRepository;
import com.ga.gymio.repository.BookingRepository;
import com.ga.gymio.repository.FitnessClassRepository;
import com.ga.gymio.repository.UserProfileRepository;
import com.ga.gymio.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Configuration
@Profile("dev")
@RequiredArgsConstructor
public class DatabaseSeeder {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final FitnessClassRepository fitnessClassRepository;
    private final BookingRepository bookingRepository;
    private final AuditLogRepository auditLogRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public org.springframework.boot.CommandLineRunner seedData() {

        return args -> {

            // Prevent the seed data from being inserted again
            if (userRepository.count() > 0) {
                return;
            }

            /*
             * =========================
             * USERS
             * =========================
             */

            User admin = createUser(
                    "admin@gymio.com",
                    "Admin@123",
                    User.Role.ADMIN
            );

            User trainer1 = createUser(
                    "trainer1@gymio.com",
                    "Trainer@123",
                    User.Role.TRAINER
            );

            User trainer2 = createUser(
                    "trainer2@gymio.com",
                    "Trainer@456",
                    User.Role.TRAINER
            );

            User customer1 = createUser(
                    "customer1@gymio.com",
                    "Customer@123",
                    User.Role.CUSTOMER
            );

            User customer2 = createUser(
                    "customer2@gymio.com",
                    "Customer@456",
                    User.Role.CUSTOMER
            );

            User customer3 = createUser(
                    "customer3@gymio.com",
                    "Customer@789",
                    User.Role.CUSTOMER
            );


            /*
             * =========================
             * USER PROFILES
             * =========================
             */

            createProfile(
                    admin,
                    "Sara",
                    "Admin",
                    "39000001",
                    LocalDate.of(1995, 5, 10)
            );

            createProfile(
                    trainer1,
                    "Ahmed",
                    "Hassan",
                    "39000002",
                    LocalDate.of(1990, 3, 15)
            );

            createProfile(
                    trainer2,
                    "Mariam",
                    "Ali",
                    "39000003",
                    LocalDate.of(1992, 8, 20)
            );

            createProfile(
                    customer1,
                    "John",
                    "Smith",
                    "39000004",
                    LocalDate.of(2000, 1, 12)
            );

            createProfile(
                    customer2,
                    "Emma",
                    "Brown",
                    "39000005",
                    LocalDate.of(1999, 7, 25)
            );

            createProfile(
                    customer3,
                    "Omar",
                    "Khalid",
                    "39000006",
                    LocalDate.of(2001, 11, 5)
            );


            /*
             * =========================
             * FITNESS CLASSES
             * =========================
             */

            LocalDateTime now = LocalDateTime.now();

            FitnessClass class1 = createClass(
                    "Morning Full Body",
                    "A beginner-friendly full body workout.",
                    FitnessClass.Type.FULLBODY,
                    FitnessClass.Level.BEGINNER,
                    now.plusDays(1).withHour(9).withMinute(0).withSecond(0).withNano(0),
                    now.plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0),
                    10,
                    trainer1
            );

            FitnessClass class2 = createClass(
                    "HIIT Blast",
                    "High intensity interval training session.",
                    FitnessClass.Type.HIIT,
                    FitnessClass.Level.MEDIUM,
                    now.plusDays(2).withHour(17).withMinute(0).withSecond(0).withNano(0),
                    now.plusDays(2).withHour(18).withMinute(0).withSecond(0).withNano(0),
                    8,
                    trainer1
            );

            FitnessClass class3 = createClass(
                    "Strength Training",
                    "Advanced strength and resistance training.",
                    FitnessClass.Type.STRENGTHTRAINING,
                    FitnessClass.Level.ADVANCED,
                    now.plusDays(3).withHour(18).withMinute(0).withSecond(0).withNano(0),
                    now.plusDays(3).withHour(19).withMinute(0).withSecond(0).withNano(0),
                    6,
                    trainer2
            );

            FitnessClass class4 = createClass(
                    "Zumba Energy",
                    "Fun beginner-friendly Zumba workout.",
                    FitnessClass.Type.ZUMBA,
                    FitnessClass.Level.BEGINNER,
                    now.plusDays(4).withHour(16).withMinute(0).withSecond(0).withNano(0),
                    now.plusDays(4).withHour(17).withMinute(0).withSecond(0).withNano(0),
                    15,
                    trainer2
            );

            FitnessClass class5 = createClass(
                    "Lower Body Focus",
                    "A focused lower body training session.",
                    FitnessClass.Type.LOWERBODY,
                    FitnessClass.Level.MEDIUM,
                    now.plusDays(5).withHour(10).withMinute(0).withSecond(0).withNano(0),
                    now.plusDays(5).withHour(11).withMinute(0).withSecond(0).withNano(0),
                    10,
                    trainer2
            );


            /*
             * =========================
             * COMPLETED CLASS
             * =========================
             *
             * Useful for demonstrating class statuses.
             */

            FitnessClass completedClass = new FitnessClass();

            completedClass.setName("Completed Upper Body");
            completedClass.setDescription(
                    "A completed upper body training session."
            );
            completedClass.setType(FitnessClass.Type.UPPERBODY);
            completedClass.setLevel(FitnessClass.Level.MEDIUM);
            completedClass.setStartTime(now.minusDays(2).withHour(10).withMinute(0));
            completedClass.setEndTime(now.minusDays(2).withHour(11).withMinute(0));
            completedClass.setCapacity(10);
            completedClass.setStatus(FitnessClass.Status.COMPLETED);
            completedClass.setTrainer(trainer1);

            completedClass = fitnessClassRepository.save(completedClass);


            /*
             * =========================
             * BOOKINGS
             * =========================
             */

            Booking booking1 = createBooking(
                    customer1,
                    class1,
                    Booking.Status.CONFIRMED
            );

            Booking booking2 = createBooking(
                    customer2,
                    class1,
                    Booking.Status.CONFIRMED
            );

            Booking booking3 = createBooking(
                    customer3,
                    class2,
                    Booking.Status.CONFIRMED
            );

            Booking booking4 = createBooking(
                    customer1,
                    class3,
                    Booking.Status.CONFIRMED
            );

            // Cancelled booking for demonstration
            Booking booking5 = createBooking(
                    customer2,
                    class3,
                    Booking.Status.CANCELLED
            );

            // Completed booking for demonstration
            Booking booking6 = createBooking(
                    customer3,
                    completedClass,
                    Booking.Status.COMPLETED
            );


            /*
             * =========================
             * AUDIT LOGS
             * =========================
             */

            createAuditLog(
                    AuditLog.Action.SIGNUP,
                    "User " + admin.getId() + " signed up",
                    admin
            );

            createAuditLog(
                    AuditLog.Action.SIGNUP,
                    "User " + trainer1.getId() + " signed up",
                    trainer1
            );

            createAuditLog(
                    AuditLog.Action.SIGNUP,
                    "User " + trainer2.getId() + " signed up",
                    trainer2
            );

            createAuditLog(
                    AuditLog.Action.SIGNUP,
                    "User " + customer1.getId() + " signed up",
                    customer1
            );

            createAuditLog(
                    AuditLog.Action.SIGNUP,
                    "User " + customer2.getId() + " signed up",
                    customer2
            );

            createAuditLog(
                    AuditLog.Action.SIGNUP,
                    "User " + customer3.getId() + " signed up",
                    customer3
            );


            createAuditLog(
                    AuditLog.Action.VERIFY_EMAIL,
                    "User " + admin.getId() + " verified their email",
                    admin
            );

            createAuditLog(
                    AuditLog.Action.VERIFY_EMAIL,
                    "User " + trainer1.getId() + " verified their email",
                    trainer1
            );

            createAuditLog(
                    AuditLog.Action.VERIFY_EMAIL,
                    "User " + trainer2.getId() + " verified their email",
                    trainer2
            );


            createAuditLog(
                    AuditLog.Action.CREATE_PROFILE,
                    "Profile created for User " + admin.getId(),
                    admin
            );

            createAuditLog(
                    AuditLog.Action.CREATE_PROFILE,
                    "Profile created for User " + trainer1.getId(),
                    trainer1
            );

            createAuditLog(
                    AuditLog.Action.CREATE_PROFILE,
                    "Profile created for User " + trainer2.getId(),
                    trainer2
            );

            createAuditLog(
                    AuditLog.Action.CREATE_PROFILE,
                    "Profile created for User " + customer1.getId(),
                    customer1
            );

            createAuditLog(
                    AuditLog.Action.CREATE_PROFILE,
                    "Profile created for User " + customer2.getId(),
                    customer2
            );

            createAuditLog(
                    AuditLog.Action.CREATE_PROFILE,
                    "Profile created for User " + customer3.getId(),
                    customer3
            );


            createAuditLog(
                    AuditLog.Action.CREATE_CLASS,
                    "Trainer " + trainer1.getId()
                            + " created Fitness Class " + class1.getId(),
                    trainer1
            );

            createAuditLog(
                    AuditLog.Action.CREATE_CLASS,
                    "Trainer " + trainer1.getId()
                            + " created Fitness Class " + class2.getId(),
                    trainer1
            );

            createAuditLog(
                    AuditLog.Action.CREATE_CLASS,
                    "Trainer " + trainer2.getId()
                            + " created Fitness Class " + class3.getId(),
                    trainer2
            );

            createAuditLog(
                    AuditLog.Action.CREATE_CLASS,
                    "Trainer " + trainer2.getId()
                            + " created Fitness Class " + class4.getId(),
                    trainer2
            );

            createAuditLog(
                    AuditLog.Action.CREATE_CLASS,
                    "Trainer " + trainer2.getId()
                            + " created Fitness Class " + class5.getId(),
                    trainer2
            );


            createAuditLog(
                    AuditLog.Action.BOOK_CLASS,
                    "User " + customer1.getId()
                            + " booked Fitness Class " + class1.getId(),
                    customer1
            );

            createAuditLog(
                    AuditLog.Action.BOOK_CLASS,
                    "User " + customer2.getId()
                            + " booked Fitness Class " + class1.getId(),
                    customer2
            );

            createAuditLog(
                    AuditLog.Action.BOOK_CLASS,
                    "User " + customer3.getId()
                            + " booked Fitness Class " + class2.getId(),
                    customer3
            );

            createAuditLog(
                    AuditLog.Action.BOOK_CLASS,
                    "User " + customer1.getId()
                            + " booked Fitness Class " + class3.getId(),
                    customer1
            );


            createAuditLog(
                    AuditLog.Action.CANCEL_BOOKING,
                    "User " + customer2.getId()
                            + " cancelled Booking " + booking5.getId(),
                    customer2
            );

            createAuditLog(
                    AuditLog.Action.COMPLETE_BOOKING,
                    "Booking " + booking6.getId() + " was completed",
                    customer3
            );

            createAuditLog(
                    AuditLog.Action.COMPLETE_CLASS,
                    "Fitness Class " + completedClass.getId()
                            + " was completed",
                    trainer1
            );

            System.out.println("========================================");
            System.out.println("Gymio seed data created successfully!");
            System.out.println("========================================");
        };
    }


    /*
     * =========================
     * USER HELPER
     * =========================
     */

    private User createUser(
            String email,
            String password,
            User.Role role
    ) {

        User user = new User();

        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role);
        user.setStatus(User.Status.ACTIVE);

        // Seeded users are already verified
        user.setEmailVerified(true);

        // No verification/reset tokens
        user.setVerificationToken(null);
        user.setVerificationTokenExpiresAt(null);
        user.setVerificationEmailSentAt(null);
        user.setPasswordResetToken(null);
        user.setPasswordResetTokenExpiresAt(null);

        return userRepository.save(user);
    }


    /*
     * =========================
     * PROFILE HELPER
     * =========================
     */

    private UserProfile createProfile(
            User user,
            String firstName,
            String lastName,
            String phoneNumber,
            LocalDate dateOfBirth
    ) {

        UserProfile profile = new UserProfile();

        profile.setFirstName(firstName);
        profile.setLastName(lastName);
        profile.setPhoneNumber(phoneNumber);
        profile.setDateOfBirth(dateOfBirth);

        /*
         * This is the value stored in the profileImage column.
         * Replace it later with the actual stored image value
         * if your upload implementation uses another format.
         */
        profile.setProfileImage("default-profile.png");

        profile.setUser(user);

        return userProfileRepository.save(profile);
    }


    /*
     * =========================
     * FITNESS CLASS HELPER
     * =========================
     */

    private FitnessClass createClass(
            String name,
            String description,
            FitnessClass.Type type,
            FitnessClass.Level level,
            LocalDateTime startTime,
            LocalDateTime endTime,
            Integer capacity,
            User trainer
    ) {

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


    /*
     * =========================
     * BOOKING HELPER
     * =========================
     */

    private Booking createBooking(
            User customer,
            FitnessClass fitnessClass,
            Booking.Status status
    ) {

        Booking booking = new Booking();

        booking.setCustomer(customer);
        booking.setFitnessClass(fitnessClass);
        booking.setStatus(status);

        return bookingRepository.save(booking);
    }


    /*
     * =========================
     * AUDIT LOG HELPER
     * =========================
     */

    private void createAuditLog(
            AuditLog.Action action,
            String description,
            User user
    ) {

        AuditLog auditLog = new AuditLog();

        auditLog.setAction(action);
        auditLog.setDescription(description);
        auditLog.setUser(user);

        auditLogRepository.save(auditLog);
    }
}