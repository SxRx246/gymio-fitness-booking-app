package com.ga.gymio.controller;

import com.ga.gymio.dto.request.CouponRequest;
import com.ga.gymio.dto.response.CouponResponse;
import com.ga.gymio.service.CouponService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * REST controller for managing coupons.
 * Provides administrator endpoints for coupon management and
 * a customer endpoint for retrieving an eligible coupon.
 */
@RestController
@RequestMapping("/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    /**
     * Creates a new coupon.
     *
     * @param request the validated coupon details
     * @return the created coupon
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public CouponResponse createCoupon(
            @Valid @RequestBody CouponRequest request) {
        return couponService.createCoupon(request);
    }

    /**
     * Retrieves all coupons for administrators.
     *
     * @return a list of coupons
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<CouponResponse> getAllCoupons() {
        return couponService.getAllCoupons();
    }

    /**
     * Retrieves a coupon by its ID.
     *
     * @param id the coupon ID
     * @return the requested coupon
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public CouponResponse getCouponById(@PathVariable Long id) {
        return couponService.getCouponById(id);
    }

    /**
     * Updates an existing coupon.
     *
     * @param id the ID of the coupon to update
     * @param request the validated updated coupon details
     * @return the updated coupon
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public CouponResponse updateCoupon(
            @PathVariable Long id,
            @Valid @RequestBody CouponRequest request) {
        return couponService.updateCoupon(id, request);
    }

    /**
     * Deactivates a coupon without permanently deleting it.
     *
     * @param id the ID of the coupon to deactivate
     * @return the deactivated coupon
     */
    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public CouponResponse deactivateCoupon(@PathVariable Long id) {
        return couponService.deactivateCoupon(id);
    }

    /**
     * Retrieves the first active coupon that is currently valid and has
     * remaining uses.
     *
     * @return an optional containing an eligible coupon, or empty if none is available
     */
    @GetMapping("/available")
    @PreAuthorize("hasRole('CUSTOMER')")
    public Optional<CouponResponse> getAvailableCoupon() {
        return couponService.getAvailableCoupon();
    }
}
