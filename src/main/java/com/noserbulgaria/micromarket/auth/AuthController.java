package com.noserbulgaria.micromarket.auth;

import com.noserbulgaria.micromarket.auth.dto.LoginRequestDTO;
import com.noserbulgaria.micromarket.auth.dto.LoginResponseDTO;
import com.noserbulgaria.micromarket.domain.user.User;
import com.noserbulgaria.micromarket.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthenticationManager authenticationManager;
  private final TokenService tokenService;
  private final UserRepository userRepository;

  @PostMapping("/login")
  public LoginResponseDTO login(@RequestBody LoginRequestDTO request) {
    authenticationManager
        .authenticate(new UsernamePasswordAuthenticationToken(request.email(), request.password()));

    User user = userRepository.findByEmail(request.email()).orElseThrow();
    String token = tokenService.generateToken(user);
    return new LoginResponseDTO(token);
  }
}
