package com.noserbulgaria.micromarket.auth.user;

import com.noserbulgaria.micromarket.common.ExtendedEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Entity
@Table(name = "users")
@EqualsAndHashCode(callSuper = true)
@SuppressWarnings("NullAway.Init")
public class User extends ExtendedEntity {

  @Column(nullable = false, unique = true)
  private String email;

  @Column(nullable = false)
  private String password;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Role role;
}
