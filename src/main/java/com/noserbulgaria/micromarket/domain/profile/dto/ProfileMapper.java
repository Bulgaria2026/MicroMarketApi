package com.noserbulgaria.micromarket.domain.profile.dto;

import com.noserbulgaria.micromarket.domain.profile.Profile;
import com.noserbulgaria.micromarket.security.user.dto.UserMapper;
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
