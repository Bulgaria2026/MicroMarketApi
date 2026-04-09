package com.noserbulgaria.micromarket.domain.order;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * REST controller for managing orders.
 */
@Tag(name = "Orders", description = "Order management API")
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

  private final OrderService orderService;
  private final OrderMapper orderMapper;

  @PreAuthorize("hasRole('ADMINISTRATOR')")
  @Transactional(readOnly = true)
  @GetMapping({""})
  /**
   * Retrieves a paginated list of orders.
   *
   * @param page the page number
   * @param size the page size
   * @param fromDate filter orders from this date
   * @param toDate filter orders to this date
   * @param customerId filter orders by customer ID
   * @param status filter orders by status
   * @return a paginated list of orders
   */
  @Operation(summary = "List all orders", description = "Retrieves a paginated list of orders. Can be filtered by date range, customer ID, or order status. Restricted to ADMINISTRATOR.")
  @ApiResponse(responseCode = "200", description = "Successfully retrieved list")
  public Page<OrderDto> findAll(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(required = false) Instant fromDate,
      @RequestParam(required = false) Instant toDate,
      @RequestParam(required = false) UUID customerId,
      @RequestParam(required = false) OrderStatusType status) {
    return orderService.findAll(page, size, fromDate, toDate, customerId, status)
        .map(orderMapper::entityToDto);
  }

  @PreAuthorize("hasRole('ADMINISTRATOR')")
  @Transactional(readOnly = true)
  @GetMapping("/{id}")
  /**
   * Retrieves the full details of a specific order by its ID.
   *
   * @param id the order ID
   * @return the order details
   */
  @Operation(summary = "Get order details", description = "Retrieves the full details of a specific order by its ID. Restricted to ADMINISTRATOR.")
  @ApiResponse(responseCode = "200", description = "Successfully retrieved order")
  @ApiResponse(responseCode = "404", description = "Order not found")
  public OrderDto findById(@PathVariable UUID id) {
    return orderService.findById(id)
        .map(orderMapper::entityToDto)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
  }

}
