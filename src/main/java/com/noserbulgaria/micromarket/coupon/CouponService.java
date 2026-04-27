package com.noserbulgaria.micromarket.coupon;

import com.noserbulgaria.micromarket.auth.user.User;
import com.noserbulgaria.micromarket.auth.user.UserRepository;
import com.noserbulgaria.micromarket.couponoffer.CouponOffer;
import com.noserbulgaria.micromarket.customer.Profile;
import com.noserbulgaria.micromarket.customer.ProfileRepository;
import com.noserbulgaria.micromarket.exception.BadRequestApiException;
import com.noserbulgaria.micromarket.exception.ConflictApiException;
import com.noserbulgaria.micromarket.exception.NotFoundApiException;
import com.noserbulgaria.micromarket.payment.stripe.StripeManagedCoupon;
import com.noserbulgaria.micromarket.payment.stripe.StripeManagedCouponRequest;
import com.noserbulgaria.micromarket.payment.stripe.StripePaymentProvider;
import com.noserbulgaria.micromarket.payment.stripe.StripePromotionCodeRequest;
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
import java.util.Objects;
import java.util.Optional;
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

  public Page<CouponResponse> findAll(Specification<Coupon> spec, Pageable pageable) {
    Page<Coupon> coupons = couponRepository.findAll(spec, pageable);
    coupons.forEach(this::syncStripeState);

    return coupons.map(couponMapper::toDto);
  }

  public Page<CouponResponse> findOwn(UUID userId, @Nullable Boolean active, Pageable pageable) {
    Page<Coupon> coupons = couponRepository.findAll(
        Specification.allOf(
            (root, _, cb) ->
                cb.equal(root.get(Coupon_.user).get("id"), userId),
            active == null
                ? Specification.unrestricted()
                : (root, _, cb) ->
                cb.equal(root.get(Coupon_.active), active)
        ),
        pageable
    );
    coupons.forEach(this::syncStripeState);

    return coupons.map(couponMapper::toDto);
  }

  public CouponResponse getByIdOrThrow(UUID id) {
    return couponRepository.findById(id)
        .map(this::syncStripeState)
        .map(couponMapper::toDto)
        .orElseThrow(() -> new NotFoundApiException("Coupon with id '%s' not found".formatted(id)));
  }

  public CouponResponse create(CouponRequest request) {
    ResolvedCouponInput input = resolveDirectCouponInputForCreate(request);
    StripeManagedCoupon stripeCoupon = stripePaymentProvider.createManagedCoupon(toManagedCouponRequest(input));

    Coupon coupon = new Coupon();
    applyLocalState(coupon, input);
    applyStripeState(coupon, stripeCoupon);

    return couponMapper.toDto(couponRepository.saveAndFlush(coupon));
  }

  public CouponResponse updateOrThrow(UUID id, CouponRequest request) {
    Coupon coupon = couponRepository.findById(id)
        .orElseThrow(() -> new NotFoundApiException("Coupon with id '%s' not found".formatted(id)));
    if (coupon.getCouponOffer() != null) {
      throw new BadRequestApiException("Purchased coupons cannot be updated manually");
    }

    ResolvedCouponInput input = resolveDirectCouponInputForUpdate(coupon, request);

    if (requiresStripeRotation(coupon, input)) {
      stripePaymentProvider.deactivatePromotionCode(coupon.getStripePromotionCodeId());
      StripeManagedCoupon stripeCoupon = stripePaymentProvider.createManagedCoupon(toManagedCouponRequest(input));
      stripePaymentProvider.deleteCoupon(coupon.getStripeCouponId());
      applyStripeState(coupon, stripeCoupon);
    } else {
      if (nullableEquals(coupon.getName(), input.name())) {
        stripePaymentProvider.updateCouponName(coupon.getStripeCouponId(), input.name());
      }
      if (coupon.isActive() != input.active()) {
        StripeManagedCoupon stripeCoupon = stripePaymentProvider.updatePromotionCodeActive(
            coupon.getStripePromotionCodeId(), input.active());
        applyStripeState(coupon, stripeCoupon);
      }
    }

    applyLocalState(coupon, input);
    couponRepository.saveAndFlush(coupon);
    return couponMapper.toDto(syncStripeState(coupon));
  }

  public CouponResponse issuePurchasedCoupon(CouponOffer couponOffer, User user) {
    String code = generateCouponCode();
    ensureCodeAvailable(code, null);

    StripeManagedCoupon stripeCoupon = stripePaymentProvider.createPromotionCode(
        couponOffer.getStripeCouponId(),
        new StripePromotionCodeRequest(
            code,
            true,
            couponOffer.getExpiryDate(),
            1,
            ensureStripeCustomerId(user)
        )
    );

    Coupon coupon = Coupon.builder()
        .couponOffer(couponOffer)
        .user(user)
        .code(stripeCoupon.code())
        .name(couponOffer.getName())
        .expiryDate(couponOffer.getExpiryDate())
        .pointCost(couponOffer.getPointCost())
        .amountOff(couponOffer.getAmountOff())
        .maxRedemptions(1)
        .timesRedeemed(stripeCoupon.timesRedeemed())
        .active(stripeCoupon.active())
        .stripeCouponId(couponOffer.getStripeCouponId())
        .stripePromotionCodeId(stripeCoupon.stripePromotionCodeId())
        .build();

    return couponMapper.toDto(couponRepository.saveAndFlush(coupon));
  }

  public Optional<Coupon> findByStripePromotionCodeIdSynced(String stripePromotionCodeId) {
    return couponRepository.findByStripePromotionCodeId(stripePromotionCodeId)
        .map(this::syncStripeState);
  }

  public Coupon syncStripeState(Coupon coupon) {
    StripeManagedCoupon stripeCoupon = stripePaymentProvider.retrievePromotionCode(coupon.getStripePromotionCodeId());
    applyStripeState(coupon, stripeCoupon);
    return couponRepository.saveAndFlush(coupon);
  }

  private void applyLocalState(Coupon coupon, ResolvedCouponInput input) {
    coupon.setCouponOffer(null);
    coupon.setUser(input.user());
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

  private boolean requiresStripeRotation(Coupon coupon, ResolvedCouponInput input) {
    return !coupon.getAmountOff().equals(input.amountOff())
        || !coupon.getCode().equals(input.code())
        || nullableEquals(coupon.getExpiryDate(), input.expiryDate())
        || nullableEquals(coupon.getMaxRedemptions(), input.maxRedemptions())
        || nullableEquals(userId(coupon.getUser()), userId(input.user()));
  }

  private ResolvedCouponInput resolveDirectCouponInputForCreate(CouponRequest request) {
    User user = resolveUser(request.userId());
    String code = request.code() != null ? normalizeCode(request.code()) : generateCouponCode();
    ensureCodeAvailable(code, null);
    return new ResolvedCouponInput(
        user,
        code,
        normalizeName(request.name()),
        request.expiryDate(),
        request.pointCost() == null ? 0 : request.pointCost(),
        normalizeAmountOff(request.amountOff()),
        normalizeMaxRedemptions(user, request.maxRedemptions()),
        request.active() == null || request.active()
    );
  }

  private ResolvedCouponInput resolveDirectCouponInputForUpdate(Coupon coupon, CouponRequest request) {
    User user = resolveUser(request.userId());
    String code = request.code() != null ? normalizeCode(request.code()) : coupon.getCode();
    ensureCodeAvailable(code, coupon.getId());
    return new ResolvedCouponInput(
        user,
        code,
        normalizeName(request.name()),
        request.expiryDate(),
        request.pointCost() == null ? 0 : request.pointCost(),
        normalizeAmountOff(request.amountOff()),
        normalizeMaxRedemptions(user, request.maxRedemptions()),
        request.active() != null ? request.active() : coupon.isActive()
    );
  }

  private StripeManagedCouponRequest toManagedCouponRequest(ResolvedCouponInput input) {
    return new StripeManagedCouponRequest(
        amountOffInMinorUnits(input.amountOff()),
        input.code(),
        input.name(),
        input.active(),
        input.expiryDate(),
        input.maxRedemptions(),
        input.user() == null ? null : ensureStripeCustomerId(input.user())
    );
  }

  private @Nullable User resolveUser(@Nullable UUID userId) {
    if (userId == null) {
      return null;
    }
    return userRepository.findById(userId)
        .orElseThrow(() -> new NotFoundApiException("User with id '%s' not found".formatted(userId)));
  }

  public String ensureStripeCustomerId(User user) {
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
    } catch (ArithmeticException ex) {
      throw new BadRequestApiException("amountOff must have at most 2 decimal places");
    }
  }

  private @Nullable Integer normalizeMaxRedemptions(@Nullable User user, @Nullable Integer maxRedemptions) {
    if (user != null) {
      return 1;
    }
    return maxRedemptions;
  }

  private long amountOffInMinorUnits(BigDecimal amountOff) {
    return amountOff.movePointRight(2).longValueExact();
  }

  private static @Nullable UUID userId(@Nullable User user) {
    return user == null ? null : user.getId();
  }

  private static boolean nullableEquals(@Nullable Object left, @Nullable Object right) {
    return !Objects.equals(left, right);
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
      @Nullable Instant expiryDate,
      int pointCost,
      BigDecimal amountOff,
      @Nullable Integer maxRedemptions,
      boolean active
  ) {
  }
}
