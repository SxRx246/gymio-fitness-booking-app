
package com.ga.gymio.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Represents the response returned after a coupon is redeemed.
 */
@Getter
@AllArgsConstructor
public class CouponRedemptionResponse
{
   private Long id;
   private String couponCode;
   private LocalDateTime redeemedAt;
}
