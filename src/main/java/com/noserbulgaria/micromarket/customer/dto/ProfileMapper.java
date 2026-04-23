package com.noserbulgaria.micromarket.customer.dto;

import com.noserbulgaria.micromarket.auth.user.dto.UserMapper;
import com.noserbulgaria.micromarket.customer.Profile;
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

  ProfileResponseDto toDto(Profile profile);

  @BeanMapping(ignoreByDefault = true)
  @Mapping(target = "points", source = "points")
  void update(ProfileRequestDto request, @MappingTarget Profile profile);
}
