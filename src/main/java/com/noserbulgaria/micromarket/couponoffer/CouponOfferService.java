package com.noserbulgaria.micromarket.couponoffer;

import com.noserbulgaria.micromarket.auth.user.CustomUserDetails;
import com.noserbulgaria.micromarket.auth.user.User;
import com.noserbulgaria.micromarket.auth.user.UserRepository;
import com.noserbulgaria.micromarket.coupon.CouponResponse;
import com.noserbulgaria.micromarket.coupon.CouponService;
import com.noserbulgaria.micromarket.customer.PointChangeReason;
import com.noserbulgaria.micromarket.customer.Profile;
import com.noserbulgaria.micromarket.customer.ProfileRepository;
import com.noserbulgaria.micromarket.exception.BadRequestApiException;
import com.noserbulgaria.micromarket.exception.NotFoundApiException;
import lombok.RequiredArgsConstructor;
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

@Service
@RequiredArgsConstructor
@Transactional
public class CouponOfferService {

  private final CouponOfferRepository couponOfferRepository;
  private final CouponOfferMapper couponOfferMapper;
  private final CouponService couponService;
  private final UserRepository userRepository;
  private final ProfileRepository profileRepository;

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

  public CouponOfferResponse create(CouponOfferRequest request) {
    validateDates(request.startDate(), request.expiryDate());

    CouponOffer couponOffer = new CouponOffer();
    applyLocalState(couponOffer, request);
    return couponOfferMapper.toDto(couponOfferRepository.saveAndFlush(couponOffer));
  }

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
    CouponOffer couponOffer = couponOfferRepository.findById(id)
        .orElseThrow(() -> new NotFoundApiException("Coupon offer with id '%s' not found".formatted(id)));
    validatePurchasable(couponOffer, Instant.now());

    User user = userRepository.findById(userDetails.getId())
        .orElseThrow(() -> new NotFoundApiException("User with id '%s' not found".formatted(userDetails.getId())));

    Profile profile = ensureProfile(user);
    if (couponOffer.getPointCost() > 0) {
      int updated = profileRepository.trySpendPoints(user.getId(), couponOffer.getPointCost(), PointChangeReason.COUPON_PURCHASED);
      if (updated != 1) {
        throw new BadRequestApiException("Insufficient points to purchase coupon offer");
      }
    }

    CouponResponse issuedCoupon = couponService.issuePurchasedCoupon(couponOffer, user);
    couponOffer.setPurchaseCount(couponOffer.getPurchaseCount() + 1);
    if (couponOffer.getMaxPurchases() != null && couponOffer.getPurchaseCount() >= couponOffer.getMaxPurchases()) {
      couponOffer.setActive(false);
    }
    couponOfferRepository.saveAndFlush(couponOffer);
    return issuedCoupon;
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

  private void validatePurchasable(CouponOffer couponOffer, Instant now) {
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

  private void validateDates(@Nullable Instant startDate, @Nullable Instant expiryDate) {
    if (startDate != null && expiryDate != null && !expiryDate.isAfter(startDate)) {
      throw new BadRequestApiException("expiryDate must be after startDate");
    }
  }

  private Profile ensureProfile(User user) {
    return profileRepository.findByUserId(user.getId())
        .orElseGet(() -> {
          Profile profile = new Profile();
          profile.setUser(user);
          profile.setPoints(0);
          return profileRepository.saveAndFlush(profile);
        });
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
}
