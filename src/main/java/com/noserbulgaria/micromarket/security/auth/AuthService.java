package com.noserbulgaria.micromarket.security.auth;

import com.noserbulgaria.micromarket.exception.ConflictApiException;
import com.noserbulgaria.micromarket.exception.NotFoundApiException;
import com.noserbulgaria.micromarket.security.auth.dto.LoginRequestDto;
import com.noserbulgaria.micromarket.security.auth.dto.RegisterRequestDto;
import com.noserbulgaria.micromarket.security.user.Role;
import com.noserbulgaria.micromarket.security.user.User;
import com.noserbulgaria.micromarket.security.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

  private final AuthenticationManager authenticationManager;
  private final UserRepository userRepository;
  private final TokenService tokenService;
  private final PasswordEncoder passwordEncoder;

  public AuthTokens register(RegisterRequestDto request) {
    if (userRepository.existsByEmail(request.email())) {
      throw new ConflictApiException("User with email '%s' already exists".formatted(request.email()));
    }

    User user = new User();
    user.setEmail(request.email());
    user.setPassword(Objects.requireNonNull(passwordEncoder.encode(request.password())));
    user.setRole(Role.USER);
    user = userRepository.save(user);

    return generateAuthTokens(user);
  }

  public AuthTokens login(LoginRequestDto request) {
    authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(request.email(), request.password()));

    User user = userRepository.findByEmail(request.email())
        .orElseThrow(() -> new NotFoundApiException("User with email '%s' not found".formatted(request.email())));
    return generateAuthTokens(user);
  }

  @Transactional(readOnly = true)
  public AuthTokens refresh(String refreshToken) {
    UUID userId = tokenService.parseRefreshToken(refreshToken);
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new NotFoundApiException("User with id '%s' not found".formatted(userId)));
    return generateAuthTokens(user);
  }

  private AuthTokens generateAuthTokens(User user) {
    String accessToken = tokenService.generateAccessToken(user);
    String refreshToken = tokenService.generateRefreshToken(user);
    long expiresIn = tokenService.getAccessTokenExpirationSeconds();
    return new AuthTokens(accessToken, refreshToken, expiresIn);
  }
}
