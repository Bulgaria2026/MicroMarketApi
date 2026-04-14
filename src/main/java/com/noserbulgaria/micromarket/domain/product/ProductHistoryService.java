package com.noserbulgaria.micromarket.domain.product;

import com.noserbulgaria.micromarket.domain.product.dto.ProductHistoryDto;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.DefaultRevisionEntity;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.query.AuditEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductHistoryService {

  private final EntityManager entityManager;

  public List<ProductHistoryDto> findHistoryByProductId(UUID productId) {
    AuditReader auditReader = AuditReaderFactory.get(entityManager);

    @SuppressWarnings("unchecked")
    List<Object[]> revisions = auditReader.createQuery()
        .forRevisionsOfEntity(Product.class, false, true)
        .add(AuditEntity.id().eq(productId))
        .addOrder(AuditEntity.revisionNumber().desc())
        .getResultList();

    return revisions.stream()
        .map(this::toHistoryDto)
        .toList();
  }

  private ProductHistoryDto toHistoryDto(Object[] revisionRow) {
    Product product = (Product) revisionRow[0];
    DefaultRevisionEntity revision = (DefaultRevisionEntity) revisionRow[1];
    RevisionType revisionType = (RevisionType) revisionRow[2];

    return new ProductHistoryDto(
        product.getId(),
        product.getCreatedAt(),
        product.getUpdatedAt(),
        product.getName(),
        product.getDescription(),
        product.getPrice(),
        product.getDiscount(),
        product.getEnabled(),
        product.getAmount(),
        (long) revision.getId(),
        Instant.ofEpochMilli(revision.getTimestamp()),
        revisionType.name()
    );
  }
}
