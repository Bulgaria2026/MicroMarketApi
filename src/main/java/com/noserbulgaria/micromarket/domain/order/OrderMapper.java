package com.noserbulgaria.micromarket.domain.order;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface OrderMapper {

    OrderDto entityToDto(Order order);

    OrderProductDto orderProductToDto(OrderProduct orderProduct);
}
