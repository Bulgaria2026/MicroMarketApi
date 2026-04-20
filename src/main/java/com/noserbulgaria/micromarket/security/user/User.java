package com.noserbulgaria.micromarket.security.user;

import com.noserbulgaria.micromarket.domain.customer.Customer;
import com.noserbulgaria.micromarket.generic.ExtendedEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Entity
@Table(name = "users")
@EqualsAndHashCode(callSuper = true)
@SuppressWarnings("NullAway.Init")
public class User extends ExtendedEntity {

  @OneToOne(optional = false, cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "customer_id", nullable = false, unique = true)
  private Customer customer;

  @Column(nullable = false, unique = true)
  private String email;

  @Column(nullable = false)
  private String password;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Role role;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private AccountStatus status = AccountStatus.ACTIVE;
}
