package com.noserbulgaria.micromarket.auth;

import com.noserbulgaria.micromarket.domain.user.Role;
import com.noserbulgaria.micromarket.domain.user.User;
import com.noserbulgaria.micromarket.domain.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

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

  @Test
  void loginWithValidCredentials_returnsToken() throws Exception {
    mockMvc.perform(post("/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"email": "user@micromarket.dev", "password": "user123"}
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").isNotEmpty());
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

}
