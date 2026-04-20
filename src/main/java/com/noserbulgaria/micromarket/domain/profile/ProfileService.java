package com.noserbulgaria.micromarket.domain.profile;

import com.noserbulgaria.micromarket.domain.profile.dto.ProfileMapper;
import com.noserbulgaria.micromarket.domain.profile.dto.ProfileResponseDto;
import com.noserbulgaria.micromarket.exception.NotFoundApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class ProfileService {

  private final ProfileRepository profileRepository;
  private final ProfileMapper profileMapper;

  @Transactional(readOnly = true)
  public ProfileResponseDto getByIdOrThrow(UUID id) {
    return profileMapper.toDto( profileRepository.findById(id)
        .orElseThrow((() -> new NotFoundApiException("Profile with id '%s' not found".formatted(id)))));
  }

  @Transactional(readOnly = true)
  public Optional<Profile> getById(UUID id) {
    return profileRepository.findById(id);
  }

  @Transactional(readOnly = true)
  public Profile getByUserIdOrThrow(UUID userId) {
    return profileRepository.findByUser_Id(userId)
        .orElseThrow(() -> new IllegalStateException("Profile for user '%s' not found".formatted(userId)));
  }
}
