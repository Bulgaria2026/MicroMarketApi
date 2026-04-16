package com.noserbulgaria.micromarket.security.auth;

import com.noserbulgaria.micromarket.security.auth.dto.AuthResponseDto;
import com.noserbulgaria.micromarket.security.auth.dto.LoginRequestDto;
import com.noserbulgaria.micromarket.security.auth.dto.RefreshRequestDto;
import com.noserbulgaria.micromarket.security.auth.dto.RegisterRequestDto;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.server.ResponseStatusException;

/**
 * Service for user authentication and token management.
 */
public interface AuthService {

  /**
   * Registers a new user account and returns a fresh token pair.
   *
   * @param request the registration credentials
   * @return access and refresh tokens for the newly created account
   * @throws ResponseStatusException if the email is already registered
   */
  AuthResponseDto register(RegisterRequestDto request);

  /**
   * Authenticates a user by email and password and returns a fresh token pair.
   *
   * @param request the login credentials
   * @return access and refresh tokens
   * @throws BadCredentialsException if credentials are invalid
   */
  AuthResponseDto login(LoginRequestDto request);

  /**
   * Exchanges a valid refresh token for a new token pair.
   *
   * @param request containing the refresh token to exchange
   * @return a new access and refresh token pair
   * @throws BadCredentialsException if the refresh token is invalid or expired
   */
  AuthResponseDto refresh(RefreshRequestDto request);
}
