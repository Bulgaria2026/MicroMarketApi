package com.noserbulgaria.micromarket.customer;

import com.noserbulgaria.micromarket.exception.NotFoundApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class ProfileService {

  private final ProfileRepository profileRepository;
  private final ProfileMapper profileMapper;

  @Transactional(readOnly = true)
  public ProfileResponse getByIdOrThrow(UUID id) {
    return profileMapper.toDto(profileRepository.findById(id)
        .orElseThrow((() -> new NotFoundApiException("Profile with id '%s' not found".formatted(id)))));
  }

  @Transactional(readOnly = true)
  public ProfileResponse getByUserIdOrThrow(UUID userId) {
    return profileMapper.toDto(profileRepository.findByUserId(userId)
        .orElseThrow(() -> new NotFoundApiException("Profile with userId '%s' not found".formatted(userId))));
  }

  public ProfileResponse updateByIdOrThrow(UUID id, ProfileRequest request) {
    Profile profile = profileRepository.findById(id)
        .orElseThrow(() -> new NotFoundApiException("Profile with id '%s' not found".formatted(id)));
    profileMapper.update(request, profile);
    return profileMapper.toDto(profileRepository.save(profile));
  }
}
