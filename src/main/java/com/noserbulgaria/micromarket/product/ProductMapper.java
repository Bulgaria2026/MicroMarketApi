package com.noserbulgaria.micromarket.product;

import org.mapstruct.*;

import java.time.Instant;

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface ProductMapper {

  ProductResponse toDto(Product product);

  ProductHistoryResponse toHistoryDto(Product product, long revisionNumber, Instant revisionTimestamp, String revisionType);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  Product toEntity(ProductRequest request);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  void update(ProductRequest request, @MappingTarget Product product);
}
