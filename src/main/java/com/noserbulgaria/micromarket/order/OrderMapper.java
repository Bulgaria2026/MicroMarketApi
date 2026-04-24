package com.noserbulgaria.micromarket.order;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface OrderMapper {

  @Mapping(target = "customerId", source = "customer.id")
  @Mapping(target = "couponId", source = "appliedCoupon.id")
  OrderResponse toDto(Order order);

  @Mapping(target = "productId", source = "product.id")
  OrderItemResponse toItemDto(OrderItem orderItem);
}
