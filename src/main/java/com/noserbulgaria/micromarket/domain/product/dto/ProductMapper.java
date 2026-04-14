package com.noserbulgaria.micromarket.domain.product.dto;

import com.noserbulgaria.micromarket.domain.product.Product;
import com.noserbulgaria.micromarket.generic.ExtendedMapper;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ProductMapper extends ExtendedMapper<Product, ProductDto> {

  @Override
  default ProductDto entityToDto(Product entity) {
    if (entity == null) {
      return null;
    }

    return new ProductDto(
        entity.getId(),
        entity.getCreatedAt(),
        entity.getUpdatedAt(),
        entity.getName(),
        entity.getDescription(),
        entity.getPrice(),
        entity.getDiscount(),
        entity.getEnabled(),
        entity.getAmount()
    );
  }

  @Override
  default Product dtoToEntity(ProductDto dto) {
    if (dto == null) {
      return null;
    }

    Product product = new Product();
    product.setName(dto.name());
    product.setDescription(dto.description());
    product.setPrice(dto.price());
    product.setDiscount(dto.discount());
    product.setEnabled(dto.enabled());
    product.setAmount(dto.amount());
    return product;
  }

  @Override
  default Set<ProductDto> entitiesToDtoSet(Set<Product> entities) {
    return entities.stream()
        .map(this::entityToDto)
        .collect(Collectors.toSet());
  }

  @Override
  default List<ProductDto> entitiesToDtoList(List<Product> entities) {
    return entities.stream()
        .map(this::entityToDto)
        .collect(Collectors.toList());
  }

  @Override
  default Set<Product> dtoSetToEntity(Set<ProductDto> dtoSet) {
    return dtoSet.stream()
        .map(this::dtoToEntity)
        .collect(Collectors.toSet());
  }
}
