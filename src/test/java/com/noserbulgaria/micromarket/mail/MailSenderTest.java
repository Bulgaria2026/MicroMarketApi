package com.noserbulgaria.micromarket.mail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.noserbulgaria.micromarket.mail.order.OrderConfirmationView;
import com.resend.Resend;
import com.resend.services.emails.Emails;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

class MailSenderTest {

  private final SpringTemplateEngine engine = buildEngine();
  private final Resend resend = mock(Resend.class);
  private final Emails emails = mock(Emails.class);
  private final MailProperties props = new MailProperties(
      true,
      new MailProperties.Resend("re_test"),
      new MailProperties.From("no-reply@micromarket.test", "MicroMarket"));
  private final MailSender sender = new MailSender(resend, engine, props);

  @Test
  void rendersOrderConfirmationAndInvokesResend() throws Exception {
    when(resend.emails()).thenReturn(emails);
    CreateEmailResponse response = mock(CreateEmailResponse.class);
    when(response.getId()).thenReturn("re_fake_123");
    when(emails.send(any(CreateEmailOptions.class))).thenReturn(response);

    OrderConfirmationView view = new OrderConfirmationView(
        "ORD-100042",
        "23 April 2026",
        List.of(new OrderConfirmationView.Item(
            "Olive Oil 500ml", "Extra virgin", 2, new BigDecimal("14.90"), 10, new BigDecimal("26.82"))),
        new BigDecimal("29.80"),
        new BigDecimal("2.98"),
        new BigDecimal("26.82"));

    sender.send("order-confirmation", "buyer@example.com", "Order ORD-100042", Map.of("order", view));

    ArgumentCaptor<CreateEmailOptions> captor = ArgumentCaptor.forClass(CreateEmailOptions.class);
    verify(emails).send(captor.capture());
    CreateEmailOptions request = captor.getValue();

    assertThat(request.getFrom()).isEqualTo("MicroMarket <no-reply@micromarket.test>");
    assertThat(request.getTo()).contains("buyer@example.com");
    assertThat(request.getSubject()).isEqualTo("Order ORD-100042");
    assertThat(request.getHtml())
        .contains("ORD-100042")
        .contains("23 April 2026")
        .contains("Olive Oil 500ml")
        .contains("Extra virgin")
        .contains("−10%");
  }

  @Test
  void skipsSendWhenMailDisabled() throws Exception {
    MailProperties disabled = new MailProperties(
        false,
        new MailProperties.Resend("re_test"),
        new MailProperties.From("no-reply@micromarket.test", "MicroMarket"));
    MailSender inert = new MailSender(resend, engine, disabled);

    inert.send("order-confirmation", "buyer@example.com", "Order", Map.of("order", sampleView()));

    verify(resend, org.mockito.Mockito.never()).emails();
  }

  private static OrderConfirmationView sampleView() {
    return new OrderConfirmationView(
        "ORD-X", "1 January 2026", List.of(),
        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
  }

  private static SpringTemplateEngine buildEngine() {
    ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
    resolver.setPrefix("email-templates/");
    resolver.setSuffix(".html");
    resolver.setTemplateMode(TemplateMode.HTML);
    resolver.setCharacterEncoding("UTF-8");
    resolver.setCacheable(false);
    SpringTemplateEngine engine = new SpringTemplateEngine();
    engine.setTemplateResolver(resolver);
    return engine;
  }
}
