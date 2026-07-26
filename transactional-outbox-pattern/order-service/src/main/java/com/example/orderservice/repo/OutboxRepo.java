package com.example.orderservice.repo;

import com.example.orderservice.entity.Outbox;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxRepo extends JpaRepository<Outbox, Long> {
}
