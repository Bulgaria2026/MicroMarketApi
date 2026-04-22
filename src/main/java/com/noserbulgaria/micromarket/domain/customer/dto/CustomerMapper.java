package com.noserbulgaria.micromarket.domain.customer.dto;

import com.noserbulgaria.micromarket.domain.customer.Customer;
import com.noserbulgaria.micromarket.domain.customer.CustomerType;
import com.noserbulgaria.micromarket.domain.guest.Guest;
import com.noserbulgaria.micromarket.domain.profile.Profile;
import com.noserbulgaria.micromarket.exception.BadRequestApiException;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface CustomerMapper {

  default CustomerResponseDto toDto(Customer customer) {
    return switch (customer) {
      case Guest guest -> toDto(guest);
      case Profile profile -> toDto(profile);
      default -> throw new BadRequestApiException(
          "Unsupported customer type '%s'".formatted(customer.getClass().getSimpleName()));
    };
  }

  @Mapping(target = "type", expression = "java(CustomerType.GUEST)")
  @Mapping(target = "role", ignore = true)
  @Mapping(target = "status", ignore = true)
  @Mapping(target = "points", ignore = true)
  CustomerResponseDto toDto(Guest guest);

  @Mapping(target = "type", expression = "java(CustomerType.PROFILE)")
  @Mapping(target = "email", source = "user.email")
  @Mapping(target = "role", source = "user.role")
  @Mapping(target = "status", source = "user.status")
  @Mapping(target = "points", source = "points")
  CustomerResponseDto toDto(Profile profile);
}
