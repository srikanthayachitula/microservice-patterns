package com.example.orderpoller.service;

import MessagePublisher.MessagePublisher;
import com.example.orderpoller.entity.Outbox;
import com.example.orderpoller.repo.OutboxRepo;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.lang.reflect.Executable;
import java.util.Date;
import java.util.List;

@Service
@EnableScheduling
public class OrderPollerService {

    @Autowired
    private OutboxRepo outboxRepo;

    @Autowired
    private KafkaTemplate<String,String> kafkaTemplate;

    @Autowired
    private MessagePublisher messagePublisher;

    @Scheduled(fixedRate = 60000 )
    @Transactional
    public void pollOutboxAndPublish() {

        List<Outbox> outboxList = outboxRepo.findProcessedFalse();
        outboxList.forEach(outbox -> {
            try{
                messagePublisher.publish(outbox.getPayload());
                outbox.setIsProcessed(true);
                outboxRepo.save(outbox);
            }
            catch(Exception ex) {

            }
        });
    }
}
