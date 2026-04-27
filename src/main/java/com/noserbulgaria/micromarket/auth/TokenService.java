package com.noserbulgaria.micromarket.auth;

import com.noserbulgaria.micromarket.exception.UnauthorizedApiException;
import com.noserbulgaria.micromarket.auth.user.User;
import lombok.Getter;
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

  @Getter
  @Value("${jwt.refresh-token.expiration}")
  private Duration refreshTokenExpiration;

  /** Creates a short-lived access token. */
  public String generateAccessToken(User user, String email) {
    Instant now = Instant.now();
    JwtClaimsSet claims = JwtClaimsSet.builder()
        .issuer(ISSUER)
        .issuedAt(now)
        .expiresAt(now.plus(accessTokenExpiration))
        .subject(user.getId().toString())
        .claim("email", email)
        .claim("roles", List.of(user.getRole().name()))
        .claim(TOKEN_TYPE_CLAIM, ACCESS_TOKEN_TYPE)
        .build();
    return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
  }

  /**
   * Creates a long-lived refresh token containing the user's identity and a unique token id.
   *
   * @param user an authenticated user
   * @param jti  the unique token identifier (maps to the {@code jti} JWT claim)
   * @return an encoded JWT refresh token
   */
  public String generateRefreshToken(User user, UUID jti) {
    Instant now = Instant.now();
    JwtClaimsSet claims = JwtClaimsSet.builder()
        .issuer(ISSUER)
        .issuedAt(now)
        .expiresAt(now.plus(refreshTokenExpiration))
        .subject(user.getId().toString())
        .id(jti.toString())
        .claim(TOKEN_TYPE_CLAIM, REFRESH_TOKEN_TYPE)
        .build();
    return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
  }

  /**
   * Validates a refresh token's signature, expiration, and type claim.
   *
   * @param token the encoded refresh token
   * @return the decoded JWT
   * @throws UnauthorizedApiException if the token is invalid, expired, or not a refresh token
   */
  public Jwt decodeRefreshToken(String token) {
    Jwt jwt;
    try {
      jwt = jwtDecoder.decode(token);
    } catch (JwtException _) {
      throw new UnauthorizedApiException("Invalid or expired refresh token");
    }
    String type = jwt.getClaimAsString(TOKEN_TYPE_CLAIM);
    if (!REFRESH_TOKEN_TYPE.equals(type)) {
      throw new UnauthorizedApiException("Expected refresh token but got '%s'".formatted(type == null ? "unknown" : type));
    }
    return jwt;
  }

  /**
   * Returns the configured access token lifetime in seconds.
   *
   * @return access token expiration in seconds
   */
  public long getAccessTokenExpirationSeconds() {
    return accessTokenExpiration.toSeconds();
  }
}
