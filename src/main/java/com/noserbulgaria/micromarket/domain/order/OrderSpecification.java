package com.noserbulgaria.micromarket.domain.order;

import org.springframework.data.jpa.domain.Specification;

import com.noserbulgaria.micromarket.generic.ExtendedSpecification;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class OrderSpecification extends ExtendedSpecification<Order> {

  private final OrderFilter filter;

  @Override
  public Specification<Order> withFilter() {
    return Specification.where(fromDate(
            filter.fromDate()))
        .and(toDate(filter.toDate()))
        .and(equalTo(filter.customerId(), "customerId"))
        .and(equalTo(filter.status(), "status"));
  }
}