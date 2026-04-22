package com.noserbulgaria.micromarket.security.user;

import com.noserbulgaria.micromarket.domain.profile.Profile;
import com.noserbulgaria.micromarket.domain.profile.ProfileRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.TestExecutionEvent;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UserControllerTest {

  private static final String ADMIN_EMAIL = "admin@micromarket.dev";
  private static final String USER_EMAIL = "user-controller-user@micromarket.dev";
  private static final String EXISTING_EMAIL = "user-controller-existing@micromarket.dev";
  private static final String UPDATED_EMAIL = "user-controller-updated@micromarket.dev";
  private static final String PASSWORD = "user12345";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private ProfileRepository profileRepository;

  @Autowired
  private PasswordEncoder passwordEncoder;

  private User userToUpdate;

  @BeforeEach
  void setUp() {
    userToUpdate = createUserWithProfile(USER_EMAIL, Role.USER);
  }

  @Test
  @WithUserDetails(value = ADMIN_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
  void patchUser_withValidRole_updatesRole() throws Exception {
    mockMvc.perform(patch("/user/{id}", userToUpdate.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"role": "ADMINISTRATOR"}
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.role").value("ADMINISTRATOR"))
        .andExpect(jsonPath("$.email").value(userToUpdate.getEmail()))
        .andExpect(jsonPath("$.status").value(AccountStatus.ACTIVE.name()));

    User updatedUser = userRepository.findById(userToUpdate.getId()).orElseThrow();
    Assertions.assertEquals(Role.ADMINISTRATOR, updatedUser.getRole());
  }

  @Test
  @WithUserDetails(value = ADMIN_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
  void patchUser_withValidEmail_updatesEmail() throws Exception {
    String newEmail = UPDATED_EMAIL;

    mockMvc.perform(patch("/user/{id}", userToUpdate.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"email": "%s"}
                """.formatted(newEmail)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value(newEmail))
        .andExpect(jsonPath("$.role").value(userToUpdate.getRole().name()))
        .andExpect(jsonPath("$.status").value(AccountStatus.ACTIVE.name()));

    User updatedUser = userRepository.findById(userToUpdate.getId()).orElseThrow();
    Assertions.assertEquals(newEmail, updatedUser.getEmail());
  }

  @Test
  @WithUserDetails(value = ADMIN_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
  void patchUser_withValidStatus_updatesStatus() throws Exception {
    mockMvc.perform(patch("/user/{id}", userToUpdate.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"status": "INACTIVE"}
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(AccountStatus.INACTIVE.name()))
        .andExpect(jsonPath("$.email").value(userToUpdate.getEmail()))
        .andExpect(jsonPath("$.role").value(userToUpdate.getRole().name()));

    User updatedUser = userRepository.findById(userToUpdate.getId()).orElseThrow();
    Assertions.assertEquals(AccountStatus.INACTIVE, updatedUser.getStatus());
  }

  @Test
  @WithUserDetails(value = ADMIN_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
  void patchUser_withDuplicateEmail_returnsConflict() throws Exception {
    User existingUser = createUserWithProfile(EXISTING_EMAIL, Role.USER);

    mockMvc.perform(patch("/user/{id}", userToUpdate.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"email": "%s"}
                """.formatted(existingUser.getEmail())))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.title").value("Conflict"))
        .andExpect(jsonPath("$.detail").value("User with email '%s' already exists".formatted(existingUser.getEmail())));
  }

  @Test
  @WithUserDetails(value = ADMIN_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
  void patchUser_withInvalidRole_returnsBadRequest() throws Exception {
    mockMvc.perform(patch("/user/{id}", userToUpdate.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"role": "INVALID"}
                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.title").value("Bad Request"))
        .andExpect(jsonPath("$.detail").value("Request body is malformed or contains invalid values."))
        .andExpect(jsonPath("$.instance").value("/user/" + userToUpdate.getId()));
  }

  @Test
  @WithUserDetails(value = ADMIN_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
  void patchUser_withInvalidStatus_returnsBadRequest() throws Exception {
    mockMvc.perform(patch("/user/{id}", userToUpdate.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"status": "WRONG"}
                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.title").value("Bad Request"))
        .andExpect(jsonPath("$.detail").value("Request body is malformed or contains invalid values."))
        .andExpect(jsonPath("$.instance").value("/user/" + userToUpdate.getId()));
  }

  @Test
  @WithUserDetails(value = ADMIN_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
  void patchUser_withInvalidEmail_returnsBadRequest() throws Exception {
    mockMvc.perform(patch("/user/{id}", userToUpdate.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"email": "not-an-email"}
                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.title").value("Request Validation Failed"))
        .andExpect(jsonPath("$.errors[0]").value("email: A valid email must be provided"));
  }

  @Test
  @WithUserDetails(value = ADMIN_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
  void patchUser_withoutUpdates_returnsBadRequest() throws Exception {
    mockMvc.perform(patch("/user/{id}", userToUpdate.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {}
                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.title").value("Request Validation Failed"))
        .andExpect(jsonPath("$.errors[0]").value("updates: At least one field must be provided"));
  }

  @Test
  @WithUserDetails(value = ADMIN_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
  void patchUser_forMissingUser_returnsNotFound() throws Exception {
    UUID missingUserId = UUID.randomUUID();

    mockMvc.perform(patch("/user/{id}", missingUserId)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"role": "ADMINISTRATOR"}
                """))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.title").value("Not Found"))
        .andExpect(jsonPath("$.detail").value("User with id '%s' not found".formatted(missingUserId)));
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
