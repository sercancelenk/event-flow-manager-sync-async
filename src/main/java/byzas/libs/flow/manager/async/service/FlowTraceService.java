package byzas.libs.flow.manager.async.service;

import byzas.libs.flow.manager.async.config.executors.CustomThreadPoolExecutor;
import byzas.libs.flow.manager.async.config.executors.ExecutorsConfig;
import byzas.libs.flow.manager.async.config.jpa.entity.EventEntity;
import byzas.libs.flow.manager.async.config.jpa.entity.EventStepEntity;
import byzas.libs.flow.manager.async.config.jpa.entity.EventStepStatus;
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
class FlowTraceService<T, V> implements JsonSupport {

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
                                EventEntity flow = flowMap.get(event.getFlowId());
                                EventStepEntity step = flow.stepFromMap(event.getStep()).orElseThrow(() -> new RuntimeException("Step not found in flow steps"));
                                step.setStatus(EventStepStatus.COMPLETED);
                                step.setInput(asJson(objectMapper, event.getData()));
                                step.setOutput(asJson(objectMapper, entry.getValue()));
                                return step;
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
                                    EventEntity flow = flowMap.get(event.getFlowId());
                                    EventStepEntity step = flow.stepFromMap(event.getStep()).orElseThrow(() -> new RuntimeException("Step not found in flow steps"));
                                    step.setStatus(EventStepStatus.ERROR);
                                    step.setErrorMessage(entry.getValue().getMessage());
                                    step.setInput(asJson(objectMapper, event.getData()));
                                    step.setOutput(asJson(objectMapper, entry.getValue()));
                                    return step;
                                }).collect(Collectors.toList()))
                .thenCompose(this::save);
    }

    private CompletableFuture<Void> save(List<EventStepEntity> flowStepEntities) {
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