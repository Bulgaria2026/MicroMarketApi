package com.noserbulgaria.micromarket.couponoffer;

import com.noserbulgaria.micromarket.auth.user.CustomUserDetails;
import com.noserbulgaria.micromarket.coupon.CouponResponse;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/coupon-offer")
@Tag(name = "Coupon Offer", description = "Coupon offer management and purchase endpoints")
public class CouponOfferController {

  private final CouponOfferService couponOfferService;

  @Operation(summary = "List active coupon offers available for purchase")
  @GetMapping("/catalog")
  public List<CouponOfferResponse> catalog() {
    return couponOfferService.catalog();
  }

  @Operation(summary = "Purchase a coupon offer with points")
  @ApiResponse(responseCode = "201", description = "Coupon purchased and issued")
  @PreAuthorize("hasRole('USER')")
  @PostMapping("/{id}/purchase")
  @ResponseStatus(HttpStatus.CREATED)
  public CouponResponse purchase(
      @Parameter(description = "Coupon offer ID") @PathVariable UUID id,
      @AuthenticationPrincipal CustomUserDetails userDetails
  ) {
    return couponOfferService.purchase(id, userDetails);
  }

  @Operation(summary = "Get all coupon offers with pagination and filtering")
  @GetMapping
  @PreAuthorize("hasRole('ADMINISTRATOR')")
  public Page<CouponOfferResponse> getAll(@ParameterObject Pageable pageable, @ParameterObject CouponOfferFilter filter) {
    return couponOfferService.findAll(CouponOfferSpecification.withFilter(filter), pageable);
  }

  @Operation(summary = "Get coupon offer by ID")
  @GetMapping("/{id}")
  @PreAuthorize("hasRole('ADMINISTRATOR')")
  public CouponOfferResponse getById(@PathVariable UUID id) {
    return couponOfferService.getByIdOrThrow(id);
  }

  @Operation(summary = "Create a new coupon offer")
  @PostMapping
  @PreAuthorize("hasRole('ADMINISTRATOR')")
  @ResponseStatus(HttpStatus.CREATED)
  public CouponOfferResponse create(@Valid @RequestBody CouponOfferRequest request) {
    return couponOfferService.create(request);
  }

  @Operation(summary = "Update an existing coupon offer")
  @PutMapping("/{id}")
  @PreAuthorize("hasRole('ADMINISTRATOR')")
  public CouponOfferResponse update(@PathVariable UUID id, @Valid @RequestBody CouponOfferRequest request) {
    return couponOfferService.updateOrThrow(id, request);
  }
}
