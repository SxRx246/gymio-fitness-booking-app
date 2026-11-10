package com.ga.gymio.repository;

import com.ga.gymio.model.Coupon;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;

public interface CouponRepository extends JpaRepository<Coupon, Long> {

    Optional<Coupon> findByCode(String code);

    boolean existsByCode(String code);

    List<Coupon> findByActiveTrue();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Coupon> findByIdAndActiveTrue(Long couponId);
}