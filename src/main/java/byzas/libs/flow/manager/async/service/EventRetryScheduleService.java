package byzas.libs.flow.manager.async.service;

import byzas.libs.flow.manager.async.config.executors.CustomThreadPoolExecutor;
import byzas.libs.flow.manager.async.config.executors.ExecutorsConfig;
import byzas.libs.flow.manager.async.config.kafka.props.EventsConfig;
import byzas.libs.flow.manager.async.config.kafka.props.ExponentialBackoffMeta;
import byzas.libs.flow.manager.async.config.kafka.props.Step;
import byzas.libs.flow.manager.async.config.redis.RedisCacheService;
import byzas.libs.flow.manager.async.model.event.*;
import byzas.libs.flow.manager.async.model.exception.ExhaustedAndRetryDisableException;
import byzas.libs.flow.manager.util.extensions.JsonSupport;
import byzas.libs.flow.manager.util.extensions.SchedulerSupport;
import byzas.libs.flow.manager.util.schedule.EventScheduledJob;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.logging.log4j.Logger;
import org.quartz.Scheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
@Log4j2
public class EventRetryScheduleService<T, V> implements JsonSupport, SchedulerSupport {
    private final RedisCacheService cacheService;
    private final ObjectMapper objectMapper;

    @Autowired
    @Qualifier(ExecutorsConfig.DEFAULT_SCHEDULE_OPERATIONS_EXECUTOR_BEAN)
    private CustomThreadPoolExecutor scheduleOperationsExecutor;

    private final Scheduler clusteredScheduler;
    private final Environment environment;
    private boolean isQuartzJdbcSupportEnabled = false;
    private final KafkaTemplate<String, EventDto> customProducer;
    private final EventsConfig eventsConfig;

    private static final String QUARTZ_JDBC_PROFILE = "quartz-jdbc";

    BiFunction<Optional<String>, EventDto<T>, CompletableFuture<Void>> sendToExhaustedQueueIfQueueNameExistsFunc = (exhaustedQueueName, evntDto) -> {
        if (exhaustedQueueName.isPresent()) {
            String queueName = exhaustedQueueName.get();
            return sendQueue(
                    evntDto,
                    queueName, 0).thenApply(any -> (Void) null);
        }
        return CompletableFuture.allOf();
    };

