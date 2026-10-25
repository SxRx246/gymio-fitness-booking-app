package com.ga.gymio.service;

import com.ga.gymio.dto.request.CouponRequest;
import com.ga.gymio.dto.response.CouponResponse;
import com.ga.gymio.model.Coupon;
import com.ga.gymio.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

/**
 * Manages coupon creation, retrieval, updates, and deletion.
 * Validates coupon rules and normalizes coupon codes.
 */
@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;

    /**
     * Creates a coupon after validating its details and ensuring its code is unique.
     *
     * @param request the coupon details
     * @return the created coupon
     */
    @Transactional
    public CouponResponse createCoupon(CouponRequest request) {
        validateCoupon(request);

        String code = normalizeCode(request.getCode());

        if (couponRepository.existsByCode(code)) {
            throw new IllegalArgumentException("Coupon code already exists.");
        }

        Coupon coupon = new Coupon();
        applyRequest(coupon, request);
        coupon.setCode(code);
        coupon.setUsedCount(0);
        coupon.setActive(true);

        return toResponse(couponRepository.save(coupon));
    }

    /**
     * Updates an existing coupon after validating its details and code uniqueness.
     *
     * @param id the ID of the coupon to update
     * @param request the updated coupon details
     * @return the updated coupon
     */
    @Transactional
    public CouponResponse updateCoupon(Long id, CouponRequest request) {
        validateCoupon(request);

        Coupon coupon = getCouponEntity(id);
        String code = normalizeCode(request.getCode());

        couponRepository.findByCode(code)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException(
                            "Coupon code already exists."
                    );
                });

        applyRequest(coupon, request);
        coupon.setCode(code);

        return toResponse(couponRepository.save(coupon));
    }

    /**
     * Retrieves a coupon by its ID.
     *
     * @param id the coupon ID
     * @return the requested coupon
     */
    @Transactional(readOnly = true)
    public CouponResponse getCouponById(Long id) {
        return toResponse(getCouponEntity(id));
    }

    /**
     * Retrieves all coupons.
     *
     * @return a list of coupons
     */
    @Transactional(readOnly = true)
    public List<CouponResponse> getAllCoupons() {
        return couponRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Deletes a coupon by its ID.
     *
     * @param id the ID of the coupon to delete
     */
    @Transactional
    public void deleteCoupon(Long id) {
        Coupon coupon = getCouponEntity(id);
        couponRepository.delete(coupon);
    }

    private Coupon getCouponEntity(Long id) {
        return couponRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("Coupon not found."));
    }

    private void validateCoupon(CouponRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Coupon request is required.");
        }

        if (request.getDiscountType() == null) {
            throw new IllegalArgumentException("Discount type is required.");
        }

        if (request.getDiscountValue() == null
                || request.getDiscountValue().signum() <= 0) {
            throw new IllegalArgumentException(
                    "Discount value must be greater than zero."
            );
        }

        if (request.getDiscountType() == Coupon.DiscountType.PERCENTAGE
                && request.getDiscountValue()
                .compareTo(new java.math.BigDecimal("100")) > 0) {
            throw new IllegalArgumentException(
                    "Percentage discount cannot exceed 100."
            );
        }

        if (request.getUsageLimit() == null
                || request.getUsageLimit() < 1) {
            throw new IllegalArgumentException(
                    "Usage limit must be at least one."
            );
        }

        LocalDateTime startDate = request.getStartDate();
        LocalDateTime expiresAt = request.getExpiresAt();

        if (startDate == null || expiresAt == null) {
            throw new IllegalArgumentException(
                    "Start date and expiration date are required."
            );
        }

        if (!expiresAt.isAfter(startDate)) {
            throw new IllegalArgumentException(
                    "Expiration date must be after the start date."
            );
        }

        if (request.getCode() == null
                || request.getCode().isBlank()) {
            throw new IllegalArgumentException("Coupon code is required.");
        }
    }

    private void applyRequest(Coupon coupon, CouponRequest request) {
        coupon.setDescription(request.getDescription());
        coupon.setDiscountType(request.getDiscountType());
        coupon.setDiscountValue(request.getDiscountValue());
        coupon.setUsageLimit(request.getUsageLimit());
        coupon.setStartDate(request.getStartDate());
        coupon.setExpiresAt(request.getExpiresAt());
    }

    private String normalizeCode(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }

    private CouponResponse toResponse(Coupon coupon) {
        return new CouponResponse(
                coupon.getId(),
                coupon.getDescription(),
                coupon.getCode(),
                coupon.getDiscountType(),
                coupon.getDiscountValue(),
                coupon.getUsageLimit(),
                coupon.getUsedCount(),
                coupon.getStartDate(),
                coupon.getExpiresAt(),
                coupon.isActive(),
                coupon.getCreatedAt(),
                coupon.getUpdatedAt()
        );
    }
}
