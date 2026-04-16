package com.noserbulgaria.micromarket.domain.product;

import com.noserbulgaria.micromarket.domain.product.dto.ProductResponseDto;
import com.noserbulgaria.micromarket.domain.product.dto.ProductMapper;
import com.noserbulgaria.micromarket.domain.product.dto.ProductRequestDto;
import com.noserbulgaria.micromarket.domain.product.dto.ProductWithHistoryResponseDto;
import com.noserbulgaria.micromarket.exception.BadRequestApiException;
import com.noserbulgaria.micromarket.exception.ExceptionContexts;
import com.noserbulgaria.micromarket.exception.NotFoundApiException;
import com.noserbulgaria.micromarket.exception.UnauthorizedApiException;
import com.noserbulgaria.micromarket.security.user.CustomUserDetails;
import com.noserbulgaria.micromarket.security.user.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ProductService {

  private final ProductRepository productRepository;
  private final ProductMapper productMapper;
  private final ProductHistoryService productHistoryService;

  public ProductResponseDto create(ProductRequestDto dto) {
    return productMapper.toDto(productRepository.save(productMapper.toEntity(dto)));
  }

  @Transactional(readOnly = true)
  public ProductWithHistoryResponseDto getByIdForCurrentUser(UUID id, @Nullable CustomUserDetails userDetails) {
    Product product = findProductByIdOrThrow(id);
    validateDisabledProductAccess(product, userDetails);
    return productMapper.toDtoWithHistory(
        product,
        productHistoryService.findHistoryByProductId(product.getId())
    );
  }

  @Transactional(readOnly = true)
  public ProductResponseDto findByNameForCurrentUser(String name, @Nullable CustomUserDetails userDetails) {
    Product product = findProductByNameOrThrow(name);
    validateDisabledProductAccess(product, userDetails);
    return productMapper.toDto(product);
  }

  @Transactional(readOnly = true)
  public Page<ProductResponseDto> findAll(Pageable pageable) {
    List<ProductResponseDto> enabledProducts = productRepository.findAll()
        .stream()
        .filter(Product::isEnabled)
        .map(productMapper::toDto)
        .toList();

    int start = (int) pageable.getOffset();
    int end = Math.min(start + pageable.getPageSize(), enabledProducts.size());
    List<ProductResponseDto> pageContent = start >= enabledProducts.size()
        ? new ArrayList<>()
        : enabledProducts.subList(start, end);

    return new PageImpl<>(pageContent, pageable, enabledProducts.size());
  }

  public ProductResponseDto updateOrThrow(UUID id, ProductRequestDto dto) {
    Product product = findProductByIdOrThrow(id);
    productMapper.update(dto, product);
    return productMapper.toDto(productRepository.save(product));
  }

  public void deleteOrThrow(UUID id) {
    Product product = findProductByIdOrThrow(id);
    if (product.isEnabled()) {
      throw new BadRequestApiException(ExceptionContexts.of(product.getName()));
    }

    productRepository.deleteAuditHistoryByProductId(id);
    productRepository.delete(product);
  }

  @Transactional(readOnly = true)
  public List<ProductResponseDto> findAllDisabled() {
    log.debug("Finding all disabled products");
    return productRepository.findAll()
        .stream()
        .filter(product -> !product.isEnabled())
        .map(productMapper::toDto)
        .toList();
  }

  private Product findProductByIdOrThrow(UUID id) {
    return productRepository.findById(id)
        .orElseThrow(() -> new NotFoundApiException(ExceptionContexts.fromUuid(id)));
  }

  private Product findProductByNameOrThrow(String name) {
    return productRepository.findAll()
        .stream()
        .filter(product -> product.getName().equalsIgnoreCase(name))
        .findFirst()
        .orElseThrow(() -> new NotFoundApiException(ExceptionContexts.of(name)));
  }

  private void validateDisabledProductAccess(Product product, @Nullable CustomUserDetails userDetails) {
    if (!product.isEnabled() && (userDetails == null || userDetails.getRole() != Role.ADMINISTRATOR)) {
      throw new UnauthorizedApiException(ExceptionContexts.of(product.getName()));
    }
  }
}
