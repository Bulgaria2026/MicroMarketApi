package com.noserbulgaria.micromarket.auth;

import com.noserbulgaria.micromarket.security.user.Role;
import com.noserbulgaria.micromarket.security.user.User;
import com.noserbulgaria.micromarket.security.user.UserRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Objects;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

  private static final String REFRESH_COOKIE_NAME = "refresh_token";

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
    user.setPassword(Objects.requireNonNull(passwordEncoder.encode("user123")));
    user.setRole(Role.USER);
    userRepository.save(user);
  }

  // --- Login tests ---

  @Test
  void loginWithValidCredentials_returnsAccessTokenAndRefreshCookie() throws Exception {
    ResultActions result = mockMvc.perform(post("/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"email": "user@micromarket.dev", "password": "user123"}
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").isNotEmpty())
        .andExpect(jsonPath("$.refreshToken").doesNotExist())
        .andExpect(jsonPath("$.expiresIn").isNumber());
    assertRefreshCookieAttributes(result);
  }

  @Test
  void loginWithInvalidPassword_returns401() throws Exception {
    mockMvc.perform(post("/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"email": "user@micromarket.dev", "password": "wrongpassword"}
                """))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.title").value("Authentication Failed"))
        .andExpect(jsonPath("$.status").value(401))
        .andExpect(jsonPath("$.detail").value("Authentication for '/auth/login' failed."))
        .andExpect(jsonPath("$.instance").value("/auth/login"));
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
  void registerWithValidData_returns201AccessTokenAndRefreshCookie() throws Exception {
    ResultActions result = mockMvc.perform(post("/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"email": "newuser@micromarket.dev", "password": "securepass123"}
                """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.accessToken").isNotEmpty())
        .andExpect(jsonPath("$.refreshToken").doesNotExist())
        .andExpect(jsonPath("$.expiresIn").isNumber());
    assertRefreshCookieAttributes(result);
  }

  @Test
  void registerWithDuplicateEmail_returns409() throws Exception {
    mockMvc.perform(post("/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"email": "user@micromarket.dev", "password": "securepass123"}
                """))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.title").value("Conflict"))
        .andExpect(jsonPath("$.status").value(409))
        .andExpect(jsonPath("$.detail").value("User with email 'user@micromarket.dev' already exists"))
        .andExpect(jsonPath("$.instance").value("/auth/register"));
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
  void refreshWithValidCookie_returnsNewAccessTokenAndRotatedRefreshCookie() throws Exception {
    String originalRefresh = loginAndCaptureRefreshToken();

    ResultActions result = mockMvc.perform(post("/auth/refresh")
            .cookie(new Cookie(REFRESH_COOKIE_NAME, originalRefresh)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").isNotEmpty())
        .andExpect(jsonPath("$.refreshToken").doesNotExist())
        .andExpect(jsonPath("$.expiresIn").isNumber());
    assertRefreshCookieAttributes(result);

    String rotatedRefresh = refreshTokenFrom(result.andReturn());
    assertNotEquals(originalRefresh, rotatedRefresh, "Refresh endpoint must rotate the cookie value");
  }

  @Test
  void refreshWithReusedToken_revokesFamilyAndReturns401() throws Exception {
    String originalRefresh = loginAndCaptureRefreshToken();

    MvcResult firstRefresh = mockMvc.perform(post("/auth/refresh")
            .cookie(new Cookie(REFRESH_COOKIE_NAME, originalRefresh)))
        .andExpect(status().isOk())
        .andReturn();
    String rotatedRefresh = refreshTokenFrom(firstRefresh);

    mockMvc.perform(post("/auth/refresh")
            .cookie(new Cookie(REFRESH_COOKIE_NAME, originalRefresh)))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.title").value("Unauthorized"))
        .andExpect(jsonPath("$.detail").value("Invalid or expired refresh token"));

    mockMvc.perform(post("/auth/refresh")
            .cookie(new Cookie(REFRESH_COOKIE_NAME, rotatedRefresh)))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.detail").value("Invalid or expired refresh token"));
  }

  @Test
  void refreshWithInvalidCookie_returns401() throws Exception {
    mockMvc.perform(post("/auth/refresh")
            .cookie(new Cookie(REFRESH_COOKIE_NAME, "invalid.jwt.token")))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.title").value("Unauthorized"))
        .andExpect(jsonPath("$.status").value(401))
        .andExpect(jsonPath("$.detail").value("Invalid or expired refresh token"))
        .andExpect(jsonPath("$.instance").value("/auth/refresh"));
  }

  @Test
  void refreshWithoutCookie_returns401() throws Exception {
    mockMvc.perform(post("/auth/refresh"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.title").value("Unauthorized"))
        .andExpect(jsonPath("$.status").value(401))
        .andExpect(jsonPath("$.detail").value("Refresh token is required"))
        .andExpect(jsonPath("$.instance").value("/auth/refresh"));
  }

  @Test
  void logoutWithoutCookie_clearsRefreshCookie() throws Exception {
    mockMvc.perform(post("/auth/logout"))
        .andExpect(status().isNoContent())
        .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString(REFRESH_COOKIE_NAME + "=")))
        .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")));
  }

  @Test
  void logoutWithValidCookie_revokesFamily() throws Exception {
    String originalRefresh = loginAndCaptureRefreshToken();

    mockMvc.perform(post("/auth/logout")
            .cookie(new Cookie(REFRESH_COOKIE_NAME, originalRefresh)))
        .andExpect(status().isNoContent())
        .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")));

    mockMvc.perform(post("/auth/refresh")
            .cookie(new Cookie(REFRESH_COOKIE_NAME, originalRefresh)))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.detail").value("Invalid or expired refresh token"));
  }

  private void assertRefreshCookieAttributes(ResultActions actions) throws Exception {
    actions
        .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString(REFRESH_COOKIE_NAME + "=")))
        .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")))
        .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("SameSite=Lax")))
        .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Path=/api/v1/auth")))
        .andExpect(header().string(HttpHeaders.SET_COOKIE, not(containsString("Secure"))));
  }

  private String loginAndCaptureRefreshToken() throws Exception {
    MvcResult loginResult = mockMvc.perform(post("/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"email": "user@micromarket.dev", "password": "user123"}
                """))
        .andExpect(status().isOk())
        .andReturn();
    return refreshTokenFrom(loginResult);
  }

  private String refreshTokenFrom(MvcResult result) {
    Cookie cookie = result.getResponse().getCookie(REFRESH_COOKIE_NAME);
    return Objects.requireNonNull(cookie, "Missing refresh cookie in response").getValue();
  }
}
