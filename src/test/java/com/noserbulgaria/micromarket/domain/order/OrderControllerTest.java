package com.noserbulgaria.micromarket.domain.order;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.UUID;

import com.noserbulgaria.micromarket.domain.product.Product;
import com.noserbulgaria.micromarket.domain.product.ProductRepository;
import com.noserbulgaria.micromarket.domain.profile.Profile;
import com.noserbulgaria.micromarket.domain.profile.ProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.TestExecutionEvent;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.noserbulgaria.micromarket.security.user.Role;
import com.noserbulgaria.micromarket.security.user.AccountStatus;
import com.noserbulgaria.micromarket.security.user.User;
import com.noserbulgaria.micromarket.security.user.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class OrderControllerTest {

  private static final String ADMIN_EMAIL = "admin@micromarket.dev";
  private static final String USER_EMAIL = "user@micromarket.dev";
  private static final String CUSTOMER_EMAIL = "order-customer@micromarket.dev";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private OrderRepository orderRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private ProductRepository productRepository;

  @Autowired
  private ProfileRepository profileRepository;

  private User customerUser;
  private Profile testProfile;
  private Order testOrder;
  private Product testProduct;

  @BeforeEach
  void setUp() {
    orderRepository.deleteAll();

    customerUser = new User();
    customerUser.setEmail(CUSTOMER_EMAIL);
    customerUser.setPassword("password");
    customerUser.setRole(Role.USER);
    customerUser.setStatus(AccountStatus.ACTIVE);
    customerUser = userRepository.saveAndFlush(customerUser);

    testProfile = new Profile();
    testProfile.setUser(customerUser);
    testProfile.setPoints(0);
    testProfile = profileRepository.saveAndFlush(testProfile);

    testProduct = new Product();
    testProduct.setName("Cola");
    testProduct.setDescription("Sparkling soft drink");
    testProduct.setPrice(new BigDecimal("10.50"));
    testProduct.setDiscount(0);
    testProduct.setEnabled(true);
    testProduct.setAmount(25L);
    testProduct = productRepository.saveAndFlush(testProduct);

    testOrder = new Order();
    testOrder.setStatus(OrderStatusType.PENDING);
    testOrder.setCustomer(testProfile);

    OrderItem item = new OrderItem();
    item.setOrder(testOrder);
    item.setProduct(testProduct);
    item.setQuantity(2);
    item.setPriceAtPurchase(new BigDecimal("10.50"));
    testOrder.getOrderItems().add(item);

    testOrder = orderRepository.saveAndFlush(testOrder);
  }

  @Test
  @WithUserDetails(value = USER_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
  void getOrdersAsUser_returnsForbidden() throws Exception {
    mockMvc.perform(get("/order")).andExpect(status().isForbidden());
  }

  @Test
  @WithUserDetails(value = USER_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
  void getOrderByIdAsUser_returnsForbidden() throws Exception {
    mockMvc.perform(get("/order/{id}", UUID.randomUUID())).andExpect(status().isForbidden());
  }

  @Test
  @WithUserDetails(value = ADMIN_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
  void getOrdersAsAdministrator_returnsOk() throws Exception {
    mockMvc.perform(get("/order").param("customerId", testProfile.getId().toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].id").value(testOrder.getId().toString()))
        .andExpect(jsonPath("$.content[0].orderItems").isArray())
        .andExpect(jsonPath("$.content[0].orderItems.length()").value(1))
        .andExpect(jsonPath("$.content[0].orderItems[0].productId").value(testProduct.getId().toString()))
        .andExpect(jsonPath("$.content[0].orderItems[0].quantity").value(2))
        .andExpect(jsonPath("$.content[0].orderItems[0].priceAtPurchase").value(10.5));
  }

  @Test
  @WithUserDetails(value = ADMIN_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
  void getOrderByIdAsAdministrator_returnsOk() throws Exception {
    mockMvc.perform(get("/order/{id}", testOrder.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(testOrder.getId().toString()))
        .andExpect(jsonPath("$.orderItems").isArray())
        .andExpect(jsonPath("$.orderItems.length()").value(1))
        .andExpect(jsonPath("$.orderItems[0].product.id").value(testProduct.getId().toString()))
        .andExpect(jsonPath("$.orderItems[0].quantity").value(2))
        .andExpect(jsonPath("$.orderItems[0].priceAtPurchase").value(10.5));
  }

  @Test
  @WithUserDetails(value = ADMIN_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
  void getOrdersAsAdministrator_withStatusFilter_returnsFiltered() throws Exception {
    Order filteredOut = new Order();
    filteredOut.setStatus(OrderStatusType.COMPLETED);
    filteredOut.setCustomer(testProfile);
    orderRepository.saveAndFlush(filteredOut);

    mockMvc.perform(get("/order")
            .param("customerId", testProfile.getId().toString())
            .param("status", OrderStatusType.PENDING.name()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(1))
        .andExpect(jsonPath("$.content[0].id").value(testOrder.getId().toString()))
        .andExpect(jsonPath("$.content[0].orderItems.length()").value(1))
        .andExpect(jsonPath("$.content[0].orderItems[0].quantity").value(2));
  }

  @Test
  @WithUserDetails(value = ADMIN_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
  void getMissingOrderAsAdministrator_returnsNotFound() throws Exception {
    UUID orderId = UUID.randomUUID();

    mockMvc.perform(get("/order/{id}", orderId))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.title").value("Not Found"))
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.detail").value("Order with id '%s' not found".formatted(orderId)))
        .andExpect(jsonPath("$.instance").value("/order/" + orderId));
  }
}
