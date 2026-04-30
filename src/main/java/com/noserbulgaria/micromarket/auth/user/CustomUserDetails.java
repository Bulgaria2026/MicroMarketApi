package com.noserbulgaria.micromarket.auth.user;

import com.noserbulgaria.micromarket.customer.Profile;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/** Spring Security principal for a registered customer (its Profile and linked User). */
public record CustomUserDetails(Profile profile) implements UserDetails {

  public User user() {
    return profile.getUser();
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return List.of(new SimpleGrantedAuthority("ROLE_" + user().getRole().name()));
  }

  @Override
  public String getPassword() {
    return user().getPassword();
  }

  @Override
  public String getUsername() {
    return profile.getCustomer().getEmail();
  }

  @Override
  public boolean isEnabled() {
    return user().getStatus() == AccountStatus.ACTIVE;
  }

  public Role getRole() {
    return user().getRole();
  }

  public UUID getId() {
    return user().getId();
  }

  public String getEmail() {
    return profile.getCustomer().getEmail();
  }
}
