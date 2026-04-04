package infrastructure.adapter.in.kafka;

import infrastructure.model.ScenarioRequest;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

@ConditionalOnProperty(name = "spring.kafka.bootstrap-servers")
@Configuration
public class KafkaConfig {

    @Bean
    public ConsumerFactory<String, ScenarioRequest> scenarioConsumerFactory(KafkaProperties props) {
        return new DefaultKafkaConsumerFactory<>(
                props.buildConsumerProperties(null),
                new StringDeserializer(),
                new JsonDeserializer<>(ScenarioRequest.class, false));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, ScenarioRequest> scenarioListenerContainerFactory(
            ConsumerFactory<String, ScenarioRequest> consumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, ScenarioRequest> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        return factory;
    }
}

