package com.noserbulgaria.micromarket.auth.user;

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
@RequestMapping("/account")
@RequiredArgsConstructor
@Tag(name = "Account", description = "Admin-managed account fields spanning User + Customer")
public class AccountController {

  private final AccountService accountService;

  @Operation(summary = "Get account by ID")
  @ApiResponse(responseCode = "200", description = "Account found")
  @ApiResponse(responseCode = "401", description = "Authentication required to access an account")
  @ApiResponse(responseCode = "403", description = "Access denied to the account")
  @ApiResponse(responseCode = "404", description = "Account not found")
  @GetMapping("/{id}")
  @PreAuthorize("hasRole('ADMINISTRATOR')")
  public AccountResponse getById(@Parameter(description = "User id") @PathVariable UUID id) {
    return accountService.getByIdOrThrow(id);
  }

  @Operation(summary = "Get own account")
  @ApiResponse(responseCode = "200", description = "Account found")
  @ApiResponse(responseCode = "401", description = "Authentication required to access account")
  @GetMapping("/own")
  public AccountResponse getOwn(@AuthenticationPrincipal CustomUserDetails userDetails) {
    return accountService.getByIdOrThrow(userDetails.getId());
  }

  @Operation(summary = "Patch admin-managed account fields")
  @ApiResponse(responseCode = "200", description = "Account updated")
  @ApiResponse(responseCode = "400", description = "Invalid account update payload")
  @ApiResponse(responseCode = "401", description = "Unauthorized")
  @ApiResponse(responseCode = "403", description = "Forbidden - insufficient permissions")
  @ApiResponse(responseCode = "404", description = "Account not found")
  @ApiResponse(responseCode = "409", description = "Email already exists")
  @PreAuthorize("hasRole('ADMINISTRATOR')")
  @PatchMapping("/{id}")
  public AccountResponse patchAccount(@PathVariable UUID id, @Valid @RequestBody AccountPatchRequest request) {
    return accountService.patchAccountOrThrow(id, request);
  }
}
