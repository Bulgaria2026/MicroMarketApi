package com.noserbulgaria.micromarket.domain.product;

import com.noserbulgaria.micromarket.domain.product.dto.ProductDto;
import com.noserbulgaria.micromarket.domain.product.dto.ProductWithHistoryDto;
import com.noserbulgaria.micromarket.domain.product.dto.ProductWriteDto;
import com.noserbulgaria.micromarket.generic.ExtendedService;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for Product entity operations.
 * Extends the generic ExtendedService to provide Product-specific functionality.
 */
public interface ProductService extends ExtendedService<Product, ProductDto> {

  ProductDto create(ProductWriteDto productWriteDto);

  /**
   * Finds a product by its ID or throws if it does not exist.
   *
   * @param id the product ID
   * @return the product DTO
   */
  ProductWithHistoryDto getByIdOrThrow(UUID id);

  /**
   * Finds a product by its name or throws if it does not exist.
   *
   * @param name the product name
   * @return the product DTO
   */
  ProductDto findByNameOrThrow(String name);

  /**
   * Updates a product or throws if it does not exist.
   *
   * @param id the product ID
   * @param productWriteDto the new product state
   * @return the updated product DTO
   */
  ProductDto updateOrThrow(UUID id, ProductWriteDto productWriteDto);

  /**
   * Deletes a product or throws if it does not exist.
   *
   * @param id the product ID
   */
  void deleteOrThrow(UUID id);

  /**
   * Finds all enabled products.
   *
   * @return a list of enabled product DTOs
   */
  List<ProductDto> findAllEnabled();
}
