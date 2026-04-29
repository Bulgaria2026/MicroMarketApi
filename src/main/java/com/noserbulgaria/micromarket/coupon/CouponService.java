package com.noserbulgaria.micromarket.coupon;

import com.noserbulgaria.micromarket.customer.StripeCustomerService;
import com.noserbulgaria.micromarket.exception.NotFoundApiException;
import com.noserbulgaria.micromarket.payment.stripe.StripeManagedCoupon;
import com.noserbulgaria.micromarket.payment.stripe.StripeManagedCouponRequest;
import com.noserbulgaria.micromarket.payment.stripe.StripePaymentProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponService {

  private final CouponRepository couponRepository;
  private final CouponTransactions couponTransactions;
  private final CouponMapper couponMapper;
  private final StripePaymentProvider stripePaymentProvider;
  private final StripeCustomerService stripeCustomerService;

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
    ResolvedCouponInput input = couponTransactions.resolveDirectCouponInputForCreate(request);
    StripeManagedCoupon stripeCoupon = stripePaymentProvider.createManagedCoupon(toManagedCouponRequest(input));
    try {
      return couponTransactions.persistDirectCoupon(input, stripeCoupon);
    } catch (RuntimeException ex) {
      deleteStripeCouponAfterFailedPersistence(stripeCoupon.stripeCouponId(), ex);
      throw ex;
    }
  }

  public CouponResponse patchOrThrow(UUID id, CouponPatchRequest request) {
    Coupon coupon = couponTransactions.patchCoupon(id, request);
    if (Boolean.FALSE.equals(request.getActive())) {
      stripePaymentProvider.deleteCoupon(coupon.getStripeCouponId());
    }

    return couponMapper.toDto(coupon);
  }

  public Optional<Coupon> findByStripePromotionCodeIdSynced(String stripePromotionCodeId) {
    return couponRepository.findByStripePromotionCodeId(stripePromotionCodeId)
        .map(this::syncStripeState);
  }

  @Transactional
  public Coupon syncStripeState(Coupon coupon) {
    StripeManagedCoupon stripeCoupon = stripePaymentProvider.retrievePromotionCode(coupon.getStripePromotionCodeId());
    applyStripeState(coupon, stripeCoupon);
    return couponRepository.saveAndFlush(coupon);
  }

  private void applyStripeState(Coupon coupon, StripeManagedCoupon stripeCoupon) {
    coupon.setStripeCouponId(stripeCoupon.stripeCouponId());
    coupon.setStripePromotionCodeId(stripeCoupon.stripePromotionCodeId());
    coupon.setCode(stripeCoupon.code());
    coupon.setTimesRedeemed(stripeCoupon.timesRedeemed());
    coupon.setActive(stripeCoupon.active());
  }

  private StripeManagedCouponRequest toManagedCouponRequest(ResolvedCouponInput input) {
    return new StripeManagedCouponRequest(
        amountOffInMinorUnits(input.amountOff()),
        input.code(),
        input.name(),
        input.active(),
        input.expiryDate(),
        input.maxRedemptions(),
        input.stripeCustomerOwnerId() == null
            ? null
            : stripeCustomerService.ensureStripeCustomer(
                input.stripeCustomerOwnerId(), Objects.requireNonNull(input.stripeCustomerEmail()))
    );
  }

  private @Nullable String normalizeName(@Nullable String name) {
    if (name == null) {
      return null;
    }
    String normalized = name.trim();
    return normalized.isEmpty() ? null : normalized;
  }

  private long amountOffInMinorUnits(BigDecimal amountOff) {
    return amountOff.movePointRight(2).longValueExact();
  }

  private void deleteStripeCouponAfterFailedPersistence(String stripeCouponId, RuntimeException original) {
    try {
      stripePaymentProvider.deleteCoupon(stripeCouponId);
    } catch (RuntimeException cleanupEx) {
      log.error("Failed to clean up Stripe coupon {} after local coupon persistence failure", stripeCouponId, cleanupEx);
      original.addSuppressed(cleanupEx);
    }
  }
}
