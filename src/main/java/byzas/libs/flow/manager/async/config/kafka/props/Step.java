package byzas.libs.flow.manager.async.config.kafka.props;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_ABSENT)
public class Step {
    private int id;
    private boolean traceDb;
    private boolean scheduledRetry;
    private ExponentialBackoffMeta scheduledRetryExponentialBackoffMeta;
    private FixedBackoffMeta scheduledRetryFixedBackoffMeta;
    private Consumer consumer;
    private Producer producer;
}