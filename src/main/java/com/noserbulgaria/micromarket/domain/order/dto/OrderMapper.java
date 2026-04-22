package com.noserbulgaria.micromarket.domain.order.dto;

import com.noserbulgaria.micromarket.domain.order.Order;
import com.noserbulgaria.micromarket.domain.order.OrderItem;
import com.noserbulgaria.micromarket.domain.product.dto.ProductMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = ReportingPolicy.ERROR,
    uses = {ProductMapper.class}
)
public interface OrderMapper {

  OrderResponseDto toDto(Order order);

  @Mapping(target = "productId", source = "product.id")
  OrderItemResponseDto toItemDto(OrderItem orderItem);

  OrderDetailResponseDto toDetailDto(Order order);

  OrderItemDetailResponseDto toItemDetailDto(OrderItem orderItem);
}
