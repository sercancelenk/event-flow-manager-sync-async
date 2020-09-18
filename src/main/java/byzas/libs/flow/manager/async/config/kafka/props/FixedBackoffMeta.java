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
@Builder
public class FixedBackoffMeta {
    private long intervalMinutes;
    private long maxElapsedTimeMinutes;
}