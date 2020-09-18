package byzas.libs.flow.manager.async.config.redis;

import byzas.libs.flow.manager.async.config.executors.CustomThreadPoolExecutor;
import byzas.libs.flow.manager.async.config.executors.ExecutorsConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static java.util.stream.Collectors.toList;

@RequiredArgsConstructor
@Service
@Log4j2
public class RedisCacheService {
    @Autowired
    @Qualifier(ExecutorsConfig.DEFAULT_CPU_OPERATIONS_EXECUTOR_BEAN)
    private CustomThreadPoolExecutor cpuOperationsExecutor;

    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;

    public CompletableFuture<Void> remove(String key) {
        return reactiveRedisTemplate.opsForValue().delete(key).toFuture()
                .thenApplyAsync(any -> null, cpuOperationsExecutor);
    }

    public CompletableFuture<String> get(String key) {
        return reactiveRedisTemplate.opsForValue().get(key).toFuture()
                .thenApplyAsync(Function.identity(), cpuOperationsExecutor);
    }

    public CompletableFuture<Void> setWithExpire(String key, String value, Duration expire) {
        return reactiveRedisTemplate.opsForValue().set(key, value, expire).toFuture()
                .thenApplyAsync(any -> null, cpuOperationsExecutor);
    }

    public CompletableFuture<Void> multiSetWithExpire(Map<String, String> data, Duration expire) {
        CompletableFuture<Boolean> saveResult = reactiveRedisTemplate.opsForValue().multiSet(data).toFuture();
        CompletableFuture<Void> expireResult = CompletableFuture.allOf(
                data.keySet().stream()
                        .map(key -> reactiveRedisTemplate.expire(key, expire).toFuture()).collect(toList()).toArray(new CompletableFuture[0]))
                .exceptionally(t -> {
                    log.error("Exception when setting timeout for polledData : {}", data);
                    return null;
                });
        return saveResult.thenCompose(any -> expireResult)
                .thenApplyAsync(Function.identity(), cpuOperationsExecutor);
    }

    public CompletableFuture<List<String>> multiGet(List<String> keys) {
        return reactiveRedisTemplate.opsForValue().multiGet(keys).toFuture()
                .thenApplyAsync(Function.identity(), cpuOperationsExecutor);
    }


}