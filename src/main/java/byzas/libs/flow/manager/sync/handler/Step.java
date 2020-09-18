package byzas.libs.flow.manager.sync.handler;

import reactor.core.publisher.Mono;

import java.util.concurrent.CompletableFuture;

public interface Step {
    String getName();

    Mono<Boolean> doActionMono(Context context);

    CompletableFuture<Boolean> doActionFuture(Context context);

    Boolean doAction(Context context);
}