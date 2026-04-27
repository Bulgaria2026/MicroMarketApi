package com.noserbulgaria.micromarket.customer;

import com.noserbulgaria.micromarket.auth.user.UserMapper;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    uses = UserMapper.class,
    unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface ProfileMapper {

  @Mapping(target = "user", source = ".")
  ProfileResponse toDto(Profile profile);

  @BeanMapping(ignoreByDefault = true)
  @Mapping(target = "points", source = "points")
  void update(ProfileRequest request, @MappingTarget Profile profile);
}