    public CompletableFuture<Execution> scheduleIfExhaustedOrSendToInstantExhaustedQueue(Execution<T, V> execution) {
        Step step = execution.getStep();
        CompletableFuture<Void> result = CompletableFuture.allOf();
        if (step.isScheduledRetry()) {
            if (execution.getExhausted().size() > 0) {
                log.debug("[RETRY_SCHEDULE] Schedule retry is enable");
                Optional<ExponentialBackoffMeta> mayBeExponentialRetryMeta = Optional.ofNullable(step.getScheduledRetryExponentialBackoffMeta());
                boolean isExponentionalRetry = mayBeExponentialRetryMeta.isPresent();
                List<CompletableFuture<Void>> scheduleResults = execution.getExhausted().keySet().stream()
                        .map(eventDto -> {
                            CompletableFuture<String> backoffJson = cacheService.get(String.format("backoff.%s", eventDto.getId()));
                            CompletableFuture<BackoffDto> backoffDtoFuture = backoffJson.thenApply(json -> {
                                log.debug("[RETRY_SCHEDULE] isEponentialRetry : {},Json from backoff : {}", isExponentionalRetry, json);
                                if (Optional.ofNullable(json).isPresent()) {
                                    return isExponentionalRetry ? (fromJson(objectMapper, ExponentialBackoffDto.class, json))
                                            : (fromJson(objectMapper, FixedBackoffDto.class, json));
                                } else {
                                    return isExponentionalRetry ? ExponentialBackoffDto.builder()
                                            .exponentialBackoffMeta(mayBeExponentialRetryMeta.get())
                                            .build() :
                                            FixedBackoffDto.builder().fixedBackoffMeta(step.getScheduledRetryFixedBackoffMeta()).build();

                                }
                            });
                            return backoffDtoFuture.thenCompose(backoffDto -> {
                                long currentTime = System.currentTimeMillis();
                                String randomUUID = UUID.randomUUID().toString();
                                String jobName = String.format("job_%s_%s", eventDto.getId(), randomUUID);
                                String jobGroupName = String.format("jobGroup_%s_%s", eventDto.getId(), randomUUID);
                                long nextBackOff = backoffDto.nextBackOff();
                                if (nextBackOff != BackoffDto.STOP) {
                                    Throwable t = execution.getExhausted().get(eventDto);
                                    if (t instanceof ExhaustedAndRetryDisableException) {
                                        ExhaustedAndRetryDisableException exhaustedAndRetryDisableException = (ExhaustedAndRetryDisableException) t;

                                        if (BooleanUtils.isTrue(exhaustedAndRetryDisableException.getCancelScheduleRetry())) {
                                            log.info("[RETRY_SCHEDULE] Scheduling retry is cancelled by user circumstances: topic= {}, eventDto={}", step.getConsumer().getTopic(), eventDto);
                                            return CompletableFuture.allOf();
                                        }

                                    }

                                    Date eventScheduledTime = new Date(currentTime + nextBackOff * 60 * 1000);
                                    String cron = generateCronExpression(eventScheduledTime);
                                    EventDto<T> eventDtoWithScheduledtime = eventDto.withScheduleTime(eventScheduledTime);
                                    Map<String, String> context = new HashMap<>();
                                    context.put("event", asJson(objectMapper, eventDtoWithScheduledtime));

                                    return CompletableFuture.supplyAsync(() -> {
                                        log.debug("[RETRY_SCHEDULE] Event is scheduling to next time {} , event : {}", eventScheduledTime, eventDto.getId());
                                        schedule(jobName, jobGroupName, cron, EventScheduledJob.class, context);
                                        return true;
                                    }, scheduleOperationsExecutor)
                                            .thenCompose(scheduleResult -> {
                                                // Notifying graceperiod scheduled time
                                                if (BooleanUtils.isTrue(scheduleResult)) {
                                                    String scheduledNotifyQueue = step.getConsumer().getScheduledNotifyQueue();
                                                    if (Optional.ofNullable(scheduledNotifyQueue).isPresent()) {
                                                        return sendQueue(eventDtoWithScheduledtime, scheduledNotifyQueue, 0);
                                                    }
                                                }
                                                return CompletableFuture.completedFuture(false);
                                            })
                                            .thenCompose(eventDtoFuture -> cacheService
                                                    .setWithExpire(String.format("backoff.%s", eventDtoWithScheduledtime.getId()), asJson(objectMapper, backoffDto), Duration.ofDays(60)))
                                            .thenCompose(any -> cacheService.remove(String.format("%s.%s", step.getConsumer().getTopic(), eventDto.getId())));
                                } else {
                                    log.debug("[RETRY_SCHEDULE] Scheduling event stopped!, Backoff stopped. Event : {}", eventDto.getId());
                                    Throwable t = execution.getExhausted().get(eventDto);
                                    if (t instanceof ExhaustedAndRetryDisableException) {
                                        ExhaustedAndRetryDisableException exhaustedAndRetryDisableException = (ExhaustedAndRetryDisableException) t;

                                        if (BooleanUtils.isTrue(exhaustedAndRetryDisableException.getLogFatalInScheduledRetriesEnd())) {
                                            log.fatal("[RETRY_SCHEDULE] EventService Schedule Retry Alarm: topic= {}, eventDto={}", step.getConsumer().getTopic(), eventDto);
                                        }

                                        if (BooleanUtils.isTrue(exhaustedAndRetryDisableException.getDoActionInScheduledRetriesEnd())) {
                                            return sendToExhaustedQueueIfQueueNameExistsFunc.apply(Optional.ofNullable(step.getConsumer().getScheduledRetryExhaustedQueueEventName()), eventDto);
                                        }
                                    } else {
                                        return sendToExhaustedQueueIfQueueNameExistsFunc.apply(Optional.ofNullable(step.getConsumer().getScheduledRetryExhaustedQueueEventName()), eventDto);
                                    }

                                    return CompletableFuture.allOf();
                                }
                            }).exceptionally(t -> {
                                log.error("[RETRY_SCHEDULE] Exception when scheduling event retry : {}", eventDto, t);
                                return null;
                            });
                        })
                        .collect(Collectors.toList());

                result = CompletableFuture.allOf(scheduleResults.toArray(new CompletableFuture[0]));
            }
        } else {
            if (execution.getExhausted().size() > 0) {
                List<CompletableFuture<Void>> sendQueueResults = execution.getExhausted().keySet().stream()
                        .map(eventDto -> {
                            Optional<String> maybeInstantRetryExhaustedQueueEventName = Optional.ofNullable(step.getConsumer().getInstantRetryExhaustedQueueEventName());
                            Throwable t = execution.getExhausted().get(eventDto);

                            if (t instanceof ExhaustedAndRetryDisableException) {
                                ExhaustedAndRetryDisableException exhaustedAndRetryDisableException = (ExhaustedAndRetryDisableException) t;
                                if (BooleanUtils.isTrue(exhaustedAndRetryDisableException.getLogFatalInInstantRetriesEnd())) {
                                    log.fatal("[RETRY_SCHEDULE] EventService Instant Reply Alarm: topic= {}, eventDto={}", step.getConsumer().getTopic(), eventDto);
                                }

                                if (BooleanUtils.isTrue(exhaustedAndRetryDisableException.getDoActionInInstantRetriesEnd())) {
                                    return sendToExhaustedQueueIfQueueNameExistsFunc.apply(maybeInstantRetryExhaustedQueueEventName, eventDto);
                                }
                            } else {
                                return sendToExhaustedQueueIfQueueNameExistsFunc.apply(maybeInstantRetryExhaustedQueueEventName, eventDto);
                            }

                            return CompletableFuture.allOf();
                        })
                        .collect(Collectors.toList());

                result = CompletableFuture.allOf(sendQueueResults.toArray(new CompletableFuture[0]));
            }
        }

        return result.thenApply(any -> execution);
    }

    public CompletableFuture<Boolean> sendQueue(EventDto<T> eventDto, String eventName, int step) {
        String zeroTopic = eventsConfig.getEvents().get(eventName).get(step).getConsumer().getTopic();
        EventDto<T> eventDtoForScheduledRetryExhaustedQueue =
                EventDto.<T>builder()
                        .appId(eventDto.getAppId())
                        .data(eventDto.getData())
                        .id(UUID.randomUUID().toString())
                        .name(eventName)
                        .step(step)
                        .transactionId(eventDto.getTransactionId())
                        .build();
        log.debug("Sending eventdto to event {}, topic: {}", eventName, zeroTopic);
        return CompletableFuture
                .allOf(customProducer.send(zeroTopic, eventDto.getId(), eventDtoForScheduledRetryExhaustedQueue)
                        .completable()
                        .exceptionally(ex -> {
                            log.fatal("Event Retry Schedule Service Sending To Queue Alarm: event: {}, topic: {}", eventName, zeroTopic, ex);
                            return null;
                        }))
                .thenApply(any -> true);
    }

    @Override
    public Scheduler getScheduler() {
        return this.clusteredScheduler;
    }

    @Override
    public Logger getLogger() {
        return log;
    }
}