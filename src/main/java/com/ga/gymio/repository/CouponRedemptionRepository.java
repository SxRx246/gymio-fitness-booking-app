package com.ga.gymio.repository;

import com.ga.gymio.model.CouponRedemption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CouponRedemptionRepository extends JpaRepository<CouponRedemption, Long> {

    Optional<CouponRedemption> findByCoupon_IdAndCustomer_Id(Long couponId, Long customerId);

    boolean existsByCoupon_IdAndCustomer_Id(Long couponId, Long customerId);

    List<CouponRedemption> findAllByCustomer_IdOrderByRedeemedAtDesc(Long customerId);

    long countByCoupon_Id(Long couponId);

    Optional<CouponRedemption> findByPayment_Id(Long paymentId);

    boolean existsByPayment_Id(Long paymentId);
}