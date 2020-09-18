package byzas.libs.flow.manager.async.config.executors;

import org.springframework.core.env.Environment;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;

public class CustomThreadPoolExecutor extends ThreadPoolTaskExecutor {

    private static final String CORE_POOL_SIZE_PARAM_NAME = ".corePoolSize";
    private static final String MAX_POOL_SIZE_PARAM_NAME = ".maxPoolSize";
    private static final String TIMEOUT_PARAM_NAME = ".timeoutMillis";
    private static final String QUEUE_CAPACITY_PARAM_NAME = ".queueCapacity";

    private Duration timeout;

    private Scheduler scheduler;

    public CustomThreadPoolExecutor(Environment env, String configName) {

        super();
        this.scheduler = Schedulers.fromExecutor(this);
        this.timeout = Duration.ofDays(Integer.MAX_VALUE); // default no timeout
        setCorePoolSize(env.getProperty(configName + CORE_POOL_SIZE_PARAM_NAME, Integer.class,
                Runtime.getRuntime().availableProcessors())); // at default core size = number of available processors
        setMaxPoolSize(env.getProperty(configName + MAX_POOL_SIZE_PARAM_NAME, Integer.class,
                getCorePoolSize())); // at default max = core pool size
        setThreadNamePrefix(configName);
        setTimeout(Duration.ofMillis(env.getProperty(configName + TIMEOUT_PARAM_NAME,
                Integer.class, Integer.MAX_VALUE))); // default no timeout
        setQueueCapacity(env.getProperty(configName + QUEUE_CAPACITY_PARAM_NAME, Integer.class, Integer.MAX_VALUE)); // default no limit
        initialize();
    }

    public Duration getTimeout() {
        return timeout;
    }

    public Scheduler getScheduler() {
        return scheduler;
    }

    public void setTimeout(Duration timeout) {
        this.timeout = timeout;
    }

}