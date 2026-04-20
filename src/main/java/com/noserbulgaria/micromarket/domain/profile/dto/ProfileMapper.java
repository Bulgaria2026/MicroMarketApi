package com.noserbulgaria.micromarket.domain.profile.dto;

import com.noserbulgaria.micromarket.domain.profile.Profile;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface ProfileMapper {

  @Mapping(target = "userId", source = "user.id")
  ProfileResponseDto toDto(Profile profile);
}
