package byzas.libs.flow.manager.async.service;

import byzas.libs.flow.manager.async.config.redis.RedisCacheService;
import byzas.libs.flow.manager.async.model.event.EventDto;
import byzas.libs.flow.manager.async.model.event.EventStateDto;
import byzas.libs.flow.manager.async.model.event.Execution;
import byzas.libs.flow.manager.async.model.exception.ExecutionFailedException;
import byzas.libs.flow.manager.util.extensions.JsonSupport;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Functions;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toMap;

@RequiredArgsConstructor
@Service
@Log4j2
public class EventExecutionStateService<T, V> implements JsonSupport {

    private final RedisCacheService cacheService;
    private final ObjectMapper objectMapper;

    public CompletableFuture<Execution<T, V>> getEventExecutionStates(Execution<T, V> execution) {
        List<ConsumerRecord<String, EventDto<T>>> data = execution.getPolledData();
        List<String> keys = data.stream().map(cr -> getEventStateKey(execution.getTopic(), cr.value().getId())).collect(toCollection(ArrayList::new));
        log.debug("EventState keys : {}", keys);
        return cacheService.multiGet(keys)
                .handle((r, t) -> {
                    Optional<Throwable> mayBeException = Optional.ofNullable(t);
                    if (mayBeException.isPresent()) {
                        throw new ExecutionFailedException(execution, mayBeException.get());
                    }
                    return r;
                })
                .thenApply(states -> {
                    Map<String, EventStateDto> stateMap = states.stream()
                            .filter(j -> !StringUtils.isEmpty(j))
                            .map(j -> fromJson(objectMapper, EventStateDto.class, j))
                            .collect(toMap(EventStateDto::getEventId, Functions.identity()));
                    log.debug("Event States : {}", states);
                    return execution.withState(stateMap);
                });
    }

    public CompletableFuture<Execution<T, V>> saveEventExecutionStates(Execution<T, V> execution) {
        CompletableFuture<Void> saveForFailure = saveFailedState(execution);
        CompletableFuture<Void> saveForSuccess = saveSuccessState(execution);
        return saveForSuccess.thenCompose(any -> saveForFailure)
                .thenApply(any -> execution);
    }

    private CompletableFuture<Void> saveSuccessState(Execution<T, V> execution) {
        if (execution.hasEventsProcessed()) {
            Map<EventDto<T>, V> processed = execution.getProcessed();
            Map<String, String> stateForSuccess = processed.entrySet().stream()
                    .collect(toMap(entry -> getEventStateKey(execution.getTopic(), entry.getKey().getId()), entry ->
                            asJson(objectMapper, EventStateDto.builder()
                                    .eventId(entry.getKey().getId())
                                    .eventName(entry.getKey().getName()).step(entry.getKey().getStep())
                                    .build())));
            log.debug("Saving state success : {}", stateForSuccess);
            return cacheService.multiSetWithExpire(stateForSuccess, Duration.ofMinutes(execution.getStep().getConsumer().getStateExpirationMinutes()));
        }
        return CompletableFuture.completedFuture(null);
    }

    private CompletableFuture<Void> saveFailedState(Execution<T, V> execution) {
        int stateEpirationMinutes = execution.getStep().getConsumer().getStateExpirationMinutes();
        CompletableFuture<Void> result = CompletableFuture.allOf();
        if (execution.hasEventsFailed()) {
            Map<String, String> stateForFailure = execution.getProcessFailed().keySet().stream()
                    .collect(toMap(event -> getEventStateKey(execution.getTopic(), event.getId()), event ->
                            asJson(objectMapper, EventStateDto.builder().eventId(event.getId())
                                    .eventName(event.getName()).step(event.getStep())
                                    .retryCount(incrementAndGetRetryCount(execution, event))
                                    .build())
                    ));
            result = result
                    .thenCompose(any -> {
                        log.debug("Saving state ForFailure : {}", stateForFailure);
                        return cacheService.multiSetWithExpire(stateForFailure, Duration.ofMinutes(stateEpirationMinutes));
                    });
        }
        return result;
    }

    public Integer incrementAndGetRetryCount(Execution<T, V> execution, EventDto<T> event) {
        return ofNullable(execution.getState()
                .get(event.getId())).map(eventState -> eventState.getRetryCount() + 1)
                .orElse(1);
    }


    private String getEventStateKey(String topic, String eventId) {
        return String.format("%s.%s", topic, eventId);
    }


}
