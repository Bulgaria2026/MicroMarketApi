package com.noserbulgaria.micromarket.mail.order;

import java.util.UUID;

public record OrderConfirmedEvent(UUID orderId) {}
