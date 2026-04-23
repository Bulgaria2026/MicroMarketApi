package com.noserbulgaria.micromarket.security.user;

import com.noserbulgaria.micromarket.security.user.dto.UserPatchRequestDto;
import com.noserbulgaria.micromarket.security.user.dto.UserResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
@Tag(name = "User", description = "User related REST-operations")
public class UserController {

  private final UserService userService;

  @Operation(summary = "Get users by ID")
  @ApiResponse(responseCode = "200", description = "User found")
  @ApiResponse(responseCode = "401", description = "Authentication required to access a user")
  @ApiResponse(responseCode = "403", description = "Access denied to the user")
  @ApiResponse(responseCode = "404", description = "User not found")
  @GetMapping("/{id}")
  @PreAuthorize("hasRole('ADMINISTRATOR')")
  public UserResponseDto getById(@Parameter(description = "User id") @PathVariable UUID id) {
    return userService.getByIdOrThrow(id);
  }

  @Operation(summary = "Get own users")
  @ApiResponse(responseCode = "200", description = "User found")
  @ApiResponse(responseCode = "401", description = "Authentication required to access user")
  @GetMapping("/own")
  public UserResponseDto getOwn(@AuthenticationPrincipal CustomUserDetails userDetails) {
    return userService.getByIdOrThrow(userDetails.getId());
  }

  @Operation(summary = "Patch admin-managed user fields")
  @ApiResponse(responseCode = "200", description = "User updated")
  @ApiResponse(responseCode = "400", description = "Invalid user update payload")
  @ApiResponse(responseCode = "401", description = "Unauthorized")
  @ApiResponse(responseCode = "403", description = "Forbidden - insufficient permissions")
  @ApiResponse(responseCode = "404", description = "User not found")
  @ApiResponse(responseCode = "409", description = "Email already exists")
  @PreAuthorize("hasRole('ADMINISTRATOR')")
  @PatchMapping("/{id}")
  public UserResponseDto patchUser(@PathVariable UUID id, @Valid @RequestBody UserPatchRequestDto request) {
    return userService.patchUserOrThrow(id, request);
  }
}
