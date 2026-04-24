package com.noserbulgaria.micromarket.coupon;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/coupon")
@Tag(name = "Coupon", description = "Coupon management endpoints")
public class CouponController {

  private final CouponService couponService;

  @Operation(summary = "Get authenticated user's coupons")
  @GetMapping("/own")
  public Page<CouponResponse> getOwn(
      @AuthenticationPrincipal com.noserbulgaria.micromarket.auth.user.CustomUserDetails userDetails,
      @ParameterObject Pageable pageable,
      @org.springframework.web.bind.annotation.RequestParam(required = false) Boolean active
  ) {
    return couponService.findOwn(userDetails.getId(), active, pageable);
  }

  @Operation(summary = "Get all coupons with pagination and filtering")
  @GetMapping
  @PreAuthorize("hasRole('ADMINISTRATOR')")
  public Page<CouponResponse> getAll(@ParameterObject Pageable pageable, @ParameterObject CouponFilter filter) {
    return couponService.findAll(CouponSpecification.withFilter(filter), pageable);
  }

  @Operation(summary = "Get coupon by ID")
  @ApiResponse(responseCode = "200", description = "Coupon found")
  @ApiResponse(responseCode = "404", description = "Coupon not found")
  @GetMapping("/{id}")
  @PreAuthorize("hasRole('ADMINISTRATOR')")
  public CouponResponse getById(@Parameter(description = "Coupon ID") @PathVariable UUID id) {
    return couponService.getByIdOrThrow(id);
  }

  @Operation(summary = "Create a new coupon")
  @ApiResponse(responseCode = "201", description = "Coupon created")
  @ApiResponse(responseCode = "404", description = "User not found")
  @PostMapping
  @PreAuthorize("hasRole('ADMINISTRATOR')")
  @ResponseStatus(HttpStatus.CREATED)
  public CouponResponse create(@Valid @RequestBody CouponRequest request) {
    return couponService.create(request);
  }

  @Operation(summary = "Update an existing coupon")
  @ApiResponse(responseCode = "200", description = "Coupon updated")
  @ApiResponse(responseCode = "404", description = "Coupon or user not found")
  @PutMapping("/{id}")
  @PreAuthorize("hasRole('ADMINISTRATOR')")
  public CouponResponse update(@PathVariable UUID id, @Valid @RequestBody CouponRequest request) {
    return couponService.updateOrThrow(id, request);
  }
}
