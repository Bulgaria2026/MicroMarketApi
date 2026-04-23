package com.noserbulgaria.micromarket.customer;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "guest")
@DiscriminatorValue("GUEST")
@EqualsAndHashCode(callSuper = true)
@SuppressWarnings("NullAway.Init")
public class Guest extends Customer {

  @Column(nullable = false, unique = true, length = 255)
  private String email;
}
