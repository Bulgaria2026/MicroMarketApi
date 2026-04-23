package com.noserbulgaria.micromarket.auth.user;

import com.noserbulgaria.micromarket.auth.refresh.RefreshTokenService;
import com.noserbulgaria.micromarket.exception.ConflictApiException;
import com.noserbulgaria.micromarket.exception.NotFoundApiException;
import lombok.RequiredArgsConstructor;
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
  public UserResponse getByIdOrThrow(UUID id) {
    return userMapper.toDto(findUserByIdOrThrow(id));
  }

  @Transactional
  public UserResponse patchUserOrThrow(UUID id, UserPatchRequest dto) {
    User user = findUserByIdOrThrow(id);

    if (dto.email() != null && !dto.email().equals(user.getEmail())) {
      if (userRepository.existsByEmail(dto.email())) {
        throw new ConflictApiException("User with email '%s' already exists".formatted(dto.email()));
      }
      user.setEmail(dto.email());
    }

    if (dto.role() != null) {
      if (!user.getRole().equals(dto.role())) {
        refreshTokenService.revokeAllForUser(user.getId());
      }
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
