package com.noserbulgaria.micromarket.domain.product;

import com.noserbulgaria.micromarket.domain.product.dto.ProductDto;
import com.noserbulgaria.micromarket.domain.product.dto.ProductWithHistoryDto;
import com.noserbulgaria.micromarket.domain.product.dto.ProductWriteDto;
import com.noserbulgaria.micromarket.security.user.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/product")
@Tag(name = "Product", description = "Product management endpoints")
public class ProductController {

  private final ProductService productService;

  @Operation(summary = "Get all products with pagination")
  @GetMapping
  public Page<ProductDto> getAll(
      @Parameter(description = "Page number (0-indexed)")
      @RequestParam(defaultValue = "0") int page,
      @Parameter(description = "Page size")
      @RequestParam(defaultValue = "10") int size,
      @Parameter(description = "Sort field")
      @RequestParam(defaultValue = "createdAt") String sort
  ) {
    return productService.findAll(
        PageRequest.of(page, size, Sort.by(sort).descending())
    );
  }

  @Operation(summary = "Get product by ID")
  @ApiResponse(responseCode = "200", description = "Product found")
  @ApiResponse(responseCode = "401", description = "Authentication required to access a disabled product")
  @ApiResponse(responseCode = "404", description = "Product not found")
  @GetMapping("/{id}")
  public ProductWithHistoryDto getById(
      @Parameter(description = "Product id")
      @PathVariable
      UUID id,
      @AuthenticationPrincipal CustomUserDetails userDetails
  ) {
    return productService.getByIdForCurrentUser(id, userDetails);
  }

  @Operation(summary = "Search product by name")
  @ApiResponse(responseCode = "200", description = "Product found")
  @ApiResponse(responseCode = "401", description = "Authentication required to access a disabled product")
  @ApiResponse(responseCode = "404", description = "Product not found")
  @GetMapping("/search/by-name")
  public ProductDto searchByName(
      @Parameter(description = "Product name")
      @RequestParam String name,
      @AuthenticationPrincipal CustomUserDetails userDetails
  ) {
    return productService.findByNameForCurrentUser(name, userDetails);
  }

  //region

  @Operation(summary = "Create a new product")
  @ApiResponse(responseCode = "201", description = "Product created successfully")
  @PreAuthorize("hasRole('ADMINISTRATOR')")
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ProductDto create(@Valid @RequestBody ProductWriteDto productDto) {
    return productService.create(productDto);
  }

  @Operation(summary = "Get all disabled products")
  @PreAuthorize("hasRole('ADMINISTRATOR')")
  @GetMapping("/disabled")
  public List<ProductDto> getAllDisabled() {
    return productService.findAllDisabled();
  }

  @Operation(summary = "Update a product")
  @ApiResponse(responseCode = "200", description = "Product updated successfully")
  @ApiResponse(responseCode = "404", description = "Product not found")
  @PreAuthorize("hasRole('ADMINISTRATOR')")
  @PutMapping("/{id}")
  public ProductDto update(
      @Parameter(description = "Product ID")
      @PathVariable UUID id,
      @Valid @RequestBody ProductWriteDto productDto
  ) {
    return productService.updateOrThrow(id, productDto);
  }

  @Operation(summary = "Delete a product")
  @ApiResponse(responseCode = "204", description = "Product deleted successfully")
  @ApiResponse(responseCode = "404", description = "Product not found")
  @PreAuthorize("hasRole('ADMINISTRATOR')")
  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable UUID id) {
    productService.deleteOrThrow(id);
  }
}
