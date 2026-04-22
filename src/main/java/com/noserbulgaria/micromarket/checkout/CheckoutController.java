package com.noserbulgaria.micromarket.checkout;

import com.noserbulgaria.micromarket.auth.user.CustomUserDetails;
import com.noserbulgaria.micromarket.order.Order;
import com.noserbulgaria.micromarket.order.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Placement + status-polling endpoints. Admin GETs on {@code /order} live on {@code OrderController}. */
@Tag(name = "Checkout", description = "Place an order and start the Stripe payment flow")
@RestController
@RequiredArgsConstructor
public class CheckoutController {

  private final OrderPlacementService orderPlacementService;
  private final OrderService orderService;

  @PostMapping("/order")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
      summary = "Place an order and start payment",
      description = """
          Anonymous callers must supply {@code email}; authenticated callers ignore it and use their Profile. Returns
          a Stripe-hosted checkout URL to redirect to. Order transitions to PAID asynchronously on webhook delivery.
          """)
  @ApiResponse(responseCode = "201", description = "Order placed and Checkout Session created")
  @ApiResponse(responseCode = "400", description = "Invalid request, insufficient stock, or disabled product")
  public PlaceOrderResponse place(
      @Valid @RequestBody PlaceOrderRequest request,
      @AuthenticationPrincipal @Nullable CustomUserDetails userDetails
  ) {
    return orderPlacementService.placeOrder(request, userDetails);
  }

  @GetMapping("/checkout/sessions/{sessionId}/status")
  @Operation(
      summary = "Fetch the order status for a checkout session",
      description = """
          Public polling endpoint used by the storefront after Stripe redirect. The opaque session id is proof of
          ownership, so no further auth is required. Reads our DB only.
          """)
  @ApiResponse(responseCode = "200", description = "Current status of the order tied to the session")
  @ApiResponse(responseCode = "404", description = "No order found for the given session id")
  public CheckoutStatusResponse status(@PathVariable String sessionId) {
    Order order = orderService.findByStripeCheckoutSessionIdOrThrow(sessionId);
    return new CheckoutStatusResponse(order.getOrderNumber(), order.getStatus());
  }
}
