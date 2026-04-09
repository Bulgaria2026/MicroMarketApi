package com.noserbulgaria.micromarket.domain.stock.dto;

import com.noserbulgaria.micromarket.domain.stock.Stock;
import com.noserbulgaria.micromarket.generic.ExtendedMapper;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface StockMapper extends ExtendedMapper<Stock, StockDto> {
}
