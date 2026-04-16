package com.noserbulgaria.micromarket.security.auth;

import com.noserbulgaria.micromarket.security.auth.dto.AuthResponseDto;
import com.noserbulgaria.micromarket.security.auth.dto.LoginRequestDto;
import com.noserbulgaria.micromarket.security.auth.dto.RegisterRequestDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Register, login, refresh access tokens, and clear refresh cookies")
public class AuthController {

  private final AuthService authService;
  private final RefreshCookieService refreshCookieService;

  @Operation(summary = "Register a new user account")
  @ApiResponse(responseCode = "201", description = "Account created, access token returned and refresh cookie set")
  @ApiResponse(responseCode = "400", description = "Validation failed")
  @ApiResponse(responseCode = "409", description = "Email already registered")
  @SecurityRequirements
  @PostMapping("/register")
  public ResponseEntity<AuthResponseDto> register(@Valid @RequestBody RegisterRequestDto request) {
    AuthTokens authTokens = authService.register(request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .header(HttpHeaders.SET_COOKIE, refreshCookieService.createRefreshTokenCookie(authTokens.refreshToken()).toString())
        .body(new AuthResponseDto(authTokens.accessToken(), authTokens.expiresIn()));
  }

  @Operation(summary = "Authenticate with email and password")
  @ApiResponse(responseCode = "200", description = "Authentication successful, access token returned and refresh cookie set")
  @ApiResponse(responseCode = "400", description = "Validation failed")
  @ApiResponse(responseCode = "401", description = "Invalid credentials")
  @SecurityRequirements
  @PostMapping("/login")
  public ResponseEntity<AuthResponseDto> login(@Valid @RequestBody LoginRequestDto request) {
    AuthTokens authTokens = authService.login(request);
    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, refreshCookieService.createRefreshTokenCookie(authTokens.refreshToken()).toString())
        .body(new AuthResponseDto(authTokens.accessToken(), authTokens.expiresIn()));
  }

  @Operation(summary = "Refresh an expired access token")
  @ApiResponse(responseCode = "200", description = "New access token returned and refresh cookie rotated")
  @ApiResponse(responseCode = "401", description = "Invalid or expired refresh token")
  @SecurityRequirements
  @PostMapping("/refresh")
  public ResponseEntity<AuthResponseDto> refresh(HttpServletRequest request) {
    String refreshToken = refreshCookieService.extractRefreshToken(request);
    AuthTokens authTokens = authService.refresh(refreshToken);
    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, refreshCookieService.createRefreshTokenCookie(authTokens.refreshToken()).toString())
        .body(new AuthResponseDto(authTokens.accessToken(), authTokens.expiresIn()));
  }

  @Operation(summary = "Clear the refresh token cookie")
  @ApiResponse(responseCode = "204", description = "Refresh token cleared")
  @SecurityRequirements
  @PostMapping("/logout")
  public ResponseEntity<Void> logout() {
    return ResponseEntity.noContent()
        .header(HttpHeaders.SET_COOKIE, refreshCookieService.clearRefreshTokenCookie().toString())
        .build();
  }
}
