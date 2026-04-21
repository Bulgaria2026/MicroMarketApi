package com.noserbulgaria.micromarket.customer;

import com.noserbulgaria.micromarket.exception.BadRequestApiException;
import com.noserbulgaria.micromarket.auth.user.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomerResolver {

  private final ProfileRepository profileRepository;
  private final GuestRepository guestRepository;

  /**
   * Returns the Customer row that owns this checkout. Authenticated users get a lazily-created Profile linked to their
   * User. Anonymous callers must provide an email; a Guest row is upserted on first use and reused thereafter. A
   * unique-constraint race between two concurrent first-time callers is resolved by catching the integrity violation
   * and re-fetching.
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public Customer resolveForCheckout(@Nullable CustomUserDetails userDetails, @Nullable String email) {
    if (userDetails != null) {
      return resolveProfile(userDetails);
    }
    if (email == null || email.isBlank()) {
      throw new BadRequestApiException("Email is required for guest checkout");
    }
    return resolveGuest(email.trim());
  }

  private Profile resolveProfile(CustomUserDetails userDetails) {
    var userId = userDetails.user().getId();
    return profileRepository.findByUserId(userId)
        .orElseGet(() -> createProfileOrReload(userDetails));
  }

  private Profile createProfileOrReload(CustomUserDetails userDetails) {
    Profile profile = new Profile();
    profile.setUser(userDetails.user());
    profile.setPoints(0);
    try {
      return profileRepository.saveAndFlush(profile);
    } catch (DataIntegrityViolationException ex) {
      return profileRepository.findByUserId(userDetails.user().getId())
          .orElseThrow(() -> ex);
    }
  }

  private Guest resolveGuest(String email) {
    return guestRepository.findByEmailIgnoreCase(email)
        .orElseGet(() -> createGuestOrReload(email));
  }

  private Guest createGuestOrReload(String email) {
    Guest guest = new Guest();
    guest.setEmail(email);
    try {
      return guestRepository.saveAndFlush(guest);
    } catch (DataIntegrityViolationException ex) {
      return guestRepository.findByEmailIgnoreCase(email)
          .orElseThrow(() -> ex);
    }
  }
}
