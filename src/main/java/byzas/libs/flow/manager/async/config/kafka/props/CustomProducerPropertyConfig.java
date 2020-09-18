package byzas.libs.flow.manager.async.config.kafka.props;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Getter
@Setter
@ConfigurationProperties(prefix = "custom-producer")
public class CustomProducerPropertyConfig {
    Map<String, Object> props;
}