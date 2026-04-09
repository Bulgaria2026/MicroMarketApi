package com.noserbulgaria.micromarket.domain.product.dto;

import com.noserbulgaria.micromarket.domain.product.Product;
import com.noserbulgaria.micromarket.generic.ExtendedMapper;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ProductMapper extends ExtendedMapper<Product, ProductDto> {
}
