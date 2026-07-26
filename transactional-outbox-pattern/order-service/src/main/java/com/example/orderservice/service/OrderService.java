package com.example.orderservice.service;

import com.example.orderservice.dto.OrderDTO;
import com.example.orderservice.entity.Order;
import com.example.orderservice.entity.Outbox;
import com.example.orderservice.repo.OrderRepo;
import com.example.orderservice.repo.OutboxRepo;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.Date;

@Service
public class OrderService {

    private final OrderRepo orderRepo;
    private final OutboxRepo outboxRepo;

    public OrderService(OrderRepo orderRepo, OutboxRepo outboxRepo) {
        this.orderRepo = orderRepo;
        this.outboxRepo = outboxRepo;
    }

    @Transactional
    public Order createOrder(OrderDTO orderDTO) {
        Order order = Order.builder()
                .name(orderDTO.name())
                .price(orderDTO.price())
                .customerID(orderDTO.customerID())
                .orderDate(orderDTO.orderDate())
                .build();

        order = orderRepo.save(order);

        Outbox outbox = Outbox.builder()
                .id(order.getId())
                .aggregateId(order.getCustomerID())
                .payload(new ObjectMapper().writeValueAsString(order))
                .createdAt(new Date())
                .isProcessed(false)
                .build();

        outboxRepo.save(outbox);

        return  order;

    }
}
