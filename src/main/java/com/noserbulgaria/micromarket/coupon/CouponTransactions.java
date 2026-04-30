package com.noserbulgaria.micromarket.coupon;

import com.noserbulgaria.micromarket.auth.user.User;
import com.noserbulgaria.micromarket.auth.user.UserRepository;
import com.noserbulgaria.micromarket.customer.Profile;
import com.noserbulgaria.micromarket.customer.ProfileRepository;
import com.noserbulgaria.micromarket.exception.BadRequestApiException;
import com.noserbulgaria.micromarket.exception.ConflictApiException;
import com.noserbulgaria.micromarket.exception.NotFoundApiException;
import com.noserbulgaria.micromarket.payment.stripe.StripeManagedCoupon;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CouponTransactions {

  private final CouponRepository couponRepository;
  private final UserRepository userRepository;
  private final ProfileRepository profileRepository;
  private final CouponMapper couponMapper;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public ResolvedCouponInput resolveDirectCouponInputForCreate(CouponRequest request) {
    User user = resolveUser(request.userId());
    Profile profile = user == null ? null : profileByUserOrThrow(user);

    String requestedCode = request.code();
    String code = requestedCode != null ? normalizeCode(requestedCode) : generateCouponCode();
    ensureCodeAvailable(code);

    UUID userId = user == null ? null : user.getId();
    UUID profileId = profile == null ? null : profile.getId();
    String customerEmail = profile == null ? null : profile.getCustomer().getEmail();

    Integer rawPointCost = request.pointCost();
    int pointCost = rawPointCost == null ? 0 : rawPointCost;
    boolean active = !Boolean.FALSE.equals(request.active());

    return ResolvedCouponInput.builder()
        .userId(userId)
        .stripeCustomerOwnerId(profileId)
        .stripeCustomerEmail(customerEmail)
        .code(code)
        .name(normalizeName(request.name()))
        .expiryDate(request.expiryDate())
        .pointCost(pointCost)
        .amountOff(normalizeAmountOff(request.amountOff()))
        .maxRedemptions(normalizeMaxRedemptions(user, request.maxRedemptions()))
        .active(active)
        .build();
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public CouponResponse persistDirectCoupon(ResolvedCouponInput input, StripeManagedCoupon stripeCoupon) {
    Coupon coupon = new Coupon();
    applyLocalState(coupon, input);
    applyStripeState(coupon, stripeCoupon);
    return couponMapper.toDto(couponRepository.saveAndFlush(coupon));
  }

  @Transactional
  public Coupon patchCoupon(UUID id, CouponPatchRequest request) {
    Coupon coupon = couponRepository.findById(id)
        .orElseThrow(() -> new NotFoundApiException("Coupon with id '%s' not found".formatted(id)));
    if (coupon.getCouponOffer() != null) {
      throw new BadRequestApiException("Purchased coupons cannot be updated manually");
    }
    if (!request.unsupportedFields().isEmpty()) {
      throw new BadRequestApiException(
          "Unsupported coupon patch field(s): %s".formatted(String.join(", ", request.unsupportedFields())));
    }
    if (request.isEmpty()) {
      throw new BadRequestApiException("Patch must include name or active");
    }

    String name = request.getName();
    if (name != null) {
      coupon.setName(normalizeName(name));
    }

    Boolean active = request.getActive();
    if (active != null && active != coupon.isActive()) {
      if (active) {
        throw new BadRequestApiException("Inactive coupons cannot be reactivated");
      }
      coupon.setActive(false);
    }

    return couponRepository.saveAndFlush(coupon);
  }

  private void applyLocalState(Coupon coupon, ResolvedCouponInput input) {
    UUID userId = input.userId();
    coupon.setCouponOffer(null);
    coupon.setUser(userId == null ? null : userRepository.getReferenceById(userId));
    coupon.setCode(input.code());
    coupon.setName(input.name());
    coupon.setExpiryDate(input.expiryDate());
    coupon.setPointCost(input.pointCost());
    coupon.setAmountOff(input.amountOff());
    coupon.setMaxRedemptions(input.maxRedemptions());
    coupon.setActive(input.active());
  }

  private void applyStripeState(Coupon coupon, StripeManagedCoupon stripeCoupon) {
    coupon.setStripeCouponId(stripeCoupon.stripeCouponId());
    coupon.setStripePromotionCodeId(stripeCoupon.stripePromotionCodeId());
    coupon.setCode(stripeCoupon.code());
    coupon.setTimesRedeemed(stripeCoupon.timesRedeemed());
    coupon.setActive(stripeCoupon.active());
  }

  private @Nullable User resolveUser(@Nullable UUID userId) {
    if (userId == null) {
      return null;
    }
    return userRepository.findById(userId)
        .orElseThrow(() -> new NotFoundApiException("User with id '%s' not found".formatted(userId)));
  }

  private Profile profileByUserOrThrow(User user) {
    return profileRepository.findByUserId(user.getId())
        .orElseThrow(() -> new IllegalStateException(
            "User '%s' has no Profile".formatted(user.getId())));
  }

  private String generateCouponCode() {
    String candidate;
    do {
      String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(Locale.ROOT);
      candidate = "MM-" + suffix;
    } while (couponRepository.existsByCode(candidate));
    return candidate;
  }

  private String normalizeCode(String code) {
    String normalized = code.trim().toUpperCase(Locale.ROOT);
    if (normalized.isBlank()) {
      throw new BadRequestApiException("code must not be blank");
    }
    return normalized;
  }

  private @Nullable String normalizeName(@Nullable String name) {
    if (name == null) {
      return null;
    }
    String normalized = name.trim();
    return normalized.isEmpty() ? null : normalized;
  }

  private BigDecimal normalizeAmountOff(BigDecimal amountOff) {
    try {
      return amountOff.setScale(2, RoundingMode.UNNECESSARY);
    } catch (ArithmeticException _) {
      throw new BadRequestApiException("amountOff must have at most 2 decimal places");
    }
  }

  private @Nullable Integer normalizeMaxRedemptions(@Nullable User user, @Nullable Integer maxRedemptions) {
    if (user != null) {
      return 1;
    }
    return maxRedemptions;
  }

  private void ensureCodeAvailable(String code) {
    if (couponRepository.existsByCode(code)) {
      throw new ConflictApiException("Coupon with code '%s' already exists".formatted(code));
    }
  }
}
