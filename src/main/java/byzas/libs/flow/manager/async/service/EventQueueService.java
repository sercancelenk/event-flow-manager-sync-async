package byzas.libs.flow.manager.async.service;

import byzas.libs.flow.manager.async.model.event.EventDto;
import byzas.libs.flow.manager.async.config.jpa.entity.EventEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
@Log4j2
@RequiredArgsConstructor
public class EventQueueService {
    private final KafkaTemplate<String, EventDto> customProducer;

    public <T> CompletableFuture<Void> sendToEventQueue(T request, EventEntity flow, String transactionid, boolean predicate) {
        if (predicate) {
            return sendToEventQueue(request, flow);
        } else {
            return CompletableFuture.allOf();
        }
    }

    public <T> CompletableFuture<Void> sendToEventQueueUnchecked(T request, EventEntity flow) {
        if(Optional.ofNullable(flow.getTransactionId()).isEmpty()) throw new RuntimeException("Flow transaction id cannot be null!");

        EventDto event = EventDto.builder()
                .id(flow.getTransactionId())
                .step(0)
                .data(request)
                .name(flow.getEventName())
                .flowId(flow.getId())
                .transactionId(flow.getTransactionId())
                .build();
        return CompletableFuture.allOf(customProducer.send(flow.getStartTopic(), event.getId(), event).completable());
    }

    public <T> CompletableFuture<Void> sendToEventQueue(T request, EventEntity event) {
        return sendToEventQueueUnchecked(request, event)
                .thenApply(any -> {
                    return any;
                })
                .exceptionally(t -> {
                    log.error("Exception when sending request {} with eventName {} to topic {}", request,
                            event.getEventName(), event.getStartTopic());
                    return null;
                });
    }
}