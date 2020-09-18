package byzas.libs.flow.manager.async.service;

import byzas.libs.flow.manager.async.model.event.EventDto;
import byzas.libs.flow.manager.async.config.jpa.entity.FlowEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@Log4j2
@RequiredArgsConstructor
public class EventQueueService {
    private final KafkaTemplate<String, EventDto> customProducer;

    public <T> CompletableFuture<Void> sendToEventQueue(T request, FlowEntity flow, String transactionid, boolean predicate) {
        if (predicate) {
            return sendToEventQueue(request, flow, transactionid);
        } else {
            return CompletableFuture.allOf();
        }
    }

    public <T> CompletableFuture<Void> sendToEventQueueUnchecked(T request, FlowEntity flow, String transactionId) {
        EventDto event = EventDto.builder()
                .id(UUID.randomUUID().toString())
                .step(0)
                .data(request)
                .name(flow.getName())
                .flowId(flow.getId())
                .transactionId(transactionId)
                .build();
        return CompletableFuture.allOf(customProducer.send(flow.getTopic(), event.getId(), event).completable());
    }

    public <T> CompletableFuture<Void> sendToEventQueue(T request, FlowEntity flow, String transactionId) {
        return sendToEventQueueUnchecked(request, flow, transactionId)
                .thenApply(any -> {
                    return any;
                })
                .exceptionally(t -> {
                    log.error("Exception when sending request {} with eventName {} to topic {}", request,
                            flow.getName(), flow.getTopic());
                    return null;
                });
    }
}