package byzas.libs.flow.manager.async.service;

import byzas.libs.flow.manager.async.config.jpa.repository.FlowRepository;
import byzas.libs.flow.manager.async.config.redis.RedisCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@RequiredArgsConstructor
@Service
@Log4j2
public class HealthCheckMonitorService {
    private final RedisCacheService cacheService;
    private final FlowRepository flowRepository;

    public CompletableFuture<Void> checkSystemHealth() {
        return isCacheHealthy()
                .thenAccept(any -> isDBHealthy());
    }

    private void isDBHealthy() {
        log.debug("[HEALT_CHECK] Db is checking..");
        Pageable limit = PageRequest.of(0, 1);
        flowRepository.findAll(limit);
    }

    private CompletableFuture<Void> isCacheHealthy() {
        log.debug("[HEALT_CHECK] Redis is checking..");
        return cacheService.get("any")
                .thenApply(any -> null);
    }

}
