package com.noserbulgaria.micromarket.security.auth;

import com.noserbulgaria.micromarket.exception.UnauthorizedApiException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Arrays;

@Service
@RequiredArgsConstructor
public class RefreshCookieService {

  private final RefreshCookieProperties refreshCookieProperties;
  private final TokenService tokenService;

  public ResponseCookie createRefreshTokenCookie(String refreshToken) {
    return createCookie(refreshToken, tokenService.getRefreshTokenExpiration());
  }

  public ResponseCookie clearRefreshTokenCookie() {
    return createCookie("", Duration.ZERO);
  }

  public String extractRefreshToken(HttpServletRequest request) {
    @Nullable Cookie[] cookies = request.getCookies();
    if (cookies == null) {
      throw new UnauthorizedApiException("Refresh token is required");
    }

    return Arrays.stream(cookies)
        .filter(cookie -> refreshCookieProperties.getName().equals(cookie.getName()))
        .map(Cookie::getValue)
        .filter(value -> !value.isBlank())
        .findFirst()
        .orElseThrow(() -> new UnauthorizedApiException("Refresh token is required"));
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
