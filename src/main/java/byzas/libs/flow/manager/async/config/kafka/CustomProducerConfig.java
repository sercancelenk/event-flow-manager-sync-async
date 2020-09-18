package byzas.libs.flow.manager.async.config.kafka;

import byzas.libs.flow.manager.async.model.event.EventDto;
import byzas.libs.flow.manager.async.config.kafka.props.CustomProducerDefaultPropertyConfig;
import byzas.libs.flow.manager.async.config.kafka.props.CustomProducerPropertyConfig;
import byzas.libs.flow.manager.util.extensions.MapSupport;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;

@Configuration
@EnableKafka
@RequiredArgsConstructor
@Log4j2
public class CustomProducerConfig implements MapSupport {
    private final CustomProducerPropertyConfig kafkaCustomProducerPropertyConfig;
    private final CustomProducerDefaultPropertyConfig kafkaCustomProducerDefaultPropertyConfig;

    @Bean("customProducer")
    public KafkaTemplate<String, EventDto> customProducer() {

        DefaultKafkaProducerFactory<String, EventDto> kafkaProducerFactory =
                new DefaultKafkaProducerFactory<>(mergeKafkaProps(kafkaCustomProducerPropertyConfig.getProps(), kafkaCustomProducerDefaultPropertyConfig.getProps()));
        return new KafkaTemplate<>(kafkaProducerFactory);
    }
}