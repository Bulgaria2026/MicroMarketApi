package com.noserbulgaria.micromarket.customer;

import com.noserbulgaria.micromarket.common.ExtendedEntity;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Entity
@Table(name = "customer")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "customer_type")
@EqualsAndHashCode(callSuper = true)
@SuppressWarnings("NullAway.Init")
public abstract class Customer extends ExtendedEntity {
}
