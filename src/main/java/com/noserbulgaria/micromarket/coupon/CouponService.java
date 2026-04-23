package com.noserbulgaria.micromarket.coupon;

import com.noserbulgaria.micromarket.auth.user.User;
import com.noserbulgaria.micromarket.auth.user.UserRepository;
import com.noserbulgaria.micromarket.customer.Profile;
import com.noserbulgaria.micromarket.customer.ProfileRepository;
import com.noserbulgaria.micromarket.exception.BadRequestApiException;
import com.noserbulgaria.micromarket.exception.ConflictApiException;
import com.noserbulgaria.micromarket.exception.NotFoundApiException;
import com.noserbulgaria.micromarket.payment.stripe.StripeManagedCoupon;
import com.noserbulgaria.micromarket.payment.stripe.StripeManagedCouponRequest;
import com.noserbulgaria.micromarket.payment.stripe.StripePaymentProvider;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class CouponService {

  private final CouponRepository couponRepository;
  private final UserRepository userRepository;
  private final ProfileRepository profileRepository;
  private final CouponMapper couponMapper;
  private final StripePaymentProvider stripePaymentProvider;

  @Transactional(readOnly = true)
  public Page<CouponResponse> findAll(Specification<Coupon> spec, Pageable pageable) {
    return couponRepository.findAll(spec, pageable)
        .map(couponMapper::toDto);
  }

  @Transactional(readOnly = true)
  public CouponResponse getByIdOrThrow(UUID id) {
    return couponRepository.findById(id)
        .map(couponMapper::toDto)
        .orElseThrow(() -> new NotFoundApiException("Coupon with id '%s' not found".formatted(id)));
  }

  public CouponResponse create(CouponRequest request) {
    ResolvedCouponInput input = resolveInputForCreate(request);
    StripeManagedCoupon stripeCoupon = stripePaymentProvider.createManagedCoupon(toStripeRequest(input));

    Coupon coupon = new Coupon();
    applyLocalState(coupon, input);
    applyStripeState(coupon, stripeCoupon);

    return couponMapper.toDto(couponRepository.saveAndFlush(coupon));
  }

  public CouponResponse updateOrThrow(UUID id, CouponRequest request) {
    Coupon coupon = couponRepository.findById(id)
        .orElseThrow(() -> new NotFoundApiException("Coupon with id '%s' not found".formatted(id)));

    ResolvedCouponInput input = resolveInputForUpdate(coupon, request);

    if (requiresStripeRotation(coupon, input)) {
      stripePaymentProvider.deactivatePromotionCode(coupon.getStripePromotionCodeId());
      StripeManagedCoupon stripeCoupon = stripePaymentProvider.createManagedCoupon(toStripeRequest(input));
      stripePaymentProvider.deleteCoupon(coupon.getStripeCouponId());
      applyStripeState(coupon, stripeCoupon);
    } else {
      if (!nullableEquals(coupon.getName(), input.name())) {
        stripePaymentProvider.updateCouponName(coupon.getStripeCouponId(), input.name());
      }
      if (coupon.isActive() != input.active()) {
        StripeManagedCoupon stripeCoupon = stripePaymentProvider.updatePromotionCodeActive(
            coupon.getStripePromotionCodeId(), input.active());
        applyStripeState(coupon, stripeCoupon);
      }
    }

    applyLocalState(coupon, input);
    return couponMapper.toDto(couponRepository.saveAndFlush(coupon));
  }

  private void applyLocalState(Coupon coupon, ResolvedCouponInput input) {
    coupon.setUser(input.user());
    coupon.setCode(input.code());
    coupon.setName(input.name());
    coupon.setStartDate(input.startDate());
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

  private boolean requiresStripeRotation(Coupon coupon, ResolvedCouponInput input) {
    return !coupon.getAmountOff().equals(input.amountOff())
        || !coupon.getCode().equals(input.code())
        || !nullableEquals(coupon.getExpiryDate(), input.expiryDate())
        || !nullableEquals(coupon.getMaxRedemptions(), input.maxRedemptions())
        || !nullableEquals(userId(coupon.getUser()), userId(input.user()));
  }

  private ResolvedCouponInput resolveInputForCreate(CouponRequest request) {
    User user = resolveUser(request.userId());
    validateDates(request.startDate(), request.expiryDate());
    String code = request.code() != null ? normalizeCode(request.code()) : generateCouponCode();
    ensureCodeAvailable(code, null);
    return new ResolvedCouponInput(
        user,
        code,
        normalizeName(request.name()),
        request.startDate(),
        request.expiryDate(),
        request.pointCost(),
        normalizeAmountOff(request.amountOff()),
        request.maxRedemptions(),
        request.active() == null || request.active()
    );
  }

  private ResolvedCouponInput resolveInputForUpdate(Coupon coupon, CouponRequest request) {
    User user = resolveUser(request.userId());
    validateDates(request.startDate(), request.expiryDate());
    String code = request.code() != null ? normalizeCode(request.code()) : coupon.getCode();
    ensureCodeAvailable(code, coupon.getId());
    return new ResolvedCouponInput(
        user,
        code,
        normalizeName(request.name()),
        request.startDate(),
        request.expiryDate(),
        request.pointCost(),
        normalizeAmountOff(request.amountOff()),
        request.maxRedemptions(),
        request.active() != null ? request.active() : coupon.isActive()
    );
  }

  private @Nullable User resolveUser(@Nullable UUID userId) {
    if (userId == null) {
      return null;
    }
    return userRepository.findById(userId)
        .orElseThrow(() -> new NotFoundApiException("User with id '%s' not found".formatted(userId)));
  }

  private StripeManagedCouponRequest toStripeRequest(ResolvedCouponInput input) {
    return new StripeManagedCouponRequest(
        amountOffInMinorUnits(input.amountOff()),
        input.code(),
        input.name(),
        input.active(),
        input.expiryDate(),
        input.maxRedemptions(),
        ensureStripeCustomerId(input.user())
    );
  }

  private @Nullable String ensureStripeCustomerId(@Nullable User user) {
    if (user == null) {
      return null;
    }

    Profile profile = profileRepository.findByUserId(user.getId())
        .orElseGet(() -> createProfileOrReload(user));
    if (profile.getStripeCustomerId() != null) {
      return profile.getStripeCustomerId();
    }

    String stripeCustomerId = stripePaymentProvider.createCustomer(user.getEmail());
    profile.setStripeCustomerId(stripeCustomerId);
    profileRepository.saveAndFlush(profile);
    return stripeCustomerId;
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

  private void validateDates(@Nullable Instant startDate, @Nullable Instant expiryDate) {
    if (startDate != null && expiryDate != null && !expiryDate.isAfter(startDate)) {
      throw new BadRequestApiException("expiryDate must be after startDate");
    }
  }

  private String generateCouponCode() {
    String candidate;
    do {
      String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(Locale.ROOT);
      candidate = "MM-" + suffix + "-MM";
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
    } catch (ArithmeticException ex) {
      throw new BadRequestApiException("amountOff must have at most 2 decimal places");
    }
  }

  private long amountOffInMinorUnits(BigDecimal amountOff) {
    return amountOff.movePointRight(2).longValueExact();
  }

  private static @Nullable UUID userId(@Nullable User user) {
    return user == null ? null : user.getId();
  }

  private static boolean nullableEquals(@Nullable Object left, @Nullable Object right) {
    return left == null ? right == null : left.equals(right);
  }

  private void ensureCodeAvailable(String code, @Nullable UUID currentCouponId) {
    if (!couponRepository.existsByCode(code)) {
      return;
    }
    if (currentCouponId == null) {
      throw new ConflictApiException("Coupon with code '%s' already exists".formatted(code));
    }
    Coupon existing = couponRepository.findOne((root, _, cb) -> cb.equal(root.get(Coupon_.code), code))
        .orElseThrow(() -> new ConflictApiException("Coupon with code '%s' already exists".formatted(code)));
    if (!existing.getId().equals(currentCouponId)) {
      throw new ConflictApiException("Coupon with code '%s' already exists".formatted(code));
    }
  }

  private record ResolvedCouponInput(
      @Nullable User user,
      String code,
      @Nullable String name,
      @Nullable Instant startDate,
      @Nullable Instant expiryDate,
      int pointCost,
      BigDecimal amountOff,
      @Nullable Integer maxRedemptions,
      boolean active
  ) {
  }
}
