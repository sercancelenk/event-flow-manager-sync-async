package byzas.libs.flow.manager.async.model.event;

import byzas.libs.flow.manager.async.config.kafka.props.ExponentialBackoffMeta;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;

@Builder
@Getter
@ToString
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@Log4j2
public class ExponentialBackoffDto implements BackoffDto {

    private ExponentialBackoffMeta exponentialBackoffMeta;
    @Builder.Default
    private long currentIntervalMinutes = -1L;
    private long currentElapsedTimeMinutes;

    @Override
    public long nextBackOff() {
        log.debug("meta, intervalMinutes : {}, maxIntervalMinutes : {}, multiplier : {}, " +
                        "maxElapsedTimeMinutes : {}, currentIntervalMinutes : {}, currentElapsedTimeMinutes : {}",
                exponentialBackoffMeta.getIntervalMinutes(), exponentialBackoffMeta.getMaxIntervalMinutes(),
                exponentialBackoffMeta.getMultiplier(), exponentialBackoffMeta.getMaxElapsedTimeMinutes(),
                getCurrentIntervalMinutes(), getCurrentElapsedTimeMinutes());
        if (this.currentElapsedTimeMinutes >= exponentialBackoffMeta.getMaxElapsedTimeMinutes()) {
            log.debug("Stopped");
            return STOP;
        }
        long nextInterval = computeNextInterval();
        this.currentElapsedTimeMinutes += nextInterval;
        return nextInterval;
    }

    private long computeNextInterval() {
        long maxInterval = exponentialBackoffMeta.getMaxIntervalMinutes();
        if (this.currentIntervalMinutes >= maxInterval) {
            return maxInterval;
        } else if (this.currentIntervalMinutes < 0) {
            long initialInterval = exponentialBackoffMeta.getIntervalMinutes();
            this.currentIntervalMinutes = (initialInterval < maxInterval
                    ? initialInterval : maxInterval);
        } else {
            this.currentIntervalMinutes = multiplyInterval(maxInterval);
        }
        return this.currentIntervalMinutes;
    }

    private long multiplyInterval(long maxInterval) {
        long i = this.currentIntervalMinutes;
        i *= exponentialBackoffMeta.getMultiplier();
        log.debug("multiply interval, i : {}, maxInterval : {}", i, maxInterval);
        return (i > maxInterval ? maxInterval : i);
    }
}