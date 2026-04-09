package com.noserbulgaria.micromarket.auth;

import com.jayway.jsonpath.JsonPath;
import com.noserbulgaria.micromarket.security.user.Role;
import com.noserbulgaria.micromarket.security.user.User;
import com.noserbulgaria.micromarket.security.user.UserRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private PasswordEncoder passwordEncoder;

  @BeforeEach
  void setUp() {
    userRepository.deleteAll();

    User user = new User();
    user.setEmail("user@micromarket.dev");
    user.setPassword(passwordEncoder.encode("user123"));
    user.setRole(Role.USER);
    userRepository.save(user);
  }

  // --- Login tests ---

  @Test
  void loginWithValidCredentials_returnsTokens() throws Exception {
    mockMvc.perform(post("/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"email": "user@micromarket.dev", "password": "user123"}
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").isNotEmpty())
        .andExpect(jsonPath("$.refreshToken").isNotEmpty())
        .andExpect(jsonPath("$.expiresIn").isNumber());
  }

  @Test
  void loginWithInvalidPassword_returns401() throws Exception {
    mockMvc.perform(post("/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"email": "user@micromarket.dev", "password": "wrongpassword"}
                """))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void loginWithNonExistentUser_returns401() throws Exception {
    mockMvc.perform(post("/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"email": "nobody@micromarket.dev", "password": "whatever"}
                """))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void loginWithBlankEmail_returns400() throws Exception {
    mockMvc.perform(post("/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"email": "", "password": "user123"}
                """))
        .andExpect(status().isBadRequest());
  }

  @Test
  void loginWithInvalidEmailFormat_returns400() throws Exception {
    mockMvc.perform(post("/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"email": "not-an-email", "password": "user123"}
                """))
        .andExpect(status().isBadRequest());
  }

  // --- Registration tests ---

  @Test
  void registerWithValidData_returns201AndTokens() throws Exception {
    mockMvc.perform(post("/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"email": "newuser@micromarket.dev", "password": "securepass123"}
                """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.accessToken").isNotEmpty())
        .andExpect(jsonPath("$.refreshToken").isNotEmpty())
        .andExpect(jsonPath("$.expiresIn").isNumber());
  }

  @Test
  void registerWithDuplicateEmail_returns409() throws Exception {
    mockMvc.perform(post("/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"email": "user@micromarket.dev", "password": "securepass123"}
                """))
        .andExpect(status().isConflict());
  }

  @Test
  void registerWithInvalidEmail_returns400() throws Exception {
    mockMvc.perform(post("/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"email": "not-an-email", "password": "securepass123"}
                """))
        .andExpect(status().isBadRequest());
  }

  @Test
  void registerWithShortPassword_returns400() throws Exception {
    mockMvc.perform(post("/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"email": "short@micromarket.dev", "password": "short"}
                """))
        .andExpect(status().isBadRequest());
  }

  @Test
  void registerWithBlankFields_returns400() throws Exception {
    mockMvc.perform(post("/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"email": "", "password": ""}
                """))
        .andExpect(status().isBadRequest());
  }

  // --- Refresh tests ---

  @Test
  void refreshWithValidToken_returnsNewTokens() throws Exception {
    MvcResult loginResult = mockMvc.perform(post("/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"email": "user@micromarket.dev", "password": "user123"}
                """))
        .andExpect(status().isOk())
        .andReturn();

    String refreshToken = JsonPath.read(loginResult.getResponse().getContentAsString(), "$.refreshToken");

    mockMvc.perform(post("/auth/refresh")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"refreshToken": "%s"}
                """.formatted(refreshToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").isNotEmpty())
        .andExpect(jsonPath("$.refreshToken").isNotEmpty())
        .andExpect(jsonPath("$.expiresIn").isNumber());
  }

  @Test
  void refreshWithInvalidToken_returns401() throws Exception {
    mockMvc.perform(post("/auth/refresh")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"refreshToken": "invalid.jwt.token"}
                """))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void refreshWithBlankToken_returns400() throws Exception {
    mockMvc.perform(post("/auth/refresh")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"refreshToken": ""}
                """))
        .andExpect(status().isBadRequest());
  }
}
