package com.noserbulgaria.micromarket.domain.product;

import com.noserbulgaria.micromarket.domain.product.dto.ProductDto;
import com.noserbulgaria.micromarket.domain.product.dto.ProductWithHistoryDto;
import com.noserbulgaria.micromarket.domain.product.dto.ProductWriteDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/product")
@Tag(name = "Product", description = "Product management endpoints")
public class ProductController {

  private final ProductService productService;

  //region Public Endpoints
  @Operation(summary = "Get all products with pagination")
  @SecurityRequirements
  @GetMapping("/public")
  public ResponseEntity<Page<ProductDto>> getAll(
      @Parameter(description = "Page number (0-indexed)")
      @RequestParam(defaultValue = "0") int page,
      @Parameter(description = "Page size")
      @RequestParam(defaultValue = "10") int size,
      @Parameter(description = "Sort field")
      @RequestParam(defaultValue = "createdAt") String sort) {
    Page<ProductDto> products = productService.findAll(
        PageRequest.of(page, size, Sort.by(sort).descending())
    );
    return ResponseEntity.ok(products);
  }

  @Operation(summary = "Get product by ID")
  @ApiResponse(responseCode = "200", description = "Product found")
  @ApiResponse(responseCode = "404", description = "Product not found")
  @SecurityRequirements
  @GetMapping("/public/{id}")
  public ResponseEntity<ProductWithHistoryDto> getById(
      @Parameter(description = "Product ID")
      @PathVariable UUID id) {
    return ResponseEntity.ok(productService.getByIdOrThrow(id));
  }

  @Operation(summary = "Get all enabled products")
  @SecurityRequirements
  @GetMapping("/public/enabled")
  public ResponseEntity<List<ProductDto>> getAllEnabled() {
    List<ProductDto> products = productService.findAllEnabled();
    return ResponseEntity.ok(products);
  }

  @Operation(summary = "Search product by name")
  @ApiResponse(responseCode = "200", description = "Product found")
  @ApiResponse(responseCode = "404", description = "Product not found")
  @SecurityRequirements
  @GetMapping("/public/search/by-name")
  public ResponseEntity<ProductDto> searchByName(
      @Parameter(description = "Product name")
      @RequestParam String name) {
    return ResponseEntity.ok(productService.findByNameOrThrow(name));
  }
  //endregion

  //region Admin-only Endpoints
  @Operation(summary = "Create a new product")
  @ApiResponse(responseCode = "201", description = "Product created successfully")
  @PreAuthorize("hasRole('ADMINISTRATOR')")
  @PostMapping
  public ResponseEntity<ProductDto> create(@Valid @RequestBody ProductWriteDto productDto) {
    ProductDto created = productService.create(productDto);
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
  }

  @Operation(summary = "Update a product")
  @ApiResponse(responseCode = "200", description = "Product updated successfully")
  @ApiResponse(responseCode = "404", description = "Product not found")
  @PreAuthorize("hasRole('ADMINISTRATOR')")
  @PutMapping("/{id}")
  public ResponseEntity<ProductDto> update(
      @Parameter(description = "Product ID")
      @PathVariable UUID id,
      @Valid @RequestBody ProductWriteDto productDto) {
    return ResponseEntity.ok(productService.updateOrThrow(id, productDto));
  }

  @Operation(summary = "Delete a product")
  @ApiResponse(responseCode = "204", description = "Product deleted successfully")
  @ApiResponse(responseCode = "404", description = "Product not found")
  @PreAuthorize("hasRole('ADMINISTRATOR')")
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(
      @Parameter(description = "Product ID")
      @PathVariable UUID id) {
    productService.deleteOrThrow(id);
    return ResponseEntity.noContent().build();
  }
  //endregion
}
