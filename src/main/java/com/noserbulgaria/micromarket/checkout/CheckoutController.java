package com.noserbulgaria.micromarket.checkout;

import com.noserbulgaria.micromarket.auth.user.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Entry point for the checkout use case. Thin adapter that maps {@code POST /order} to {@link OrderPlacementService}.
 * Lives in the checkout package because placing an order is orchestration across Order + Customer + Stripe, not a
 * CRUD operation on the Order aggregate — {@link com.noserbulgaria.micromarket.order.OrderController} still
 * owns the admin GET endpoints on the same URL.
 */
@Tag(name = "Checkout", description = "Place an order and start the Stripe payment flow")
@RestController
@RequiredArgsConstructor
public class CheckoutController {

  private final OrderPlacementService orderPlacementService;

  @PostMapping("/order")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
      summary = "Place an order and start payment",
      description = """
          Creates an Order in PENDING_PAYMENT and initiates a Stripe PaymentIntent. Public: anonymous callers must
          supply an email so a Guest customer can be linked or created; authenticated callers have a Profile resolved
          (or created) automatically and any supplied email is ignored. The frontend completes the payment with the
          returned clientSecret via Stripe Elements — raw card data never touches this backend. The Order transitions
          to PAID asynchronously when Stripe delivers the payment_intent.succeeded webhook.
          """)
  @ApiResponse(responseCode = "201", description = "Order placed and payment initiated")
  @ApiResponse(responseCode = "400", description = "Invalid request, insufficient stock, or disabled product")
  public PlaceOrderResponse place(
      @Valid @RequestBody PlaceOrderRequest request,
      @AuthenticationPrincipal @Nullable CustomUserDetails userDetails
  ) {
    return orderPlacementService.placeOrder(request, userDetails);
  }
}
