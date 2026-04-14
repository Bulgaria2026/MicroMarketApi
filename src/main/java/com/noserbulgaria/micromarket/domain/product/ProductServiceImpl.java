package com.noserbulgaria.micromarket.domain.product;

import com.noserbulgaria.micromarket.domain.product.dto.ProductDto;
import com.noserbulgaria.micromarket.domain.product.dto.ProductMapper;
import com.noserbulgaria.micromarket.domain.product.dto.ProductWithHistoryDto;
import com.noserbulgaria.micromarket.domain.product.dto.ProductWriteDto;
import com.noserbulgaria.micromarket.exception.BadRequestException;
import com.noserbulgaria.micromarket.exception.EntityNotFoundException;
import com.noserbulgaria.micromarket.generic.ExtendedServiceImpl;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
  private final EntityManager entityManager;

  public ProductServiceImpl(
      ProductRepository productRepository,
      ProductMapper productMapper,
      ProductHistoryService productHistoryService,
      EntityManager entityManager) {
    super(productRepository, productMapper);
    this.productRepository = productRepository;
    this.productHistoryService = productHistoryService;
    this.entityManager = entityManager;
  }

  @Override
  public ProductDto create(ProductWriteDto dto) {
    return toProductDto(productRepository.save(super.mapper.toEntity(dto)));
  }

  @Override
  public ProductDto create(ProductDto dto) {
    return toProductDto(productRepository.save(super.mapper.dtoToEntity(dto)));
  }

  @Override
  @Transactional(readOnly = true)
  public ProductWithHistoryDto getByIdOrThrow(UUID id) {
    Product product = findProductByIdOrThrow(id);
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
        productHistoryService.findHistoryByProductId(id)
    );
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<ProductDto> findById(UUID id) {
    return productRepository.findById(id)
        .map(this::toProductDto);
  }

  @Override
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
    return productRepository.findAll(pageable)
        .map(this::toProductDto);
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
  public ProductDto updateOrThrow(UUID id, ProductWriteDto dto) {
    Product product = findProductByIdOrThrow(id);
    super.mapper.updateProductFromWriteDto(dto, product);
    return toProductDto(productRepository.save(product));
  }

  @Override
  public boolean delete(UUID id) {
    return productRepository.findById(id)
        .map(product -> {
          if (Boolean.TRUE.equals(product.getEnabled())) {
            throw new BadRequestException("Product must be disabled before it can be deleted");
          }

          deleteAuditHistory(id);
          productRepository.delete(product);
          return true;
        })
        .orElse(false);
  }

  @Override
  public void deleteOrThrow(UUID id) {
    Product product = findProductByIdOrThrow(id);
    if (Boolean.TRUE.equals(product.getEnabled())) {
      throw new BadRequestException("Product must be disabled before it can be deleted");
    }

    deleteAuditHistory(id);
    productRepository.delete(product);
  }

  @Override
  @Transactional(readOnly = true)
  public ProductDto findByNameOrThrow(String name) {
    log.debug("Finding product by name with exception on miss: {}", name);
    return productRepository.findAll()
        .stream()
        .filter(product -> product.getName().equalsIgnoreCase(name))
        .findFirst()
        .map(this::toProductDto)
        .orElseThrow(() -> new EntityNotFoundException("Product not found with name: " + name));
  }

  @Override
  @Transactional(readOnly = true)
  public List<ProductDto> findAllEnabled() {
    log.debug("Finding all enabled products");
    return productRepository.findAll()
        .stream()
        .filter(Product::getEnabled)
        .map(this::toProductDto)
        .collect(Collectors.toList());
  }

  private ProductDto toProductDto(Product product) {
    return super.mapper.entityToDto(product);
  }

  private Product findProductByIdOrThrow(UUID id) {
    return productRepository.findById(id)
        .orElseThrow(() -> new EntityNotFoundException("Product not found with id: " + id));
  }

  private void deleteAuditHistory(UUID productId) {
    entityManager.createNativeQuery("DELETE FROM product_aud WHERE id = :productId")
        .setParameter("productId", productId)
        .executeUpdate();
  }
}
