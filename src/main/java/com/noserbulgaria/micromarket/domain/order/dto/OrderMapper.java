package com.noserbulgaria.micromarket.domain.order.dto;

import com.noserbulgaria.micromarket.domain.order.Order;
import com.noserbulgaria.micromarket.domain.order.OrderItem;
import com.noserbulgaria.micromarket.generic.ExtendedMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface OrderMapper extends ExtendedMapper<Order, OrderResponseDto> {

  @Override
  OrderResponseDto entityToDto(Order order);

  @Override
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "orderItems", ignore = true)
  Order dtoToEntity(OrderResponseDto dto);

  @Mapping(target = "productId", source = "product.id")
  OrderItemRequestDto orderItemToDto(OrderItem orderItem);
}
