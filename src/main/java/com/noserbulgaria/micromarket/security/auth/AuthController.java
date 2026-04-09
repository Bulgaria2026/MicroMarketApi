package com.noserbulgaria.micromarket.security.auth;

import com.noserbulgaria.micromarket.security.auth.dto.AuthResponseDTO;
import com.noserbulgaria.micromarket.security.auth.dto.LoginRequestDTO;
import com.noserbulgaria.micromarket.security.auth.dto.RefreshRequestDTO;
import com.noserbulgaria.micromarket.security.auth.dto.RegisterRequestDTO;
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
  public AuthResponseDTO register(@Valid @RequestBody RegisterRequestDTO request) {
    return authService.register(request);
  }

  @Operation(summary = "Authenticate with email and password")
  @ApiResponse(responseCode = "200", description = "Authentication successful, tokens returned")
  @ApiResponse(responseCode = "400", description = "Validation failed")
  @ApiResponse(responseCode = "401", description = "Invalid credentials")
  @SecurityRequirements
  @PostMapping("/login")
  public AuthResponseDTO login(@Valid @RequestBody LoginRequestDTO request) {
    return authService.login(request);
  }

  @Operation(summary = "Refresh an expired access token")
  @ApiResponse(responseCode = "200", description = "New token pair returned")
  @ApiResponse(responseCode = "401", description = "Invalid or expired refresh token")
  @SecurityRequirements
  @PostMapping("/refresh")
  public AuthResponseDTO refresh(@Valid @RequestBody RefreshRequestDTO request) {
    return authService.refresh(request);
  }
}
