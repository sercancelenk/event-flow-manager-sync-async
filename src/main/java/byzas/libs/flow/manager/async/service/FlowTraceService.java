package byzas.libs.flow.manager.async.service;

import byzas.libs.flow.manager.async.config.executors.ExecutorsConfig;
import byzas.libs.flow.manager.async.config.jpa.entity.FlowStepEntity;
import byzas.libs.flow.manager.async.config.jpa.entity.FlowStepStatus;
import byzas.libs.flow.manager.async.config.executors.CustomThreadPoolExecutor;
import byzas.libs.flow.manager.async.model.event.EventDto;
import byzas.libs.flow.manager.async.model.event.Execution;
import byzas.libs.flow.manager.util.extensions.JsonSupport;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
@Log4j2
public class FlowTraceService<T, V> implements JsonSupport {

    private final ObjectMapper objectMapper;
    private final FlowService flowService;

    @Autowired
    @Qualifier(ExecutorsConfig.DEFAULT_DB_OPERATIONS_EXECUTOR_BEAN)
    private CustomThreadPoolExecutor sardisJpaDbOperationsExecutor;

    public CompletableFuture<Void> saveAllProcessed(Map<EventDto<T>, V> events) {
        List<Long> flowIds = events.entrySet().stream()
                .filter(entry -> {
                    if (entry.getKey().getFlowId() != null) {
                        log.debug("Event with name and step {} {} flow id : {}", entry.getKey().getName(),
                                entry.getKey().getStep(), entry.getKey().getFlowId());
                        return true;
                    } else {
                        log.debug("Event with name and step {} {} flow id is null", entry.getKey().getName(),
                                entry.getKey().getStep());
                        return false;
                    }
                })
                .map(entry -> entry.getKey().getFlowId())
                .collect(Collectors.toList());
        log.debug("FlowIds : {}", flowIds);
        return flowService.getFlows(flowIds)
                .thenApply(flowMap -> {
                    log.debug("Flow map : {}", flowMap);
                    return events.entrySet().stream()
                            .filter(entry -> entry.getKey().getFlowId() != null)
                            .map(entry -> {
                                EventDto<T> event = entry.getKey();
                                return FlowStepEntity.builder()
                                        .eventId(event.getId())
                                        .flow(flowMap.get(event.getFlowId()))
                                        .input(asJson(objectMapper, event.getData()))
                                        .output(asJson(objectMapper, entry.getValue()))
                                        .status(FlowStepStatus.COMPLETED)
                                        .stepId(event.getStep())
                                        .transactionId(event.getTransactionId())
                                        .build();
                            }).collect(Collectors.toList());
                })
                .thenCompose(this::save);
    }

    public CompletableFuture<Void> saveAllFailed(Map<EventDto<T>, Throwable> events) {
        List<Long> flowIds = events.entrySet().stream()
                .filter(entry -> entry.getKey().getFlowId() != null)
                .map(entry -> entry.getKey().getFlowId())
                .collect(Collectors.toList());
        return flowService.getFlows(flowIds)
                .thenApply(flowMap ->
                        events.entrySet().stream()
                                .filter(entry -> entry.getKey().getFlowId() != null)
                                .map(entry -> {
                                    EventDto<T> event = entry.getKey();
                                    return FlowStepEntity.builder()
                                            .eventId(event.getId())
                                            .flow(flowMap.get(event.getFlowId()))
                                            .input(asJson(objectMapper, event.getData()))
                                            .output(entry.getValue().getMessage())
                                            .status(FlowStepStatus.ERROR)
                                            .stepId(event.getStep())
                                            .transactionId(event.getTransactionId())
                                            .build();
                                }).collect(Collectors.toList()))
                .thenCompose(this::save);
    }

    private CompletableFuture<Void> save(List<FlowStepEntity> flowStepEntities) {
        log.debug("Before saving flowStep entities : {}", flowStepEntities);
        return CompletableFuture.supplyAsync(() -> flowService.saveFlowStepAll(flowStepEntities), sardisJpaDbOperationsExecutor)
                .exceptionally(t -> {
                    log.error("Exception when saving failed events to DB, flowStepEntities : {}", flowStepEntities, t);
                    return null;
                })
                .thenApply(any -> null);
    }

    public CompletableFuture<Void> trace(Execution<T, V> execution) {
        CompletableFuture<Void> result = CompletableFuture.allOf();
        log.debug("Trace db execution processed size : {}", execution.getProcessed().size());
        log.debug("Trace db execution exhausted size : {}", execution.getExhausted().size());
        if (execution.getProcessed().size() > 0) {
            result = result.thenCompose(any1 -> saveAllProcessed(execution.getProcessed()));
        }
        if (execution.getExhausted().size() > 0) {
            result = result.thenCompose(any1 -> saveAllFailed(execution.getExhausted()));
        }
        return result;
    }
}