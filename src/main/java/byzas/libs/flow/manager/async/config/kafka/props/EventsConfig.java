package byzas.libs.flow.manager.async.config.kafka.props;

import byzas.libs.flow.manager.util.YamlPropertySourceFactory;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@ConfigurationProperties(prefix = "event")
@Getter
@Setter
@RequiredArgsConstructor
@ToString
@PropertySource(value = "classpath:event-handler-kafka-defaults.yml", factory = YamlPropertySourceFactory.class)
public class EventsConfig {
    private Map<String, Map<Integer, Step>> events;
    private Map<String, Object> consumerPropsDefaults;
    private Map<String, Object> producerPropsDefaults;
}