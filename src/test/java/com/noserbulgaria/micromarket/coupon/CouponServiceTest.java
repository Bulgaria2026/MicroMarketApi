package com.noserbulgaria.micromarket.coupon;

import com.noserbulgaria.micromarket.customer.StripeCustomerService;
import com.noserbulgaria.micromarket.payment.stripe.StripeManagedCoupon;
import com.noserbulgaria.micromarket.payment.stripe.StripePaymentProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CouponServiceTest {

  @Mock
  private CouponRepository couponRepository;
  @Mock
  private CouponTransactions couponTransactions;
  @Mock
  private CouponMapper couponMapper;
  @Mock
  private StripePaymentProvider stripePaymentProvider;
  @Mock
  private StripeCustomerService stripeCustomerService;

  @Test
  void create_whenLocalPersistenceFailsDeletesStripeCouponAndRethrows() {
    CouponService service = new CouponService(
        couponRepository, couponTransactions, couponMapper, stripePaymentProvider, stripeCustomerService);
    CouponRequest request = new CouponRequest(
        null, "SAVE-FAIL", null, null, 0, new BigDecimal("5.00"), null, true);
    ResolvedCouponInput input = new ResolvedCouponInput(
        null, null, null, "SAVE-FAIL", null, null, 0, new BigDecimal("5.00"), null, true);
    StripeManagedCoupon stripeCoupon = new StripeManagedCoupon("coupon_123", "promo_123", "SAVE-FAIL", 0, true);
    DataIntegrityViolationException failure = new DataIntegrityViolationException("duplicate code");

    when(couponTransactions.resolveDirectCouponInputForCreate(request)).thenReturn(input);
    when(stripePaymentProvider.createManagedCoupon(any())).thenReturn(stripeCoupon);
    when(couponTransactions.persistDirectCoupon(input, stripeCoupon)).thenThrow(failure);

    assertThatThrownBy(() -> service.create(request)).isSameAs(failure);

    verify(stripePaymentProvider).deleteCoupon("coupon_123");
  }
}
