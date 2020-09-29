package byzas.libs.flow.manager.async.config.kafka.props;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@ToString
public class ExponentialBackoffMeta {
    private long intervalMinutes;
    private long maxIntervalMinutes;
    private double multiplier;
    private long maxElapsedTimeMinutes;
}
