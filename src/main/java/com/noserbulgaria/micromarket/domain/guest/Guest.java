package com.noserbulgaria.micromarket.domain.guest;

import com.noserbulgaria.micromarket.domain.customer.Customer;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "guests")
@EqualsAndHashCode(callSuper = true)
@SuppressWarnings("NullAway.Init")
public class Guest extends Customer {

  @Column(nullable = false, unique = true)
  private String email;
}
