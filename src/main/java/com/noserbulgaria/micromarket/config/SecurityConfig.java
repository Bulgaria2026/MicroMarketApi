package com.noserbulgaria.micromarket.config;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import com.noserbulgaria.micromarket.exception.AuthenticationUserNotFoundException;
import com.noserbulgaria.micromarket.exception.ExceptionContexts;
import com.noserbulgaria.micromarket.exception.UnauthorizedApiException;
import com.noserbulgaria.micromarket.security.user.CustomUserDetails;
import com.noserbulgaria.micromarket.security.user.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.net.URI;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  private static final String[] AUTHENTICATED_GET_ENDPOINTS = {
      "/product/disabled"
  };

  private static final String[] PUBLIC_GET_ENDPOINTS = {
      "/product",
      "/product/search/by-name",
      "/product/{id}"
  };

  private final RSAPublicKey rsaPublicKey;
  private final RSAPrivateKey rsaPrivateKey;

  @Bean
  SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter
  ) {
    http
        .cors(Customizer.withDefaults())
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(HttpMethod.GET, AUTHENTICATED_GET_ENDPOINTS).authenticated()
            .requestMatchers(HttpMethod.GET, PUBLIC_GET_ENDPOINTS).permitAll()
            .requestMatchers(
                "/auth/**",
                "/swagger-ui/**",
                "/v3/api-docs/**"
            ).permitAll()
            .anyRequest().authenticated())
        .oauth2ResourceServer(oauth2 -> oauth2
            .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)))
        .exceptionHandling(exceptions -> exceptions
            .authenticationEntryPoint(problemDetailAuthenticationEntryPoint())
            .accessDeniedHandler(problemDetailAccessDeniedHandler()));
    return http.build();
  }

  @Bean
  Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter(
      CustomUserDetailsService userDetailsService
  ) {
    JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
    authoritiesConverter.setAuthoritiesClaimName("roles");
    authoritiesConverter.setAuthorityPrefix("ROLE_");

    return jwt -> {
      String email = jwt.getClaimAsString("email");
      if (email == null) {
        throw new UnauthorizedApiException(ExceptionContexts.of("email"));
      }
      CustomUserDetails principal = userDetailsService.loadUserByUsername(email);
      return new UsernamePasswordAuthenticationToken(
          principal, jwt, authoritiesConverter.convert(jwt));
    };
  }

  @Bean
  JwtDecoder jwtDecoder() {
    return NimbusJwtDecoder.withPublicKey(this.rsaPublicKey).build();
  }

  @Bean
  JwtEncoder jwtEncoder() {
    JWK jwk = new RSAKey.Builder(this.rsaPublicKey).privateKey(this.rsaPrivateKey).build();
    JWKSource<SecurityContext> jwks = new ImmutableJWKSet<>(new JWKSet(jwk));
    return new NimbusJwtEncoder(jwks);
  }

  @Bean
  PasswordEncoder passwordEncoder() {
    return PasswordEncoderFactories.createDelegatingPasswordEncoder();
  }

  @Bean
  CorsConfigurationSource corsConfigurationSource(
      @Value("${cors.allowed-origins:}") List<String> allowedOrigins
  ) {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(allowedOrigins);
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
    config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
    config.setAllowCredentials(true);
    config.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
  }

  @Bean
  AuthenticationManager authenticationManager(
      UserDetailsService userDetailsService,
      PasswordEncoder passwordEncoder
  ) {
    DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);
    authProvider.setPasswordEncoder(passwordEncoder);
    return new ProviderManager(authProvider);
  }

  private AuthenticationEntryPoint problemDetailAuthenticationEntryPoint() {
    return (request, response, ex) -> {
      ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
          HttpStatus.UNAUTHORIZED,
          authenticationDetail(request.getRequestURI(), ex)
      );
      URI type = URI.create("about:blank");
      URI instance = URI.create(request.getRequestURI());
      String title = "Unauthorized";
      String detail = problemDetail.getDetail() == null
          ? "Authentication is required to access '%s'.".formatted(request.getRequestURI())
          : problemDetail.getDetail();
      problemDetail.setTitle(title);
      problemDetail.setType(type);
      problemDetail.setInstance(instance);
      writeProblemDetail(response, type.toString(), title, detail, instance.toString(), HttpStatus.UNAUTHORIZED.value());
    };
  }

  private AccessDeniedHandler problemDetailAccessDeniedHandler() {
    return (request, response, ex) -> {
      ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
          HttpStatus.FORBIDDEN,
          "Access to '%s' is forbidden.".formatted(request.getRequestURI())
      );
      URI type = URI.create("about:blank");
      URI instance = URI.create(request.getRequestURI());
      String title = "Access Denied";
      String detail = "Access to '%s' is forbidden.".formatted(request.getRequestURI());
      problemDetail.setType(type);
      problemDetail.setTitle(title);
      problemDetail.setInstance(instance);
      writeProblemDetail(response, type.toString(), title, detail, instance.toString(), HttpStatus.FORBIDDEN.value());
    };
  }

  private void writeProblemDetail(
      jakarta.servlet.http.HttpServletResponse response,
      String type,
      String title,
      String detail,
      String instance,
      int status
  ) throws java.io.IOException {
    response.setStatus(status);
    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    response.getWriter().write("""
        {"type":"%s","title":"%s","status":%d,"detail":"%s","instance":"%s"}
        """.formatted(
        escapeJson(type),
        escapeJson(title),
        status,
        escapeJson(detail),
        escapeJson(instance)
    ));
  }

  private String escapeJson(String value) {
    return value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"");
  }

  private String authenticationDetail(String requestUri, Exception ex) {
    if (ex instanceof UnauthorizedApiException || ex instanceof AuthenticationUserNotFoundException) {
      return java.util.Objects.requireNonNullElse(
          ex.getMessage(),
          "Authentication is required to access '%s'.".formatted(requestUri)
      );
    }
    return "Authentication is required to access '%s'.".formatted(requestUri);
  }
}
