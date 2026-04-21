package com.noserbulgaria.micromarket.auth;

import com.noserbulgaria.micromarket.auth.refresh.RefreshCookieService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
  public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
    AuthTokens authTokens = authService.register(request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .header(HttpHeaders.SET_COOKIE, refreshCookieService.createRefreshTokenCookie(authTokens.refreshToken()).toString())
        .body(new AuthResponse(authTokens.accessToken(), authTokens.expiresIn()));
  }

  @Operation(summary = "Authenticate with email and password")
  @ApiResponse(responseCode = "200", description = "Authentication successful, access token returned and refresh cookie set")
  @ApiResponse(responseCode = "400", description = "Validation failed")
  @ApiResponse(responseCode = "401", description = "Invalid credentials")
  @SecurityRequirements
  @PostMapping("/login")
  public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
    AuthTokens authTokens = authService.login(request);
    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, refreshCookieService.createRefreshTokenCookie(authTokens.refreshToken()).toString())
        .body(new AuthResponse(authTokens.accessToken(), authTokens.expiresIn()));
  }

  @Operation(summary = "Refresh an expired access token")
  @Parameter(
      in = ParameterIn.COOKIE,
      name = "refresh_token",
      description = "Refresh token cookie — set automatically by login/register/refresh. "
          + "In Swagger UI, leave this field empty; the browser attaches the cookie on its own.",
      schema = @Schema(type = "string")
  )
  @ApiResponse(responseCode = "200", description = "New access token returned and refresh cookie rotated")
  @ApiResponse(responseCode = "401", description = "Invalid, expired, or reused refresh token")
  @SecurityRequirements
  @PostMapping("/refresh")
  public ResponseEntity<AuthResponse> refresh(HttpServletRequest request) {
    String refreshToken = refreshCookieService.extractRefreshToken(request);
    AuthTokens authTokens = authService.refresh(refreshToken);
    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, refreshCookieService.createRefreshTokenCookie(authTokens.refreshToken()).toString())
        .body(new AuthResponse(authTokens.accessToken(), authTokens.expiresIn()));
  }

  @Operation(summary = "Revoke the current refresh token family and clear the cookie")
  @ApiResponse(responseCode = "204", description = "Refresh token revoked and cookie cleared")
  @SecurityRequirements
  @PostMapping("/logout")
  public ResponseEntity<Void> logout(HttpServletRequest request) {
    authService.revokeFamilyFromToken(
        refreshCookieService.extractOptionalRefreshToken(request).orElse(null));
    return ResponseEntity.noContent()
        .header(HttpHeaders.SET_COOKIE, refreshCookieService.clearRefreshTokenCookie().toString())
        .build();
  }
}
