package com.noserbulgaria.micromarket.security.user;

import com.noserbulgaria.micromarket.exception.AuthenticationUserNotFoundException;
import com.noserbulgaria.micromarket.exception.ExceptionContexts;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

/**
 * Loads user details by email address for Spring Security authentication.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

  private final UserRepository userRepository;

  @Override
  public CustomUserDetails loadUserByUsername(String email) {
    return userRepository.findByEmail(email)
        .map(CustomUserDetails::new)
        .orElseThrow(() -> new AuthenticationUserNotFoundException(ExceptionContexts.fromEmail(email)));
  }
}
