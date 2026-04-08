package com.noserbulgaria.micromarket.auth;

import com.noserbulgaria.micromarket.domain.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TokenService {

  private static final Duration TOKEN_VALIDITY = Duration.ofHours(24);
  private static final String ISSUER = "micro-market";
  private final JwtEncoder jwtEncoder;

  public String generateToken(User user) {
    Instant now = Instant.now();
    JwtClaimsSet claims = JwtClaimsSet.builder()
        .issuer(ISSUER)
        .issuedAt(now)
        .expiresAt(now.plus(TOKEN_VALIDITY))
        .subject(user.getId().toString())
        .claim("email", user.getEmail())
        .claim("roles", List.of(user.getRole().name()))
        .build();
    return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
  }
}
