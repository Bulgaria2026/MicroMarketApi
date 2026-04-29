package com.noserbulgaria.micromarket.payment.stripe;

public record StripeManagedCoupon(
    String stripeCouponId,
    String stripePromotionCodeId,
    String code,
    int timesRedeemed,
    boolean active
) {
}
