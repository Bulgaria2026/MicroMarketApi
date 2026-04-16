package com.noserbulgaria.micromarket.domain.product;

import com.noserbulgaria.micromarket.domain.product.dto.ProductDto;
import com.noserbulgaria.micromarket.domain.product.dto.ProductWithHistoryDto;
import com.noserbulgaria.micromarket.domain.product.dto.ProductRequestDto;
import com.noserbulgaria.micromarket.generic.ExtendedService;
import com.noserbulgaria.micromarket.security.user.CustomUserDetails;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for Product entity operations. Extends the generic ExtendedService to provide Product-specific
 * functionality.
 */
@NullMarked
public interface ProductService extends ExtendedService<Product, ProductDto> {

  ProductDto create(ProductRequestDto productWriteDto);

  /**
   * Returns the public or admin representation of a product for the current caller.
   *
   * Anonymous and non-admin callers receive the public product DTO and cannot see disabled products. Administrators
   * receive the full DTO including history and disabled products.
   *
   * @param id          the product ID
   * @param userDetails the current userDetails, or {@code null} for anonymous requests
   * @return the DTO visible to the current caller
   */
  ProductWithHistoryDto getByIdForCurrentUser(UUID id, @Nullable CustomUserDetails userDetails);

  /**
   * Returns the product visible to the current caller when searching by name.
   *
   * Anonymous and non-admin callers can only see enabled products. Administrators can also see
   * disabled products.
   *
   * @param name        the product name
   * @param userDetails the current userDetails, or {@code null} for anonymous requests
   * @return the product DTO visible to the current caller
   */
  ProductDto findByNameForCurrentUser(String name, @Nullable CustomUserDetails userDetails);

  /**
   * Finds a product by its name or throws if it does not exist.
   *
   * @param name            the product name
   * @param includeDisabled whether disabled products should be considered visible
   * @return the product DTO
   */
  ProductDto findByNameOrThrow(String name, boolean includeDisabled);

  /**
   * Updates a product or throws if it does not exist.
   *
   * @param id              the product ID
   * @param productWriteDto the new product state
   * @return the updated product DTO
   */
  ProductDto updateOrThrow(UUID id, ProductRequestDto productWriteDto);

  /**
   * Deletes a product or throws if it does not exist.
   *
   * @param id the product ID
   */
  void deleteOrThrow(UUID id);

  /**
   * Finds all disabled products.
   *
   * @return a list of disabled product DTOs
   */
  List<ProductDto> findAllDisabled();
}
