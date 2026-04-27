package com.noserbulgaria.micromarket.auth.user;

import com.noserbulgaria.micromarket.customer.ProfileRepository;
import com.noserbulgaria.micromarket.exception.AuthenticationUserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

  private final ProfileRepository profileRepository;

  @Override
  public CustomUserDetails loadUserByUsername(String email) {
    return profileRepository.findByCustomer_Email(email.toLowerCase(Locale.ROOT))
        .map(CustomUserDetails::new)
        .orElseThrow(() -> new AuthenticationUserNotFoundException(
            "User with email '%s' not found".formatted(email)));
  }
}
