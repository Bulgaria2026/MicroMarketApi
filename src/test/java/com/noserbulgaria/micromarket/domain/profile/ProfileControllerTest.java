package com.noserbulgaria.micromarket.domain.profile;

import com.jayway.jsonpath.JsonPath;
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

import java.util.Objects;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProfileControllerTest {

  private static final String PASSWORD = "user12345";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private ProfileRepository profileRepository;

  @Autowired
  private PasswordEncoder passwordEncoder;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  private User adminUser;
  private User ownerUser;
  private User otherUser;
  private Profile ownerProfile;

  @BeforeEach
  void setUp() {
    cleanDatabase();

    adminUser = createUserWithProfile("admin@micromarket.dev", Role.ADMINISTRATOR);
    ownerUser = createUserWithProfile("owner@micromarket.dev", Role.USER);
    otherUser = createUserWithProfile("other@micromarket.dev", Role.USER);
    ownerProfile = profileRepository.findByUserId(ownerUser.getId()).orElseThrow();
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

  @Test
  void getById_asOwner_returnsProfile() throws Exception {
    mockMvc.perform(get("/profile/own")
            .header("Authorization", bearer(accessTokenFor(ownerUser.getEmail(), PASSWORD))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(ownerProfile.getId().toString()))
        .andExpect(jsonPath("$.user.id").value(ownerUser.getId().toString()))
        .andExpect(jsonPath("$.user.email").value(ownerUser.getEmail()))
        .andExpect(jsonPath("$.user.role").value(ownerUser.getRole().name()))
        .andExpect(jsonPath("$.user.status").value(ownerUser.getStatus().name()))
        .andExpect(jsonPath("$.points").value(ownerProfile.getPoints()))
        .andExpect(jsonPath("$.createdAt").isNotEmpty())
        .andExpect(jsonPath("$.updatedAt").isNotEmpty())
        .andExpect(jsonPath("$.user.password").doesNotExist())
        .andExpect(jsonPath("$.password").doesNotExist());
  }

  @Test
  void getById_asAdministrator_returnsProfile() throws Exception {
    mockMvc.perform(get("/profile/{id}", ownerProfile.getId())
            .header("Authorization", bearer(accessTokenFor(adminUser.getEmail(), PASSWORD))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(ownerProfile.getId().toString()))
        .andExpect(jsonPath("$.user.id").value(ownerUser.getId().toString()))
        .andExpect(jsonPath("$.user.email").value(ownerUser.getEmail()))
        .andExpect(jsonPath("$.user.password").doesNotExist());
  }

  @Test
  void getById_asDifferentUser_returnsForbidden() throws Exception {
    mockMvc.perform(get("/profile/{id}", ownerProfile.getId())
            .header("Authorization", bearer(accessTokenFor(otherUser.getEmail(), PASSWORD))))
        .andExpect(status().isForbidden());
  }

  @Test
  void getById_missingProfile_returnsNotFound() throws Exception {
    UUID profileId = UUID.randomUUID();

    mockMvc.perform(get("/profile/{id}", profileId)
            .header("Authorization", bearer(accessTokenFor(adminUser.getEmail(), PASSWORD))))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.title").value("Not Found"))
        .andExpect(jsonPath("$.detail").value("Profile with id '%s' not found".formatted(profileId)));
  }

  @Test
  void updateById_asAdministrator_updatesProfile() throws Exception {
    mockMvc.perform(put("/profile/{id}", ownerProfile.getId())
            .header("Authorization", bearer(accessTokenFor(adminUser.getEmail(), PASSWORD)))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"points": 200}
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(ownerProfile.getId().toString()))
        .andExpect(jsonPath("$.user.id").value(ownerUser.getId().toString()))
        .andExpect(jsonPath("$.points").value(200));
  }

  @Test
  void updateById_asOwner_returnsForbidden() throws Exception {
    mockMvc.perform(put("/profile/{id}", ownerProfile.getId())
            .header("Authorization", bearer(accessTokenFor(ownerUser.getEmail(), PASSWORD)))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"points": 200}
                """))
        .andExpect(status().isForbidden());
  }

  @Test
  void updateById_invalidBody_returnsBadRequest() throws Exception {
    mockMvc.perform(put("/profile/{id}", ownerProfile.getId())
            .header("Authorization", bearer(accessTokenFor(adminUser.getEmail(), PASSWORD)))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"points": -1}
                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.title").value("Request Validation Failed"))
        .andExpect(jsonPath("$.errors[0]").value("points: Points must be zero or greater"));
  }

  @Test
  void updateById_missingProfile_returnsNotFound() throws Exception {
    UUID profileId = UUID.randomUUID();

    mockMvc.perform(put("/profile/{id}", profileId)
            .header("Authorization", bearer(accessTokenFor(adminUser.getEmail(), PASSWORD)))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"points": 200}
                """))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.title").value("Not Found"))
        .andExpect(jsonPath("$.detail").value("Profile with id '%s' not found".formatted(profileId)));
  }

  private User createUserWithProfile(String email, Role role) {
    User user = new User();
    user.setEmail(email);
    user.setPassword(Objects.requireNonNull(passwordEncoder.encode(PASSWORD)));
    user.setRole(role);
    user.setStatus(AccountStatus.ACTIVE);
    user = userRepository.saveAndFlush(user);

    Profile profile = new Profile();
    profile.setUser(user);
    profile.setPoints(0);
    profileRepository.saveAndFlush(profile);
    return user;
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
