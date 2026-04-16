package com.noserbulgaria.micromarket.domain.product.dto;

import com.noserbulgaria.micromarket.domain.product.Product;
import org.mapstruct.*;

import java.time.Instant;

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface ProductMapper {

  ProductResponseDto toDto(Product product);

  ProductHistoryResponseDto toHistoryDto(Product product, long revisionNumber, Instant revisionTimestamp, String revisionType);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  Product toEntity(ProductRequestDto request);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  void update(ProductRequestDto request, @MappingTarget Product product);
}
