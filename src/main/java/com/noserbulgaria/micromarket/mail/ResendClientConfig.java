package com.noserbulgaria.micromarket.mail;

import com.resend.Resend;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

@Configuration
@EnableRetry
@EnableConfigurationProperties(MailProperties.class)
public class ResendClientConfig {

  @Bean
  Resend resendClient(MailProperties properties) {
    return new Resend(properties.resend().apiKey());
  }
}
