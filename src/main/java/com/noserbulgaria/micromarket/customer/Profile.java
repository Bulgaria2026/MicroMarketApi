package com.noserbulgaria.micromarket.customer;

import com.noserbulgaria.micromarket.auth.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@Entity
@Table(name = "profile")
@DiscriminatorValue("PROFILE")
@EqualsAndHashCode(callSuper = true)
@SuppressWarnings("NullAway.Init")
public class Profile extends Customer {

  @EqualsAndHashCode.Exclude
  @ToString.Exclude
  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false, unique = true)
  private User user;

  @Min(0)
  @Column(nullable = false)
  private long points;

  @Enumerated(EnumType.STRING)
  private PointChangeReason lastChangeReason;
}
