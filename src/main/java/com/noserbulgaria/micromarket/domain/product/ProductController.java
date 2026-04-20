package com.noserbulgaria.micromarket.domain.product;

import com.noserbulgaria.micromarket.domain.product.dto.ProductHistoryResponseDto;
import com.noserbulgaria.micromarket.domain.product.dto.ProductRequestDto;
import com.noserbulgaria.micromarket.domain.product.dto.ProductResponseDto;
import com.noserbulgaria.micromarket.security.user.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/product")
@Tag(name = "Product", description = "Product management endpoints")
public class ProductController {

  private final ProductService productService;

  @Operation(summary = "Get all products with pagination and filtering")
  @GetMapping
  public Page<ProductResponseDto> getAll(
      @ParameterObject Pageable pageable,
      @ParameterObject ProductFilter filter,
      @AuthenticationPrincipal @Nullable CustomUserDetails userDetails
  ) {
    return productService.findAll(ProductSpecification.forRole(filter, userDetails), pageable);
  }

  @Operation(summary = "Get product by ID")
  @ApiResponse(responseCode = "200", description = "Product found")
  @ApiResponse(responseCode = "401", description = "Authentication required to access a disabled product")
  @ApiResponse(responseCode = "404", description = "Product not found")
  @GetMapping("/{id}")
  public ProductResponseDto getById(
      @Parameter(description = "Product id") @PathVariable UUID id,
      @AuthenticationPrincipal @Nullable CustomUserDetails userDetails
  ) {
    return productService.getByIdForCurrentUser(id, userDetails);
  }

  @Operation(summary = "Create a new product")
  @ApiResponse(responseCode = "201", description = "Product created successfully")
  @PreAuthorize("hasRole('ADMINISTRATOR')")
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ProductResponseDto create(@Valid @RequestBody ProductRequestDto productDto) {
    return productService.create(productDto);
  }

  @Operation(summary = "Get product revision history")
  @ApiResponse(responseCode = "200", description = "History retrieved")
  @ApiResponse(responseCode = "401", description = "Authentication required to access a disabled product's history")
  @ApiResponse(responseCode = "404", description = "Product not found")
  @GetMapping("/{id}/history")
  public List<ProductHistoryResponseDto> getHistory(
      @Parameter(description = "Product ID") @PathVariable UUID id,
      @AuthenticationPrincipal @Nullable CustomUserDetails userDetails
  ) {
    return productService.getHistory(id, userDetails);
  }

  @Operation(summary = "Update a product")
  @ApiResponse(responseCode = "200", description = "Product updated successfully")
  @ApiResponse(responseCode = "404", description = "Product not found")
  @PreAuthorize("hasRole('ADMINISTRATOR')")
  @PutMapping("/{id}")
  public ProductResponseDto update(
      @Parameter(description = "Product ID") @PathVariable UUID id,
      @Valid @RequestBody ProductRequestDto productDto
  ) {
    return productService.updateOrThrow(id, productDto);
  }
}
