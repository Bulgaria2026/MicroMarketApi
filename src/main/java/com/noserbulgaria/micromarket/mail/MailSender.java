package com.noserbulgaria.micromarket.mail;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailSender {

  private final Resend resend;
  private final SpringTemplateEngine emailTemplateEngine;
  private final MailProperties properties;

  @Retryable(
      retryFor = {ResendException.class, IOException.class},
      maxAttempts = 4,
      backoff = @Backoff(delay = 500, multiplier = 2.0, maxDelay = 8_000)
  )
  public void send(String template, String to, String subject, Map<String, Object> variables) throws ResendException {
    if (!properties.enabled()) {
      log.info("Mail disabled; skipping send of '{}' to {}", template, to);
      return;
    }

    String html = render(template, variables);
    CreateEmailOptions request = CreateEmailOptions.builder()
        .from(properties.from().formatted())
        .to(to)
        .subject(subject)
        .html(html)
        .build();

    CreateEmailResponse response = resend.emails().send(request);
    log.info("Sent '{}' to {} (resend id {})", template, to, response.getId());
  }

  @Recover
  void recover(Exception cause, String template, String to, String subject, Map<String, Object> variables) {
    log.error("Giving up on sending '{}' to {} after retries", template, to, cause);
  }

  private String render(String template, Map<String, Object> variables) {
    Context context = new Context(Locale.ENGLISH);
    context.setVariables(variables);
    return emailTemplateEngine.process(template, context);
  }
}
