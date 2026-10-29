package com.ga.gymio.config;

import com.ga.gymio.model.AuditLog;
import com.ga.gymio.model.Booking;
import com.ga.gymio.model.Coupon;
import com.ga.gymio.model.CouponRedemption;
import com.ga.gymio.model.FitnessClass;
import com.ga.gymio.model.MembershipPlan;
import com.ga.gymio.model.Payment;
import com.ga.gymio.model.User;
import com.ga.gymio.model.UserProfile;
import com.ga.gymio.model.WaitlistEntry;
import com.ga.gymio.repository.AuditLogRepository;
import com.ga.gymio.repository.BookingRepository;
import com.ga.gymio.repository.CouponRedemptionRepository;
import com.ga.gymio.repository.CouponRepository;
import com.ga.gymio.repository.FitnessClassRepository;
import com.ga.gymio.repository.MembershipPlanRepository;
import com.ga.gymio.repository.PaymentRepository;
import com.ga.gymio.repository.UserProfileRepository;
import com.ga.gymio.repository.UserRepository;
import com.ga.gymio.repository.WaitlistEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

<<<<<<< Updated upstream
=======
/**
 * Populates the database with sample Gymio data when the application runs
 * with the dev profile and the database does not contain any users.
 */
>>>>>>> Stashed changes
@Configuration
@Profile("dev")
@RequiredArgsConstructor
public class DatabaseSeeder {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final FitnessClassRepository fitnessClassRepository;
    private final BookingRepository bookingRepository;
    private final AuditLogRepository auditLogRepository;

    private final MembershipPlanRepository membershipPlanRepository;
    private final PaymentRepository paymentRepository;
    private final CouponRepository couponRepository;
    private final CouponRedemptionRepository couponRedemptionRepository;
    private final WaitlistEntryRepository waitlistEntryRepository;

    private final PasswordEncoder passwordEncoder;

