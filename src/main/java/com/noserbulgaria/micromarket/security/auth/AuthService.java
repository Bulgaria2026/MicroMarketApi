package com.noserbulgaria.micromarket.security.auth;

import com.noserbulgaria.micromarket.exception.ConflictApiException;
import com.noserbulgaria.micromarket.exception.NotFoundApiException;
import com.noserbulgaria.micromarket.security.auth.dto.LoginRequestDto;
import com.noserbulgaria.micromarket.security.auth.dto.RegisterRequestDto;
import com.noserbulgaria.micromarket.security.auth.refresh.RefreshTokenService;
import com.noserbulgaria.micromarket.security.auth.refresh.RotationResult;
import com.noserbulgaria.micromarket.security.user.Role;
import com.noserbulgaria.micromarket.security.user.User;
import com.noserbulgaria.micromarket.security.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AuthService {

  private final AuthenticationManager authenticationManager;
  private final UserRepository userRepository;
  private final TokenService tokenService;
  private final RefreshTokenService refreshTokenService;
  private final PasswordEncoder passwordEncoder;

  @Transactional
  public AuthTokens register(RegisterRequestDto request) {
    if (userRepository.existsByEmail(request.email())) {
      throw new ConflictApiException("User with email '%s' already exists".formatted(request.email()));
    }

    User user = new User();
    user.setEmail(request.email());
    user.setPassword(Objects.requireNonNull(passwordEncoder.encode(request.password())));
    user.setRole(Role.USER);
    user = userRepository.save(user);

    return buildAuthTokens(user, refreshTokenService.issueForNewFamily(user));
  }

  @Transactional
  public AuthTokens login(LoginRequestDto request) {
    authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(request.email(), request.password()));

    User user = userRepository.findByEmail(request.email())
        .orElseThrow(() -> new NotFoundApiException("User with email '%s' not found".formatted(request.email())));
    return buildAuthTokens(user, refreshTokenService.issueForNewFamily(user));
  }

  public AuthTokens refresh(String rawRefreshToken) {
    RotationResult result = refreshTokenService.rotate(rawRefreshToken);
    return buildAuthTokens(result.user(), result.newRawToken());
  }

  public void revokeFamilyFromToken(@Nullable String rawRefreshToken) {
    refreshTokenService.revokeFamilyOfToken(rawRefreshToken);
  }

  private AuthTokens buildAuthTokens(User user, String refreshToken) {
    String accessToken = tokenService.generateAccessToken(user);
    long expiresIn = tokenService.getAccessTokenExpirationSeconds();
    return new AuthTokens(accessToken, refreshToken, expiresIn);
  }
}
