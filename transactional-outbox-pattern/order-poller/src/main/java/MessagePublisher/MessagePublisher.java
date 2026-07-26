package MessagePublisher;

import Config.KafkaConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class MessagePublisher {

    @Autowired
    private KafkaTemplate<String,String> kafkaTemplate;

    @Autowired
    private KafkaConfig kafkaConfig;


    public void publish(String payload) {
        kafkaTemplate.send(kafkaConfig.getTopicName(), payload);
    }
}
