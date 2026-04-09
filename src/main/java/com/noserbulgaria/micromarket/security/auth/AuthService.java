package com.noserbulgaria.micromarket.security.auth;

import com.noserbulgaria.micromarket.security.auth.dto.AuthResponseDTO;
import com.noserbulgaria.micromarket.security.auth.dto.LoginRequestDTO;
import com.noserbulgaria.micromarket.security.auth.dto.RefreshRequestDTO;
import com.noserbulgaria.micromarket.security.auth.dto.RegisterRequestDTO;
import com.noserbulgaria.micromarket.security.auth.exception.DuplicateEmailException;
import org.springframework.security.authentication.BadCredentialsException;

/**
 * Service for user authentication and token management.
 */
public interface AuthService {

  /**
   * Registers a new user account and returns a fresh token pair.
   *
   * @param request the registration credentials
   * @return access and refresh tokens for the newly created account
   * @throws DuplicateEmailException if the email is already registered
   */
  AuthResponseDTO register(RegisterRequestDTO request);

  /**
   * Authenticates a user by email and password and returns a fresh token pair.
   *
   * @param request the login credentials
   * @return access and refresh tokens
   * @throws BadCredentialsException if credentials are invalid
   */
  AuthResponseDTO login(LoginRequestDTO request);

  /**
   * Exchanges a valid refresh token for a new token pair.
   *
   * @param request containing the refresh token to exchange
   * @return a new access and refresh token pair
   * @throws BadCredentialsException if the refresh token is invalid or expired
   */
  AuthResponseDTO refresh(RefreshRequestDTO request);
}
