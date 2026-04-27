package com.noserbulgaria.micromarket.auth.user;

import com.noserbulgaria.micromarket.customer.Profile;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface UserMapper {

  @Mapping(target = "id", source = "user.id")
  @Mapping(target = "createdAt", source = "user.createdAt")
  @Mapping(target = "updatedAt", source = "user.updatedAt")
  @Mapping(target = "email", source = "customer.email")
  @Mapping(target = "role", source = "user.role")
  @Mapping(target = "status", source = "user.status")
  UserResponse toDto(Profile profile);
}
