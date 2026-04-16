package com.noserbulgaria.micromarket.security.auth;

import com.noserbulgaria.micromarket.exception.UnauthorizedApiException;
import com.noserbulgaria.micromarket.security.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Generates and validates JWT access and refresh tokens.
 *
 * <p>Access tokens carry user identity and roles for API authorization.
 * Refresh tokens carry only the user ID and are used solely to get new token pairs without re-authenticating.
 */
@Service
@RequiredArgsConstructor
public class TokenService {

  private static final String ISSUER = "micro-market";
  private static final String TOKEN_TYPE_CLAIM = "type";
  private static final String ACCESS_TOKEN_TYPE = "access";
  private static final String REFRESH_TOKEN_TYPE = "refresh";

  private final JwtEncoder jwtEncoder;
  private final JwtDecoder jwtDecoder;

  @Value("${jwt.access-token.expiration}")
  private Duration accessTokenExpiration;

  @Value("${jwt.refresh-token.expiration}")
  private Duration refreshTokenExpiration;

  /**
   * Creates a short-lived access token containing the user's identity and roles.
   *
   * @param user an authenticated user
   * @return an encoded JWT access token
   */
  public String generateAccessToken(User user) {
    Instant now = Instant.now();
    JwtClaimsSet claims = JwtClaimsSet.builder()
        .issuer(ISSUER)
        .issuedAt(now)
        .expiresAt(now.plus(accessTokenExpiration))
        .subject(user.getId().toString())
        .claim("email", user.getEmail())
        .claim("roles", List.of(user.getRole().name()))
        .claim(TOKEN_TYPE_CLAIM, ACCESS_TOKEN_TYPE)
        .build();
    return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
  }

  /**
   * Creates a long-lived refresh token containing only the user's identity.
   *
   * @param user an authenticated user
   * @return an encoded JWT refresh token
   */
  public String generateRefreshToken(User user) {
    Instant now = Instant.now();
    JwtClaimsSet claims = JwtClaimsSet.builder()
        .issuer(ISSUER)
        .issuedAt(now)
        .expiresAt(now.plus(refreshTokenExpiration))
        .subject(user.getId().toString())
        .claim(TOKEN_TYPE_CLAIM, REFRESH_TOKEN_TYPE)
        .build();
    return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
  }

  /**
   * Validates a refresh token and extracts the user ID.
   *
   * @param token the encoded refresh token
   * @return the user ID from the token's subject claim
   * @throws UnauthorizedApiException if the token is invalid, expired, or not a refresh token
   */
  public UUID parseRefreshToken(String token) {
    Jwt jwt;
    try {
      jwt = jwtDecoder.decode(token);
    } catch (JwtException e) {
      throw new UnauthorizedApiException("Invalid or expired refresh token");
    }
    String type = jwt.getClaimAsString(TOKEN_TYPE_CLAIM);
    if (!REFRESH_TOKEN_TYPE.equals(type)) {
      throw new UnauthorizedApiException("Expected refresh token but got '%s'".formatted(type == null ? "unknown" : type));
    }
    return UUID.fromString(jwt.getSubject());
  }

  /**
   * Returns the configured access token lifetime in seconds.
   *
   * @return access token expiration in seconds
   */
  public long getAccessTokenExpirationSeconds() {
    return accessTokenExpiration.toSeconds();
  }

  /**
   * Returns the configured refresh token lifetime.
   *
   * @return refresh token expiration duration
   */
  public Duration getRefreshTokenExpiration() {
    return refreshTokenExpiration;
  }
}
