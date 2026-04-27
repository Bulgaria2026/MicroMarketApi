package com.noserbulgaria.micromarket.checkout;

import com.jayway.jsonpath.JsonPath;
import com.noserbulgaria.micromarket.customer.Customer;
import com.noserbulgaria.micromarket.customer.CustomerRepository;
import com.noserbulgaria.micromarket.customer.Profile;
import com.noserbulgaria.micromarket.order.Order;
import com.noserbulgaria.micromarket.order.OrderItem;
import com.noserbulgaria.micromarket.order.OrderRepository;
import com.noserbulgaria.micromarket.order.OrderStatusType;
import com.noserbulgaria.micromarket.product.Product;
import com.noserbulgaria.micromarket.product.ProductRepository;
import com.noserbulgaria.micromarket.exception.BadRequestApiException;
import com.noserbulgaria.micromarket.payment.stripe.StripeCheckoutSession;
import com.noserbulgaria.micromarket.payment.stripe.StripePaymentProvider;
import com.noserbulgaria.micromarket.payment.stripe.StripeWebhookEvent;
import com.noserbulgaria.micromarket.payment.stripe.event.StripeEventRepository;
import com.noserbulgaria.micromarket.auth.user.Role;
import com.noserbulgaria.micromarket.auth.user.User;
import com.noserbulgaria.micromarket.auth.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderPlacementIntegrationTests {

  private static final String USER_EMAIL = "user@micromarket.dev";
  private static final String PASSWORD = "user123";
  private static final String SIGNATURE_HEADER = "Stripe-Signature";

  @Autowired private MockMvc mockMvc;
  @Autowired private UserRepository userRepository;
  @Autowired private ProductRepository productRepository;
  @Autowired private OrderRepository orderRepository;
  @Autowired private CustomerRepository customerRepository;
  @Autowired private StripeEventRepository stripeEventRepository;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private TransactionTemplate transactionTemplate;

  @MockitoBean private StripePaymentProvider stripePaymentProvider;

  private Product sparklingWater;
  private Product coffeeBeans;

  @BeforeEach
  void setUp() {
    stripeEventRepository.deleteAll();
    orderRepository.deleteAll();
    customerRepository.deleteAll();
    productRepository.deleteAll();
    userRepository.deleteAll();

    AtomicInteger customerCounter = new AtomicInteger();
    when(stripePaymentProvider.createCustomer(any()))
        .thenAnswer(_ -> "cus_fake_" + customerCounter.incrementAndGet());
    when(stripePaymentProvider.createCheckoutSession(any(), any()))
        .thenAnswer(inv -> {
          Order order = inv.getArgument(0);
          String sessionId = "cs_fake_" + order.getId();
          return new StripeCheckoutSession(sessionId, "https://checkout.stripe.test/" + sessionId);
        });

    User user = new User();
    user.setPassword(Objects.requireNonNull(passwordEncoder.encode(PASSWORD)));
    user.setRole(Role.USER);
    user = userRepository.saveAndFlush(user);

    Customer customer = new Customer();
    customer.setEmail(USER_EMAIL);
    Profile profile = new Profile();
    profile.setCustomer(customer);
    profile.setUser(user);
    profile.setPoints(0);
    customer.setProfile(profile);
    customerRepository.saveAndFlush(customer);

    sparklingWater = product("Sparkling Water", new BigDecimal("29.99"), 0, true, 100);
    coffeeBeans = product("Coffee Beans", new BigDecimal("99.50"), 10, true, 5);
  }

  @AfterEach
  void tearDown() {
    stripeEventRepository.deleteAll();
    orderRepository.deleteAll();
    customerRepository.deleteAll();
    productRepository.deleteAll();
    userRepository.deleteAll();
  }

  @Test
  void guestCheckout_createsGuestAndPendingOrderWithCheckoutSession() throws Exception {
    MvcResult result = mockMvc.perform(post("/order")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body(sparklingWater.getId(), 2, "guest@example.com")))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.orderId").isNotEmpty())
        .andExpect(jsonPath("$.orderNumber").value(org.hamcrest.Matchers.matchesRegex("^MM-\\d{6}$")))
        .andExpect(jsonPath("$.totalAmount").value(59.98))
        .andExpect(jsonPath("$.checkoutUrl").value(org.hamcrest.Matchers.startsWith("https://checkout.stripe.test/cs_fake_")))
        .andReturn();

    UUID orderId = UUID.fromString(JsonPath.read(result.getResponse().getContentAsString(), "$.orderId"));
    UUID customerId = transactionTemplate.execute(_ -> {
      Order order = orderRepository.findById(orderId).orElseThrow();
      assertThat(order.getOrderNumber()).matches("^MM-\\d{6}$");
      assertThat(order.getEmail()).isEqualTo("guest@example.com");
      assertThat(order.getStatus()).isEqualTo(OrderStatusType.PENDING_PAYMENT);
      assertThat(order.getStripeCheckoutSessionId()).isEqualTo("cs_fake_" + orderId);
      assertThat(order.getOrderItems()).hasSize(1);
      assertThat(order.getTotalAmount()).isEqualByComparingTo("59.98");
      OrderItem line = order.getOrderItems().iterator().next();
      assertThat(line.getProductName()).isEqualTo("Sparkling Water");
      assertThat(line.getOriginalUnitPrice()).isEqualByComparingTo("29.99");
      assertThat(line.getPriceAtPurchase()).isEqualByComparingTo("29.99");
      return order.getCustomer().getId();
    });

    Customer guest = customerRepository.findByEmail("guest@example.com").orElseThrow();
    assertThat(guest.isRegistered()).isFalse();
    assertThat(customerId).isEqualTo(guest.getId());
  }

  @Test
  void guestCheckout_twiceWithSameEmail_reusesSameGuest() throws Exception {
    mockMvc.perform(post("/order")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body(sparklingWater.getId(), 1, "repeat@example.com")))
        .andExpect(status().isCreated());
    mockMvc.perform(post("/order")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body(sparklingWater.getId(), 1, "REPEAT@EXAMPLE.COM")))
        .andExpect(status().isCreated());
    assertThat(customerRepository.findByEmail("repeat@example.com")).isPresent();
    assertThat(customerRepository.findAll().stream().filter(c -> !c.isRegistered()).toList()).hasSize(1);
  }

  @Test
  void guestCheckout_twiceWithSameEmail_reusesSameStripeCustomerId() throws Exception {
    placeOrderAsGuest(sparklingWater.getId(), 1, "stripe-reuse@example.com");
    placeOrderAsGuest(sparklingWater.getId(), 1, "stripe-reuse@example.com");

    Customer guest = customerRepository.findByEmail("stripe-reuse@example.com").orElseThrow();
    assertThat(guest.getStripeCustomerId()).isNotNull();

    verify(stripePaymentProvider, times(1)).createCustomer("stripe-reuse@example.com");
    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    verify(stripePaymentProvider, times(2)).createCheckoutSession(any(), captor.capture());
    assertThat(captor.getAllValues())
        .containsExactly(guest.getStripeCustomerId(), guest.getStripeCustomerId());
  }

  @Test
  void guestCheckout_withoutEmail_returnsBadRequest() throws Exception {
    mockMvc.perform(post("/order")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body(sparklingWater.getId(), 1, null)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detail").value("Email is required for guest checkout"));
  }

  @Test
  void authenticatedCheckout_reusesExistingCustomer() throws Exception {
    String token = accessTokenFor(USER_EMAIL, PASSWORD);
    long customersBefore = customerRepository.count();

    mockMvc.perform(post("/order")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body(sparklingWater.getId(), 1, null)))
        .andExpect(status().isCreated());

    assertThat(customerRepository.count()).isEqualTo(customersBefore);

    mockMvc.perform(post("/order")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body(sparklingWater.getId(), 1, null)))
        .andExpect(status().isCreated());

    assertThat(customerRepository.count()).isEqualTo(customersBefore);
  }

  @Test
  void checkout_computesDiscountedTotalServerSide() throws Exception {
    MvcResult result = mockMvc.perform(post("/order")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body(coffeeBeans.getId(), 2, "discount@example.com")))
        .andExpect(status().isCreated())
        .andReturn();

    BigDecimal amount = new BigDecimal(
        JsonPath.read(result.getResponse().getContentAsString(), "$.totalAmount").toString());
    assertThat(amount).isEqualByComparingTo("179.10");
  }

  @Test
  void checkout_insufficientStock_returnsBadRequestAndDoesNotPersistOrder() throws Exception {
    long customersBefore = customerRepository.count();
    mockMvc.perform(post("/order")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body(coffeeBeans.getId(), 999, "bulk@example.com")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("Insufficient stock")));

    assertThat(orderRepository.findAll()).isEmpty();
    assertThat(customerRepository.count()).isEqualTo(customersBefore);
  }

  @Test
  void checkout_disabledProduct_respondsAsIfItDoesNotExist() throws Exception {
    Product disabled = product("Hidden", new BigDecimal("5.00"), 0, false, 10);
    mockMvc.perform(post("/order")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body(disabled.getId(), 1, "off@example.com")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detail").value("One or more products do not exist"));
  }

  @Test
  void checkout_unknownProduct_returnsBadRequest() throws Exception {
    mockMvc.perform(post("/order")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body(UUID.randomUUID(), 1, "ghost@example.com")))
        .andExpect(status().isBadRequest());
  }

  @Test
  void webhook_checkoutSucceeded_marksOrderPaidCapturesPiIdAndDecrementsStock() throws Exception {
    UUID orderId = placedOrderId(coffeeBeans.getId(), 2, "paid@example.com");
    String sessionId = sessionIdFor(orderId);

    stubCheckoutSucceeded("evt_success_1", sessionId, "pi_charge_ok");
    mockMvc.perform(post("/webhooks/stripe")
            .header(SIGNATURE_HEADER, "valid")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
        .andExpect(status().isOk());

    Order paid = orderRepository.findById(orderId).orElseThrow();
    assertThat(paid.getStatus()).isEqualTo(OrderStatusType.PAID);
    Product refreshed = productRepository.findById(coffeeBeans.getId()).orElseThrow();
    assertThat(refreshed.getAmount()).isEqualTo(3L);
  }

  @Test
  void webhook_asyncPaymentSucceeded_marksOrderPaid() throws Exception {
    UUID orderId = placedOrderId(sparklingWater.getId(), 1, "async@example.com");
    String sessionId = sessionIdFor(orderId);

    stubCheckoutSucceeded("evt_async_ok", sessionId, "pi_async_ok");
    mockMvc.perform(post("/webhooks/stripe")
            .header(SIGNATURE_HEADER, "valid")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
        .andExpect(status().isOk());

    Order paid = orderRepository.findById(orderId).orElseThrow();
    assertThat(paid.getStatus()).isEqualTo(OrderStatusType.PAID);
  }

  @Test
  void webhook_replayedSucceededEvent_isNoOp() throws Exception {
    UUID orderId = placedOrderId(sparklingWater.getId(), 4, "replay@example.com");
    String sessionId = sessionIdFor(orderId);

    stubCheckoutSucceeded("evt_dup", sessionId, "pi_dup");
    mockMvc.perform(post("/webhooks/stripe")
            .header(SIGNATURE_HEADER, "valid")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
        .andExpect(status().isOk());

    mockMvc.perform(post("/webhooks/stripe")
            .header(SIGNATURE_HEADER, "valid")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
        .andExpect(status().isOk());

    Product refreshed = productRepository.findById(sparklingWater.getId()).orElseThrow();
    assertThat(refreshed.getAmount()).isEqualTo(96L);
  }

  @Test
  void webhook_invalidSignature_returnsBadRequest() throws Exception {
    when(stripePaymentProvider.verifyAndParse(any(), eq("invalid")))
        .thenThrow(new BadRequestApiException("Invalid Stripe webhook signature"));

    mockMvc.perform(post("/webhooks/stripe")
            .header(SIGNATURE_HEADER, "invalid")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void webhook_stockExhaustedBetweenCheckoutAndWebhook_cancelsOrderAndRefunds() throws Exception {
    UUID orderId = placedOrderId(coffeeBeans.getId(), 2, "race@example.com");
    String sessionId = sessionIdFor(orderId);

    Product product = productRepository.findById(coffeeBeans.getId()).orElseThrow();
    product.setAmount(0L);
    productRepository.saveAndFlush(product);

    stubCheckoutSucceeded("evt_race", sessionId, "pi_race");
    mockMvc.perform(post("/webhooks/stripe")
            .header(SIGNATURE_HEADER, "valid")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
        .andExpect(status().isOk());

    Order cancelled = orderRepository.findById(orderId).orElseThrow();
    assertThat(cancelled.getStatus()).isEqualTo(OrderStatusType.CANCELLED);

    ArgumentCaptor<String> refundCaptor = ArgumentCaptor.forClass(String.class);
    verify(stripePaymentProvider).refund(refundCaptor.capture());
    assertThat(refundCaptor.getValue()).isEqualTo("pi_race");
  }

  @Test
  void webhook_checkoutFailed_marksOrderPaymentFailed() throws Exception {
    UUID orderId = placedOrderId(sparklingWater.getId(), 1, "fail@example.com");
    String sessionId = sessionIdFor(orderId);

    stubCheckoutFailed("evt_fail_1", sessionId);
    mockMvc.perform(post("/webhooks/stripe")
            .header(SIGNATURE_HEADER, "valid")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
        .andExpect(status().isOk());

    Order failed = orderRepository.findById(orderId).orElseThrow();
    assertThat(failed.getStatus()).isEqualTo(OrderStatusType.PAYMENT_FAILED);
    Product refreshed = productRepository.findById(sparklingWater.getId()).orElseThrow();
    assertThat(refreshed.getAmount()).isEqualTo(100L);
  }

  @Test
  void webhook_checkoutExpired_marksOrderCancelled() throws Exception {
    UUID orderId = placedOrderId(sparklingWater.getId(), 1, "expired@example.com");
    String sessionId = sessionIdFor(orderId);

    stubCheckoutExpired("evt_expired", sessionId);
    mockMvc.perform(post("/webhooks/stripe")
            .header(SIGNATURE_HEADER, "valid")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
        .andExpect(status().isOk());

    Order cancelled = orderRepository.findById(orderId).orElseThrow();
    assertThat(cancelled.getStatus()).isEqualTo(OrderStatusType.CANCELLED);
    verify(stripePaymentProvider, never()).refund(any());
    Product refreshed = productRepository.findById(sparklingWater.getId()).orElseThrow();
    assertThat(refreshed.getAmount()).isEqualTo(100L);
  }

  @Test
  void webhook_checkoutExpiredAfterPaid_isNoOp() throws Exception {
    UUID orderId = placedOrderId(sparklingWater.getId(), 1, "late-expire@example.com");
    String sessionId = sessionIdFor(orderId);

    stubCheckoutSucceeded("evt_paid_first", sessionId, "pi_paid_first");
    mockMvc.perform(post("/webhooks/stripe")
            .header(SIGNATURE_HEADER, "valid")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
        .andExpect(status().isOk());

    stubCheckoutExpired("evt_expired_late", sessionId);
    mockMvc.perform(post("/webhooks/stripe")
            .header(SIGNATURE_HEADER, "valid")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
        .andExpect(status().isOk());

    Order order = orderRepository.findById(orderId).orElseThrow();
    assertThat(order.getStatus()).isEqualTo(OrderStatusType.PAID);
  }

  @Test
  void webhook_paymentRefunded_marksOrderRefundedAndLeavesStockUntouched() throws Exception {
    UUID orderId = placedOrderId(coffeeBeans.getId(), 2, "refund@example.com");
    String sessionId = sessionIdFor(orderId);

    stubCheckoutSucceeded("evt_refund_prep", sessionId, "pi_refund_full");
    mockMvc.perform(post("/webhooks/stripe")
            .header(SIGNATURE_HEADER, "valid")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
        .andExpect(status().isOk());

    assertThat(orderRepository.findById(orderId).orElseThrow().getStatus()).isEqualTo(OrderStatusType.PAID);
    long stockAfterPaid = productRepository.findById(coffeeBeans.getId()).orElseThrow().getAmount();

    stubPaymentRefunded("evt_refund_done", "pi_refund_full");
    mockMvc.perform(post("/webhooks/stripe")
            .header(SIGNATURE_HEADER, "valid")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
        .andExpect(status().isOk());

    Order refunded = orderRepository.findById(orderId).orElseThrow();
    assertThat(refunded.getStatus()).isEqualTo(OrderStatusType.REFUNDED);
    assertThat(refunded.getStripePaymentIntentId()).isEqualTo("pi_refund_full");

    long stockAfterRefund = productRepository.findById(coffeeBeans.getId()).orElseThrow().getAmount();
    assertThat(stockAfterRefund)
        .as("Dashboard refund must not auto-restore stock — inventory reconciliation is manual")
        .isEqualTo(stockAfterPaid);
  }

  @Test
  void webhook_replayedRefundEvent_isNoOp() throws Exception {
    UUID orderId = placedOrderId(sparklingWater.getId(), 1, "refund-replay@example.com");
    String sessionId = sessionIdFor(orderId);

    stubCheckoutSucceeded("evt_replay_paid", sessionId, "pi_replay");
    mockMvc.perform(post("/webhooks/stripe")
            .header(SIGNATURE_HEADER, "valid")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
        .andExpect(status().isOk());

    stubPaymentRefunded("evt_replay_refund", "pi_replay");
    mockMvc.perform(post("/webhooks/stripe")
            .header(SIGNATURE_HEADER, "valid")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
        .andExpect(status().isOk());
    mockMvc.perform(post("/webhooks/stripe")
            .header(SIGNATURE_HEADER, "valid")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
        .andExpect(status().isOk());

    assertThat(orderRepository.findById(orderId).orElseThrow().getStatus()).isEqualTo(OrderStatusType.REFUNDED);
  }

  @Test
  void webhook_refundEventForSelfTriggeredRefundOnCancelledOrder_isNoOp() throws Exception {
    UUID orderId = placedOrderId(coffeeBeans.getId(), 2, "self-refund@example.com");
    String sessionId = sessionIdFor(orderId);

    Product product = productRepository.findById(coffeeBeans.getId()).orElseThrow();
    product.setAmount(0L);
    productRepository.saveAndFlush(product);

    stubCheckoutSucceeded("evt_self_cancel", sessionId, "pi_self_refund");
    mockMvc.perform(post("/webhooks/stripe")
            .header(SIGNATURE_HEADER, "valid")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
        .andExpect(status().isOk());

    assertThat(orderRepository.findById(orderId).orElseThrow().getStatus()).isEqualTo(OrderStatusType.CANCELLED);
    assertThat(orderRepository.findById(orderId).orElseThrow().getStripePaymentIntentId())
        .as("PI id must be persisted even on the stock-exhaustion → CANCELLED path so the incoming charge.refunded can be correlated")
        .isEqualTo("pi_self_refund");

    stubPaymentRefunded("evt_self_refund_echo", "pi_self_refund");
    mockMvc.perform(post("/webhooks/stripe")
            .header(SIGNATURE_HEADER, "valid")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
        .andExpect(status().isOk());

    assertThat(orderRepository.findById(orderId).orElseThrow().getStatus())
        .as("Status guard must block CANCELLED → REFUNDED, order stays CANCELLED")
        .isEqualTo(OrderStatusType.CANCELLED);
  }

  @Test
  void webhook_refundFailedAfterRefund_revertsOrderToPaid() throws Exception {
    UUID orderId = placedOrderId(sparklingWater.getId(), 1, "refund-cancel@example.com");
    String sessionId = sessionIdFor(orderId);

    stubCheckoutSucceeded("evt_cancel_paid", sessionId, "pi_refund_cancel");
    mockMvc.perform(post("/webhooks/stripe")
            .header(SIGNATURE_HEADER, "valid")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
        .andExpect(status().isOk());

    stubPaymentRefunded("evt_cancel_refund", "pi_refund_cancel");
    mockMvc.perform(post("/webhooks/stripe")
            .header(SIGNATURE_HEADER, "valid")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
        .andExpect(status().isOk());

    assertThat(orderRepository.findById(orderId).orElseThrow().getStatus()).isEqualTo(OrderStatusType.REFUNDED);

    stubPaymentRefundFailed("evt_cancel_failed", "pi_refund_cancel");
    mockMvc.perform(post("/webhooks/stripe")
            .header(SIGNATURE_HEADER, "valid")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
        .andExpect(status().isOk());

    assertThat(orderRepository.findById(orderId).orElseThrow().getStatus()).isEqualTo(OrderStatusType.PAID);
  }

  @Test
  void webhook_refundFailedOnPaidOrder_isNoOp() throws Exception {
    UUID orderId = placedOrderId(sparklingWater.getId(), 1, "refund-fail-paid@example.com");
    String sessionId = sessionIdFor(orderId);

    stubCheckoutSucceeded("evt_failpaid_paid", sessionId, "pi_fail_on_paid");
    mockMvc.perform(post("/webhooks/stripe")
            .header(SIGNATURE_HEADER, "valid")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
        .andExpect(status().isOk());

    stubPaymentRefundFailed("evt_failpaid_failed", "pi_fail_on_paid");
    mockMvc.perform(post("/webhooks/stripe")
            .header(SIGNATURE_HEADER, "valid")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
        .andExpect(status().isOk());

    assertThat(orderRepository.findById(orderId).orElseThrow().getStatus()).isEqualTo(OrderStatusType.PAID);
  }

  @Test
  void webhook_partialRefundIgnoredByProvider_leavesOrderPaid() throws Exception {
    UUID orderId = placedOrderId(sparklingWater.getId(), 1, "partial@example.com");
    String sessionId = sessionIdFor(orderId);

    stubCheckoutSucceeded("evt_partial_paid", sessionId, "pi_partial");
    mockMvc.perform(post("/webhooks/stripe")
            .header(SIGNATURE_HEADER, "valid")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
        .andExpect(status().isOk());

    stubVerifyEmpty();
    mockMvc.perform(post("/webhooks/stripe")
            .header(SIGNATURE_HEADER, "valid")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
        .andExpect(status().isOk());

    assertThat(orderRepository.findById(orderId).orElseThrow().getStatus()).isEqualTo(OrderStatusType.PAID);
  }

  @Test
  void authenticatedCheckout_reusesStripeCustomerOnSecondOrder() throws Exception {
    String token = accessTokenFor(USER_EMAIL, PASSWORD);

    mockMvc.perform(post("/order")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body(sparklingWater.getId(), 1, null)))
        .andExpect(status().isCreated());
    mockMvc.perform(post("/order")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body(sparklingWater.getId(), 1, null)))
        .andExpect(status().isCreated());

    Customer customer = customerRepository.findByEmail(USER_EMAIL).orElseThrow();
    assertThat(customer.getStripeCustomerId()).isNotNull();

    ArgumentCaptor<String> customerCaptor = ArgumentCaptor.forClass(String.class);
    verify(stripePaymentProvider, times(2)).createCheckoutSession(any(), customerCaptor.capture());
    assertThat(customerCaptor.getAllValues())
        .containsExactly(customer.getStripeCustomerId(), customer.getStripeCustomerId());
  }

  @Test
  void webhook_checkoutSucceeded_writesEnversRevisionForTransition() throws Exception {
    UUID orderId = placedOrderId(sparklingWater.getId(), 1, "envers@example.com");
    String sessionId = sessionIdFor(orderId);

    stubCheckoutSucceeded("evt_envers", sessionId, "pi_envers");
    mockMvc.perform(post("/webhooks/stripe")
            .header(SIGNATURE_HEADER, "valid")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
        .andExpect(status().isOk());

    transactionTemplate.executeWithoutResult(_ -> {
      var revisions = orderRepository.findRevisions(orderId).getContent();
      assertThat(revisions).hasSizeGreaterThanOrEqualTo(2);
      assertThat(revisions.getFirst().getEntity().getStatus()).isEqualTo(OrderStatusType.PENDING_PAYMENT);
      assertThat(revisions.getLast().getEntity().getStatus()).isEqualTo(OrderStatusType.PAID);
    });
  }

  @Test
  void checkoutStatus_returnsOrderStateForSession() throws Exception {
    UUID orderId = placedOrderId(sparklingWater.getId(), 1, "status@example.com");
    String sessionId = sessionIdFor(orderId);
    String orderNumber = orderRepository.findById(orderId).orElseThrow().getOrderNumber();

    mockMvc.perform(get("/checkout/sessions/{sessionId}/status", sessionId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.orderNumber").value(orderNumber))
        .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"));

    stubCheckoutSucceeded("evt_status", sessionId, "pi_status");
    mockMvc.perform(post("/webhooks/stripe")
            .header(SIGNATURE_HEADER, "valid")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
        .andExpect(status().isOk());

    mockMvc.perform(get("/checkout/sessions/{sessionId}/status", sessionId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("PAID"));
  }

  @Test
  void checkoutStatus_unknownSessionId_returnsNotFound() throws Exception {
    mockMvc.perform(get("/checkout/sessions/{sessionId}/status", "cs_bogus"))
        .andExpect(status().isNotFound());
  }

  private void stubCheckoutSucceeded(String eventId, String sessionId, String paymentIntentId) {
    when(stripePaymentProvider.verifyAndParse(any(), eq("valid")))
        .thenReturn(Optional.of(new StripeWebhookEvent.CheckoutSucceeded(eventId, sessionId, paymentIntentId)));
  }

  private void stubCheckoutFailed(String eventId, String sessionId) {
    when(stripePaymentProvider.verifyAndParse(any(), eq("valid")))
        .thenReturn(Optional.of(new StripeWebhookEvent.CheckoutFailed(eventId, sessionId)));
  }

  private void stubCheckoutExpired(String eventId, String sessionId) {
    when(stripePaymentProvider.verifyAndParse(any(), eq("valid")))
        .thenReturn(Optional.of(new StripeWebhookEvent.CheckoutExpired(eventId, sessionId)));
  }

  private void stubPaymentRefunded(String eventId, String paymentIntentId) {
    when(stripePaymentProvider.verifyAndParse(any(), eq("valid")))
        .thenReturn(Optional.of(new StripeWebhookEvent.PaymentRefunded(eventId, paymentIntentId)));
  }

  private void stubPaymentRefundFailed(String eventId, String paymentIntentId) {
    when(stripePaymentProvider.verifyAndParse(any(), eq("valid")))
        .thenReturn(Optional.of(new StripeWebhookEvent.PaymentRefundFailed(eventId, paymentIntentId)));
  }

  private void stubVerifyEmpty() {
    when(stripePaymentProvider.verifyAndParse(any(), eq("valid")))
        .thenReturn(Optional.empty());
  }

  private Product product(String name, BigDecimal price, int discount, boolean enabled, long amount) {
    Product p = new Product();
    p.setName(name);
    p.setDescription("Test product");
    p.setPrice(price);
    p.setDiscount(discount);
    p.setEnabled(enabled);
    p.setAmount(amount);
    return productRepository.saveAndFlush(p);
  }

  private static String body(UUID productId, int quantity, String email) {
    if (email == null) {
      return """
          {"items":[{"productId":"%s","quantity":%d}]}
          """.formatted(productId, quantity);
    }
    return """
        {"items":[{"productId":"%s","quantity":%d}],"email":"%s"}
        """.formatted(productId, quantity, email);
  }

  private MvcResult placeOrderAsGuest(UUID productId, int quantity, String email) throws Exception {
    return mockMvc.perform(post("/order")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body(productId, quantity, email)))
        .andExpect(status().isCreated())
        .andReturn();
  }

  private UUID placedOrderId(UUID productId, int quantity, String email) throws Exception {
    MvcResult result = placeOrderAsGuest(productId, quantity, email);
    return UUID.fromString(JsonPath.read(result.getResponse().getContentAsString(), "$.orderId"));
  }

  private String sessionIdFor(UUID orderId) {
    return Objects.requireNonNull(
        orderRepository.findById(orderId).orElseThrow().getStripeCheckoutSessionId());
  }

  private String accessTokenFor(String email, String password) throws Exception {
    MvcResult loginResult = mockMvc.perform(post("/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"email": "%s", "password": "%s"}
                """.formatted(email, password)))
        .andExpect(status().isOk())
        .andReturn();
    return JsonPath.read(loginResult.getResponse().getContentAsString(), "$.accessToken");
  }
}
