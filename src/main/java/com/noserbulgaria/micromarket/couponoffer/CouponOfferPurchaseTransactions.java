package com.noserbulgaria.micromarket.couponoffer;

import com.noserbulgaria.micromarket.auth.user.User;
import com.noserbulgaria.micromarket.auth.user.UserRepository;
import com.noserbulgaria.micromarket.coupon.Coupon;
import com.noserbulgaria.micromarket.coupon.CouponMapper;
import com.noserbulgaria.micromarket.coupon.CouponRepository;
import com.noserbulgaria.micromarket.coupon.CouponResponse;
import com.noserbulgaria.micromarket.customer.PointChangeReason;
import com.noserbulgaria.micromarket.customer.Profile;
import com.noserbulgaria.micromarket.customer.ProfileRepository;
import com.noserbulgaria.micromarket.exception.BadRequestApiException;
import com.noserbulgaria.micromarket.exception.NotFoundApiException;
import com.noserbulgaria.micromarket.payment.stripe.StripeManagedCoupon;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CouponOfferPurchaseTransactions {

  private final CouponOfferRepository couponOfferRepository;
  private final CouponRepository couponRepository;
  private final CouponMapper couponMapper;
  private final UserRepository userRepository;
  private final ProfileRepository profileRepository;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public CouponOfferPurchaseReservation reserve(UUID couponOfferId, UUID userId) {
    CouponOffer couponOffer = couponOfferRepository.findByIdForUpdate(couponOfferId)
        .orElseThrow(() -> new NotFoundApiException("Coupon offer with id '%s' not found".formatted(couponOfferId)));
    validatePurchasable(couponOffer);

    User user = userRepository.findById(userId)
        .orElseThrow(() -> new NotFoundApiException("User with id '%s' not found".formatted(userId)));
    Profile profile = ensureProfile(user);

    if (couponOffer.getPointCost() > 0) {
      int updated = profileRepository.trySpendPoints(user.getId(), couponOffer.getPointCost(), PointChangeReason.COUPON_PURCHASED);
      if (updated != 1) {
        throw new BadRequestApiException("Insufficient points to purchase coupon offer");
      }
    }

    couponOffer.setPurchaseCount(couponOffer.getPurchaseCount() + 1);
    boolean deactivatedOffer = false;
    if (couponOffer.getMaxPurchases() != null && couponOffer.getPurchaseCount() >= couponOffer.getMaxPurchases()) {
      couponOffer.setActive(false);
      deactivatedOffer = true;
    }
    couponOfferRepository.saveAndFlush(couponOffer);

    return new CouponOfferPurchaseReservation(
        couponOffer.getId(),
        user.getId(),
        profile.getId(),
        profile.getCustomer().getEmail(),
        generateCouponCode(),
        couponOffer.getName(),
        couponOffer.getExpiryDate(),
        couponOffer.getPointCost(),
        couponOffer.getAmountOff(),
        deactivatedOffer
    );
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public CouponResponse persistPurchasedCoupon(CouponOfferPurchaseReservation reservation, StripeManagedCoupon stripeCoupon) {
    Coupon coupon = Coupon.builder()
        .couponOffer(couponOfferRepository.getReferenceById(reservation.couponOfferId()))
        .user(userRepository.getReferenceById(reservation.userId()))
        .code(stripeCoupon.code())
        .name(reservation.name())
        .expiryDate(reservation.expiryDate())
        .pointCost(reservation.pointCost())
        .amountOff(reservation.amountOff())
        .maxRedemptions(1)
        .timesRedeemed(stripeCoupon.timesRedeemed())
        .active(stripeCoupon.active())
        .stripeCouponId(stripeCoupon.stripeCouponId())
        .stripePromotionCodeId(stripeCoupon.stripePromotionCodeId())
        .build();
    return couponMapper.toDto(couponRepository.saveAndFlush(coupon));
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void compensate(CouponOfferPurchaseReservation reservation) {
    if (reservation.pointCost() > 0) {
      profileRepository.addPoints(
          reservation.stripeCustomerOwnerId(), reservation.pointCost(), PointChangeReason.REFUND);
    }

    CouponOffer couponOffer = couponOfferRepository.findByIdForUpdate(reservation.couponOfferId())
        .orElseThrow(() -> new NotFoundApiException(
            "Coupon offer with id '%s' not found".formatted(reservation.couponOfferId())));
    if (couponOffer.getPurchaseCount() > 0) {
      couponOffer.setPurchaseCount(couponOffer.getPurchaseCount() - 1);
    }
    if (reservation.deactivatedOffer()) {
      couponOffer.setActive(true);
    }
    couponOfferRepository.saveAndFlush(couponOffer);
  }

  private void validatePurchasable(CouponOffer couponOffer) {
    var now = java.time.Instant.now();
    if (!couponOffer.isActive()) {
      throw new BadRequestApiException("Coupon offer is inactive");
    }
    if (couponOffer.getStartDate() != null && couponOffer.getStartDate().isAfter(now)) {
      throw new BadRequestApiException("Coupon offer is not yet available");
    }
    if (couponOffer.getExpiryDate() != null && !couponOffer.getExpiryDate().isAfter(now)) {
      throw new BadRequestApiException("Coupon offer has expired");
    }
    if (couponOffer.getMaxPurchases() != null && couponOffer.getPurchaseCount() >= couponOffer.getMaxPurchases()) {
      throw new BadRequestApiException("Coupon offer has reached its purchase limit");
    }
  }

  private Profile ensureProfile(User user) {
    return profileRepository.findByUserId(user.getId())
        .orElseGet(() -> createProfileOrReload(user));
  }

  private Profile createProfileOrReload(User user) {
    Profile profile = new Profile();
    profile.setUser(user);
    profile.setPoints(0);
    try {
      return profileRepository.saveAndFlush(profile);
    } catch (DataIntegrityViolationException ex) {
      return profileRepository.findByUserId(user.getId())
          .orElseThrow(() -> ex);
    }
  }

  private String generateCouponCode() {
    String candidate;
    do {
      String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(Locale.ROOT);
      candidate = "MM-" + suffix;
    } while (couponRepository.existsByCode(candidate));
    return candidate;
  }
}
