package com.noserbulgaria.micromarket.security.auth.refresh;

import com.noserbulgaria.micromarket.exception.UnauthorizedApiException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

@Service
public class RefreshCookieService {

  private final RefreshCookieProperties refreshCookieProperties;
  private final Duration refreshTokenMaxAge;

  public RefreshCookieService(
      RefreshCookieProperties refreshCookieProperties,
      @Value("${jwt.refresh-token.expiration}") Duration refreshTokenMaxAge) {
    this.refreshCookieProperties = refreshCookieProperties;
    this.refreshTokenMaxAge = refreshTokenMaxAge;
  }

  public ResponseCookie createRefreshTokenCookie(String refreshToken) {
    return createCookie(refreshToken, refreshTokenMaxAge);
  }

  public ResponseCookie clearRefreshTokenCookie() {
    return createCookie("", Duration.ZERO);
  }

  /**
   * Reads the refresh token cookie, throwing 401 if missing.
   */
  public String extractRefreshToken(HttpServletRequest request) {
    return extractOptionalRefreshToken(request)
        .orElseThrow(() -> new UnauthorizedApiException("Refresh token is required"));
  }

  /**
   * Reads the refresh token cookie if present. Use on idempotent endpoints that must not fail
   * when the cookie is absent (e.g. {@code /auth/logout}).
   */
  public Optional<String> extractOptionalRefreshToken(HttpServletRequest request) {
    @Nullable Cookie[] cookies = request.getCookies();
    if (cookies == null) {
      return Optional.empty();
    }

    return Arrays.stream(cookies)
        .filter(Objects::nonNull)
        .filter(cookie -> Objects.equals(refreshCookieProperties.getName(), cookie.getName()))
        .map(Cookie::getValue)
        .filter(value -> !value.isBlank())
        .findFirst();
  }

  private ResponseCookie createCookie(String value, Duration maxAge) {
    return ResponseCookie.from(refreshCookieProperties.getName(), value)
        .httpOnly(true)
        .secure(refreshCookieProperties.isSecure())
        .sameSite(refreshCookieProperties.getSameSite())
        .path(refreshCookieProperties.getPath())
        .maxAge(maxAge)
        .build();
  }
}
