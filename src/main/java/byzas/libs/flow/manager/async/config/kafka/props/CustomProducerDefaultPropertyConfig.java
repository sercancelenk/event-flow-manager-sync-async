package byzas.libs.flow.manager.async.config.kafka.props;

import byzas.libs.flow.manager.util.YamlPropertySourceFactory;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Getter
@Setter
@ConfigurationProperties(prefix = "custom-producer-default")
@PropertySource(value = "classpath:event-handler-kafka-defaults.yml", factory = YamlPropertySourceFactory.class)
public class CustomProducerDefaultPropertyConfig {
    Map<String, Object> props;
}
