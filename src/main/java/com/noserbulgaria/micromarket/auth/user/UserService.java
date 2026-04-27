package com.noserbulgaria.micromarket.auth.user;

import com.noserbulgaria.micromarket.auth.refresh.RefreshTokenService;
import com.noserbulgaria.micromarket.customer.Customer;
import com.noserbulgaria.micromarket.customer.CustomerRepository;
import com.noserbulgaria.micromarket.customer.Profile;
import com.noserbulgaria.micromarket.customer.ProfileRepository;
import com.noserbulgaria.micromarket.exception.ConflictApiException;
import com.noserbulgaria.micromarket.exception.NotFoundApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

  private final ProfileRepository profileRepository;
  private final CustomerRepository customerRepository;
  private final UserMapper userMapper;
  private final RefreshTokenService refreshTokenService;

  @Transactional(readOnly = true)
  public UserResponse getByIdOrThrow(UUID userId) {
    return userMapper.toDto(profileByUserIdOrThrow(userId));
  }

  @Transactional
  public UserResponse patchUserOrThrow(UUID userId, UserPatchRequest dto) {
    Profile profile = profileByUserIdOrThrow(userId);
    Customer customer = profile.getCustomer();
    User user = profile.getUser();

    String email = dto.email();
    if (email != null) {
      String normalized = email.toLowerCase(Locale.ROOT);
      if (!normalized.equals(customer.getEmail())) {
        if (customerRepository.findByEmail(normalized).isPresent()) {
          throw new ConflictApiException("User with email '%s' already exists".formatted(dto.email()));
        }
        customer.setEmail(normalized);
      }
    }

    Role role = dto.role();
    if (role != null && !user.getRole().equals(role)) {
      refreshTokenService.revokeAllForUser(user.getId());
      user.setRole(role);
    }

    AccountStatus status = dto.status();
    if (status != null) {
      if (status == AccountStatus.INACTIVE && user.getStatus() != AccountStatus.INACTIVE) {
        refreshTokenService.revokeAllForUser(user.getId());
      }
      user.setStatus(status);
    }

    return userMapper.toDto(profileRepository.save(profile));
  }

  private Profile profileByUserIdOrThrow(UUID userId) {
    return profileRepository.findByUserId(userId)
        .orElseThrow(() -> new NotFoundApiException("User with id '%s' not found".formatted(userId)));
  }
}
