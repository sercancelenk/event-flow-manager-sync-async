package byzas.libs.flow.manager.async.config.executors;

import byzas.libs.flow.manager.util.YamlPropertySourceFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableAsync;

import javax.annotation.PostConstruct;

@Configuration
@EnableAsync
@RequiredArgsConstructor
@Log4j2
@PropertySource(value = "classpath:event-handler-executors-config.yml", factory = YamlPropertySourceFactory.class)
public class ExecutorsConfig {
    private static final String PREFIX = "event-handler";

    public static final String DEFAULT_DB_OPERATIONS_EXECUTOR_BEAN = PREFIX + "DefaultDbOperationsExecutor";
    private static final String DEFAULT_DB_OPERATIONS_EXECUTOR_CONFIG = PREFIX + ".default." + "dbOperationsExecutor";

    public static final String DEFAULT_CPU_OPERATIONS_EXECUTOR_BEAN = PREFIX + "DefaultCpuOperationsExecutor";
    private static final String DEFAULT_CPU_OPERATIONS_EXECUTOR_CONFIG = PREFIX + ".default." + "cpuOperationsExecutor";

    public static final String DEFAULT_SCHEDULE_OPERATIONS_EXECUTOR_BEAN = PREFIX + "DefaultScheduleOperationsExecutor";
    private static final String DEFAULT_SCHEDULE_OPERATIONS_EXECUTOR_CONFIG = PREFIX + ".default." + "scheduleOperationsExecutor";

    private final Environment environment;

    @PostConstruct
    public void init(){
        log.info("Executors COnfig initialized");
    }

    @Bean(DEFAULT_DB_OPERATIONS_EXECUTOR_BEAN)
    public CustomThreadPoolExecutor defaultDbOperationsExecutor() {
        return new CustomThreadPoolExecutor(environment, DEFAULT_DB_OPERATIONS_EXECUTOR_CONFIG);
    }

    @Bean(DEFAULT_CPU_OPERATIONS_EXECUTOR_BEAN)
    public CustomThreadPoolExecutor defaultCpuOperationsExecutor() {
        return new CustomThreadPoolExecutor(environment, DEFAULT_CPU_OPERATIONS_EXECUTOR_CONFIG);
    }

    @Bean(DEFAULT_SCHEDULE_OPERATIONS_EXECUTOR_BEAN)
    public CustomThreadPoolExecutor defaultScheduleOperationsExecutor() {
        return new CustomThreadPoolExecutor(environment, DEFAULT_SCHEDULE_OPERATIONS_EXECUTOR_CONFIG);
    }
}