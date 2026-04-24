package com.noserbulgaria.micromarket.coupon;

import com.noserbulgaria.micromarket.auth.user.CustomUserDetails;
import com.noserbulgaria.micromarket.auth.user.User;
import com.noserbulgaria.micromarket.auth.user.UserRepository;
import com.noserbulgaria.micromarket.couponoffer.CouponOffer;
import com.noserbulgaria.micromarket.customer.Profile;
import com.noserbulgaria.micromarket.customer.ProfileRepository;
import com.noserbulgaria.micromarket.exception.BadRequestApiException;
import com.noserbulgaria.micromarket.exception.ConflictApiException;
import com.noserbulgaria.micromarket.exception.NotFoundApiException;
import com.noserbulgaria.micromarket.order.Order;
import com.noserbulgaria.micromarket.order.OrderRepository;
import com.noserbulgaria.micromarket.order.OrderStatusType;
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
import java.util.EnumSet;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class CouponService {

  private static final EnumSet<OrderStatusType> USER_USAGE_STATUSES =
      EnumSet.of(OrderStatusType.PENDING_PAYMENT, OrderStatusType.PAID, OrderStatusType.REFUNDED);

  private final CouponRepository couponRepository;
  private final UserRepository userRepository;
  private final ProfileRepository profileRepository;
  private final CouponMapper couponMapper;
  private final StripePaymentProvider stripePaymentProvider;
  private final OrderRepository orderRepository;

  @Transactional(readOnly = true)
  public Page<CouponResponse> findAll(Specification<Coupon> spec, Pageable pageable) {
    return couponRepository.findAll(spec, pageable)
        .map(couponMapper::toDto);
  }

  @Transactional(readOnly = true)
  public Page<CouponResponse> findOwn(UUID userId, @Nullable Boolean active, Pageable pageable) {
    return couponRepository.findAll(
            Specification.allOf(
                (root, _, cb) -> cb.equal(root.get(Coupon_.user).get("id"), userId),
                active == null ? Specification.unrestricted() : (root, _, cb) -> cb.equal(root.get(Coupon_.active), active)
            ),
            pageable)
        .map(couponMapper::toDto);
  }

  @Transactional(readOnly = true)
  public CouponResponse getByIdOrThrow(UUID id) {
    return couponRepository.findById(id)
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
    return couponMapper.toDto(couponRepository.saveAndFlush(coupon));
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
        .startDate(couponOffer.getStartDate())
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

  @Transactional(readOnly = true)
  public @Nullable Coupon resolveForCheckout(@Nullable UUID couponId, @Nullable CustomUserDetails userDetails, UUID customerId) {
    if (couponId == null) {
      return null;
    }
    if (userDetails == null) {
      throw new BadRequestApiException("Coupons require authenticated checkout");
    }

    Coupon coupon = couponRepository.findById(couponId)
        .orElseThrow(() -> new NotFoundApiException("Coupon with id '%s' not found".formatted(couponId)));

    validateForCheckout(coupon, userDetails.getId(), customerId);
    return coupon;
  }

  public void markRedeemed(Order order) {
    Coupon coupon = order.getAppliedCoupon();
    if (coupon == null) {
      return;
    }

    int nextTimesRedeemed = coupon.getTimesRedeemed() + 1;
    coupon.setTimesRedeemed(nextTimesRedeemed);
    Integer maxRedemptions = coupon.getMaxRedemptions();
    if (maxRedemptions != null && nextTimesRedeemed >= maxRedemptions && coupon.isActive()) {
      coupon.setActive(false);
      stripePaymentProvider.deactivatePromotionCode(coupon.getStripePromotionCodeId());
    }
    couponRepository.save(coupon);
  }

  private void validateForCheckout(Coupon coupon, UUID userId, UUID customerId) {
    Instant now = Instant.now();
    if (!coupon.isActive()) {
      throw new BadRequestApiException("Coupon is inactive");
    }
    if (coupon.getStartDate() != null && coupon.getStartDate().isAfter(now)) {
      throw new BadRequestApiException("Coupon is not yet active");
    }
    if (coupon.getExpiryDate() != null && !coupon.getExpiryDate().isAfter(now)) {
      throw new BadRequestApiException("Coupon has expired");
    }
    if (coupon.getUser() != null && !coupon.getUser().getId().equals(userId)) {
      throw new BadRequestApiException("Coupon cannot be used by the current user");
    }
    Integer maxRedemptions = coupon.getMaxRedemptions();
    if (maxRedemptions != null && coupon.getTimesRedeemed() >= maxRedemptions) {
      throw new BadRequestApiException("Coupon has no remaining redemptions");
    }
    if (orderRepository.existsByAppliedCouponIdAndCustomerIdAndStatusIn(coupon.getId(), customerId, USER_USAGE_STATUSES)) {
      throw new BadRequestApiException("Coupon can only be used once per user");
    }
  }

  private void applyLocalState(Coupon coupon, ResolvedCouponInput input) {
    coupon.setCouponOffer(null);
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
        || nullableEquals(coupon.getExpiryDate(), input.expiryDate())
        || nullableEquals(coupon.getMaxRedemptions(), input.maxRedemptions())
        || nullableEquals(userId(coupon.getUser()), userId(input.user()));
  }

  private ResolvedCouponInput resolveDirectCouponInputForCreate(CouponRequest request) {
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
        request.pointCost() == null ? 0 : request.pointCost(),
        normalizeAmountOff(request.amountOff()),
        normalizeMaxRedemptions(user, request.maxRedemptions()),
        request.active() == null || request.active()
    );
  }

  private ResolvedCouponInput resolveDirectCouponInputForUpdate(Coupon coupon, CouponRequest request) {
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

  private void validateDates(@Nullable Instant startDate, @Nullable Instant expiryDate) {
    if (startDate != null && expiryDate != null && !expiryDate.isAfter(startDate)) {
      throw new BadRequestApiException("expiryDate must be after startDate");
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
      @Nullable Instant startDate,
      @Nullable Instant expiryDate,
      int pointCost,
      BigDecimal amountOff,
      @Nullable Integer maxRedemptions,
      boolean active
  ) {
  }
}
