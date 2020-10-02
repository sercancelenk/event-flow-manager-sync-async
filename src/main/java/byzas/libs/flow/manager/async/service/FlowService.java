package byzas.libs.flow.manager.async.service;

import byzas.libs.flow.manager.async.config.executors.CustomThreadPoolExecutor;
import byzas.libs.flow.manager.async.config.executors.ExecutorsConfig;
import byzas.libs.flow.manager.async.config.jpa.entity.EventEntity;
import byzas.libs.flow.manager.async.config.jpa.entity.EventStepEntity;
import byzas.libs.flow.manager.async.config.jpa.entity.EventStepStatus;
import byzas.libs.flow.manager.async.config.jpa.repository.FlowRepository;
import byzas.libs.flow.manager.async.config.jpa.repository.FlowStepRepository;
import byzas.libs.flow.manager.async.config.kafka.props.EventsConfig;
import byzas.libs.flow.manager.async.config.kafka.props.Step;
import byzas.libs.flow.manager.async.model.exception.FlowAlreadyExistsException;
import byzas.libs.flow.manager.util.SpringContext;
import com.google.common.base.Functions;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@Log4j2
@RequiredArgsConstructor
public class FlowService {
    private final FlowRepository flowRepository;
    private final FlowStepRepository flowStepRepository;
    private final EventsConfig eventsConfig;
    private final EventQueueService eventQueueService;

    @Qualifier(ExecutorsConfig.DEFAULT_DB_OPERATIONS_EXECUTOR_BEAN)
    @Autowired
    private CustomThreadPoolExecutor sardisJpaDbOperationsExecutor;

    public CompletableFuture<Map<Long, EventEntity>> getFlows(List<Long> flowIds) {
        return CompletableFuture
                .supplyAsync(() -> flowRepository.findAllByIdIn(flowIds)
                        .stream().collect(Collectors.toMap(EventEntity::getId, Functions.identity(), (f1, f2) -> f1)), sardisJpaDbOperationsExecutor);
    }

    private CompletableFuture<List<EventEntity>> getFlowsBy(String key, String eventType) {
        return CompletableFuture.supplyAsync(() -> flowRepository.findAllByKeyAndEventType(key, eventType), sardisJpaDbOperationsExecutor);
    }

    private CompletableFuture<Void> canProcessFlow(String eventKey, String eventType) {
        return getFlowsBy(eventKey, eventType)
                .thenCompose(flowEntities -> {
                    CompletableFuture<Void> response = new CompletableFuture<>();
                    String[] beanNames = SpringContext.applicationContext.getBeanNamesForType(EventStartDecider.class);
                    if (beanNames.length > 0) {
                        EventStartDecider decider = SpringContext.applicationContext.getBean(beanNames[0], EventStartDecider.class);
                        if (!decider.decide(flowEntities)) {
                            response.completeExceptionally(new FlowAlreadyExistsException(eventKey));
                            return response;
                        }
                    }

                    response.complete(null);
                    return response;
                });
    }

    private CompletableFuture<EventEntity> saveFlow(EventEntity flow) {
        return canProcessFlow(flow.getKey(), flow.getEventType())
                .thenCompose(any -> CompletableFuture
                        .supplyAsync(() -> {
                            return flowRepository.save(flow);
                        }, sardisJpaDbOperationsExecutor));
    }

    private CompletableFuture<EventStepEntity> saveFlowStep(EventStepEntity flowStep) {
        return CompletableFuture
                .supplyAsync(() -> flowStepRepository.save(flowStep), sardisJpaDbOperationsExecutor);
    }

    protected CompletableFuture<Iterable<EventStepEntity>> saveFlowStepAll(List<EventStepEntity> flowSteps) {
        return CompletableFuture
                .supplyAsync(() -> flowStepRepository.saveAll(flowSteps), sardisJpaDbOperationsExecutor);
    }

    public <T> CompletableFuture<Void> startFlow(String eventName, String eventType, int startStep, String key, T data) {
        String transactionEventId = UUID.randomUUID().toString();
        Map<Integer, Step> event = eventsConfig.getEvents().get(eventName);


        EventEntity flow = EventEntity.builder()
                .eventName(eventName)
                .key(key)
                .eventType(eventType)
                .startTopic(event.get(startStep).getConsumer().getTopic())
                .transactionId(transactionEventId)
                .build();

        return saveFlow(flow)
                .thenCompose(persistedFlow -> {
                    List<EventStepEntity> flowSteps = event.keySet().stream().mapToInt(k -> k).filter(k -> k >= startStep).boxed()
                            .map(stepId -> EventStepEntity.builder()
                                    .status(EventStepStatus.INITIAL)
                                    .stepId(stepId)
                                    .eventFlow(persistedFlow)
                                    .transactionId(transactionEventId).build()
                            ).collect(Collectors.toList());
                    return saveFlowStepAll(flowSteps).thenCompose(any -> CompletableFuture.completedFuture(persistedFlow));
                })
                .thenCompose(persistedFlow ->
                        eventQueueService.sendToEventQueue(data, persistedFlow));
    }
}