    @Bean
    public CommandLineRunner seedData() {
        return args -> {

            // Preserve the existing seed-on-empty behavior.
            if (userRepository.count() > 0) {
                System.out.println(
                        "Gymio seed data skipped: users already exist."
                );
                return;
            }

            LocalDateTime now = LocalDateTime.now();

            /**
             * Creates a user with an encoded password and saves it to the database.
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

            /**
             * Creates and saves a membership plan with the given name, duration, and price.
             */

            MembershipPlan monthlyPlan = createMembershipPlan(
                    "Monthly Fitness",
                    30,
                    new BigDecimal("25.00")
            );

            MembershipPlan quarterlyPlan = createMembershipPlan(
                    "Quarterly Fitness",
                    90,
                    new BigDecimal("65.00")
            );

            MembershipPlan annualPlan = createMembershipPlan(
                    "Annual Fitness",
                    365,
                    new BigDecimal("240.00")
            );

            /**
             * Assigns a membership plan to a user and updates their membership dates and status.
             */

            assignMembership(
                    customer1,
                    monthlyPlan,
                    LocalDate.now().minusDays(5),
                    User.MembershipStatus.ACTIVE
            );

            assignMembership(
                    customer2,
                    quarterlyPlan,
                    LocalDate.now().minusDays(10),
                    User.MembershipStatus.ACTIVE
            );

            // Customer 3 has no membership yet.

            /**
             * Creates and saves a user profile with the given personal details.
             */

            createProfile(
                    admin, "Sara", "Admin",
                    "39000001", LocalDate.of(1995, 5, 10)
            );

            createProfile(
                    trainer1, "Ahmed", "Hassan",
                    "39000002", LocalDate.of(1990, 3, 15)
            );

            createProfile(
                    trainer2, "Mariam", "Ali",
                    "39000003", LocalDate.of(1992, 8, 20)
            );

            createProfile(
                    customer1, "John", "Smith",
                    "39000004", LocalDate.of(2000, 1, 12)
            );

            createProfile(
                    customer2, "Emma", "Brown",
                    "39000005", LocalDate.of(1999, 7, 25)
            );

            createProfile(
                    customer3, "Omar", "Khalid",
                    "39000006", LocalDate.of(2001, 11, 5)
            );

            /**
             * Creates and saves a scheduled fitness class with the given details and trainer.
             */

            FitnessClass class1 = createClass(
                    "Morning Full Body",
                    "A beginner-friendly full body workout.",
                    FitnessClass.Type.FULLBODY,
                    FitnessClass.Level.BEGINNER,
                    now.plusDays(1).withHour(9).withMinute(0)
                            .withSecond(0).withNano(0),
                    now.plusDays(1).withHour(10).withMinute(0)
                            .withSecond(0).withNano(0),
                    10,
                    trainer1
            );

            FitnessClass class2 = createClass(
                    "HIIT Blast",
                    "High intensity interval training session.",
                    FitnessClass.Type.HIIT,
                    FitnessClass.Level.MEDIUM,
                    now.plusDays(2).withHour(17).withMinute(0)
                            .withSecond(0).withNano(0),
                    now.plusDays(2).withHour(18).withMinute(0)
                            .withSecond(0).withNano(0),
                    8,
                    trainer1
            );

            FitnessClass class3 = createClass(
                    "Strength Training",
                    "Advanced strength and resistance training.",
                    FitnessClass.Type.STRENGTHTRAINING,
                    FitnessClass.Level.ADVANCED,
                    now.plusDays(3).withHour(18).withMinute(0)
                            .withSecond(0).withNano(0),
                    now.plusDays(3).withHour(19).withMinute(0)
                            .withSecond(0).withNano(0),
                    6,
                    trainer2
            );

            FitnessClass class4 = createClass(
                    "Zumba Energy",
                    "Fun beginner-friendly Zumba workout.",
                    FitnessClass.Type.ZUMBA,
                    FitnessClass.Level.BEGINNER,
                    now.plusDays(4).withHour(16).withMinute(0)
                            .withSecond(0).withNano(0),
                    now.plusDays(4).withHour(17).withMinute(0)
                            .withSecond(0).withNano(0),
                    15,
                    trainer2
            );

            FitnessClass class5 = createClass(
                    "Lower Body Focus",
                    "A focused lower body training session.",
                    FitnessClass.Type.LOWERBODY,
                    FitnessClass.Level.MEDIUM,
                    now.plusDays(5).withHour(10).withMinute(0)
                            .withSecond(0).withNano(0),
                    now.plusDays(5).withHour(11).withMinute(0)
                            .withSecond(0).withNano(0),
                    10,
                    trainer2
            );

            FitnessClass completedClass = new FitnessClass();
            completedClass.setName("Completed Upper Body");
            completedClass.setDescription(
                    "A completed upper body training session."
            );
            completedClass.setType(FitnessClass.Type.UPPERBODY);
            completedClass.setLevel(FitnessClass.Level.MEDIUM);
            completedClass.setStartTime(
                    now.minusDays(2).withHour(10).withMinute(0)
            );
            completedClass.setEndTime(
                    now.minusDays(2).withHour(11).withMinute(0)
            );
            completedClass.setCapacity(10);
            completedClass.setStatus(FitnessClass.Status.COMPLETED);
            completedClass.setTrainer(trainer1);
            completedClass = fitnessClassRepository.save(completedClass);

            /**
             * Creates and saves a booking for a customer in a fitness class.
             */

            Booking booking1 = createBooking(
                    customer1, class1, Booking.Status.CONFIRMED
            );

            Booking booking2 = createBooking(
                    customer2, class1, Booking.Status.CONFIRMED
            );

            Booking booking3 = createBooking(
                    customer3, class2, Booking.Status.CONFIRMED
            );

            Booking booking4 = createBooking(
                    customer1, class3, Booking.Status.CONFIRMED
            );

            Booking booking5 = createBooking(
                    customer2, class3, Booking.Status.CANCELLED
            );

            Booking booking6 = createBooking(
                    customer3, completedClass, Booking.Status.COMPLETED
            );

            /**
             * Creates and saves a coupon with its discount, usage limit, and validity period.
             */

            Coupon welcomeCoupon = createCoupon(
                    "WELCOME10",
                    "10% discount for a first eligible purchase.",
                    Coupon.DiscountType.PERCENTAGE,
                    new BigDecimal("10.00"),
                    100,
                    1,
                    now.minusDays(30),
                    now.plusDays(90)
            );

            Coupon fixedCoupon = createCoupon(
                    "FITNESS5",
                    "Five-unit discount on an eligible purchase.",
                    Coupon.DiscountType.FIXED_AMOUNT,
                    new BigDecimal("5.00"),
                    50,
                    0,
                    now.minusDays(10),
                    now.plusDays(60)
            );

            /**
             * Creates and saves a payment with the given customer, details, and status.
             */

            Payment bookingPayment1 = createPayment(
                    customer1,
                    booking1,
                    null,
                    welcomeCoupon,
                    new BigDecimal("22.500"),
                    Payment.PaymentType.BOOKING,
                    Payment.Status.COMPLETED,
                    "GYMIO-BOOKING-001"
            );

            Payment bookingPayment2 = createPayment(
                    customer2,
                    booking2,
                    null,
                    null,
                    new BigDecimal("25.000"),
                    Payment.PaymentType.BOOKING,
                    Payment.Status.COMPLETED,
                    "GYMIO-BOOKING-002"
            );

            createPayment(
                    customer3,
                    null,
                    annualPlan,
                    null,
                    annualPlan.getPrice(),
                    Payment.PaymentType.MEMBERSHIP,
                    Payment.Status.COMPLETED,
                    "GYMIO-MEMBERSHIP-001"
            );

            createPayment(
                    customer3,
                    null,
                    monthlyPlan,
                    null,
                    monthlyPlan.getPrice(),
                    Payment.PaymentType.MEMBERSHIP,
                    Payment.Status.PENDING,
                    "GYMIO-MEMBERSHIP-002"
            );

            createPayment(
                    customer2,
                    null,
                    null,
                    null,
                    new BigDecimal("15.000"),
                    Payment.PaymentType.BOOKING,
                    Payment.Status.FAILED,
                    "GYMIO-FAILED-001"
            );

            /**
             * Creates and saves a record of a customer's coupon redemption.
             */

            createCouponRedemption(
                    welcomeCoupon,
                    customer1,
                    bookingPayment1
            );

            /*
             * WAITLIST ENTRIES
             *
             * These entries demonstrate the waitlist statuses.
             * They use class4, for which these customers have no booking.
             */
            /**
             * Creates and saves a waitlist entry for a customer in a fitness class.
             */
            createWaitlistEntry(
                    customer1,
                    class4,
                    WaitlistEntry.Status.WAITING,
                    null,
                    null
            );

            createWaitlistEntry(
                    customer2,
                    class4,
                    WaitlistEntry.Status.NOTIFIED,
                    now.minusMinutes(10),
                    now.plusHours(2)
            );

            createWaitlistEntry(
                    customer3,
                    class4,
                    WaitlistEntry.Status.CANCELLED,
                    null,
                    null
            );

            /**
             * Creates and saves an audit log record for a user's action.
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
            System.out.println("Membership plans: 3");
            System.out.println("Payments: 5");
            System.out.println("Coupons: 2");
            System.out.println("Coupon redemptions: 1");
            System.out.println("Waitlist entries: 3");
            System.out.println("========================================");
        };
    }

<<<<<<< Updated upstream

    /*
     * =========================
     * USER HELPER
     * =========================
     */

=======
>>>>>>> Stashed changes
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
        user.setEmailVerified(true);

        user.setVerificationToken(null);
        user.setVerificationTokenExpiresAt(null);
        user.setVerificationEmailSentAt(null);
        user.setPasswordResetToken(null);
        user.setPasswordResetTokenExpiresAt(null);

        return userRepository.save(user);
    }

    private MembershipPlan createMembershipPlan(
            String name,
            int durationByDays,
            BigDecimal price
    ) {
        MembershipPlan plan = new MembershipPlan();

        plan.setName(name);
        plan.setDurationByDays(durationByDays);
        plan.setPrice(price);
        plan.setActive(true);

        return membershipPlanRepository.save(plan);
    }

    private void assignMembership(
            User user,
            MembershipPlan plan,
            LocalDate startDate,
            User.MembershipStatus status
    ) {
        user.setMembershipPlan(plan);
        user.setMembershipStartDate(startDate);
        user.setMembershipEndDate(
                startDate.plusDays(plan.getDurationByDays())
        );
        user.setMembershipStatus(status);

        userRepository.save(user);
    }

<<<<<<< Updated upstream
    /*
     * =========================
     * PROFILE HELPER
     * =========================
     */

=======
>>>>>>> Stashed changes
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
        profile.setProfileImage("default-profile.png");
        profile.setUser(user);

        return userProfileRepository.save(profile);
    }

<<<<<<< Updated upstream

