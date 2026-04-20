package com.noserbulgaria.micromarket.security.user;

import com.jayway.jsonpath.JsonPath;
import com.noserbulgaria.micromarket.domain.profile.Profile;
import com.noserbulgaria.micromarket.domain.profile.ProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Objects;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserListControllerTest {

  private static final String PASSWORD = "user12345";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private ProfileRepository profileRepository;

  @Autowired
  private PasswordEncoder passwordEncoder;

  private User adminUser;
  private User activeUser;
  private User inactiveAdminUser;

  @BeforeEach
  void setUp() {
    adminUser = createUserWithProfile(uniqueEmail("admin"), Role.ADMINISTRATOR, AccountStatus.ACTIVE);
    activeUser = createUserWithProfile(uniqueEmail("user"), Role.USER, AccountStatus.ACTIVE);
    inactiveAdminUser = createUserWithProfile(uniqueEmail("inactive-admin"), Role.ADMINISTRATOR, AccountStatus.INACTIVE);
  }

  @Test
  void getAll_asNonAdmin_returnsForbidden() throws Exception {
    mockMvc.perform(get("/user")
            .header("Authorization", bearer(accessTokenFor(activeUser.getEmail(), PASSWORD))))
        .andExpect(status().isForbidden());
  }

  @Test
  void getAll_asAdmin_returnsPaginatedUsers() throws Exception {
    mockMvc.perform(get("/user")
            .header("Authorization", bearer(accessTokenFor(adminUser.getEmail(), PASSWORD)))
            .param("page", "0")
            .param("size", "2"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(2))
        .andExpect(jsonPath("$.page.totalElements").isNumber())
        .andExpect(jsonPath("$.content[0].email").isNotEmpty())
        .andExpect(jsonPath("$.content[0].password").doesNotExist())
        .andExpect(jsonPath("$.content[0].customer").doesNotExist());
  }

  @Test
  void getAll_asAdmin_withFilters_returnsMatchingUsers() throws Exception {
    mockMvc.perform(get("/user")
            .header("Authorization", bearer(accessTokenFor(adminUser.getEmail(), PASSWORD)))
            .param("email", "inactive-admin")
            .param("role", Role.ADMINISTRATOR.name())
            .param("status", AccountStatus.INACTIVE.name()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(1))
        .andExpect(jsonPath("$.content[0].id").value(inactiveAdminUser.getId().toString()))
        .andExpect(jsonPath("$.content[0].email").value(inactiveAdminUser.getEmail()))
        .andExpect(jsonPath("$.content[0].role").value(Role.ADMINISTRATOR.name()))
        .andExpect(jsonPath("$.content[0].status").value(AccountStatus.INACTIVE.name()));
  }

  private User createUserWithProfile(String email, Role role, AccountStatus status) {
    User user = new User();
    user.setEmail(email);
    user.setPassword(Objects.requireNonNull(passwordEncoder.encode(PASSWORD)));
    user.setRole(role);
    user.setStatus(status);
    user = userRepository.saveAndFlush(user);

    Profile profile = new Profile();
    profile.setUser(user);
    profile.setPoints(0);
    profileRepository.saveAndFlush(profile);
    return user;
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
