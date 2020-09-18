package byzas.libs.flow.manager.async.config.kafka.props;

import lombok.Getter;
import lombok.Setter;
import org.springframework.context.annotation.Profile;

import java.util.Map;

@Getter
@Setter
public class Producer {
    private String nextTopic;
    private String nextDataClass;
    private Map<String,Object> props;
}