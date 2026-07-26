package Config;

import lombok.Getter;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.convert.ValueConverter;

@Configuration
@Getter
public class KafkaConfig {
    @Value("${order.main.poller.topic}")
    private String topicName;

    @Bean
    protected NewTopic createNewTopic() {
        return new NewTopic(topicName, 3, (short)1);
    }
}
