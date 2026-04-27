package com.noserbulgaria.micromarket.mail.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.noserbulgaria.micromarket.customer.CustomerRepository;
import com.noserbulgaria.micromarket.mail.EmailAsyncConfig;
import com.noserbulgaria.micromarket.mail.MailSender;
import com.noserbulgaria.micromarket.order.Order;
import com.noserbulgaria.micromarket.order.OrderRepository;
import com.noserbulgaria.micromarket.order.OrderStatusType;
import com.noserbulgaria.micromarket.payment.stripe.StripeCheckoutSession;
import com.noserbulgaria.micromarket.payment.stripe.StripePaymentProvider;
import com.noserbulgaria.micromarket.payment.stripe.StripeWebhookEvent;
import com.noserbulgaria.micromarket.payment.stripe.event.StripeEventRepository;
import com.noserbulgaria.micromarket.product.Product;
import com.noserbulgaria.micromarket.product.ProductRepository;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderConfirmationMailIntegrationTest {

  private static final String SIGNATURE_HEADER = "Stripe-Signature";

  /** Runs the {@code @TransactionalEventListener(AFTER_COMMIT)} inline so verify() sees it. */
  @TestConfiguration
  static class SyncExecutorConfig {
    @Bean(EmailAsyncConfig.EXECUTOR_BEAN_NAME)
    Executor emailExecutor() {
      return new SyncTaskExecutor();
    }
  }

  @Autowired private MockMvc mockMvc;
  @Autowired private ProductRepository productRepository;
  @Autowired private OrderRepository orderRepository;
  @Autowired private CustomerRepository customerRepository;
  @Autowired private StripeEventRepository stripeEventRepository;

  @MockitoBean private StripePaymentProvider stripePaymentProvider;
  @MockitoBean private MailSender mailSender;

  private Product oliveOil;

  @BeforeEach
  void setUp() {
    stripeEventRepository.deleteAll();
    orderRepository.deleteAll();
    customerRepository.deleteAll();
    productRepository.deleteAll();

    AtomicInteger customerCounter = new AtomicInteger();
    when(stripePaymentProvider.createCustomer(any()))
        .thenAnswer(_ -> "cus_fake_" + customerCounter.incrementAndGet());
    when(stripePaymentProvider.createCheckoutSession(any(), any()))
        .thenAnswer(inv -> {
          Order order = inv.getArgument(0);
          String sessionId = "cs_fake_" + order.getId();
          return new StripeCheckoutSession(sessionId, "https://checkout.stripe.test/" + sessionId);
        });

    oliveOil = product("Olive Oil 500ml", "Extra virgin, Puglia single-estate",
        new BigDecimal("14.90"), 10, 100);
  }

  @AfterEach
  void tearDown() {
    stripeEventRepository.deleteAll();
    orderRepository.deleteAll();
    customerRepository.deleteAll();
    productRepository.deleteAll();
  }

  @Test
  void successfulCheckout_sendsConfirmationEmailWithMappedView() throws Exception {
    UUID orderId = placeOrder(oliveOil.getId(), 2, "buyer@example.com");
    String sessionId = sessionIdFor(orderId);
    String orderNumber = orderRepository.findById(orderId).orElseThrow().getOrderNumber();

    stubCheckoutSucceeded("evt_ok", sessionId, "pi_ok");
    mockMvc.perform(post("/webhooks/stripe")
            .header(SIGNATURE_HEADER, "valid")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
        .andExpect(status().isOk());

    assertThat(orderRepository.findById(orderId).orElseThrow().getStatus())
        .isEqualTo(OrderStatusType.PAID);

    ArgumentCaptor<String> templateCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> toCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> subjectCaptor = ArgumentCaptor.forClass(String.class);
    @SuppressWarnings("unchecked")
    ArgumentCaptor<Map<String, Object>> varsCaptor = ArgumentCaptor.forClass(Map.class);
    verify(mailSender).send(
        templateCaptor.capture(),
        toCaptor.capture(),
        subjectCaptor.capture(),
        varsCaptor.capture());

    assertThat(templateCaptor.getValue()).isEqualTo("order-confirmation");
    assertThat(toCaptor.getValue()).isEqualTo("buyer@example.com");
    assertThat(subjectCaptor.getValue()).contains(orderNumber);

    Object orderVar = varsCaptor.getValue().get("order");
    assertThat(orderVar).isInstanceOf(OrderConfirmationView.class);
    OrderConfirmationView view = (OrderConfirmationView) orderVar;
    assertThat(view.number()).isEqualTo(orderNumber);
    assertThat(view.total()).isEqualByComparingTo("26.82"); // 14.90 * 2 * (1 - 0.10)
    assertThat(view.items()).hasSize(1);
    OrderConfirmationView.Item item = view.items().getFirst();
    assertThat(item.name()).isEqualTo("Olive Oil 500ml");
    assertThat(item.description()).isEqualTo("Extra virgin, Puglia single-estate");
    assertThat(item.amount()).isEqualTo(2);
    assertThat(item.discount()).isEqualTo(10);
  }

  @Test
  void failedCheckout_doesNotSendEmail() throws Exception {
    UUID orderId = placeOrder(oliveOil.getId(), 1, "nope@example.com");
    String sessionId = sessionIdFor(orderId);

    when(stripePaymentProvider.verifyAndParse(any(), eq("valid")))
        .thenReturn(Optional.of(new StripeWebhookEvent.CheckoutFailed("evt_fail", sessionId)));
    mockMvc.perform(post("/webhooks/stripe")
            .header(SIGNATURE_HEADER, "valid")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
        .andExpect(status().isOk());

    assertThat(orderRepository.findById(orderId).orElseThrow().getStatus())
        .isEqualTo(OrderStatusType.PAYMENT_FAILED);
    verify(mailSender, never()).send(any(), any(), any(), any());
  }

  private void stubCheckoutSucceeded(String eventId, String sessionId, String paymentIntentId) {
    when(stripePaymentProvider.verifyAndParse(any(), eq("valid")))
        .thenReturn(Optional.of(new StripeWebhookEvent.CheckoutSucceeded(eventId, sessionId, paymentIntentId)));
  }

  private Product product(String name, String description, BigDecimal price, int discount, long amount) {
    Product p = new Product();
    p.setName(name);
    p.setDescription(description);
    p.setPrice(price);
    p.setDiscount(discount);
    p.setEnabled(true);
    p.setAmount(amount);
    return productRepository.saveAndFlush(p);
  }

  private UUID placeOrder(UUID productId, int quantity, String email) throws Exception {
    String body = """
        {"items":[{"productId":"%s","quantity":%d}],"email":"%s"}
        """.formatted(productId, quantity, email);
    MvcResult result = mockMvc.perform(post("/order")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isCreated())
        .andReturn();
    return UUID.fromString(JsonPath.read(result.getResponse().getContentAsString(), "$.orderId"));
  }

  private String sessionIdFor(UUID orderId) {
    return Objects.requireNonNull(
        orderRepository.findById(orderId).orElseThrow().getStripeCheckoutSessionId());
  }
}
