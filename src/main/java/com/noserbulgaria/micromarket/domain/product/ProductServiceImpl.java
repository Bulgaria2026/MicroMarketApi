package com.noserbulgaria.micromarket.domain.product;

import com.noserbulgaria.micromarket.domain.product.dto.ProductDto;
import com.noserbulgaria.micromarket.domain.product.dto.ProductMapper;
import com.noserbulgaria.micromarket.domain.product.dto.ProductRequestDto;
import com.noserbulgaria.micromarket.domain.product.dto.ProductWithHistoryDto;
import com.noserbulgaria.micromarket.exception.BadRequestException;
import com.noserbulgaria.micromarket.exception.EntityNotFoundException;
import com.noserbulgaria.micromarket.exception.UnauthorizedException;
import com.noserbulgaria.micromarket.generic.ExtendedServiceImpl;
import com.noserbulgaria.micromarket.security.user.CustomUserDetails;
import com.noserbulgaria.micromarket.security.user.Role;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class ProductServiceImpl extends ExtendedServiceImpl<Product, ProductDto, ProductRepository, ProductMapper>
    implements ProductService {

  private final ProductRepository productRepository;
  private final ProductHistoryService productHistoryService;

  public ProductServiceImpl(
      ProductRepository productRepository,
      ProductMapper productMapper,
      ProductHistoryService productHistoryService
  ) {
    super(productRepository, productMapper);
    this.productRepository = productRepository;
    this.productHistoryService = productHistoryService;
  }

  @Override
  public ProductDto create(ProductRequestDto dto) {
    return toProductDto(productRepository.save(super.mapper.toEntity(dto)));
  }

  @Override
  public ProductDto create(ProductDto dto) {
    return toProductDto(productRepository.save(super.mapper.dtoToEntity(dto)));
  }

  @Override
  @Transactional(readOnly = true)
  public ProductWithHistoryDto getByIdForCurrentUser(UUID id, @Nullable CustomUserDetails userDetails) {
    Product product = findProductByIdOrThrow(id);
    validateDisabledProductAccess(product, userDetails);
    return toProductWithHistoryDto(product);
  }

  @Override
  @Transactional(readOnly = true)
  public ProductDto findByNameForCurrentUser(String name, @Nullable CustomUserDetails userDetails) {
    Product product = findProductByNameOrThrow(name);
    validateDisabledProductAccess(product, userDetails);
    return toProductDto(product);
  }

  @Transactional(readOnly = true)
  public Optional<ProductDto> findById(UUID id) {
    return productRepository.findById(id)
        .map(this::toProductDto);
  }

  @Transactional(readOnly = true)
  public List<ProductDto> findAll() {
    return productRepository.findAll()
        .stream()
        .map(this::toProductDto)
        .collect(Collectors.toList());
  }

  @Override
  @Transactional(readOnly = true)
  public Page<ProductDto> findAll(Pageable pageable) {
    List<ProductDto> enabledProducts = productRepository.findAll()
        .stream()
        .filter(Product::getEnabled)
        .map(this::toProductDto)
        .toList();

    int start = (int) pageable.getOffset();
    int end = Math.min(start + pageable.getPageSize(), enabledProducts.size());
    List<ProductDto> pageContent = start >= enabledProducts.size()
        ? new ArrayList<>()
        : enabledProducts.subList(start, end);

    return new PageImpl<>(pageContent, pageable, enabledProducts.size());
  }

  @Override
  public Optional<ProductDto> update(UUID id, ProductDto dto) {
    return productRepository.findById(id)
        .map(existing -> {
          super.mapper.updateProductFromDto(dto, existing);
          return toProductDto(productRepository.save(existing));
        });
  }

  @Override
  public ProductDto updateOrThrow(UUID id, ProductRequestDto dto) {
    Product product = findProductByIdOrThrow(id);
    super.mapper.updateProductFromWriteDto(dto, product);
    return toProductDto(productRepository.save(product));
  }

  @Override
  public boolean delete(UUID id) {
    return productRepository.findById(id)
        .map(product -> {
          if (product.getEnabled()) {
            throw new BadRequestException("Product must be disabled before it can be deleted");
          }

          productRepository.deleteAuditHistoryByProductId(id);
          productRepository.delete(product);
          return true;
        })
        .orElse(false);
  }

  @Override
  public void deleteOrThrow(UUID id) {
    Product product = findProductByIdOrThrow(id);
    if (product.getEnabled()) {
      throw new BadRequestException("Product must be disabled before it can be deleted");
    }

    productRepository.deleteAuditHistoryByProductId(id);
    productRepository.delete(product);
  }

  @Override
  @Transactional(readOnly = true)
  public ProductDto findByNameOrThrow(String name, boolean includeDisabled) {
    log.debug("Finding product by name with exception on miss: {}", name);
    Product product = findProductByNameOrThrow(name);
    if (!includeDisabled && !product.getEnabled()) {
      throw new EntityNotFoundException("Product not found with name: " + name);
    }

    return toProductDto(product);
  }

  @Override
  @Transactional(readOnly = true)
  public List<ProductDto> findAllDisabled() {
    log.debug("Finding all disabled products");
    return productRepository.findAll()
        .stream()
        .filter(product -> !product.getEnabled())
        .map(this::toProductDto)
        .collect(Collectors.toList());
  }

  private ProductDto toProductDto(Product product) {
    return super.mapper.entityToDto(product);
  }

  private ProductWithHistoryDto toProductWithHistoryDto(Product product) {
    ProductDto summary = toProductDto(product);

    return new ProductWithHistoryDto(
        summary.id(),
        summary.createdAt(),
        summary.updatedAt(),
        summary.name(),
        summary.description(),
        summary.price(),
        summary.discount(),
        summary.enabled(),
        summary.amount(),
        productHistoryService.findHistoryByProductId(product.getId())
    );
  }

  private Product findProductByIdOrThrow(UUID id) {
    return productRepository.findById(id)
        .orElseThrow(() -> new EntityNotFoundException("Product not found with id: " + id));
  }

  private Product findProductByNameOrThrow(String name) {
    return productRepository.findAll()
        .stream()
        .filter(product -> product.getName().equalsIgnoreCase(name))
        .findFirst()
        .orElseThrow(() -> new EntityNotFoundException("Product not found with name: " + name));
  }

  private void validateDisabledProductAccess(Product product, @Nullable CustomUserDetails userDetails) {
    if (!product.getEnabled()) {
      {
        if (userDetails == null || userDetails.getRole() != (Role.ADMINISTRATOR)) {
          throw new UnauthorizedException("Authentication required to access disabled products");
        }
      }
    }
  }
}
