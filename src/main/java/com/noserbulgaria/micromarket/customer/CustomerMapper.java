package com.noserbulgaria.micromarket.customer;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface CustomerMapper {

  @Mapping(target = "type", expression = "java(customer.isRegistered() ? CustomerType.PROFILE : CustomerType.GUEST)")
  @Mapping(target = "role", source = "profile.user.role")
  @Mapping(target = "status", source = "profile.user.status")
  @Mapping(target = "points", source = "profile.points")
  CustomerResponse toDto(Customer customer);
}
