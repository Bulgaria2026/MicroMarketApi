package com.noserbulgaria.micromarket.domain.product;

import com.noserbulgaria.micromarket.domain.product.dto.*;
import com.noserbulgaria.micromarket.exception.ForbiddenApiException;
import com.noserbulgaria.micromarket.exception.NotFoundApiException;
import com.noserbulgaria.micromarket.exception.UnauthorizedApiException;
import com.noserbulgaria.micromarket.security.user.CustomUserDetails;
import com.noserbulgaria.micromarket.security.user.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ProductService {

  private final ProductRepository productRepository;
  private final ProductMapper productMapper;

  public ProductResponseDto create(ProductRequestDto dto) {
    return productMapper.toDto(productRepository.save(productMapper.toEntity(dto)));
  }

  @Transactional(readOnly = true)
  public ProductWithHistoryResponseDto getByIdForCurrentUser(UUID id, @Nullable CustomUserDetails userDetails) {
    Product product = findProductByIdOrThrow(id);
    validateDisabledProductAccess(product, userDetails);
    return productMapper.toDtoWithHistory(product, findRevisions(product.getId()));
  }

  @Transactional(readOnly = true)
  public ProductResponseDto findByNameForCurrentUser(String name, @Nullable CustomUserDetails userDetails) {
    Product product = findProductByNameOrThrow(name);
    validateDisabledProductAccess(product, userDetails);
    return productMapper.toDto(product);
  }

  @Transactional(readOnly = true)
  public Page<ProductResponseDto> findAll(Pageable pageable) {
    return productRepository.findAllByEnabledTrue(pageable).map(productMapper::toDto);
  }

  public ProductResponseDto updateOrThrow(UUID id, ProductRequestDto dto) {
    Product product = findProductByIdOrThrow(id);
    productMapper.update(dto, product);
    return productMapper.toDto(productRepository.save(product));
  }

  @Transactional(readOnly = true)
  public List<ProductResponseDto> findAllDisabled() {
    return productRepository.findAllByEnabledFalse().stream()
        .map(productMapper::toDto)
        .toList();
  }

  private Product findProductByIdOrThrow(UUID id) {
    return productRepository.findById(id)
        .orElseThrow(() -> new NotFoundApiException("Product with id '%s' not found".formatted(id)));
  }

  private Product findProductByNameOrThrow(String name) {
    return productRepository.findByNameIgnoreCase(name)
        .orElseThrow(() -> new NotFoundApiException("Product with name '%s' not found".formatted(name)));
  }

  private List<ProductHistoryResponseDto> findRevisions(UUID productId) {
    return productRepository.findRevisions(productId).stream()
        .map(revision -> productMapper.toHistoryDto(
            revision.getEntity(),
            revision.getRequiredRevisionNumber().longValue(),
            revision.getMetadata().getRequiredRevisionInstant(),
            revision.getMetadata().getRevisionType().name()
        ))
        .toList();
  }

  private void validateDisabledProductAccess(Product product, @Nullable CustomUserDetails userDetails) {
    if (product.isEnabled()) {
      return;
    }
    if (userDetails == null) {
      throw new UnauthorizedApiException(
          "Authentication is required to access disabled product '%s'".formatted(product.getName()));
    }
    if (userDetails.getRole() != Role.ADMINISTRATOR) {
      throw new ForbiddenApiException(
          "Access to disabled product '%s' requires administrator role".formatted(product.getName()));
    }
  }
}
