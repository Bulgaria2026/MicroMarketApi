package com.noserbulgaria.micromarket.domain.customer;

import com.jayway.jsonpath.JsonPath;
import com.noserbulgaria.micromarket.domain.guest.Guest;
import com.noserbulgaria.micromarket.domain.guest.GuestRepository;
import com.noserbulgaria.micromarket.domain.profile.Profile;
import com.noserbulgaria.micromarket.domain.profile.ProfileRepository;
import com.noserbulgaria.micromarket.security.user.AccountStatus;
import com.noserbulgaria.micromarket.security.user.Role;
import com.noserbulgaria.micromarket.security.user.User;
import com.noserbulgaria.micromarket.security.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CustomerControllerTest {

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
  private JdbcTemplate jdbcTemplate;

  private User adminUser;
  private User activeUser;
  private User inactiveAdminUser;
  private Profile adminProfile;
  private Profile activeProfile;
  private Profile inactiveAdminProfile;
  private Guest guestCustomer;

  @BeforeEach
  void setUp() {
    cleanDatabase();

    adminUser = createUser(uniqueEmail("admin"), Role.ADMINISTRATOR, AccountStatus.ACTIVE);
    adminProfile = createProfile(adminUser, 50);

    pause();
    activeUser = createUser(uniqueEmail("user"), Role.USER, AccountStatus.ACTIVE);
    activeProfile = createProfile(activeUser, 15);

    pause();
    inactiveAdminUser = createUser(uniqueEmail("inactive-admin"), Role.ADMINISTRATOR, AccountStatus.INACTIVE);
    inactiveAdminProfile = createProfile(inactiveAdminUser, 120);

    pause();
    guestCustomer = createGuest(uniqueEmail("guest"));
  }

  @Test
  void getAll_asNonAdmin_returnsForbidden() throws Exception {
    mockMvc.perform(get("/customer")
            .header("Authorization", bearer(accessTokenFor(activeUser.getEmail(), PASSWORD))))
        .andExpect(status().isForbidden());
  }

  @Test
  void getAll_asAdmin_returnsPaginatedMixedCustomers() throws Exception {
    mockMvc.perform(get("/customer")
            .header("Authorization", bearer(accessTokenFor(adminUser.getEmail(), PASSWORD)))
            .param("page", "0")
            .param("size", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(4))
        .andExpect(jsonPath("$.page.totalElements").value(4))
        .andExpect(jsonPath("$.content[0].createdAt").isNotEmpty())
        .andExpect(jsonPath("$.content[0].updatedAt").isNotEmpty());
  }

  @Test
  void getAll_returnsGuestRowsWithNullableProfileFields() throws Exception {
    mockMvc.perform(get("/customer")
            .header("Authorization", bearer(accessTokenFor(adminUser.getEmail(), PASSWORD)))
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
  void getAll_returnsProfileRowsWithFilledFields() throws Exception {
    mockMvc.perform(get("/customer")
            .header("Authorization", bearer(accessTokenFor(adminUser.getEmail(), PASSWORD)))
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
  void getAll_withEmailFilter_matchesGuestsAndProfiles() throws Exception {
    mockMvc.perform(get("/customer")
            .header("Authorization", bearer(accessTokenFor(adminUser.getEmail(), PASSWORD)))
            .param("email", "admin"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(2));
  }

  @Test
  void getAll_withCreatedAtRange_filtersCustomers() throws Exception {
    Instant to = activeProfile.getCreatedAt();

    mockMvc.perform(get("/customer")
            .header("Authorization", bearer(accessTokenFor(adminUser.getEmail(), PASSWORD)))
            .param("createdFrom", activeProfile.getCreatedAt().toString())
            .param("createdTo", to.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(1))
        .andExpect(jsonPath("$.content[0].id").value(activeProfile.getId().toString()));
  }

  @Test
  void getAll_withRoleFilter_returnsOnlyMatchingProfiles() throws Exception {
    mockMvc.perform(get("/customer")
            .header("Authorization", bearer(accessTokenFor(adminUser.getEmail(), PASSWORD)))
            .param("role", Role.ADMINISTRATOR.name()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(2))
        .andExpect(jsonPath("$.content[*].type").value(org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is(CustomerType.PROFILE.name()))));
  }

  @Test
  void getAll_withStatusFilter_returnsOnlyMatchingProfiles() throws Exception {
    mockMvc.perform(get("/customer")
            .header("Authorization", bearer(accessTokenFor(adminUser.getEmail(), PASSWORD)))
            .param("status", AccountStatus.INACTIVE.name()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(1))
        .andExpect(jsonPath("$.content[0].id").value(inactiveAdminProfile.getId().toString()))
        .andExpect(jsonPath("$.content[0].type").value(CustomerType.PROFILE.name()));
  }

  @Test
  void getAll_withPointsRange_returnsOnlyMatchingProfiles() throws Exception {
    mockMvc.perform(get("/customer")
            .header("Authorization", bearer(accessTokenFor(adminUser.getEmail(), PASSWORD)))
            .param("minPoints", "100")
            .param("maxPoints", "200"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(1))
        .andExpect(jsonPath("$.content[0].id").value(inactiveAdminProfile.getId().toString()))
        .andExpect(jsonPath("$.content[0].points").value(120));
  }

  @Test
  void getAll_withCombinedFilters_returnsMatchingProfiles() throws Exception {
    mockMvc.perform(get("/customer")
            .header("Authorization", bearer(accessTokenFor(adminUser.getEmail(), PASSWORD)))
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
  void getAll_withSupportedSort_returnsOrderedCustomers() throws Exception {
    mockMvc.perform(get("/customer")
            .header("Authorization", bearer(accessTokenFor(adminUser.getEmail(), PASSWORD)))
            .param("sort", "createdAt,asc"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].id").value(adminProfile.getId().toString()));
  }

  @Test
  void getAll_withUnsupportedSort_returnsBadRequest() throws Exception {
    mockMvc.perform(get("/customer")
            .header("Authorization", bearer(accessTokenFor(adminUser.getEmail(), PASSWORD)))
            .param("sort", "email,asc"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.title").value("Bad Request"))
        .andExpect(jsonPath("$.detail").value("Sorting by 'email' is not supported"))
        .andExpect(jsonPath("$.instance").value("/customer"));
  }

  @Test
  void getAllUsersEndpoint_noLongerExists() throws Exception {
    mockMvc.perform(get("/user")
            .header("Authorization", bearer(accessTokenFor(adminUser.getEmail(), PASSWORD))))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.title").value("Resource Not Found"))
        .andExpect(jsonPath("$.detail").value("Resource 'user' was not found."));
  }

  private void cleanDatabase() {
    jdbcTemplate.update("DELETE FROM refresh_tokens");
    jdbcTemplate.update("DELETE FROM order_item");
    jdbcTemplate.update("DELETE FROM orders");
    jdbcTemplate.update("DELETE FROM profile");
    jdbcTemplate.update("DELETE FROM guest");
    jdbcTemplate.update("DELETE FROM users");
    jdbcTemplate.update("DELETE FROM customer");
    jdbcTemplate.update("DELETE FROM product");
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

  private String uniqueEmail(String prefix) {
    return "%s-%s@micromarket.dev".formatted(prefix, UUID.randomUUID());
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

  private String bearer(String accessToken) {
    return "Bearer " + accessToken;
  }
}
