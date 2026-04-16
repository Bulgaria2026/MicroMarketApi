package com.noserbulgaria.micromarket.domain.order.dto;

import com.noserbulgaria.micromarket.domain.order.Order;
import com.noserbulgaria.micromarket.domain.order.OrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface OrderMapper {

  OrderResponseDto entityToDto(Order order);

  @Mapping(target = "productId", source = "product.id")
  OrderItemRequestDto orderItemToDto(OrderItem orderItem);
}
