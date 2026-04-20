package com.noserbulgaria.micromarket.domain.profile;

import com.noserbulgaria.micromarket.domain.profile.dto.ProfileResponseDto;
import com.noserbulgaria.micromarket.security.user.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/profile")
@Tag(name = "Profile", description = "Profile management endpoints")
public class ProfileController {

  private final ProfileService profileService;

  @Operation(summary = "Get profile by ID")
  @ApiResponse(responseCode = "200", description = "Profile found")
  @ApiResponse(responseCode = "401", description = "Authentication required to access a profile")
  @ApiResponse(responseCode = "403", description = "Access denied to the profile")
  @ApiResponse(responseCode = "404", description = "Profile not found")
  @GetMapping("/{id}")
  @PreAuthorize("hasRole('ADMINISTRATOR')")
  public ProfileResponseDto getById(@Parameter(description = "Profile id") @PathVariable UUID id) {
    return profileService.getByIdOrThrow(id);
  }

  @Operation(summary = "Get own profile")
  @ApiResponse(responseCode = "200", description = "Profile found")
  @ApiResponse(responseCode = "401", description = "Authentication required to access a profile")
  @GetMapping("/own")
  public ProfileResponseDto getOwn(@AuthenticationPrincipal CustomUserDetails userDetails) {
    return profileService.getByUserIdOrThrow(userDetails.getId());
  }
}
