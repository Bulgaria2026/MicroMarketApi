package com.noserbulgaria.micromarket.couponoffer;

import com.noserbulgaria.micromarket.customer.StripeCustomerService;
import com.noserbulgaria.micromarket.payment.stripe.StripeManagedCoupon;
import com.noserbulgaria.micromarket.payment.stripe.StripePaymentProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CouponOfferServiceTest {

  @Mock
  private CouponOfferRepository couponOfferRepository;
  @Mock
  private CouponOfferMapper couponOfferMapper;
  @Mock
  private CouponOfferPurchaseTransactions purchaseTransactions;
  @Mock
  private StripeCustomerService stripeCustomerService;
  @Mock
  private StripePaymentProvider stripePaymentProvider;

  @Test
  void purchase_whenStripeCouponCreationFailsCompensatesReservation() {
    CouponOfferService service = service();
    CouponOfferPurchaseReservation reservation = reservation();
    RuntimeException failure = new RuntimeException("stripe down");

    when(purchaseTransactions.reserve(reservation.couponOfferId(), reservation.userId())).thenReturn(reservation);
    when(stripeCustomerService.ensureStripeCustomer(reservation.stripeCustomerOwnerId(), reservation.stripeCustomerEmail()))
        .thenReturn("cus_123");
    when(stripePaymentProvider.createManagedCoupon(any())).thenThrow(failure);

    assertThatThrownBy(() -> service.purchase(reservation.couponOfferId(), newUserDetails(reservation.userId())))
        .isSameAs(failure);

    verify(purchaseTransactions).compensate(reservation);
    verify(stripePaymentProvider, never()).deleteCoupon(any());
  }

  @Test
  void purchase_whenLocalCouponPersistenceFailsDeletesStripeCouponAndCompensatesReservation() {
    CouponOfferService service = service();
    CouponOfferPurchaseReservation reservation = reservation();
    StripeManagedCoupon stripeCoupon = new StripeManagedCoupon("coupon_123", "promo_123", reservation.code(), 0, true);
    DataIntegrityViolationException failure = new DataIntegrityViolationException("duplicate promotion id");

    when(purchaseTransactions.reserve(reservation.couponOfferId(), reservation.userId())).thenReturn(reservation);
    when(stripeCustomerService.ensureStripeCustomer(reservation.stripeCustomerOwnerId(), reservation.stripeCustomerEmail()))
        .thenReturn("cus_123");
    when(stripePaymentProvider.createManagedCoupon(any())).thenReturn(stripeCoupon);
    when(purchaseTransactions.persistPurchasedCoupon(reservation, stripeCoupon)).thenThrow(failure);

    assertThatThrownBy(() -> service.purchase(reservation.couponOfferId(), newUserDetails(reservation.userId())))
        .isSameAs(failure);

    verify(stripePaymentProvider).deleteCoupon("coupon_123");
    verify(purchaseTransactions).compensate(reservation);
  }

  private CouponOfferService service() {
    return new CouponOfferService(
        couponOfferRepository, couponOfferMapper, purchaseTransactions, stripeCustomerService, stripePaymentProvider);
  }

  private CouponOfferPurchaseReservation reservation() {
    UUID userId = UUID.randomUUID();
    return new CouponOfferPurchaseReservation(
        UUID.randomUUID(),
        userId,
        UUID.randomUUID(),
        "user@example.com",
        "MM-TEST",
        "Test offer",
        null,
        20,
        new BigDecimal("5.00"),
        false
    );
  }

  private com.noserbulgaria.micromarket.auth.user.CustomUserDetails newUserDetails(UUID userId) {
    com.noserbulgaria.micromarket.auth.user.User user = new com.noserbulgaria.micromarket.auth.user.User();
    user.setId(userId);
    com.noserbulgaria.micromarket.customer.Customer customer =
        new com.noserbulgaria.micromarket.customer.Customer();
    customer.setEmail("user@example.com");
    com.noserbulgaria.micromarket.customer.Profile profile =
        new com.noserbulgaria.micromarket.customer.Profile();
    profile.setUser(user);
    profile.setCustomer(customer);
    customer.setProfile(profile);
    return new com.noserbulgaria.micromarket.auth.user.CustomUserDetails(profile);
  }
}