    /*
     * =========================
     * FITNESS CLASS HELPER
     * =========================
     */

=======
>>>>>>> Stashed changes
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

<<<<<<< Updated upstream

    /*
     * =========================
     * BOOKING HELPER
     * =========================
     */

=======
>>>>>>> Stashed changes
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

    private Coupon createCoupon(
            String code,
            String description,
            Coupon.DiscountType discountType,
            BigDecimal discountValue,
            int usageLimit,
            int usedCount,
            LocalDateTime startDate,
            LocalDateTime expiresAt
    ) {
        Coupon coupon = new Coupon();

        coupon.setCode(code);
        coupon.setDescription(description);
        coupon.setDiscountType(discountType);
        coupon.setDiscountValue(discountValue);
        coupon.setUsageLimit(usageLimit);
        coupon.setUsedCount(usedCount);
        coupon.setStartDate(startDate);
        coupon.setExpiresAt(expiresAt);
        coupon.setActive(true);

        return couponRepository.save(coupon);
    }

    private Payment createPayment(
            User customer,
            Booking booking,
            MembershipPlan membershipPlan,
            Coupon coupon,
            BigDecimal amount,
            Payment.PaymentType paymentType,
            Payment.Status status,
            String providerPaymentId
    ) {
        Payment payment = new Payment();

        payment.setCustomer(customer);
        payment.setBooking(booking);
        payment.setMembershipPlan(membershipPlan);
        payment.setCoupon(coupon);
        payment.setAmount(amount);
        payment.setCurrency("BHD");
        payment.setPaymentType(paymentType);
        payment.setPaymentMethod(Payment.PaymentMethod.CREDIT_CARD);
        payment.setStatus(status);
        payment.setProviderName("Gymio Demo");
        payment.setProviderPaymentId(providerPaymentId);

        return paymentRepository.save(payment);
    }

    private CouponRedemption createCouponRedemption(
            Coupon coupon,
            User customer,
            Payment payment
    ) {
        CouponRedemption redemption = new CouponRedemption();

        redemption.setCoupon(coupon);
        redemption.setCustomer(customer);
        redemption.setPayment(payment);

        return couponRedemptionRepository.save(redemption);
    }

    private WaitlistEntry createWaitlistEntry(
            User customer,
            FitnessClass fitnessClass,
            WaitlistEntry.Status status,
            LocalDateTime notifiedAt,
            LocalDateTime offerExpiresAt
    ) {
        WaitlistEntry entry = new WaitlistEntry();

        entry.setCustomer(customer);
        entry.setFitnessClass(fitnessClass);
        entry.setStatus(status);
        entry.setNotifiedAt(notifiedAt);
        entry.setOfferExpiresAt(offerExpiresAt);

        return waitlistEntryRepository.save(entry);
    }

<<<<<<< Updated upstream
    /*
     * =========================
     * AUDIT LOG HELPER
     * =========================
     */

=======
>>>>>>> Stashed changes
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