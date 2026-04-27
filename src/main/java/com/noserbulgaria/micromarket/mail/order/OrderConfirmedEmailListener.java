package com.noserbulgaria.micromarket.mail.order;

import com.noserbulgaria.micromarket.mail.EmailAsyncConfig;
import com.noserbulgaria.micromarket.mail.MailSender;
import com.noserbulgaria.micromarket.order.Order;
import com.noserbulgaria.micromarket.order.OrderService;
import com.resend.core.exception.ResendException;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderConfirmedEmailListener {

  static final String TEMPLATE = "order-confirmation";

  private final OrderService orderService;
  private final OrderConfirmationViewMapper mapper;
  private final MailSender mailSender;

  @Async(EmailAsyncConfig.EXECUTOR_BEAN_NAME)
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
  public void on(OrderConfirmedEvent event) throws ResendException {
    Order order = orderService.findEntityByIdOrThrow(event.orderId());
    OrderConfirmationView view = mapper.toView(order);
    String subject = "Your MicroMarket order %s is confirmed".formatted(order.getOrderNumber());
    log.debug("Dispatching order-confirmation email for order {}", order.getId());
    mailSender.send(TEMPLATE, order.getEmail(), subject, Map.of("order", view));
  }
}
