package com.noserbulgaria.micromarket.security.auth;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "auth.refresh-cookie")
public class RefreshCookieProperties {

  private String name = "refresh_token";
  private String path = "/api/v1/auth";
  private String sameSite = "Lax";
  private boolean secure = false;
}
