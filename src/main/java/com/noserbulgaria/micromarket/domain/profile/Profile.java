package com.noserbulgaria.micromarket.domain.profile;

import com.noserbulgaria.micromarket.domain.customer.Customer;
import com.noserbulgaria.micromarket.security.user.User;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@EqualsAndHashCode(callSuper = true)
@SuppressWarnings("NullAway.Init")
public class Profile extends Customer {

  @OneToOne(optional = false)
  @JoinColumn(name = "user_id", nullable = false, unique = true)
  private User user;

  @Min(0)
  private long points;
}
