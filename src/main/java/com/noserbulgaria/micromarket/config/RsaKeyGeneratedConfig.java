package com.noserbulgaria.micromarket.config;

import com.noserbulgaria.micromarket.exception.ExceptionContexts;
import com.noserbulgaria.micromarket.exception.InternalServerApiException;
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
      throw new InternalServerApiException(ExceptionContexts.of("rsa-keypair"));
    }
    log.info("No RSA keys configured - generating ephemeral key pair for dev/test");
    try {
      KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
      generator.initialize(2048);
      this.keyPair = generator.generateKeyPair();
    } catch (NoSuchAlgorithmException e) {
      log.error("Failed to generate RSA key pair", e);
      throw new InternalServerApiException(ExceptionContexts.of("rsa-keypair"));
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
