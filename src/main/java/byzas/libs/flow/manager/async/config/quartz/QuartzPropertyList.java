package byzas.libs.flow.manager.async.config.quartz;

import byzas.libs.flow.manager.util.YamlPropertySourceFactory;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "quartz")
@Getter
@Setter
@PropertySource(value = "classpath:event-handler-quartz-config.yml", factory = YamlPropertySourceFactory.class)
public class QuartzPropertyList {
    private Map<String, String> properties;
}