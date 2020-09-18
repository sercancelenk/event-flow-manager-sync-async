package byzas.libs.flow.manager.async.config.kafka.props;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.context.annotation.Profile;

import java.util.Map;

@Getter
@Setter
@RequiredArgsConstructor
public class Consumer {
    private String topic;
    private String dataClass;
    private String eventListenerBeanName;
    private Map<String,Object> props;
    private int concurrency;
    private int retryCount;
    private int timeoutMillis;
    private int stateExpirationMinutes;
    private int syncCommitTimeoutSecond;
    private boolean syncCommit;
    private long backoffIntervalMillis;
    private String scheduledRetryExhaustedQueueEventName;
    private String scheduledNotifyQueue;
    private String instantRetryExhaustedQueueEventName;
}