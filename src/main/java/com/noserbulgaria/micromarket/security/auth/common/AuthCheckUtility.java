package com.noserbulgaria.micromarket.security.auth.common;

import com.noserbulgaria.micromarket.domain.profile.Profile;
import com.noserbulgaria.micromarket.domain.profile.ProfileService;
import com.noserbulgaria.micromarket.security.user.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component("auth")
@RequiredArgsConstructor
public class AuthCheckUtility {

  private final ProfileService profileService;

  public boolean isProfileOwner(UUID id, @Nullable CustomUserDetails userDetails) {
    if (userDetails == null) {
      return false;
    }
    Optional<Profile> profile = profileService.getById(id);
    return profile.map(value -> value.getUser().getId().equals(userDetails.getId())).orElse(false);
  }
}
