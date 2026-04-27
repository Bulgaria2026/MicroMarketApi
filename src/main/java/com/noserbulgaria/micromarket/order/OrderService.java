package com.noserbulgaria.micromarket.order;

import com.noserbulgaria.micromarket.customer.ProfileRepository;
import com.noserbulgaria.micromarket.exception.NotFoundApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

  private final OrderRepository orderRepository;
  private final OrderMapper orderMapper;
  private final ProfileRepository profileRepository;

  public OrderResponse findByIdOrThrow(UUID id) {
    return orderRepository.findById(id)
        .map(orderMapper::toDto)
        .orElseThrow(() -> new NotFoundApiException("Order with id '%s' not found".formatted(id)));
  }

  public Order findEntityByIdOrThrow(UUID id) {
    return orderRepository.findById(id)
        .orElseThrow(() -> new NotFoundApiException("Order with id '%s' not found".formatted(id)));
  }

  public Page<OrderResponse> findAll(Specification<Order> spec, Pageable pageable) {
    return orderRepository.findAll(spec, pageable)
        .map(orderMapper::toDto);
  }

  public Page<OrderResponse> findOwnOrders(UUID userId, OwnOrderFilter filter, Pageable pageable) {
    return profileRepository.findByUserId(userId)
        .map(profile -> orderRepository
            .findAll(OrderSpecification.forCustomer(profile.getCustomer().getId(), filter), pageable)
            .map(orderMapper::toDto))
        .orElseGet(Page::empty);
  }

  public Order findByStripeCheckoutSessionIdOrThrow(String stripeCheckoutSessionId) {
    return orderRepository.findByStripeCheckoutSessionId(stripeCheckoutSessionId)
        .orElseThrow(() -> new NotFoundApiException(
            "Order with Stripe Checkout session id '%s' not found".formatted(stripeCheckoutSessionId)));
  }

  public Order findByStripePaymentIntentIdOrThrow(String stripePaymentIntentId) {
    return orderRepository.findByStripePaymentIntentId(stripePaymentIntentId)
        .orElseThrow(() -> new NotFoundApiException(
            "Order with Stripe Payment Intent id '%s' not found".formatted(stripePaymentIntentId)));
  }
}
