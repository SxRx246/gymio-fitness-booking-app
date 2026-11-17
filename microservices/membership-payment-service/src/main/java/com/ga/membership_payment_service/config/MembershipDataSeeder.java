package com.ga.membership_payment_service.config;

import com.ga.membership_payment_service.model.Coupon;
import com.ga.membership_payment_service.model.MembershipPlan;
import com.ga.membership_payment_service.repository.CouponRepository;
import com.ga.membership_payment_service.repository.MembershipPlanRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Configuration
@Profile("docker")
public class MembershipDataSeeder {

    private static final Logger log =
            LoggerFactory.getLogger(MembershipDataSeeder.class);

    @Bean
    CommandLineRunner seedMembershipData(
            MembershipPlanRepository membershipPlanRepository,
            CouponRepository couponRepository) {

        return args -> {
            seedPlans(membershipPlanRepository);
            seedCoupons(couponRepository);

            log.info("Membership and coupon data seeding completed.");
        };
    }

    private void seedPlans(MembershipPlanRepository repository) {

        List<MembershipPlan> plans = List.of(
                createPlan("Basic", 30, "15.000"),
                createPlan("Quarterly", 90, "40.000"),
                createPlan("Half-Yearly", 180, "70.000"),
                createPlan("Annual", 365, "120.000")
        );

        for (MembershipPlan plan : plans) {
            if (repository.existsByName(plan.getName())) {
                log.info("Membership plan already exists: {}", plan.getName());
                continue;
            }

            repository.save(plan);
            log.info("Created membership plan: {}", plan.getName());
        }
    }

    private MembershipPlan createPlan(
            String name, int durationByDays, String price) {

        MembershipPlan plan = new MembershipPlan();
        plan.setName(name);
        plan.setDurationByDays(durationByDays);
        plan.setPrice(new BigDecimal(price));
        plan.setActive(true);

        return plan;
    }

    private void seedCoupons(CouponRepository repository) {

        LocalDateTime now = LocalDateTime.now();

        List<Coupon> coupons = List.of(
                createCoupon(
                        "WELCOME10",
                        "10% discount for new customers",
                        Coupon.DiscountType.PERCENTAGE,
                        "10.00", 100, now, now.plusMonths(6)
                ),
                createCoupon(
                        "GYM20",
                        "20% discount on eligible purchases",
                        Coupon.DiscountType.PERCENTAGE,
                        "20.00", 50, now, now.plusMonths(3)
                ),
                createCoupon(
                        "SAVE5",
                        "BHD 5 discount",
                        Coupon.DiscountType.FIXED_AMOUNT,
                        "5.00", 100, now, now.plusMonths(6)
                ),
                createCoupon(
                        "SAVE10",
                        "BHD 10 discount",
                        Coupon.DiscountType.FIXED_AMOUNT,
                        "10.00", 30, now, now.plusMonths(3)
                )
        );

        for (Coupon coupon : coupons) {
            if (repository.existsByCode(coupon.getCode())) {
                log.info("Coupon already exists: {}", coupon.getCode());
                continue;
            }

            repository.save(coupon);
            log.info("Created coupon: {}", coupon.getCode());
        }
    }

    private Coupon createCoupon(
            String code,
            String description,
            Coupon.DiscountType discountType,
            String discountValue,
            int usageLimit,
            LocalDateTime startDate,
            LocalDateTime expiresAt) {

        Coupon coupon = new Coupon();
        coupon.setCode(code);
        coupon.setDescription(description);
        coupon.setDiscountType(discountType);
        coupon.setDiscountValue(new BigDecimal(discountValue));
        coupon.setUsageLimit(usageLimit);
        coupon.setUsedCount(0);
        coupon.setStartDate(startDate);
        coupon.setExpiresAt(expiresAt);
        coupon.setActive(true);

        return coupon;
    }
}
