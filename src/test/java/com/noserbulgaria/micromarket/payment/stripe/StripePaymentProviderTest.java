package com.noserbulgaria.micromarket.payment.stripe;

import com.noserbulgaria.micromarket.order.Order;
import com.stripe.StripeClient;
import com.stripe.model.Charge;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.Refund;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.param.checkout.SessionCreateParams;
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
