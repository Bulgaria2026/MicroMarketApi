package com.noserbulgaria.micromarket.domain.order;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import com.noserbulgaria.micromarket.domain.order.dto.OrderDto;
import com.noserbulgaria.micromarket.domain.order.dto.OrderItemDto;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface OrderMapper {

    OrderDto entityToDto(Order order);

    OrderItemDto orderItemToDto(OrderItem orderItem);
}
