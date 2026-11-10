package com.ga.gymio.service;

import com.ga.gymio.dto.request.CouponRequest;
import com.ga.gymio.dto.response.CouponResponse;
import com.ga.gymio.model.Coupon;
import com.ga.gymio.repository.CouponRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CouponServiceTest {

    @Mock
    private CouponRepository couponRepository;

    @InjectMocks
    private CouponService couponService;

    private CouponRequest request;

    @BeforeEach
    void setUp() {
        request = new CouponRequest();
        request.setDescription("Discount for new customers");
        request.setCode("  save20  ");
        request.setDiscountType(Coupon.DiscountType.PERCENTAGE);
        request.setDiscountValue(new BigDecimal("20.00"));
        request.setUsageLimit(10);
        request.setStartDate(LocalDateTime.now().minusMinutes(5));
        request.setExpiresAt(LocalDateTime.now().plusDays(7));
    }

    @Test
    void createCoupon_shouldSaveCouponWithNormalizedCode() {
        when(couponRepository.existsByCode("SAVE20")).thenReturn(false);
        when(couponRepository.save(any(Coupon.class)))
                .thenAnswer(invocation -> {
                    Coupon coupon = invocation.getArgument(0);
                    coupon.setId(1L);
                    return coupon;
                });

        CouponResponse response = couponService.createCoupon(request);

        assertEquals(1L, response.getId());
        assertEquals("SAVE20", response.getCode());
        assertEquals(0, response.getUsedCount());
        assertTrue(response.isActive());
        assertEquals(new BigDecimal("20.00"), response.getDiscountValue());

        verify(couponRepository).existsByCode("SAVE20");
        verify(couponRepository).save(any(Coupon.class));
    }

    @Test
    void createCoupon_shouldRejectDuplicateCode() {
        when(couponRepository.existsByCode("SAVE20")).thenReturn(true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> couponService.createCoupon(request)
        );

        assertEquals("Coupon code already exists.", exception.getMessage());
        verify(couponRepository, never()).save(any(Coupon.class));
    }

    @Test
    void createCoupon_shouldRejectNullRequest() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> couponService.createCoupon(null)
        );

        assertEquals("Coupon request is required.", exception.getMessage());
        verifyNoInteractions(couponRepository);
    }

    @Test
    void createCoupon_shouldRejectMissingDiscountType() {
        request.setDiscountType(null);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> couponService.createCoupon(request)
        );

        assertEquals("Discount type is required.", exception.getMessage());
    }

    @Test
    void createCoupon_shouldRejectZeroDiscount() {
        request.setDiscountValue(BigDecimal.ZERO);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> couponService.createCoupon(request)
        );

        assertEquals(
                "Discount value must be greater than zero.",
                exception.getMessage()
        );
    }

    @Test
    void createCoupon_shouldRejectPercentageAbove100() {
        request.setDiscountValue(new BigDecimal("100.01"));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> couponService.createCoupon(request)
        );

        assertEquals(
                "Percentage discount cannot exceed 100.",
                exception.getMessage()
        );
    }

    @Test
    void createCoupon_shouldAllowFixedAmountAbove100() {
        request.setDiscountType(Coupon.DiscountType.FIXED_AMOUNT);
        request.setDiscountValue(new BigDecimal("150.00"));

        when(couponRepository.existsByCode("SAVE20")).thenReturn(false);
        when(couponRepository.save(any(Coupon.class)))
                .thenAnswer(invocation -> {
                    Coupon coupon = invocation.getArgument(0);
                    coupon.setId(2L);
                    return coupon;
                });

        CouponResponse response = couponService.createCoupon(request);

        assertEquals(
                Coupon.DiscountType.FIXED_AMOUNT,
                response.getDiscountType()
        );
        assertEquals(new BigDecimal("150.00"), response.getDiscountValue());
    }

    @Test
    void createCoupon_shouldRejectInvalidUsageLimit() {
        request.setUsageLimit(0);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> couponService.createCoupon(request)
        );

        assertEquals(
                "Usage limit must be at least one.",
                exception.getMessage()
        );
    }

    @Test
    void createCoupon_shouldRejectMissingDates() {
        request.setStartDate(null);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> couponService.createCoupon(request)
        );

        assertEquals(
                "Start date and expiration date are required.",
                exception.getMessage()
        );
    }

    @Test
    void createCoupon_shouldRejectExpirationBeforeOrEqualToStart() {
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        request.setStartDate(start);
        request.setExpiresAt(start);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> couponService.createCoupon(request)
        );

        assertEquals(
                "Expiration date must be after the start date.",
                exception.getMessage()
        );
    }

    @Test
    void createCoupon_shouldRejectBlankCode() {
        request.setCode("   ");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> couponService.createCoupon(request)
        );

        assertEquals("Coupon code is required.", exception.getMessage());
    }

    @Test
    void getCouponById_shouldReturnCouponWhenFound() {
        Coupon coupon = createCoupon(1L, "SAVE20");
        when(couponRepository.findById(1L)).thenReturn(Optional.of(coupon));

        CouponResponse response = couponService.getCouponById(1L);

        assertEquals(1L, response.getId());
        assertEquals("SAVE20", response.getCode());
        assertTrue(response.isActive());
    }

    @Test
    void getCouponById_shouldThrowWhenNotFound() {
        when(couponRepository.findById(99L)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> couponService.getCouponById(99L)
        );

        assertEquals("Coupon not found.", exception.getMessage());
    }

    @Test
    void getAllCoupons_shouldReturnAllCoupons() {
        when(couponRepository.findAll()).thenReturn(List.of(
                createCoupon(1L, "SAVE20"),
                createCoupon(2L, "WELCOME10")
        ));

        List<CouponResponse> responses = couponService.getAllCoupons();

        assertEquals(2, responses.size());
        assertEquals("SAVE20", responses.get(0).getCode());
        assertEquals("WELCOME10", responses.get(1).getCode());
    }

    @Test
    void updateCoupon_shouldUpdateCouponDetails() {
        Coupon existing = createCoupon(1L, "OLD_CODE");
        request.setCode("  new_code ");

        when(couponRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(couponRepository.findByCode("NEW_CODE")).thenReturn(Optional.empty());
        when(couponRepository.save(any(Coupon.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CouponResponse response = couponService.updateCoupon(1L, request);

        assertEquals("NEW_CODE", response.getCode());
        assertEquals("Discount for new customers", response.getDescription());
        assertEquals(new BigDecimal("20.00"), response.getDiscountValue());
        assertEquals(0, response.getUsedCount());
        assertTrue(response.isActive());
    }

    @Test
    void updateCoupon_shouldRejectCodeUsedByAnotherCoupon() {
        Coupon existing = createCoupon(1L, "OLD_CODE");
        Coupon other = createCoupon(2L, "SAVE20");

        when(couponRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(couponRepository.findByCode("SAVE20")).thenReturn(Optional.of(other));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> couponService.updateCoupon(1L, request)
        );

        assertEquals("Coupon code already exists.", exception.getMessage());
        verify(couponRepository, never()).save(any(Coupon.class));
    }

    @Test
    void deactivateCoupon_shouldSetActiveToFalse() {
        Coupon coupon = createCoupon(1L, "SAVE20");

        when(couponRepository.findById(1L)).thenReturn(Optional.of(coupon));
        when(couponRepository.save(any(Coupon.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CouponResponse response = couponService.deactivateCoupon(1L);

        assertFalse(response.isActive());
        verify(couponRepository).save(coupon);
    }

    @Test
    void getAvailableCoupon_shouldReturnCurrentlyValidCoupon() {
        Coupon coupon = createCoupon(1L, "SAVE20");
        when(couponRepository.findByActiveTrue()).thenReturn(List.of(coupon));

        Optional<CouponResponse> response = couponService.getAvailableCoupon();

        assertTrue(response.isPresent());
        assertEquals("SAVE20", response.orElseThrow().getCode());
    }

    @Test
    void getAvailableCoupon_shouldIgnoreExpiredCoupon() {
        Coupon coupon = createCoupon(1L, "EXPIRED");
        coupon.setExpiresAt(LocalDateTime.now().minusMinutes(1));

        when(couponRepository.findByActiveTrue()).thenReturn(List.of(coupon));

        Optional<CouponResponse> response = couponService.getAvailableCoupon();

        assertTrue(response.isEmpty());
    }

    @Test
    void getAvailableCoupon_shouldIgnoreCouponThatHasReachedUsageLimit() {
        Coupon coupon = createCoupon(1L, "FULL");
        coupon.setUsageLimit(5);
        coupon.setUsedCount(5);

        when(couponRepository.findByActiveTrue()).thenReturn(List.of(coupon));

        Optional<CouponResponse> response = couponService.getAvailableCoupon();

        assertTrue(response.isEmpty());
    }

    private Coupon createCoupon(Long id, String code) {
        Coupon coupon = new Coupon();
        coupon.setId(id);
        coupon.setDescription("Test coupon");
        coupon.setCode(code);
        coupon.setDiscountType(Coupon.DiscountType.PERCENTAGE);
        coupon.setDiscountValue(new BigDecimal("20.00"));
        coupon.setUsageLimit(10);
        coupon.setUsedCount(0);
        coupon.setStartDate(LocalDateTime.now().minusDays(1));
        coupon.setExpiresAt(LocalDateTime.now().plusDays(7));
        coupon.setActive(true);
        return coupon;
    }
}
