package byzas.libs.flow.manager.async.service;

import byzas.libs.flow.manager.async.config.kafka.EventServiceKafkaConfig;
import byzas.libs.flow.manager.async.config.kafka.props.EventsConfig;
import byzas.libs.flow.manager.async.config.kafka.props.Step;
import byzas.libs.flow.manager.async.model.event.EventDto;
import byzas.libs.flow.manager.async.model.event.EventStateDto;
import byzas.libs.flow.manager.async.model.exception.ExecutionFailedException;
import byzas.libs.flow.manager.async.model.exception.ExhaustedAndRetryDisableException;
import byzas.libs.flow.manager.async.model.event.Execution;
import byzas.libs.flow.manager.util.SpringContext;
import byzas.libs.flow.manager.util.extensions.JsonSupport;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.BatchAcknowledgingConsumerAwareMessageListener;
import org.springframework.kafka.support.Acknowledgment;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

@Log4j2
// todo exponential retry - currentTimeElapsed must be double not long. multiplier 1.5 has the same affect with multipler 1.
public abstract class EventFlowListener<T, V>
        implements BatchAcknowledgingConsumerAwareMessageListener<String, EventDto<T>>, JsonSupport {

    @Autowired
    private EventExecutionStateService<T, V> eventExecutionStateService;
    @Autowired
    private EventPublishService<T, V> eventPublishService;
    @Autowired
    private EventRetryScheduleService<T, V> eventRetryScheduleService;
    @Autowired
    private EventsConfig eventsConfig;
    @Autowired
    private EventServiceKafkaConfig eventServiceKafkaConfig;
    @Autowired
    private HealthCheckMonitorService healthCheckMonitorService;

    protected EventFlowListener() {
    }

    public abstract CompletableFuture<V> process(EventDto<T> data);

    public void onMessage(List<ConsumerRecord<String, EventDto<T>>> data, Acknowledgment acknowledgment, Consumer<?, ?> consumer) {
        Optional<Execution<T, V>> mayBeExecution = Execution.fromPolledData(eventsConfig, data);
        if (!mayBeExecution.isPresent()) {
            return;
        }
        Execution<T, V> execution = mayBeExecution.get();
        try {
            // if healthCheck || getEventExecutionStates fails,
            // the whole batch is replayed (ExecutionFailedException is thrown)
            checkSystemHealth(execution)
                    .thenCompose(any -> eventExecutionStateService.getEventExecutionStates(execution))
                    .thenCompose(this::filter)
                    .thenCompose(this::process)
                    // if anything fails after this point, the batch is not replayed to avoid any inconsistency.
                    .thenCompose(this::sendNextQueue)
                    .thenCompose(eventExecutionStateService::saveEventExecutionStates)
                    .thenCompose(this::traceToDB)
                    .thenCompose(eventRetryScheduleService::scheduleIfExhaustedOrSendToInstantExhaustedQueue)
                    .thenAccept(this::replayForFailedEvents)
                    .get(execution.getTimeout(), TimeUnit.MILLISECONDS);
            acknowledgment.acknowledge();
        } catch (ExecutionException e) {
            // Replay only if ExecutionFailedException is thrown.
            triggerReplayIfFailed(execution, acknowledgment, e);
        } catch (Exception e) {
            // Log, do not replay!
            logErrorAndAcknowledge(execution, acknowledgment, e);
        }
    }

    private CompletableFuture<Execution<T, V>> checkSystemHealth(Execution<T, V> execution) {
        return healthCheckMonitorService.checkSystemHealth()
                .handle((any, t) -> {
                    if (Optional.ofNullable(t).isPresent()) {
                        throw new ExecutionFailedException(execution, t);
                    }
                    return execution;
                });
    }

    private CompletableFuture<Execution<T, V>> sendNextQueue(Execution<T, V> execution) {
        EventDto sampleEvent = execution.getPolledData().get(0).value();
        KafkaTemplate<String, EventDto> producer = eventServiceKafkaConfig.getProducer(sampleEvent.getName(), sampleEvent.getStep());
        return eventPublishService.sendNextQueue(producer, execution);
    }

    // filter :
    // pass => "events coming for the first time" + "processFailed events of which retry count < max_retry"
    // do not pass => "processed events came twice or more" + "processFailed events of which retry counts exhausted"
    // todo log when event is discarded due to a. processed already b. retry > max retry
    private CompletableFuture<Execution<T, V>> filter(Execution<T, V> execution) {
        List<EventDto<T>> eventsToBeProcessed = execution.getPolledData().stream().map(ConsumerRecord::value).filter(event -> {
            Optional<EventStateDto> state = ofNullable(execution.getState().get(event.getId()));
            return state.map(s -> {
                boolean canRetry = s.canRetry(execution.getStep().getConsumer().getRetryCount());
                log.debug("EventState, canRetry : {}, state : {}", canRetry, s);
                return canRetry;
            }).orElse(true);
        }).collect(toList());

        return CompletableFuture.completedFuture(execution.withEventsToBeProcessed(eventsToBeProcessed));
    }

    private CompletableFuture<Execution<T, V>> process(Execution<T, V> execution) {
        Map<EventDto<T>, CompletableFuture<V>> processResults =
                execution.getToBeProcessed().stream()
                        .collect(toMap(Function.identity(), item -> {
                            try {
                                log.debug("Processing event {} -> {}", item.getName(), item);
                                return process(item);
                            } catch (Exception e) {
                                log.error("Exception when processing event : {}", item, e);
                                CompletableFuture<V> completableFuture = new CompletableFuture<>();
                                completableFuture.completeExceptionally(e);
                                return completableFuture;
                            }
                        }));

        CompletableFuture<Void> allFutures = CompletableFuture.allOf(processResults.values()
                .toArray(new CompletableFuture[0]))
                .exceptionally(t -> null);

        return allFutures.thenApply(any -> {
            Map<EventDto<T>, V> processed = processResults.entrySet().stream()
                    .filter(entry -> !entry.getValue().isCompletedExceptionally())
                    .collect(HashMap::new, (m, v) -> m.put(v.getKey(), v.getValue().join()), HashMap::putAll);

            Map<EventDto<T>, Throwable> failed = processResults.entrySet().stream()
                    .filter(entry -> entry.getValue().isCompletedExceptionally())
                    .collect(HashMap::new, (m, v) -> {
                        EventDto<T> event = v.getKey();
                        try {
                            v.getValue().get(); // this will throw ExecutionException
                        } catch (ExhaustedAndRetryDisableException cire) {
                            log.error("CancelInstantReplayException occured while processing event. Event will not instant replay. Message: {}, Cause: {}", cire.getMessage(), cire.getCause(), cire);
                            m.put(event, cire);
                        } catch (ExecutionException e) {
                            log.error("Error occured while processing event. Message: {}, Cause: {}", e.getMessage(), e.getCause(), e);
                            m.put(event, e.getCause());
                        } catch (InterruptedException i) {
                            log.error("Interrupted when accumulating errors in process", i);
                        }
                    }, HashMap::putAll);

            Map<EventDto<T>, Throwable> exhausted = failed.entrySet().stream()
                    .filter(entry -> {
                        EventDto<T> event = entry.getKey();
                        Throwable t = entry.getValue();
                        int maxRetryCount = execution.getStep().getConsumer().getRetryCount();
                        int retryCount;
                        if (t instanceof ExhaustedAndRetryDisableException) {
                            ExhaustedAndRetryDisableException exhaustedAndRetryDisableException = (ExhaustedAndRetryDisableException) t;
                            if (BooleanUtils.isTrue(exhaustedAndRetryDisableException.getCancelInstantRetry())) {
                                // set retry count == max count for further state redis save
                                retryCount = maxRetryCount;
                                EventStateDto eventState = ofNullable(execution.getState().get(event.getId()))
                                        .map(es -> es.withRetryCount(retryCount))
                                        .orElse(EventStateDto.<V>builder().eventName(event.getName()).step(event.getStep())
                                                .eventId(event.getId()).retryCount(retryCount).build());
                                execution.getState().put(event.getId(), eventState);
                            } else {
                                retryCount = eventExecutionStateService.incrementAndGetRetryCount(execution, event);
                            }
                        } else {
                            retryCount = eventExecutionStateService.incrementAndGetRetryCount(execution, event);
                        }
                        return retryCount == maxRetryCount;
                    }).collect(toMap(Map.Entry::getKey, Map.Entry::getValue));

            return execution.withProcessedAndFailedAndExhausted(processed, failed, exhausted);
        });
    }

    private CompletableFuture<Execution<T, V>> traceToDB(Execution<T, V> execution) {
        Step step = execution.getStep();
        CompletableFuture<Void> result = CompletableFuture.allOf();
        if (step.isTraceDb()) {
            log.debug("Before trace execution with topic : {} , step : {} ", execution.getTopic(),
                    execution.getStep());
            FlowTraceService<T, V> flowTraceService = (FlowTraceService<T, V>) SpringContext.applicationContext.getBean("flowTraceService");
            result = result.thenCompose(any -> flowTraceService.trace(execution));
        } else {
            log.debug("Trace execution with topic : {} , step : {} skipped because traceDB param is false", execution.getTopic(),
                    execution.getStep());
        }
        return result.thenApply(any -> execution);
    }

    private void triggerReplayIfFailed(Execution<T, V> execution, Acknowledgment acknowledgment, ExecutionException e) {
        Optional<Throwable> mayBeException = Optional.ofNullable(e.getCause());
        if (mayBeException.isPresent() && mayBeException.get() instanceof ExecutionFailedException) {
            ExecutionFailedException executionFailedException = (ExecutionFailedException) e.getCause();
            log.error("Temporary Error, batch will be polled again, execution : {}"
                    , executionFailedException.getExecution(), executionFailedException.getCause());
            throw executionFailedException;
        }
        logErrorAndAcknowledge(execution, acknowledgment, e);
    }

    private void logErrorAndAcknowledge(Execution<T, V> execution, Acknowledgment acknowledgment, Exception e) {
        log.error("Exception when processing polledData, won't be polled again, execution : {}", execution, e);
        acknowledgment.acknowledge();
    }

    private void replayForFailedEvents(Execution<T, V> execution) {
        if (execution.hasEventsFailed()) {
            throw new ExecutionFailedException(execution, new RuntimeException("There are failed events"));
        }
    }
}