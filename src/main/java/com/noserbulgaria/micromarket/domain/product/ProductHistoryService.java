package com.noserbulgaria.micromarket.domain.product;

import com.noserbulgaria.micromarket.domain.product.dto.ProductHistoryDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.history.Revision;
import org.springframework.data.history.Revisions;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductHistoryService {

  private final ProductRepository productRepository;

  public List<ProductHistoryDto> findHistoryByProductId(UUID productId) {
    Revisions<Integer, Product> revisions = productRepository.findRevisions(productId);

    return revisions.stream()
        .map(this::toHistoryDto)
        .toList();
  }

  private ProductHistoryDto toHistoryDto(Revision<Integer, Product> revision) {
    Product product = revision.getEntity();

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
        revision.getRequiredRevisionNumber().longValue(),
        revision.getMetadata().getRequiredRevisionInstant(),
        revision.getMetadata().getRevisionType().name()
    );
  }
}
