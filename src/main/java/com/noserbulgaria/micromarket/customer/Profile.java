package com.noserbulgaria.micromarket.customer;

import com.noserbulgaria.micromarket.auth.user.User;
import com.noserbulgaria.micromarket.common.ExtendedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/** Registered-account state for a Customer. Shares the Customer's primary key (1:1 via {@code @MapsId}). */
@Data
@Entity
@Table(name = "profile")
@EqualsAndHashCode(callSuper = true)
@SuppressWarnings("NullAway.Init")
public class Profile extends ExtendedEntity {

  @MapsId
  @ToString.Exclude
  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "id")
  private Customer customer;

  @ToString.Exclude
  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", unique = true, nullable = false)
  private User user;

  @Min(0)
  @Column(nullable = false)
  private long points;
}
