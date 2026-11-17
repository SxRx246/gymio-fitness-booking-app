package com.ga.fitness_booking_service.config;

import com.ga.fitness_booking_service.client.UserServiceClient;
import com.ga.fitness_booking_service.dto.response.TrainerValidationResponse;
import com.ga.fitness_booking_service.model.FitnessClass;
import com.ga.fitness_booking_service.repository.FitnessClassRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.time.LocalDateTime;
import java.util.List;

@Configuration
@Profile("docker")
public class FitnessDataSeeder {

    private static final Logger log =
            LoggerFactory.getLogger(FitnessDataSeeder.class);

    @Bean
    CommandLineRunner seedFitnessClasses(
            FitnessClassRepository fitnessClassRepository,
            UserServiceClient userServiceClient) {

        return args -> {

            TrainerValidationResponse trainer =
                    userServiceClient.validateTrainerByEmail("trainer@gymio.com")
                            .orElseThrow(() -> new IllegalStateException(
                                    "Demo trainer was not found in User Service"));

            if (!"TRAINER".equals(trainer.role())
                    || !"ACTIVE".equals(trainer.status())
                    || !trainer.emailVerified()) {
                throw new IllegalStateException(
                        "Demo trainer is not eligible to teach classes");
            }

            Long trainerId = trainer.id();

            LocalDateTime baseTime = LocalDateTime.now()
                    .plusDays(1)
                    .withHour(9)
                    .withMinute(0)
                    .withSecond(0)
                    .withNano(0);

            List<FitnessClass> classes = List.of(

                    createClass(
                            "Full Body Workout",
                            "A complete workout targeting all major muscle groups.",
                            FitnessClass.Type.FULLBODY,
                            FitnessClass.Level.BEGINNER,
                            baseTime,
                            15,
                            trainerId
                    ),

                    createClass(
                            "Upper Body Strength",
                            "Build upper body strength through guided exercises.",
                            FitnessClass.Type.UPPERBODY,
                            FitnessClass.Level.MEDIUM,
                            baseTime.plusHours(2),
                            12,
                            trainerId
                    ),

                    createClass(
                            "Lower Body Training",
                            "Improve lower body strength and endurance.",
                            FitnessClass.Type.LOWERBODY,
                            FitnessClass.Level.BEGINNER,
                            baseTime.plusDays(1),
                            15,
                            trainerId
                    ),

                    createClass(
                            "HIIT Challenge",
                            "High-intensity interval training for endurance and fitness.",
                            FitnessClass.Type.HIIT,
                            FitnessClass.Level.ADVANCED,
                            baseTime.plusDays(1).plusHours(2),
                            10,
                            trainerId
                    ),

                    createClass(
                            "Zumba Fitness",
                            "An energetic dance-based fitness session.",
                            FitnessClass.Type.ZUMBA,
                            FitnessClass.Level.BEGINNER,
                            baseTime.plusDays(2),
                            20,
                            trainerId
                    ),

                    createClass(
                            "Glutes and Core",
                            "A focused workout for core stability and glute strength.",
                            FitnessClass.Type.GLUTESANDCORE,
                            FitnessClass.Level.MEDIUM,
                            baseTime.plusDays(2).plusHours(2),
                            12,
                            trainerId
                    ),

                    createClass(
                            "Advanced Strength Training",
                            "A challenging strength session using progressive resistance.",
                            FitnessClass.Type.STRENGTHTRAINING,
                            FitnessClass.Level.ADVANCED,
                            baseTime.plusDays(3),
                            10,
                            trainerId
                    )
            );

            int createdCount = 0;

            for (FitnessClass fitnessClass : classes) {

                boolean exists = fitnessClassRepository
                        .existsByNameAndTrainerId(
                                fitnessClass.getName(),
                                trainerId
                        );

                if (exists) {
                    log.info("Class already exists: {}",
                            fitnessClass.getName());
                    continue;
                }

                fitnessClassRepository.save(fitnessClass);
                createdCount++;

                log.info("Created fitness class: {}",
                        fitnessClass.getName());
            }

            log.info(
                    "Fitness data seeding completed. Created {} classes.",
                    createdCount
            );
        };
    }

    /**
     * Creates a scheduled fitness class.
     *
     * @param name class name
     * @param description class description
     * @param type fitness class type
     * @param level difficulty level
     * @param startTime scheduled start time
     * @param capacity maximum number of participants
     * @param trainerId assigned trainer ID
     * @return configured fitness class
     */
    private static FitnessClass createClass(
            String name,
            String description,
            FitnessClass.Type type,
            FitnessClass.Level level,
            LocalDateTime startTime,
            int capacity,
            Long trainerId) {

        FitnessClass fitnessClass = new FitnessClass();

        fitnessClass.setName(name);
        fitnessClass.setDescription(description);
        fitnessClass.setType(type);
        fitnessClass.setLevel(level);
        fitnessClass.setStartTime(startTime);
        fitnessClass.setEndTime(startTime.plusHours(1));
        fitnessClass.setCapacity(capacity);
        fitnessClass.setStatus(FitnessClass.Status.SCHEDULED);
        fitnessClass.setTrainerId(trainerId);

        return fitnessClass;
    }
}
