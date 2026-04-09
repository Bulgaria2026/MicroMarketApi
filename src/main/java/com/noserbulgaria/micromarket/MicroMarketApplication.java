package com.noserbulgaria.micromarket;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class MicroMarketApplication {

  static void main(String[] args) {
    SpringApplication.run(MicroMarketApplication.class, args);
  }

}
