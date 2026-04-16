package com.noserbulgaria.micromarket.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Set;

@Slf4j
@Configuration
@ConditionalOnMissingBean(RSAPublicKey.class)
public class RsaKeyGeneratedConfig {

  private static final Set<String> DEV_PROFILES = Set.of("dev", "test");

  private final KeyPair keyPair;

  public RsaKeyGeneratedConfig(Environment environment) {
    if (Set.of(environment.getActiveProfiles()).stream().noneMatch(DEV_PROFILES::contains)) {
      throw new IllegalStateException("No RSA keys configured and not running in a dev/test profile");
    }
    log.info("No RSA keys configured - generating ephemeral key pair for dev/test");
    try {
      KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
      generator.initialize(2048);
      this.keyPair = generator.generateKeyPair();
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("Failed to generate RSA key pair", e);
    }
  }

  @Bean
  RSAPublicKey rsaPublicKey() {
    return (RSAPublicKey) keyPair.getPublic();
  }

  @Bean
  RSAPrivateKey rsaPrivateKey() {
    return (RSAPrivateKey) keyPair.getPrivate();
  }
}
