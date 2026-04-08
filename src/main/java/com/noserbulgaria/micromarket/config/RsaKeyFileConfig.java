package com.noserbulgaria.micromarket.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

@Configuration
@ConditionalOnProperty(name = {"jwt.public.key", "jwt.private.key"})
public class RsaKeyFileConfig {

  @Bean
  RSAPublicKey rsaPublicKey(@Value("${jwt.public.key}") RSAPublicKey key) {
    return key;
  }

  @Bean
  RSAPrivateKey rsaPrivateKey(@Value("${jwt.private.key}") RSAPrivateKey key) {
    return key;
  }
}
