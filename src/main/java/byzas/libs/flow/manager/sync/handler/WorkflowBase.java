package byzas.libs.flow.manager.sync.handler;

import reactor.core.publisher.Mono;

import java.util.concurrent.CompletableFuture;

public interface WorkflowBase {
    Boolean processWorkflow(Context context);

    Mono<Boolean> processWorkflowMono(Context context);

    CompletableFuture<Boolean> processWorkflowFuture(Context context);
}