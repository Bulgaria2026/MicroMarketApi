package com.noserbulgaria.micromarket.auth.user.dto;

import com.noserbulgaria.micromarket.auth.user.User;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface UserMapper {

  UserResponseDto toDto(User user);
}
