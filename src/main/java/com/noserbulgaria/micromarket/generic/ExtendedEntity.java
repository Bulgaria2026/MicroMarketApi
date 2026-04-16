package com.noserbulgaria.micromarket.generic;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.envers.Audited;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Data
@Audited
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@SuppressWarnings("NullAway.Init")
public abstract class ExtendedEntity {

  @Id
  @GeneratedValue
  @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
  private UUID id;

  @CreatedDate
  @Column(nullable = false, updatable = false)
  private Instant createdAt;

  @LastModifiedDate
  @Column(nullable = true)
  private Instant updatedAt;
}
