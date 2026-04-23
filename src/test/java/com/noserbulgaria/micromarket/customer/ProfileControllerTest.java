package com.noserbulgaria.micromarket.customer;

import com.noserbulgaria.micromarket.auth.user.AccountStatus;
import com.noserbulgaria.micromarket.auth.user.Role;
import com.noserbulgaria.micromarket.auth.user.User;
import com.noserbulgaria.micromarket.auth.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.TestExecutionEvent;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ProfileControllerTest {

  private static final String ADMIN_EMAIL = "admin@micromarket.dev";
  private static final String OWNER_EMAIL = "profile-owner@micromarket.dev";
  private static final String OTHER_EMAIL = "profile-other@micromarket.dev";
  private static final String PASSWORD = "user12345";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private ProfileRepository profileRepository;

  @Autowired
  private PasswordEncoder passwordEncoder;

  private User ownerUser;
  private Profile ownerProfile;

  @BeforeEach
  void setUp() {
    ownerUser = createUserWithProfile(OWNER_EMAIL, Role.USER);
    createUserWithProfile(OTHER_EMAIL, Role.USER);
    ownerProfile = profileRepository.findByUserId(ownerUser.getId()).orElseThrow();
  }

  @Test
  @WithUserDetails(value = OWNER_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
  void getById_asOwner_returnsProfile() throws Exception {
    mockMvc.perform(get("/profile/own"))
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
  @WithUserDetails(value = ADMIN_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
  void getById_asAdministrator_returnsProfile() throws Exception {
    mockMvc.perform(get("/profile/{id}", ownerProfile.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(ownerProfile.getId().toString()))
        .andExpect(jsonPath("$.user.id").value(ownerUser.getId().toString()))
        .andExpect(jsonPath("$.user.email").value(ownerUser.getEmail()))
        .andExpect(jsonPath("$.user.password").doesNotExist());
  }

  @Test
  @WithUserDetails(value = OTHER_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
  void getById_asDifferentUser_returnsForbidden() throws Exception {
    mockMvc.perform(get("/profile/{id}", ownerProfile.getId()))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithUserDetails(value = ADMIN_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
  void getById_missingProfile_returnsNotFound() throws Exception {
    UUID profileId = UUID.randomUUID();

    mockMvc.perform(get("/profile/{id}", profileId))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.title").value("Not Found"))
        .andExpect(jsonPath("$.detail").value("Profile with id '%s' not found".formatted(profileId)));
  }

  @Test
  @WithUserDetails(value = ADMIN_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
  void updateById_asAdministrator_updatesProfile() throws Exception {
    mockMvc.perform(put("/profile/{id}", ownerProfile.getId())
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
  @WithUserDetails(value = OWNER_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
  void updateById_asOwner_returnsForbidden() throws Exception {
    mockMvc.perform(put("/profile/{id}", ownerProfile.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"points": 200}
                """))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithUserDetails(value = ADMIN_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
  void updateById_invalidBody_returnsBadRequest() throws Exception {
    mockMvc.perform(put("/profile/{id}", ownerProfile.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"points": -1}
                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.title").value("Request Validation Failed"))
        .andExpect(jsonPath("$.errors[0]").value("points: Points must be zero or greater"));
  }

  @Test
  @WithUserDetails(value = ADMIN_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
  void updateById_missingProfile_returnsNotFound() throws Exception {
    UUID profileId = UUID.randomUUID();

    mockMvc.perform(put("/profile/{id}", profileId)
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
}
