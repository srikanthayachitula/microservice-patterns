package com.example.orderpoller.repo;

import com.example.orderpoller.entity.Outbox;
import com.example.orderservice.entity.Outbox;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OutboxRepo extends JpaRepository<Outbox, Long> {
    List<Outbox> findProcessedFalse();
}
