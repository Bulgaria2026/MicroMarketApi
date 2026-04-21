package com.noserbulgaria.micromarket.auth.refresh;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "auth.refresh-token")
public class RefreshTokenCleanupProperties {

  private String cleanupCron = "0 0 * * * *";
  private Duration cleanupGrace = Duration.ofDays(7);
}
