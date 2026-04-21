package com.noserbulgaria.micromarket.order;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.UUID;

import com.noserbulgaria.micromarket.customer.Profile;
import com.noserbulgaria.micromarket.customer.ProfileRepository;
import com.noserbulgaria.micromarket.product.Product;
import com.noserbulgaria.micromarket.product.ProductRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.noserbulgaria.micromarket.auth.user.Role;
import com.noserbulgaria.micromarket.auth.user.User;
import com.noserbulgaria.micromarket.auth.user.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderControllerTest {

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

  private User testUser;

  private Profile testProfile;

  private Order testOrder;

  private Product testProduct;

  @BeforeEach
  void setUp() {
    orderRepository.deleteAll();
    profileRepository.deleteAll();
    productRepository.deleteAll();
    userRepository.deleteAll();

    testUser = new User();
    testUser.setEmail("admin@test.local");
    testUser.setPassword("password");
    testUser.setRole(Role.ADMINISTRATOR);
    testUser = userRepository.saveAndFlush(testUser);

    testProfile = new Profile();
    testProfile.setUser(testUser);
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

    testOrder = Order.builder()
        .orderNumber("MM-T00001")
        .status(OrderStatusType.PENDING_PAYMENT)
        .customer(testProfile)
        .email("admin@test.local")
        .totalAmount(new BigDecimal("21.00"))
        .build();

    OrderItem item = OrderItem.builder()
        .order(testOrder)
        .product(testProduct)
        .productName(testProduct.getName())
        .quantity(2)
        .originalUnitPrice(new BigDecimal("10.50"))
        .priceAtPurchase(new BigDecimal("10.50"))
        .build();
    testOrder.getOrderItems().add(item);

    testOrder = orderRepository.saveAndFlush(testOrder);
  }

  @AfterEach
  void tearDown() {
    orderRepository.deleteAll();
    profileRepository.deleteAll();
    productRepository.deleteAll();
    userRepository.deleteAll();
  }

  @Test
  @WithMockUser(roles = "USER")
  void getOrdersAsUser_returnsForbidden() throws Exception {
    mockMvc.perform(get("/order")).andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = "USER")
  void getOrderByIdAsUser_returnsForbidden() throws Exception {
    mockMvc.perform(get("/order/{id}", UUID.randomUUID())).andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = "ADMINISTRATOR")
  void getOrdersAsAdministrator_returnsOk() throws Exception {
    mockMvc.perform(get("/order"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].id").value(testOrder.getId().toString()))
        .andExpect(jsonPath("$.content[0].orderNumber").value("MM-T00001"))
        .andExpect(jsonPath("$.content[0].customerId").value(testProfile.getId().toString()))
        .andExpect(jsonPath("$.content[0].email").value("admin@test.local"))
        .andExpect(jsonPath("$.content[0].totalAmount").value(21.00))
        .andExpect(jsonPath("$.content[0].orderItems").isArray())
        .andExpect(jsonPath("$.content[0].orderItems.length()").value(1))
        .andExpect(jsonPath("$.content[0].orderItems[0].productId").value(testProduct.getId().toString()))
        .andExpect(jsonPath("$.content[0].orderItems[0].productName").value("Cola"))
        .andExpect(jsonPath("$.content[0].orderItems[0].quantity").value(2))
        .andExpect(jsonPath("$.content[0].orderItems[0].originalUnitPrice").value(10.5))
        .andExpect(jsonPath("$.content[0].orderItems[0].priceAtPurchase").value(10.5));
  }

  @Test
  @WithMockUser(roles = "ADMINISTRATOR")
  void getOrderByIdAsAdministrator_returnsOk() throws Exception {
    mockMvc.perform(get("/order/{id}", testOrder.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(testOrder.getId().toString()))
        .andExpect(jsonPath("$.orderNumber").value("MM-T00001"))
        .andExpect(jsonPath("$.customerId").value(testProfile.getId().toString()))
        .andExpect(jsonPath("$.email").value("admin@test.local"))
        .andExpect(jsonPath("$.totalAmount").value(21.00))
        .andExpect(jsonPath("$.orderItems").isArray())
        .andExpect(jsonPath("$.orderItems.length()").value(1))
        .andExpect(jsonPath("$.orderItems[0].productId").value(testProduct.getId().toString()))
        .andExpect(jsonPath("$.orderItems[0].productName").value("Cola"))
        .andExpect(jsonPath("$.orderItems[0].quantity").value(2))
        .andExpect(jsonPath("$.orderItems[0].originalUnitPrice").value(10.5))
        .andExpect(jsonPath("$.orderItems[0].priceAtPurchase").value(10.5));
  }

  @Test
  @WithMockUser(roles = "ADMINISTRATOR")
  void getOrdersAsAdministrator_withStatusFilter_returnsFiltered() throws Exception {
    Order filteredOut = Order.builder()
        .orderNumber("MM-T00002")
        .status(OrderStatusType.PAID)
        .customer(testProfile)
        .email("admin@test.local")
        .totalAmount(new BigDecimal("10.50"))
        .build();
    orderRepository.saveAndFlush(filteredOut);

    mockMvc.perform(get("/order").param("status", OrderStatusType.PENDING_PAYMENT.name()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(1))
        .andExpect(jsonPath("$.content[0].id").value(testOrder.getId().toString()))
        .andExpect(jsonPath("$.content[0].orderItems.length()").value(1))
        .andExpect(jsonPath("$.content[0].orderItems[0].quantity").value(2));
  }

  @Test
  @WithMockUser(roles = "ADMINISTRATOR")
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
