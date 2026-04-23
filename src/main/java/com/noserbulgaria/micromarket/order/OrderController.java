package com.noserbulgaria.micromarket.order;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Orders", description = "Administrator read access to orders")
@RestController
@RequestMapping("/order")
@RequiredArgsConstructor
public class OrderController {

  private final OrderService orderService;

  @PreAuthorize("hasRole('ADMINISTRATOR')")
  @GetMapping
  @Operation(summary = "List all orders", description = "Retrieves a paginated list of orders. Can be filtered by date range, customer ID, or order status. Restricted to ADMINISTRATOR.")
  @ApiResponse(responseCode = "200", description = "Successfully retrieved list")
  @ApiResponse(responseCode = "401", description = "Unauthorized, authentication required")
  @ApiResponse(responseCode = "403", description = "Forbidden, insufficient permissions")
  public Page<OrderResponse> findAll(@ParameterObject Pageable pageable, @ParameterObject OrderFilter filter) {
    return orderService.findAll(OrderSpecification.withFilter(filter), pageable);
  }

  @PreAuthorize("hasRole('ADMINISTRATOR')")
  @GetMapping("/{id}")
  @Operation(summary = "Get order details", description = "Retrieves the full details of a specific order by its ID. Restricted to ADMINISTRATOR.")
  @ApiResponse(responseCode = "200", description = "Successfully retrieved order")
  @ApiResponse(responseCode = "404", description = "Order not found")
  @ApiResponse(responseCode = "401", description = "Unauthorized, authentication required")
  @ApiResponse(responseCode = "403", description = "Forbidden, insufficient permissions")
  public OrderResponse findById(@PathVariable UUID id) {
    return orderService.findByIdOrThrow(id);
  }
}
