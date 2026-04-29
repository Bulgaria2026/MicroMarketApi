package com.noserbulgaria.micromarket.couponoffer;

import com.noserbulgaria.micromarket.auth.user.CustomUserDetails;
import com.noserbulgaria.micromarket.coupon.CouponResponse;
import com.noserbulgaria.micromarket.customer.StripeCustomerService;
import com.noserbulgaria.micromarket.exception.BadRequestApiException;
import com.noserbulgaria.micromarket.exception.NotFoundApiException;
import com.noserbulgaria.micromarket.payment.stripe.StripeManagedCoupon;
import com.noserbulgaria.micromarket.payment.stripe.StripeManagedCouponRequest;
import com.noserbulgaria.micromarket.payment.stripe.StripePaymentProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponOfferService {

  private final CouponOfferRepository couponOfferRepository;
  private final CouponOfferMapper couponOfferMapper;
  private final CouponOfferPurchaseTransactions purchaseTransactions;
  private final StripeCustomerService stripeCustomerService;
  private final StripePaymentProvider stripePaymentProvider;

  @Transactional(readOnly = true)
  public Page<CouponOfferResponse> findAll(Specification<CouponOffer> spec, Pageable pageable) {
    return couponOfferRepository.findAll(spec, pageable)
        .map(couponOfferMapper::toDto);
  }

  @Transactional(readOnly = true)
  public CouponOfferResponse getByIdOrThrow(UUID id) {
    return couponOfferRepository.findById(id)
        .map(couponOfferMapper::toDto)
        .orElseThrow(() -> new NotFoundApiException("Coupon offer with id '%s' not found".formatted(id)));
  }

  @Transactional(readOnly = true)
  public List<CouponOfferResponse> catalog() {
    Instant now = Instant.now();
    return couponOfferRepository.findAll(
            CouponOfferSpecification.availableAt(now),
            Sort.by(Sort.Direction.DESC, "createdAt"))
        .stream()
        .map(couponOfferMapper::toDto)
        .toList();
  }

  @Transactional
  public CouponOfferResponse create(CouponOfferRequest request) {
    validateDates(request.startDate(), request.expiryDate());

    CouponOffer couponOffer = new CouponOffer();
    applyLocalState(couponOffer, request);
    return couponOfferMapper.toDto(couponOfferRepository.saveAndFlush(couponOffer));
  }

  @Transactional
  public CouponOfferResponse updateOrThrow(UUID id, CouponOfferRequest request) {
    CouponOffer couponOffer = couponOfferRepository.findById(id)
        .orElseThrow(() -> new NotFoundApiException("Coupon offer with id '%s' not found".formatted(id)));

    validateDates(request.startDate(), request.expiryDate());
    if (request.maxPurchases() != null && request.maxPurchases() < couponOffer.getPurchaseCount()) {
      throw new BadRequestApiException("maxPurchases cannot be less than purchaseCount");
    }

    applyLocalState(couponOffer, request);
    return couponOfferMapper.toDto(couponOfferRepository.saveAndFlush(couponOffer));
  }

  public CouponResponse purchase(UUID id, CustomUserDetails userDetails) {
    CouponOfferPurchaseReservation reservation = purchaseTransactions.reserve(id, userDetails.getId());
    StripeManagedCoupon stripeCoupon = null;
    try {
      String stripeCustomerId = stripeCustomerService.ensureStripeCustomer(
          reservation.stripeCustomerOwnerId(), reservation.stripeCustomerEmail());
      stripeCoupon = stripePaymentProvider.createManagedCoupon(toManagedCouponRequest(reservation, stripeCustomerId));
      return purchaseTransactions.persistPurchasedCoupon(reservation, stripeCoupon);
    } catch (RuntimeException ex) {
      if (stripeCoupon != null) {
        deleteStripeCouponAfterFailedPurchase(stripeCoupon.stripeCouponId(), ex);
      }
      compensateReservation(reservation, ex);
      throw ex;
    }
  }

  private StripeManagedCouponRequest toManagedCouponRequest(
      CouponOfferPurchaseReservation reservation, String stripeCustomerId) {
    return new StripeManagedCouponRequest(
        amountOffInMinorUnits(reservation.amountOff()),
        reservation.code(),
        reservation.name(),
        true,
        reservation.expiryDate(),
        1,
        stripeCustomerId
    );
  }

  private void applyLocalState(CouponOffer couponOffer, CouponOfferRequest request) {
    couponOffer.setName(normalizeName(request.name()));
    couponOffer.setDescription(normalizeDescription(request.description()));
    couponOffer.setStartDate(request.startDate());
    couponOffer.setExpiryDate(request.expiryDate());
    couponOffer.setPointCost(request.pointCost());
    couponOffer.setAmountOff(normalizeAmountOff(request.amountOff()));
    couponOffer.setMaxPurchases(request.maxPurchases());
    couponOffer.setActive(request.active() == null || request.active());
  }

  private void validateDates(@Nullable Instant startDate, @Nullable Instant expiryDate) {
    if (startDate != null && expiryDate != null && !expiryDate.isAfter(startDate)) {
      throw new BadRequestApiException("expiryDate must be after startDate");
    }
  }

  private String normalizeName(String name) {
    String normalized = name.trim();
    if (normalized.isBlank()) {
      throw new BadRequestApiException("name must not be blank");
    }
    return normalized;
  }

  private @Nullable String normalizeDescription(@Nullable String description) {
    if (description == null) {
      return null;
    }
    String normalized = description.trim();
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

  private void deleteStripeCouponAfterFailedPurchase(String stripeCouponId, RuntimeException original) {
    try {
      stripePaymentProvider.deleteCoupon(stripeCouponId);
    } catch (RuntimeException cleanupEx) {
      log.error("Failed to clean up Stripe coupon {} after coupon offer purchase failure", stripeCouponId, cleanupEx);
      original.addSuppressed(cleanupEx);
    }
  }

  private void compensateReservation(CouponOfferPurchaseReservation reservation, RuntimeException original) {
    try {
      purchaseTransactions.compensate(reservation);
    } catch (RuntimeException compensationEx) {
      log.error(
          "Failed to compensate coupon offer reservation for offer {} and user {}",
          reservation.couponOfferId(), reservation.userId(), compensationEx);
      original.addSuppressed(compensationEx);
    }
  }
}
