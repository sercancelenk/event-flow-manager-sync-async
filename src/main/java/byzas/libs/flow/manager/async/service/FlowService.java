package byzas.libs.flow.manager.async.service;

import byzas.libs.flow.manager.async.config.executors.ExecutorsConfig;
import byzas.libs.flow.manager.async.config.executors.CustomThreadPoolExecutor;
import byzas.libs.flow.manager.async.config.jpa.entity.FlowEntity;
import byzas.libs.flow.manager.async.config.jpa.entity.FlowStepEntity;
import byzas.libs.flow.manager.async.config.jpa.repository.FlowRepository;
import byzas.libs.flow.manager.async.config.jpa.repository.FlowStepRepository;
import byzas.libs.flow.manager.async.config.kafka.props.EventsConfig;
import byzas.libs.flow.manager.async.config.kafka.props.Step;
import byzas.libs.flow.manager.async.model.exception.FlowAlreadyExistsException;
import com.google.common.base.Functions;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

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

    public CompletableFuture<Map<Long, FlowEntity>> getFlows(List<Long> flowIds) {
        return CompletableFuture
                .supplyAsync(() -> flowRepository.findByIdIn(flowIds)
                        .stream().collect(Collectors.toMap(FlowEntity::getId, Functions.identity(), (f1, f2) -> f1)), sardisJpaDbOperationsExecutor);
    }

    private CompletableFuture<List<FlowEntity>> getFlowsBy(String key, String type, String keyType) {
        return CompletableFuture.supplyAsync(() -> flowRepository.findAllByKeyAndTypeAndKeyType(key, type, keyType), sardisJpaDbOperationsExecutor);
    }

    private CompletableFuture<Void> canProcessFlow(FlowEntity flow) {
        return canProcessFlow(flow, Optional.ofNullable(flow.getParam()).isPresent() ? flow.getParam() : "");
    }

    private CompletableFuture<Void> canProcessFlow(FlowEntity flow, String param) {
        return getFlowsBy(flow.getKey(), flow.getType(), flow.getKeyType())
                .thenCompose(flowEntities -> {
                    CompletableFuture<Void> response = new CompletableFuture<>();
                    if (CollectionUtils.isNotEmpty(flowEntities)) {
                        response.completeExceptionally(new FlowAlreadyExistsException(flow.getKey() + flow.getKeyType() + flow.getType()));
                        return response;
                    }
                    response.complete(null);
                    return response;
                });
    }

    private CompletableFuture<List<FlowEntity>> saveFlowAll(List<FlowEntity> flows) {
        return CompletableFuture
                .supplyAsync(() -> {
                    List<FlowEntity> flowsPersisted = StreamSupport.stream(flowRepository.saveAll(flows).spliterator(), false)
                            .collect(Collectors.toList());
                    return flowsPersisted;
                }, sardisJpaDbOperationsExecutor);
    }

    private CompletableFuture<FlowEntity> saveFlow(FlowEntity flow) {
        return canProcessFlow(flow)
                .thenCompose(any -> CompletableFuture
                        .supplyAsync(() -> {
                            FlowEntity flowPersisted = flowRepository.save(flow);
                            return flowPersisted;
                        }, sardisJpaDbOperationsExecutor));
    }

    private CompletableFuture<FlowStepEntity> saveFlowStep(FlowStepEntity flowStep) {
        return CompletableFuture
                .supplyAsync(() -> flowStepRepository.save(flowStep), sardisJpaDbOperationsExecutor);
    }

    protected CompletableFuture<Iterable<FlowStepEntity>> saveFlowStepAll(List<FlowStepEntity> flowSteps) {
        return CompletableFuture
                .supplyAsync(() -> flowStepRepository.saveAll(flowSteps), sardisJpaDbOperationsExecutor);
    }

    public <T> CompletableFuture<Void> startFlow(String eventName, int startStep, String key, String keyType, String flowType, T data){
        Map<Integer, Step> event = eventsConfig.getEvents().get(eventName);
        Step step = event.get(startStep);
        FlowEntity flow = FlowEntity.builder()
                .name(eventName)
                .key(key)
                .keyType(keyType)
                .topic(step.getConsumer().getTopic())
                .type(flowType)
                .stepCount(event.values().size())
                .build();
        return saveFlow(flow)
                .thenCompose(flowPersisted ->
                        eventQueueService.sendToEventQueue(data, flowPersisted, UUID.randomUUID().toString()));
    }
}