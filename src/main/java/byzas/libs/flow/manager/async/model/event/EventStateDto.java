package byzas.libs.flow.manager.async.model.event;

import byzas.libs.flow.manager.util.serializer.EventStateDeserializer;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Builder
@Getter
@ToString
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonDeserialize(using = EventStateDeserializer.class)
public class EventStateDto {
    private String eventName;
    private int step;
    private String eventId;
    private int retryCount;
    @Builder.Default
    private boolean processed = false;


    public boolean canRetry(int maxRetry) {
        return !processed && (retryCount < maxRetry);
    }

    public EventStateDto withRetryCount(int retryCount) {
        return EventStateDto.builder()
                .eventId(getEventId())
                .eventName(getEventName())
                .retryCount(retryCount)
                .step(getStep())
                .processed(isProcessed())
                .build();
    }

}