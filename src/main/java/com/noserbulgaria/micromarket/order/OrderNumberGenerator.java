package com.noserbulgaria.micromarket.order;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Pulls the next order_number from the {@code order_number_seq} sequence and formats it as {@code MM-NNNNNN}. Uses
 * the database sequence (not application state) so it survives restarts and remains monotonic across multiple
 * instances. The 6-digit zero-pad gives ~1M readable codes; once exceeded the format degrades gracefully to wider
 * numbers (still unique, still sortable).
 */
@Component
@RequiredArgsConstructor
public class OrderNumberGenerator {

  private final EntityManager entityManager;

  public String next() {
    Number value = (Number) entityManager
        .createNativeQuery("SELECT nextval('order_number_seq')")
        .getSingleResult();
    return "MM-%06d".formatted(value.longValue());
  }
}
