package byzas.libs.flow.manager.async.model.event;

import byzas.libs.flow.manager.async.config.kafka.props.EventsConfig;
import byzas.libs.flow.manager.async.config.kafka.props.Step;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.kafka.support.serializer.DeserializationException;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer2;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.util.*;

import static java.util.stream.Collectors.toList;

@Builder
@Getter
@Log4j2
public class Execution<T, V> {
    private List<ConsumerRecord<String, EventDto<T>>> polledData;
    private Step step;
    private String topic;
    private Map<String, EventStateDto> state;
    private List<EventDto<T>> toBeProcessed;
    private Map<EventDto<T>, V> processed;
    private Map<EventDto<T>, Throwable> processFailed;
    private Map<EventDto<T>, Throwable> exhausted;

    private long timeout;

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("\npolled : %s", polledData.stream().map(ConsumerRecord::value).collect(toList())));
        sb.append(String.format("\nstep : %s", step));
        Optional.ofNullable(topic).ifPresent(t -> sb.append(String.format("\ntopic : %s", t)));
        Optional.ofNullable(state).ifPresent(t -> sb.append(String.format("\nstate : %s", t)));
        Optional.ofNullable(toBeProcessed).ifPresent(t -> sb.append(String.format("\ntoBeProcessed : %s", t)));
        Optional.ofNullable(processed).ifPresent(t -> sb.append(String.format("\nprocessed : %s", t)));
        Optional.ofNullable(processFailed).ifPresent(t -> sb.append(String.format("\nprocessFailed : %s", t)));
        return sb.toString();
    }

    public boolean hasEventsFailed() {
        return processFailed.size() > 0;
    }

    public boolean hasInstantRetryExhausted() {
        return exhausted.size() > 0;
    }

    public boolean hasEventsProcessed() {
        return processed.size() > 0;
    }

    public boolean hasEventsToSend() {
        return processed.size() > 0;
    }

    public Execution<T, V> withProcessedAndFailedAndExhausted(Map<EventDto<T>, V> processed, Map<EventDto<T>, Throwable> failed,
                                                              Map<EventDto<T>, Throwable> exhausted) {
        return Execution.<T, V>builder()
                .polledData(getPolledData())
                .step(getStep())
                .topic(getTopic())
                .state(getState())
                .toBeProcessed(getToBeProcessed())
                .processed(processed)
                .processFailed(failed)
                .exhausted(exhausted)
                .build();
    }


    public Execution<T, V> withEventsToBeProcessed(List<EventDto<T>> toBeProcessed) {
        return Execution.<T, V>builder()
                .polledData(getPolledData())
                .step(getStep())
                .topic(getTopic())
                .state(getState())
                .toBeProcessed(toBeProcessed)
                .build();
    }

    public Execution<T, V> withState(Map<String, EventStateDto> stateIn) {
        return Execution.<T, V>builder()
                .polledData(getPolledData())
                .topic(getTopic())
                .step(getStep())
                .state(stateIn)
                .build();
    }

    // Events with the same key are merged into one (last one in order).
    // If key or value could not be deserialized, event logged and skipped.
    public static <T, V> Optional<Execution<T, V>> fromPolledData(EventsConfig eventsConfig, List<ConsumerRecord<String, EventDto<T>>> polledData) {
        Map<String, ConsumerRecord<String, EventDto<T>>> polledDataMap = polledData
                .stream()
                .filter(cr -> {
                    if (cr.key() == null || cr.value() == null) {
                        try {
                            Header header = cr.headers().lastHeader(ErrorHandlingDeserializer2.VALUE_DESERIALIZER_EXCEPTION_HEADER);
                            if (Optional.ofNullable(header).isPresent()) {
                                DeserializationException ex = (DeserializationException) new ObjectInputStream(
                                        new ByteArrayInputStream(header.value())).readObject();
                                log.error("DeserializationException", ex);
                                log.error("Can not parse kafka data json, will be skipped : {}", new String(ex.getData()));
                            }
                        } catch (Exception e) {
                            log.error("Exception when getting kafka exception header", e);
                        }
                        return false;
                    }
                    return true;
                })
                .reduce(Collections.emptyMap(),
                        (acc, consumerRecord) -> {
                            Map<String, ConsumerRecord<String, EventDto<T>>> m1 = new HashMap<>();
                            m1.putAll(acc);
                            m1.put(consumerRecord.key(), consumerRecord);
                            return m1;
                        },
                        (acc1, acc2) -> {
                            Map<String, ConsumerRecord<String, EventDto<T>>> m2 = new HashMap<>();
                            m2.putAll(acc1);
                            m2.putAll(acc2);
                            return m2;
                        }
                );

        List<ConsumerRecord<String, EventDto<T>>> polledDataFiltered = new ArrayList<>(polledDataMap.values());

        if (polledDataFiltered.size() > 0) {
            ConsumerRecord<String, EventDto<T>> sampleConsumerRecord = polledDataFiltered.get(0);
            EventDto<T> sampleEvent = sampleConsumerRecord.value();
            String topic = sampleConsumerRecord.topic();
            Step step = eventsConfig.getEvents().get(sampleEvent.getName()).get(sampleEvent.getStep());
            return Optional.of(Execution.<T, V>builder()
                    .polledData(polledDataFiltered)
                    .step(step)
                    .topic(topic)
                    .timeout(step.getConsumer().getTimeoutMillis())
                    .build());
        }

        return Optional.empty();
    }
}