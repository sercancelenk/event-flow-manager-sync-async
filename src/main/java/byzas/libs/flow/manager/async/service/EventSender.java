package byzas.libs.flow.manager.async.service;

import byzas.libs.flow.manager.async.config.kafka.props.EventsConfig;
import byzas.libs.flow.manager.async.model.event.EventDto;
import byzas.libs.flow.manager.async.config.kafka.EventServiceKafkaConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@Log4j2
@RequiredArgsConstructor
public class EventSender {
    private final KafkaTemplate<String, EventDto> customProducer;
    private final EventsConfig eventsConfig;
    private final EventServiceKafkaConfig eventServiceKafkaConfig;

    public CompletableFuture<Void> send(String topic, EventDto event) {
        return customProducer.send(topic, event.getId(), event).completable().thenApply(any -> null);
    }

    public CompletableFuture<Void> send(EventDto event) {
        String topic = eventsConfig.getEvents().get(event.getName()).get(event.getStep()).getConsumer().getTopic();
        KafkaTemplate<String, EventDto> template = eventServiceKafkaConfig.getProducer(event.getName(), event.getStep());
        return template.send(topic, event.getId(), event).completable().thenApply(any -> null);
    }

    public CompletableFuture<Void> sendAll(List<EventDto> events) {
        return CompletableFuture.allOf(events.stream().map(this::send).toArray(CompletableFuture[]::new));
    }
}