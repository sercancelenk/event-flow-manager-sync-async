package byzas.libs.flow.manager.async.service;

import byzas.libs.flow.manager.async.config.kafka.props.EventsConfig;
import byzas.libs.flow.manager.async.model.event.EventDto;
import byzas.libs.flow.manager.async.model.event.Execution;
import byzas.libs.flow.manager.util.extensions.JsonSupport;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;

@RequiredArgsConstructor
@Service
@Log4j2
class EventPublishService<T, V> implements JsonSupport {

    private final EventsConfig eventsConfig;

    public CompletableFuture<Execution<T, V>> sendNextQueue(KafkaTemplate<String, EventDto> producer, Execution<T, V> execution) {
        boolean hasEventsToSend = execution.hasEventsToSend();
        if (hasEventsToSend) {
            boolean nextStepExits = ofNullable(producer).isPresent();
            if (nextStepExits) {
                List<EventDto> eventsToPublish = execution.getProcessed().entrySet().stream()
                        // null value means => do not send to next queue! STOP!
                        .filter(entry -> {
                            boolean isValueNull = false;
                            if (entry.getValue() == null) {
                                isValueNull = true;
                                log.debug("Skipping null value to publish to next queue , event : {}",
                                        entry.getKey());
                            }
                            return !isValueNull;
                        })
                        .map(entry -> {
                            EventDto event = entry.getKey();
                            return EventDto.<V>builder()
                                    .id(event.getId())
                                    .name(event.getName())
                                    .data(entry.getValue())
                                    .step(event.getStep() + 1)
                                    .scheduleTime(event.getScheduleTime())
                                    .transactionId(event.getTransactionId())
                                    .flowId(event.getFlowId())
                                    .build();
                        }).collect(toList());
                return sendAll(eventsToPublish, producer)
                        .thenApply(any -> execution)
                        .exceptionally(t -> {
                            log.fatal("Sending Polled Data To Next Queue Alarm : {}", execution, t);
                            return execution;
                        });
            }
        }
        return CompletableFuture.completedFuture(execution);
    }

    public CompletableFuture<Void> sendAll(List<EventDto> events, KafkaTemplate<String, EventDto> template) {
        events.stream().forEach(System.out::println);
        return CompletableFuture.allOf(events.stream().map(event ->
                send(event, template)).toArray(CompletableFuture[]::new));
    }

    private CompletableFuture<Void> send(EventDto event, KafkaTemplate<String, EventDto> template) {
        String topic = eventsConfig.getEvents().get(event.getName()).get(event.getStep() - 1).getProducer().getNextTopic();
        return template.send(topic, event.getId(), event).completable().thenApply(any -> null);
    }
}