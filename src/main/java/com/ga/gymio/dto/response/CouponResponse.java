package com.ga.gymio.dto.response;

import com.ga.gymio.model.Coupon;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class CouponResponse {
   private Long id;
   private String description;
   private String code;
   private Coupon.DiscountType discountType;
   private BigDecimal discountValue;
   private Integer usageLimit;
   private Integer usedCount;
   private LocalDateTime startDate;
   private LocalDateTime expiresAt;
   private boolean active;
   private LocalDateTime createdAt;
   private LocalDateTime updatedAt;

}
