package com.noserbulgaria.micromarket.domain.customer;

import com.noserbulgaria.micromarket.domain.customer.dto.CustomerResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/customer")
@RequiredArgsConstructor
@Tag(name = "Customer", description = "Customer directory REST-operations")
public class CustomerController {

  private final CustomerService customerService;

  @Operation(summary = "Get all customers with pagination and filtering")
  @ApiResponse(responseCode = "200", description = "Customers retrieved")
  @ApiResponse(responseCode = "400", description = "Invalid filters or sort")
  @ApiResponse(responseCode = "401", description = "Unauthorized")
  @ApiResponse(responseCode = "403", description = "Forbidden - insufficient permissions")
  @PreAuthorize("hasRole('ADMINISTRATOR')")
  @GetMapping
  public Page<CustomerResponseDto> getAll(
      @ParameterObject Pageable pageable,
      @ParameterObject CustomerFilter filter
  ) {
    return customerService.findAll(filter, pageable);
  }
}
