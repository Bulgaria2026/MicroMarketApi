package com.noserbulgaria.micromarket.coupon;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface CouponMapper {

  @Mapping(target = "userId", source = "user.id")
  CouponResponse toDto(Coupon coupon);
}
