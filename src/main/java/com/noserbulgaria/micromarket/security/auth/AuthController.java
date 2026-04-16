package com.noserbulgaria.micromarket.security.auth;

import com.noserbulgaria.micromarket.security.auth.dto.AuthResponseDto;
import com.noserbulgaria.micromarket.security.auth.dto.LoginRequestDto;
import com.noserbulgaria.micromarket.security.auth.dto.RefreshRequestDto;
import com.noserbulgaria.micromarket.security.auth.dto.RegisterRequestDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Register, login, and refresh JWT tokens")
public class AuthController {

  private final AuthService authService;

  @Operation(summary = "Register a new user account")
  @ApiResponse(responseCode = "201", description = "Account created, tokens returned")
  @ApiResponse(responseCode = "400", description = "Validation failed")
  @ApiResponse(responseCode = "409", description = "Email already registered")
  @SecurityRequirements
  @PostMapping("/register")
  @ResponseStatus(HttpStatus.CREATED)
  public AuthResponseDto register(@Valid @RequestBody RegisterRequestDto request) {
    return authService.register(request);
  }

  @Operation(summary = "Authenticate with email and password")
  @ApiResponse(responseCode = "200", description = "Authentication successful, tokens returned")
  @ApiResponse(responseCode = "400", description = "Validation failed")
  @ApiResponse(responseCode = "401", description = "Invalid credentials")
  @SecurityRequirements
  @PostMapping("/login")
  public AuthResponseDto login(@Valid @RequestBody LoginRequestDto request) {
    return authService.login(request);
  }

  @Operation(summary = "Refresh an expired access token")
  @ApiResponse(responseCode = "200", description = "New token pair returned")
  @ApiResponse(responseCode = "401", description = "Invalid or expired refresh token")
  @SecurityRequirements
  @PostMapping("/refresh")
  public AuthResponseDto refresh(@Valid @RequestBody RefreshRequestDto request) {
    return authService.refresh(request);
  }
}
