package com.noserbulgaria.micromarket.customer;

import com.noserbulgaria.micromarket.auth.user.AccountStatus;
import com.noserbulgaria.micromarket.auth.user.Role;
import com.noserbulgaria.micromarket.auth.user.User;
import com.noserbulgaria.micromarket.auth.user.UserRepository;
import com.noserbulgaria.micromarket.order.Order;
import com.noserbulgaria.micromarket.order.OrderRepository;
import com.noserbulgaria.micromarket.payment.stripe.StripeCheckoutSession;
import com.noserbulgaria.micromarket.payment.stripe.StripePaymentProvider;
import com.noserbulgaria.micromarket.payment.stripe.event.StripeEventRepository;
import com.noserbulgaria.micromarket.product.Product;
import com.noserbulgaria.micromarket.product.ProductRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pins down the cross-table email-uniqueness invariant: a Customer email belongs to either an unregistered guest or a
 * registered account, never both, and registering with a guest's email upgrades that customer in place.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EmailUniquenessIntegrationTests {

  private static final String EMAIL = "shared@example.com";
  private static final String PASSWORD = "securepass123";

  @Autowired private MockMvc mockMvc;
  @Autowired private UserRepository userRepository;
  @Autowired private ProductRepository productRepository;
  @Autowired private OrderRepository orderRepository;
  @Autowired private CustomerRepository customerRepository;
  @Autowired private StripeEventRepository stripeEventRepository;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private TransactionTemplate transactionTemplate;

  @MockitoBean private StripePaymentProvider stripePaymentProvider;

  private Product cola;

  @BeforeEach
  void setUp() {
    stripeEventRepository.deleteAll();
    orderRepository.deleteAll();
    customerRepository.deleteAll();
    productRepository.deleteAll();
    userRepository.deleteAll();

    AtomicInteger counter = new AtomicInteger();
    when(stripePaymentProvider.createCustomer(any()))
        .thenAnswer(_ -> "cus_fake_" + counter.incrementAndGet());
    when(stripePaymentProvider.createCheckoutSession(any(), any()))
        .thenAnswer(inv -> {
          Order order = inv.getArgument(0);
          String sessionId = "cs_fake_" + order.getId();
          return new StripeCheckoutSession(sessionId, "https://checkout.stripe.test/" + sessionId);
        });

    cola = new Product();
    cola.setName("Cola");
    cola.setDescription("Soft drink");
    cola.setPrice(new BigDecimal("10.00"));
    cola.setDiscount(0);
    cola.setEnabled(true);
    cola.setAmount(100L);
    cola = productRepository.saveAndFlush(cola);
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
  void guestCheckout_withEmailOfRegisteredCustomer_returns409() throws Exception {
    seedRegisteredCustomer(EMAIL);

    mockMvc.perform(post("/order")
            .contentType(MediaType.APPLICATION_JSON)
            .content(orderBody(cola.getId(), 1, EMAIL)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.detail").value(
            org.hamcrest.Matchers.containsString("registered, please log in")));

    Customer customer = customerRepository.findByEmail(EMAIL).orElseThrow();
    assertThat(customer.isRegistered())
        .as("Failed guest checkout must not flip the existing registered customer")
        .isTrue();
  }

  @Test
  void register_withEmailOfExistingGuest_upgradesInPlaceAndPreservesOrdersAndStripeCustomerId() throws Exception {
    placeGuestOrder(EMAIL);

    UUID customerId = transactionTemplate.execute(_ -> {
      Customer guest = customerRepository.findByEmail(EMAIL).orElseThrow();
      assertThat(guest.isRegistered()).isFalse();
      assertThat(guest.getStripeCustomerId()).isNotNull();
      return guest.getId();
    });
    String stripeCustomerIdBefore = transactionTemplate.execute(_ ->
        customerRepository.findById(Objects.requireNonNull(customerId)).orElseThrow().getStripeCustomerId());
    UUID orderIdBefore = transactionTemplate.execute(_ -> orderRepository.findAll().getFirst().getId());

    mockMvc.perform(post("/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"email":"%s","password":"%s"}
                """.formatted(EMAIL, PASSWORD)))
        .andExpect(status().isCreated());

    transactionTemplate.executeWithoutResult(_ -> {
      Customer reloaded = customerRepository.findByEmail(EMAIL).orElseThrow();
      assertThat(reloaded.getId()).isEqualTo(customerId);
      assertThat(reloaded.isRegistered()).isTrue();
      assertThat(reloaded.getStripeCustomerId()).isEqualTo(stripeCustomerIdBefore);

      Order preservedOrder = orderRepository.findById(Objects.requireNonNull(orderIdBefore)).orElseThrow();
      assertThat(preservedOrder.getCustomer().getId()).isEqualTo(customerId);
    });
  }

  @Test
  void register_withEmailOfRegisteredCustomer_returns409() throws Exception {
    mockMvc.perform(post("/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"email":"%s","password":"%s"}
                """.formatted(EMAIL, PASSWORD)))
        .andExpect(status().isCreated());

    mockMvc.perform(post("/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"email":"%s","password":"%s"}
                """.formatted(EMAIL, PASSWORD)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.detail").value("User with email '%s' already exists".formatted(EMAIL)));
  }

  @Test
  void guestCheckout_normalizesEmailCaseToLowercase() throws Exception {
    placeGuestOrder("Mixed.Case@Example.COM");

    Customer customer = customerRepository.findByEmail("mixed.case@example.com").orElseThrow();
    assertThat(customer.isRegistered()).isFalse();
    assertThat(customer.getEmail()).isEqualTo("mixed.case@example.com");
  }

  private void seedRegisteredCustomer(String email) {
    User user = new User();
    user.setPassword(Objects.requireNonNull(passwordEncoder.encode(PASSWORD)));
    user.setRole(Role.USER);
    user.setStatus(AccountStatus.ACTIVE);
    user = userRepository.saveAndFlush(user);

    Customer customer = new Customer();
    customer.setEmail(email);
    Profile profile = new Profile();
    profile.setCustomer(customer);
    profile.setUser(user);
    profile.setPoints(0);
    customer.setProfile(profile);
    customerRepository.saveAndFlush(customer);
  }

  private void placeGuestOrder(String email) throws Exception {
    mockMvc.perform(post("/order")
            .contentType(MediaType.APPLICATION_JSON)
            .content(orderBody(cola.getId(), 1, email)))
        .andExpect(status().isCreated());
  }

  private static String orderBody(UUID productId, int quantity, String email) {
    return """
        {"items":[{"productId":"%s","quantity":%d}],"email":"%s"}
        """.formatted(productId, quantity, email);
  }
}
