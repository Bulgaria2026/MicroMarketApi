package com.noserbulgaria.micromarket.domain.order;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.noserbulgaria.micromarket.domain.user.Role;
import com.noserbulgaria.micromarket.domain.user.User;
import com.noserbulgaria.micromarket.domain.user.UserRepository;

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

  private User testUser;

  private Order testOrder;

  @BeforeEach
  void setUp() {
    orderRepository.deleteAll();
    userRepository.deleteAll();

    testUser = new User();
    testUser.setEmail("admin@test.local");
    testUser.setPassword("password");
    testUser.setRole(Role.ADMINISTRATOR);
    testUser = userRepository.saveAndFlush(testUser);

    testOrder = new Order();
    testOrder.setStatus(OrderStatusType.PENDING);
    testOrder.setCustomerId(testUser.getId());
    testOrder = orderRepository.saveAndFlush(testOrder);
  }

  @Test
  @WithMockUser(roles = "USER")
  void getOrdersAsUser_returnsForbidden() throws Exception {
    mockMvc.perform(get("/orders"))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = "USER")
  void getOrderByIdAsUser_returnsForbidden() throws Exception {
    mockMvc.perform(get("/orders/{id}", UUID.randomUUID()))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = "ADMINISTRATOR")
  void getOrdersAsAdministrator_returnsOk() throws Exception {
    mockMvc.perform(get("/orders"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].id").value(testOrder.getId().toString()));
  }

  @Test
  @WithMockUser(roles = "ADMINISTRATOR")
  void getOrdersAsAdministrator_withStatusFilter_returnsFiltered() throws Exception {
    Order filteredOut = new Order();
    filteredOut.setStatus(OrderStatusType.COMPLETED);
    filteredOut.setCustomerId(testUser.getId());
    orderRepository.saveAndFlush(filteredOut);

    mockMvc.perform(get("/orders").param("status", OrderStatusType.PENDING.name()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(1))
        .andExpect(jsonPath("$.content[0].id").value(testOrder.getId().toString()));
  }

  @Test
  @WithMockUser(roles = "ADMINISTRATOR")
  void getMissingOrderAsAdministrator_returnsNotFound() throws Exception {
    mockMvc.perform(get("/orders/{id}", UUID.randomUUID()))
        .andExpect(status().isNotFound());
  }
}
