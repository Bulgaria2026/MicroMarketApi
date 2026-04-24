package com.noserbulgaria.micromarket.mail;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.templateresolver.ITemplateResolver;

/**
 * Dedicated Thymeleaf engine for transactional emails. Resolves templates from the
 * {@code com.noserbulgaria.micromarket:email-templates} JAR under {@code classpath:email-templates/}.
 */
@Configuration
public class EmailTemplateConfig {

  public static final String TEMPLATE_ENGINE_BEAN_NAME = "emailTemplateEngine";

  @Bean(TEMPLATE_ENGINE_BEAN_NAME)
  SpringTemplateEngine emailTemplateEngine() {
    SpringTemplateEngine engine = new SpringTemplateEngine();
    engine.setTemplateResolver(emailHtmlResolver());
    return engine;
  }

  private ITemplateResolver emailHtmlResolver() {
    ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
    resolver.setPrefix("email-templates/");
    resolver.setSuffix(".html");
    resolver.setTemplateMode(TemplateMode.HTML);
    resolver.setCharacterEncoding("UTF-8");
    resolver.setCacheable(true);
    return resolver;
  }
}
