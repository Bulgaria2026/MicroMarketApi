package com.noserbulgaria.micromarket.domain.product;

import com.noserbulgaria.micromarket.domain.product.dto.ProductHistoryResponseDto;
import com.noserbulgaria.micromarket.domain.product.dto.ProductMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductHistoryService {

  private final ProductRepository productRepository;
  private final ProductMapper productMapper;

  public List<ProductHistoryResponseDto> findHistoryByProductId(UUID productId) {
    return productRepository.findRevisions(productId).stream()
        .map(revision -> productMapper.toHistoryDto(
            revision.getEntity(),
            revision.getRequiredRevisionNumber().longValue(),
            revision.getMetadata().getRequiredRevisionInstant(),
            revision.getMetadata().getRevisionType().name()
        ))
        .toList();
  }
}
