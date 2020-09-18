package byzas.libs.flow.manager.async.model.event;

import byzas.libs.flow.manager.async.config.kafka.props.FixedBackoffMeta;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Builder
@Getter
@ToString
@JsonInclude(JsonInclude.Include.NON_ABSENT)
public class FixedBackoffDto implements BackoffDto {

    private FixedBackoffMeta fixedBackoffMeta;
    private long elapsedTimeMinutes;

    @Override
    public long nextBackOff() {
        if (this.elapsedTimeMinutes >= fixedBackoffMeta.getMaxElapsedTimeMinutes()) {
            return STOP;
        }
        this.elapsedTimeMinutes += fixedBackoffMeta.getIntervalMinutes();
        return fixedBackoffMeta.getIntervalMinutes();
    }
}