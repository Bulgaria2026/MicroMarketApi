package com.noserbulgaria.micromarket.customer;

import com.noserbulgaria.micromarket.auth.user.AccountStatus;
import com.noserbulgaria.micromarket.auth.user.Role;
import com.noserbulgaria.micromarket.auth.user.User;
import com.noserbulgaria.micromarket.auth.user.UserRepository;
import com.noserbulgaria.micromarket.order.OrderRepository;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.TestExecutionEvent;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CustomerControllerTest {

  private static final String ADMIN_EMAIL = "admin@micromarket.dev";
  private static final String USER_EMAIL = "user@micromarket.dev";
  private static final String INACTIVE_ADMIN_EMAIL = "customer-inactive-admin@micromarket.dev";
  private static final String GUEST_EMAIL = "customer-guest@micromarket.dev";
  private static final String PASSWORD = "user12345";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private CustomerRepository customerRepository;

  @Autowired
  private PasswordEncoder passwordEncoder;

  @Autowired
  private OrderRepository orderRepository;

  private User activeUser;
  private Customer activeCustomer;
  private Customer inactiveAdminCustomer;
  private Customer guestCustomer;

  @BeforeEach
  void setUp() {
    orderRepository.deleteAll();
    orderRepository.flush();

    User adminUser = Objects.requireNonNull(
        customerRepository.findByEmail(ADMIN_EMAIL).orElseThrow().getProfile()).getUser();
    activeUser = Objects.requireNonNull(
        customerRepository.findByEmail(USER_EMAIL).orElseThrow().getProfile()).getUser();

    customerRepository.deleteAll();
    customerRepository.flush();

    upsertRegisteredCustomer(ADMIN_EMAIL, adminUser, 50L);
    activeCustomer = upsertRegisteredCustomer(USER_EMAIL, activeUser, 15L);

    User inactiveAdminUser = createUser(Role.ADMINISTRATOR, AccountStatus.INACTIVE);
    inactiveAdminCustomer = upsertRegisteredCustomer(INACTIVE_ADMIN_EMAIL, inactiveAdminUser, 120L);

    guestCustomer = upsertGuest(GUEST_EMAIL);
  }

  @Test
  @WithUserDetails(value = USER_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
  void getAll_asNonAdmin_returnsForbidden() throws Exception {
    mockMvc.perform(get("/customer"))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithUserDetails(value = ADMIN_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
  void getAll_asAdmin_returnsPaginatedMixedCustomers() throws Exception {
    mockMvc.perform(get("/customer")
            .param("page", "0")
            .param("size", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(4))
        .andExpect(jsonPath("$.page.totalElements").value(4))
        .andExpect(jsonPath("$.content[0].createdAt").isNotEmpty())
        .andExpect(jsonPath("$.content[0].updatedAt").isNotEmpty());
  }

  @Test
  @WithUserDetails(value = ADMIN_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
  void getAll_returnsGuestRowsWithNullableProfileFields() throws Exception {
    mockMvc.perform(get("/customer")
            .param("email", "guest"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(1))
        .andExpect(jsonPath("$.content[0].type").value(CustomerType.GUEST.name()))
        .andExpect(jsonPath("$.content[0].email").value(guestCustomer.getEmail()))
        .andExpect(jsonPath("$.content[0].role").isEmpty())
        .andExpect(jsonPath("$.content[0].status").isEmpty())
        .andExpect(jsonPath("$.content[0].points").isEmpty());
  }

  @Test
  @WithUserDetails(value = ADMIN_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
  void getAll_returnsProfileRowsWithFilledFields() throws Exception {
    mockMvc.perform(get("/customer")
            .param("email", USER_EMAIL))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(1))
        .andExpect(jsonPath("$.content[0].type").value(CustomerType.PROFILE.name()))
        .andExpect(jsonPath("$.content[0].email").value(USER_EMAIL))
        .andExpect(jsonPath("$.content[0].role").value(activeUser.getRole().name()))
        .andExpect(jsonPath("$.content[0].status").value(activeUser.getStatus().name()))
        .andExpect(jsonPath("$.content[0].points").value(
            Objects.requireNonNull(activeCustomer.getProfile()).getPoints()));
  }

  @Test
  @WithUserDetails(value = ADMIN_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
  void getAll_withEmailFilter_matchesGuestsAndProfiles() throws Exception {
    mockMvc.perform(get("/customer")
            .param("email", "admin"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(2));
  }

  @Test
  @WithUserDetails(value = ADMIN_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
  void getAll_withGuestTypeFilter_returnsOnlyGuests() throws Exception {
    mockMvc.perform(get("/customer")
            .param("type", CustomerType.GUEST.name()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(1))
        .andExpect(jsonPath("$.content[0].id").value(guestCustomer.getId().toString()))
        .andExpect(jsonPath("$.content[0].type").value(CustomerType.GUEST.name()));
  }

  @Test
  @WithUserDetails(value = ADMIN_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
  void getAll_withProfileTypeFilter_returnsOnlyProfiles() throws Exception {
    mockMvc.perform(get("/customer")
            .param("type", CustomerType.PROFILE.name()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(3))
        .andExpect(jsonPath("$.content[*].type").value(org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is(CustomerType.PROFILE.name()))));
  }

  @Test
  @WithUserDetails(value = ADMIN_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
  void getAll_withRoleFilter_returnsOnlyMatchingProfiles() throws Exception {
    mockMvc.perform(get("/customer")
            .param("role", Role.ADMINISTRATOR.name()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(2))
        .andExpect(jsonPath("$.content[*].type").value(org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is(CustomerType.PROFILE.name()))));
  }

  @Test
  @WithUserDetails(value = ADMIN_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
  void getAll_withStatusFilter_returnsOnlyMatchingProfiles() throws Exception {
    mockMvc.perform(get("/customer")
            .param("status", AccountStatus.INACTIVE.name()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(1))
        .andExpect(jsonPath("$.content[0].id").value(inactiveAdminCustomer.getId().toString()))
        .andExpect(jsonPath("$.content[0].type").value(CustomerType.PROFILE.name()));
  }

  @Test
  @WithUserDetails(value = ADMIN_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
  void getAll_withPointsRange_returnsOnlyMatchingProfiles() throws Exception {
    mockMvc.perform(get("/customer")
            .param("minPoints", "100")
            .param("maxPoints", "200"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(1))
        .andExpect(jsonPath("$.content[0].id").value(inactiveAdminCustomer.getId().toString()))
        .andExpect(jsonPath("$.content[0].points").value(120));
  }

  @Test
  @WithUserDetails(value = ADMIN_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
  void getAll_withCombinedFilters_returnsMatchingProfiles() throws Exception {
    mockMvc.perform(get("/customer")
            .param("email", "inactive-admin")
            .param("role", Role.ADMINISTRATOR.name())
            .param("status", AccountStatus.INACTIVE.name())
            .param("minPoints", "100")
            .param("maxPoints", "200"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(1))
        .andExpect(jsonPath("$.content[0].id").value(inactiveAdminCustomer.getId().toString()));
  }

  @Test
  @WithUserDetails(value = ADMIN_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
  void getAll_withUnsupportedSort_returnsBadRequest() throws Exception {
    mockMvc.perform(get("/customer")
            .param("sort", "email,asc"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.title").value("Bad Request"))
        .andExpect(jsonPath("$.detail").value("Sorting by 'email' is not supported"))
        .andExpect(jsonPath("$.instance").value("/customer"));
  }

  @Test
  @WithUserDetails(value = ADMIN_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
  void getAllUsersEndpoint_noLongerExists() throws Exception {
    mockMvc.perform(get("/user"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.title").value("Resource Not Found"))
        .andExpect(jsonPath("$.detail").value("Resource 'user' was not found."));
  }

  private User createUser(Role role, AccountStatus status) {
    User user = new User();
    user.setPassword(Objects.requireNonNull(passwordEncoder.encode(PASSWORD)));
    user.setRole(role);
    user.setStatus(status);
    return userRepository.saveAndFlush(user);
  }

  private Customer upsertRegisteredCustomer(String email, User user, long points) {
    Customer customer = new Customer();
    customer.setEmail(email);
    Profile profile = new Profile();
    profile.setCustomer(customer);
    profile.setUser(user);
    profile.setPoints(points);
    customer.setProfile(profile);
    return customerRepository.saveAndFlush(customer);
  }

  private Customer upsertGuest(String email) {
    Customer customer = new Customer();
    customer.setEmail(email);
    return customerRepository.saveAndFlush(customer);
  }
}
