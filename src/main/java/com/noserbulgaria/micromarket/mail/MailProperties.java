package com.noserbulgaria.micromarket.mail;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "mail")
public record MailProperties(
    @DefaultValue("true") boolean enabled,
    Resend resend,
    From from
) {

  public record Resend(String apiKey) {}

  public record From(String address, @DefaultValue("MicroMarket") String name) {

    public String formatted() {
      return "%s <%s>".formatted(name, address);
    }
  }
}
