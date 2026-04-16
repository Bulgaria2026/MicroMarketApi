package com.noserbulgaria.micromarket.domain.order;

import com.noserbulgaria.micromarket.domain.order.dto.OrderMapper;
import com.noserbulgaria.micromarket.domain.order.dto.OrderResponseDto;
import com.noserbulgaria.micromarket.generic.ExtendedServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class OrderServiceImpl extends ExtendedServiceImpl<Order, OrderResponseDto, OrderRepository, OrderMapper>
    implements OrderService {

  public OrderServiceImpl(OrderRepository orderRepository, OrderMapper orderMapper) {
    super(orderRepository, orderMapper);
  }
}
