package com.noserbulgaria.micromarket.customer;

import com.noserbulgaria.micromarket.exception.BadRequestApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class CustomerService {

  private static final Set<String> SUPPORTED_SORT_PROPERTIES = Set.of("createdAt", "id");

  private final CustomerRepository customerRepository;
  private final CustomerMapper customerMapper;

  @Transactional(readOnly = true)
  public Page<CustomerResponse> findAll(CustomerFilter filter, Pageable pageable) {
    return customerRepository.findAll(
        CustomerSpecification.withFilter(filter),
        validatedPageable(pageable)
    ).map(customerMapper::toDto);
  }

  private Pageable validatedPageable(Pageable pageable) {
    if (pageable.getSort().isUnsorted()) {
      return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    pageable.getSort().stream()
        .map(Sort.Order::getProperty)
        .filter(property -> !SUPPORTED_SORT_PROPERTIES.contains(property))
        .findFirst()
        .ifPresent(property -> {
          throw new BadRequestApiException("Sorting by '%s' is not supported".formatted(property));
        });

    return pageable;
  }
}
