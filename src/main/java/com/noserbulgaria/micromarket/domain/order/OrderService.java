package com.noserbulgaria.micromarket.domain.order;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;

/**
 * Service interface for managing orders.
 */
public interface OrderService {

  /**
   * Finds all orders based on the provided filters.
   *
   * @param page the page number
   * @param size the page size
   * @param fromDate filter orders from this date
   * @param toDate filter orders to this date
   * @param customerId filter orders by customer ID
   * @param status filter orders by status
   * @return a paginated list of orders
   */
  Page<Order> findAll(int page, int size, Instant fromDate, Instant toDate, UUID customerId, OrderStatusType status);

  /**
   * Finds an order by its ID.
   *
   * @param id the order ID
   * @return an optional containing the order if found, or empty otherwise
   */
  Optional<Order> findById(UUID id);
}
