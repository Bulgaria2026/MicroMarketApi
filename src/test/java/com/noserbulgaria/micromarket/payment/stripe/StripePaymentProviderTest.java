package com.noserbulgaria.micromarket.payment.stripe;

import com.noserbulgaria.micromarket.exception.StripeApiException;
import com.noserbulgaria.micromarket.order.Order;
import com.stripe.StripeClient;
import com.stripe.exception.ApiConnectionException;
import com.stripe.exception.InvalidRequestException;
import com.stripe.exception.RateLimitException;
import com.stripe.model.Charge;
import com.stripe.model.Coupon;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.PromotionCode;
import com.stripe.model.Refund;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.param.CouponCreateParams;
import com.stripe.param.PromotionCodeCreateParams;
import com.stripe.param.PromotionCodeUpdateParams;
import com.stripe.param.checkout.SessionCreateParams;
import com.stripe.service.CouponService;
import com.stripe.service.PromotionCodeService;
import com.stripe.service.CheckoutService;
import com.stripe.service.V1Services;
import com.stripe.service.checkout.SessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StripePaymentProviderTest {

  @Mock private StripeClient stripeClient;
  @Mock private V1Services v1Services;
  @Mock private CheckoutService checkoutService;
  @Mock private SessionService sessionService;
  @Mock private CouponService couponService;
  @Mock private PromotionCodeService promotionCodeService;

  private final StripeProperties properties = new StripeProperties(
      "sk_test_dummy", "whsec_test_dummy", "https://success", "https://cancel", 30);

  private StripePaymentProvider provider;

  @BeforeEach
  void setUp() {
    provider = new StripePaymentProvider(stripeClient, properties);
  }

  @Test
  void createCheckoutSession_setsMetadataOnBothSessionAndPaymentIntent() throws Exception {
    UUID orderId = UUID.randomUUID();
    Order order = new Order();
    order.setId(orderId);
    order.setOrderNumber("MM-123456");

    Session resultSession = new Session();
    resultSession.setId("cs_test_abc");
    resultSession.setUrl("https://checkout.stripe.test/cs_test_abc");

    when(stripeClient.v1()).thenReturn(v1Services);
    when(v1Services.checkout()).thenReturn(checkoutService);
    when(checkoutService.sessions()).thenReturn(sessionService);
    when(sessionService.create(any(SessionCreateParams.class), any(RequestOptions.class)))
        .thenReturn(resultSession);

    provider.createCheckoutSession(order, "cus_test");

    ArgumentCaptor<SessionCreateParams> captor = ArgumentCaptor.forClass(SessionCreateParams.class);
    verify(sessionService).create(captor.capture(), any(RequestOptions.class));
    SessionCreateParams params = captor.getValue();

    assertThat(params.getMetadata())
        .as("order_number must be on the Session metadata (visible on Checkout Sessions in the dashboard)")
        .containsEntry("order_number", "MM-123456");
    assertThat(params.getPaymentIntentData())
        .as("payment_intent_data must be set so metadata propagates to the PaymentIntent")
        .isNotNull();
    assertThat(params.getPaymentIntentData().getMetadata())
        .as("order_number must be on the PaymentIntent metadata (visible on Payments in the dashboard)")
        .containsEntry("order_number", "MM-123456");
  }

  @Test
  void createCoupon_usesHardcodedEuroCurrency() throws Exception {
    Coupon createdCoupon = new Coupon();
    createdCoupon.setId("coupon_123");

    when(stripeClient.v1()).thenReturn(v1Services);
    when(v1Services.coupons()).thenReturn(couponService);
    when(couponService.create(any(CouponCreateParams.class))).thenReturn(createdCoupon);

    String result = provider.createCoupon(500L, "Welcome coupon");

    assertThat(result).isEqualTo("coupon_123");

    ArgumentCaptor<CouponCreateParams> captor = ArgumentCaptor.forClass(CouponCreateParams.class);
    verify(couponService).create(captor.capture());
    CouponCreateParams params = captor.getValue();
    assertThat(params.getAmountOff()).isEqualTo(500L);
    assertThat(params.getCurrency()).isEqualTo("eur");
    assertThat(params.getDuration()).isEqualTo(CouponCreateParams.Duration.ONCE);
    assertThat(params.getName()).isEqualTo("Welcome coupon");
  }

  @Test
  void createManagedCoupon_setsStripeCouponAndPromotionCodeParams() throws Exception {
    Coupon createdCoupon = new Coupon();
    createdCoupon.setId("coupon_123");

    PromotionCode createdPromotionCode = new PromotionCode();
    createdPromotionCode.setId("promo_123");
    createdPromotionCode.setCode("WELCOME-5");
    createdPromotionCode.setTimesRedeemed(0L);
    createdPromotionCode.setActive(true);

    when(stripeClient.v1()).thenReturn(v1Services);
    when(v1Services.coupons()).thenReturn(couponService);
    when(v1Services.promotionCodes()).thenReturn(promotionCodeService);
    when(couponService.create(any(CouponCreateParams.class))).thenReturn(createdCoupon);
    when(promotionCodeService.create(any(PromotionCodeCreateParams.class))).thenReturn(createdPromotionCode);

    provider.createManagedCoupon(new StripeManagedCouponRequest(
        500L,
        "WELCOME-5",
        "Welcome coupon",
        true,
        java.time.Instant.parse("2026-12-31T23:59:59Z"),
        10,
        "cus_123"
    ));

    ArgumentCaptor<CouponCreateParams> couponCaptor = ArgumentCaptor.forClass(CouponCreateParams.class);
    verify(couponService).create(couponCaptor.capture());
    CouponCreateParams couponParams = couponCaptor.getValue();
    assertThat(couponParams.getAmountOff()).isEqualTo(500L);
    assertThat(couponParams.getCurrency()).isEqualTo("eur");
    assertThat(couponParams.getDuration()).isEqualTo(CouponCreateParams.Duration.ONCE);
    assertThat(couponParams.getName()).isEqualTo("Welcome coupon");

    ArgumentCaptor<PromotionCodeCreateParams> promoCaptor = ArgumentCaptor.forClass(PromotionCodeCreateParams.class);
    verify(promotionCodeService).create(promoCaptor.capture());
    PromotionCodeCreateParams promoParams = promoCaptor.getValue();
    assertThat(promoParams.getCode()).isEqualTo("WELCOME-5");
    assertThat(promoParams.getCustomer()).isEqualTo("cus_123");
    assertThat(promoParams.getMaxRedemptions()).isEqualTo(10L);
    assertThat(promoParams.getPromotion()).isNotNull();
    assertThat(promoParams.getPromotion().getCoupon()).isEqualTo("coupon_123");
    assertThat(promoParams.getPromotion().getType()).isEqualTo(PromotionCodeCreateParams.Promotion.Type.COUPON);
  }

  @Test
  void createPromotionCode_setsCustomerRestrictionAndUsageLimits() throws Exception {
    PromotionCode createdPromotionCode = new PromotionCode();
    createdPromotionCode.setId("promo_123");
    createdPromotionCode.setCode("WELCOME-5");
    createdPromotionCode.setTimesRedeemed(0L);
    createdPromotionCode.setActive(true);

    when(stripeClient.v1()).thenReturn(v1Services);
    when(v1Services.promotionCodes()).thenReturn(promotionCodeService);
    when(promotionCodeService.create(any(PromotionCodeCreateParams.class))).thenReturn(createdPromotionCode);

    StripeManagedCoupon result = provider.createPromotionCode("coupon_123", new StripePromotionCodeRequest(
        "WELCOME-5",
        true,
        java.time.Instant.parse("2026-12-31T23:59:59Z"),
        1,
        "cus_123"
    ));

    assertThat(result.stripeCouponId()).isEqualTo("coupon_123");
    assertThat(result.stripePromotionCodeId()).isEqualTo("promo_123");
    assertThat(result.code()).isEqualTo("WELCOME-5");

    ArgumentCaptor<PromotionCodeCreateParams> captor = ArgumentCaptor.forClass(PromotionCodeCreateParams.class);
    verify(promotionCodeService).create(captor.capture());
    PromotionCodeCreateParams params = captor.getValue();
    assertThat(params.getCustomer()).isEqualTo("cus_123");
    assertThat(params.getMaxRedemptions()).isEqualTo(1L);
    assertThat(params.getCode()).isEqualTo("WELCOME-5");
  }

  @Test
  void createCheckoutSession_allowsPromotionCodesWithoutBackendDiscount() throws Exception {
    UUID orderId = UUID.randomUUID();
    Order order = new Order();
    order.setId(orderId);
    order.setOrderNumber("MM-123456");

    Session resultSession = new Session();
    resultSession.setId("cs_test_abc");
    resultSession.setUrl("https://checkout.stripe.test/cs_test_abc");

    when(stripeClient.v1()).thenReturn(v1Services);
    when(v1Services.checkout()).thenReturn(checkoutService);
    when(checkoutService.sessions()).thenReturn(sessionService);
    when(sessionService.create(any(SessionCreateParams.class), any(RequestOptions.class)))
        .thenReturn(resultSession);

    provider.createCheckoutSession(order, "cus_test");

    ArgumentCaptor<SessionCreateParams> captor = ArgumentCaptor.forClass(SessionCreateParams.class);
    verify(sessionService).create(captor.capture(), any(RequestOptions.class));
    assertThat(captor.getValue().getAllowPromotionCodes()).isTrue();
    assertThat(captor.getValue().getDiscounts()).isNull();
  }

  @Test
  void updatePromotionCodeActive_returnsUpdatedManagedCoupon() throws Exception {
    PromotionCode promotionCode = new PromotionCode();
    promotionCode.setId("promo_123");
    promotionCode.setCode("WELCOME-5");
    promotionCode.setTimesRedeemed(4L);
    promotionCode.setActive(false);
    PromotionCode.Promotion promotion = new PromotionCode.Promotion();
    promotion.setCoupon("coupon_123");
    promotionCode.setPromotion(promotion);

    when(stripeClient.v1()).thenReturn(v1Services);
    when(v1Services.promotionCodes()).thenReturn(promotionCodeService);
    when(promotionCodeService.update(anyString(), any(PromotionCodeUpdateParams.class))).thenReturn(promotionCode);

    StripeManagedCoupon result = provider.updatePromotionCodeActive("promo_123", false);

    assertThat(result.stripeCouponId()).isEqualTo("coupon_123");
    assertThat(result.stripePromotionCodeId()).isEqualTo("promo_123");
    assertThat(result.code()).isEqualTo("WELCOME-5");
    assertThat(result.timesRedeemed()).isEqualTo(4);
    assertThat(result.active()).isFalse();

    ArgumentCaptor<PromotionCodeUpdateParams> captor = ArgumentCaptor.forClass(PromotionCodeUpdateParams.class);
    verify(promotionCodeService).update(org.mockito.ArgumentMatchers.eq("promo_123"), captor.capture());
    assertThat(captor.getValue().getActive()).isFalse();
  }

  @Test
  void deleteCoupon_deletesStripeCoupon() throws Exception {
    when(stripeClient.v1()).thenReturn(v1Services);
    when(v1Services.coupons()).thenReturn(couponService);

    provider.deleteCoupon("coupon_123");

    verify(couponService).delete("coupon_123");
  }

  @Test
  void retrievePromotionCode_invalidRequestMapsToNotFound() throws Exception {
    when(stripeClient.v1()).thenReturn(v1Services);
    when(v1Services.promotionCodes()).thenReturn(promotionCodeService);
    when(promotionCodeService.retrieve("promo_missing"))
        .thenThrow(new InvalidRequestException("No such promotion_code", "id", "req_123", "resource_missing", 404, null));

    assertThatThrownBy(() -> provider.retrievePromotionCode("promo_missing"))
        .isInstanceOf(StripeApiException.class)
        .hasMessage("Failed to retrieve Stripe promotion code promo_missing");
  }

  @Test
  void deleteCoupon_rateLimitMapsToTooManyRequests() throws Exception {
    when(stripeClient.v1()).thenReturn(v1Services);
    when(v1Services.coupons()).thenReturn(couponService);
    when(couponService.delete("coupon_123"))
        .thenThrow(new RateLimitException("Too many requests", null, "req_123", "rate_limit", 429, null));

    assertThatThrownBy(() -> provider.deleteCoupon("coupon_123"))
        .isInstanceOf(StripeApiException.class)
        .hasMessage("Failed to delete Stripe coupon coupon_123");
  }

  @Test
  void createCheckoutSession_connectionFailureMapsToServiceUnavailable() throws Exception {
    Order order = new Order();
    order.setId(UUID.randomUUID());
    order.setOrderNumber("MM-123456");

    when(stripeClient.v1()).thenReturn(v1Services);
    when(v1Services.checkout()).thenReturn(checkoutService);
    when(checkoutService.sessions()).thenReturn(sessionService);
    when(sessionService.create(any(SessionCreateParams.class), any(RequestOptions.class)))
        .thenThrow(new ApiConnectionException("Stripe unreachable"));

    assertThatThrownBy(() -> provider.createCheckoutSession(order, "cus_test"))
        .isInstanceOf(StripeApiException.class)
        .hasMessage("Failed to create Stripe Checkout session for order %s".formatted(order.getId()));
  }

  @Test
  void retrievePromotionCodeWithoutCouponReferenceMapsToBadGateway() throws Exception {
    PromotionCode promotionCode = new PromotionCode();
    promotionCode.setId("promo_orphan");
    promotionCode.setCode("ORPHAN");
    promotionCode.setTimesRedeemed(0L);
    promotionCode.setActive(true);

    when(stripeClient.v1()).thenReturn(v1Services);
    when(v1Services.promotionCodes()).thenReturn(promotionCodeService);
    when(promotionCodeService.retrieve("promo_orphan")).thenReturn(promotionCode);

    assertThatThrownBy(() -> provider.retrievePromotionCode("promo_orphan"))
        .isInstanceOf(StripeApiException.class)
        .hasMessage("Stripe promotion code promo_orphan has no coupon reference");
  }

  @Test
  void verifyAndParse_chargeRefunded_fullRefund_emitsPaymentRefunded() throws Exception {
    Event event = mock(Event.class);
    when(event.getId()).thenReturn("evt_refund_full");
    when(event.getType()).thenReturn("charge.refunded");

    Charge charge = new Charge();
    charge.setId("ch_full");
    charge.setPaymentIntent("pi_full");
    charge.setRefunded(true);
    charge.setAmount(1000L);
    charge.setAmountRefunded(1000L);

    EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
    when(deserializer.getObject()).thenReturn(Optional.of(charge));
    when(event.getDataObjectDeserializer()).thenReturn(deserializer);

    when(stripeClient.constructEvent(anyString(), anyString(), anyString())).thenReturn(event);

    Optional<StripeWebhookEvent> result = provider.verifyAndParse("{}", "sig");

    assertThat(result).isPresent();
    assertThat(result.get()).isInstanceOf(StripeWebhookEvent.PaymentRefunded.class);
    StripeWebhookEvent.PaymentRefunded refunded = (StripeWebhookEvent.PaymentRefunded) result.get();
    assertThat(refunded.eventId()).isEqualTo("evt_refund_full");
    assertThat(refunded.paymentIntentId()).isEqualTo("pi_full");
  }

  @Test
  void verifyAndParse_chargeRefunded_partialRefund_returnsEmpty() throws Exception {
    Event event = mock(Event.class);
    when(event.getType()).thenReturn("charge.refunded");

    Charge charge = new Charge();
    charge.setId("ch_partial");
    charge.setPaymentIntent("pi_partial");
    charge.setRefunded(false);
    charge.setAmount(1000L);
    charge.setAmountRefunded(300L);

    EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
    when(deserializer.getObject()).thenReturn(Optional.of(charge));
    when(event.getDataObjectDeserializer()).thenReturn(deserializer);

    when(stripeClient.constructEvent(anyString(), anyString(), anyString())).thenReturn(event);

    Optional<StripeWebhookEvent> result = provider.verifyAndParse("{}", "sig");

    assertThat(result).isEmpty();
  }

  @Test
  void verifyAndParse_refundFailed_emitsPaymentRefundFailed() throws Exception {
    Event event = mock(Event.class);
    when(event.getId()).thenReturn("evt_refund_failed");
    when(event.getType()).thenReturn("refund.failed");

    Refund refund = new Refund();
    refund.setId("re_1");
    refund.setPaymentIntent("pi_reverted");
    refund.setStatus("failed");

    EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
    when(deserializer.getObject()).thenReturn(Optional.of(refund));
    when(event.getDataObjectDeserializer()).thenReturn(deserializer);

    when(stripeClient.constructEvent(anyString(), anyString(), anyString())).thenReturn(event);

    Optional<StripeWebhookEvent> result = provider.verifyAndParse("{}", "sig");

    assertThat(result).isPresent();
    assertThat(result.get()).isInstanceOf(StripeWebhookEvent.PaymentRefundFailed.class);
    StripeWebhookEvent.PaymentRefundFailed failed = (StripeWebhookEvent.PaymentRefundFailed) result.get();
    assertThat(failed.eventId()).isEqualTo("evt_refund_failed");
    assertThat(failed.paymentIntentId()).isEqualTo("pi_reverted");
  }
}
