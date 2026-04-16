package com.noserbulgaria.micromarket.domain.order.dto;

import com.noserbulgaria.micromarket.domain.order.Order;
import com.noserbulgaria.micromarket.domain.order.OrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface OrderMapper {

  OrderResponseDTO entityToDto(Order order);

  @Mapping(target = "productId", source = "product.id")
  OrderItemRequestDTO orderItemToDto(OrderItem orderItem);
}
