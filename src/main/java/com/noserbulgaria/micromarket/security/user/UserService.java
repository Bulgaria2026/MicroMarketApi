package com.noserbulgaria.micromarket.security.user;

import com.noserbulgaria.micromarket.exception.ConflictApiException;
import com.noserbulgaria.micromarket.exception.NotFoundApiException;
import com.noserbulgaria.micromarket.security.auth.refresh.RefreshTokenService;
import com.noserbulgaria.micromarket.security.user.dto.UserMapper;
import com.noserbulgaria.micromarket.security.user.dto.UserPatchRequestDto;
import com.noserbulgaria.micromarket.security.user.dto.UserResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;
  private final UserMapper userMapper;
  private final RefreshTokenService refreshTokenService;

  @Transactional(readOnly = true)
  public Page<UserResponseDto> findAll(UserFilter filter, Pageable pageable) {
    return userRepository.findAll(UserSpecification.withFilter(filter), pageable)
        .map(userMapper::toDto);
  }

  @Transactional(readOnly = true)
  public UserResponseDto getByIdOrThrow(UUID id) {
    return userMapper.toDto(findUserByIdOrThrow(id));
  }

  @Transactional
  public UserResponseDto patchUserOrThrow(UUID id, UserPatchRequestDto dto) {
    User user = findUserByIdOrThrow(id);

    if (dto.email() != null && !dto.email().equals(user.getEmail())) {
      if (userRepository.existsByEmail(dto.email())) {
        throw new ConflictApiException("User with email '%s' already exists".formatted(dto.email()));
      }
      user.setEmail(dto.email());
    }

    if (dto.role() != null) {
      user.setRole(dto.role());
    }

    if (dto.status() != null) {
      revokeAll(dto.status(), user);
      user.setStatus(dto.status());
    }

    return userMapper.toDto(userRepository.save(user));
  }

  private User findUserByIdOrThrow(UUID id) {
    return userRepository.findById(id)
        .orElseThrow(() -> new NotFoundApiException("User with id '%s' not found".formatted(id)));
  }

  private void revokeAll(AccountStatus status, User user) {
    if (status == AccountStatus.INACTIVE && user.getStatus() != AccountStatus.INACTIVE) {
      refreshTokenService.revokeAllForUser(user.getId());
    }
  }
}
