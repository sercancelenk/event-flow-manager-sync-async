package byzas.libs.flow.manager.async.model.event;

import byzas.libs.flow.manager.util.serializer.EventDeserializer;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.*;
import java.util.stream.IntStream;

@Builder
@Getter
@ToString
@JsonDeserialize(using = EventDeserializer.class)
@JsonInclude(JsonInclude.Include.NON_ABSENT)
public class EventDto<T> {
    private String id;
    private String name;
    private int step;
    private T data;
    private Integer appId;
    private Date scheduleTime;
    private Long flowId;
    private String transactionId;

    public EventDto<T> withScheduleTime(Date scheduleTime) {
        return EventDto
                .<T>builder()
                .appId(getAppId())
                .data(getData())
                .scheduleTime(scheduleTime)
                .id(getId())
                .name(getName())
                .step(getStep())
                .flowId(getFlowId())
                .transactionId(getTransactionId())
                .build();
    }

    public List<EventDto<T>> cloneTest(int number) {
        return IntStream.range(0, number)
                .boxed()
                .reduce(new ArrayList<>(),
                        (acc, item) -> {
                            acc.add(EventDto.<T>builder().id(UUID.randomUUID().toString()).transactionId(transactionId).name(name).step(step).data(data).appId(appId).build());
                            return acc;
                        }, (a, b) -> a);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EventDto<?> eventDto = (EventDto<?>) o;
        return Objects.equals(getId(), eventDto.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }
}
