package com.noserbulgaria.micromarket.domain.customer;

import com.noserbulgaria.micromarket.domain.guest.Guest;
import com.noserbulgaria.micromarket.domain.guest.GuestRepository;
import com.noserbulgaria.micromarket.domain.profile.Profile;
import com.noserbulgaria.micromarket.domain.profile.ProfileRepository;
import com.noserbulgaria.micromarket.security.user.AccountStatus;
import com.noserbulgaria.micromarket.security.user.Role;
import com.noserbulgaria.micromarket.security.user.User;
import com.noserbulgaria.micromarket.security.user.UserRepository;
import com.noserbulgaria.micromarket.domain.order.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.TestExecutionEvent;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

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
  private static final String PASSWORD = "user12345";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private ProfileRepository profileRepository;

  @Autowired
  private GuestRepository guestRepository;

  @Autowired
  private PasswordEncoder passwordEncoder;

  @Autowired
  private OrderRepository orderRepository;

  private User adminUser;
  private User activeUser;
  private User inactiveAdminUser;
  private Profile adminProfile;
  private Profile activeProfile;
  private Profile inactiveAdminProfile;
  private Guest guestCustomer;

  @BeforeEach
  void setUp() {
    orderRepository.deleteAll();
    orderRepository.flush();
    guestRepository.deleteAll();
    guestRepository.flush();
    profileRepository.deleteAll();
    profileRepository.flush();

    adminUser = userRepository.findByEmail(ADMIN_EMAIL).orElseThrow();
    adminProfile = createProfile(adminUser, 50);

    pause();
    activeUser = userRepository.findByEmail(USER_EMAIL).orElseThrow();
    activeProfile = createProfile(activeUser, 15);

    pause();
    inactiveAdminUser = createUser(INACTIVE_ADMIN_EMAIL, Role.ADMINISTRATOR, AccountStatus.INACTIVE);
    inactiveAdminProfile = createProfile(inactiveAdminUser, 120);

    pause();
    guestCustomer = createGuest("customer-guest@micromarket.dev");
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
            .param("email", activeUser.getEmail()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(1))
        .andExpect(jsonPath("$.content[0].type").value(CustomerType.PROFILE.name()))
        .andExpect(jsonPath("$.content[0].email").value(activeUser.getEmail()))
        .andExpect(jsonPath("$.content[0].role").value(activeUser.getRole().name()))
        .andExpect(jsonPath("$.content[0].status").value(activeUser.getStatus().name()))
        .andExpect(jsonPath("$.content[0].points").value(activeProfile.getPoints()));
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
  void getAll_withCreatedAtRange_filtersCustomers() throws Exception {
    Instant to = activeProfile.getCreatedAt();

    mockMvc.perform(get("/customer")
            .param("createdFrom", activeProfile.getCreatedAt().toString())
            .param("createdTo", to.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(1))
        .andExpect(jsonPath("$.content[0].id").value(activeProfile.getId().toString()));
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
        .andExpect(jsonPath("$.content[0].id").value(inactiveAdminProfile.getId().toString()))
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
        .andExpect(jsonPath("$.content[0].id").value(inactiveAdminProfile.getId().toString()))
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
        .andExpect(jsonPath("$.content[0].id").value(inactiveAdminProfile.getId().toString()));
  }

  @Test
  @WithUserDetails(value = ADMIN_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
  void getAll_withSupportedSort_returnsOrderedCustomers() throws Exception {
    mockMvc.perform(get("/customer")
            .param("sort", "createdAt,asc"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].id").value(adminProfile.getId().toString()));
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

  private User createUser(String email, Role role, AccountStatus status) {
    User user = new User();
    user.setEmail(email);
    user.setPassword(Objects.requireNonNull(passwordEncoder.encode(PASSWORD)));
    user.setRole(role);
    user.setStatus(status);
    return userRepository.saveAndFlush(user);
  }

  private Profile createProfile(User user, long points) {
    Profile profile = new Profile();
    profile.setUser(user);
    profile.setPoints(points);
    return profileRepository.saveAndFlush(profile);
  }

  private Guest createGuest(String email) {
    Guest guest = new Guest();
    guest.setEmail(email);
    return guestRepository.saveAndFlush(guest);
  }

  private void pause() {
    try {
      Thread.sleep(10);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted while preparing test data", e);
    }
  }
}
