package com.noserbulgaria.micromarket.couponoffer;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface CouponOfferMapper {

  CouponOfferResponse toDto(CouponOffer couponOffer);
}
