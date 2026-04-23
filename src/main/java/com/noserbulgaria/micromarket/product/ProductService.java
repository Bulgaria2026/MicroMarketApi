package com.noserbulgaria.micromarket.product;

import com.noserbulgaria.micromarket.auth.user.CustomUserDetails;
import com.noserbulgaria.micromarket.auth.user.Role;
import com.noserbulgaria.micromarket.exception.NotFoundApiException;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class ProductService {

  private final ProductRepository productRepository;
  private final ProductMapper productMapper;

  public ProductResponse create(ProductRequest dto) {
    return productMapper.toDto(productRepository.save(productMapper.toEntity(dto)));
  }

  @Transactional(readOnly = true)
  public ProductResponse getByIdForCurrentUser(UUID id, @Nullable CustomUserDetails userDetails) {
    Product product = findProductByIdOrThrow(id);
    validateDisabledProductAccess(product, userDetails);
    return productMapper.toDto(product);
  }

  @Transactional(readOnly = true)
  public Page<ProductResponse> findAll(Specification<Product> spec, Pageable pageable) {
    return productRepository.findAll(spec, pageable).map(productMapper::toDto);
  }

  public ProductResponse updateOrThrow(UUID id, ProductRequest dto) {
    Product product = findProductByIdOrThrow(id);
    productMapper.update(dto, product);
    return productMapper.toDto(productRepository.save(product));
  }

  @Transactional(readOnly = true)
  public List<ProductHistoryResponse> getHistory(UUID id, @Nullable CustomUserDetails userDetails) {
    Product product = findProductByIdOrThrow(id);
    validateDisabledProductAccess(product, userDetails);
    return findRevisions(id);
  }

  private Product findProductByIdOrThrow(UUID id) {
    return productRepository.findById(id)
        .orElseThrow(() -> new NotFoundApiException("Product with id '%s' not found".formatted(id)));
  }

  private List<ProductHistoryResponse> findRevisions(UUID productId) {
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
    boolean isAdmin = userDetails != null && userDetails.getRole() == Role.ADMINISTRATOR;
    if (!isAdmin) {
      throw new NotFoundApiException("Product with id '%s' not found".formatted(product.getId()));
    }
  }
}
