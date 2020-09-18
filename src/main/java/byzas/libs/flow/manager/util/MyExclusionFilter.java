package byzas.libs.flow.manager.util;

import org.springframework.boot.autoconfigure.AutoConfigurationImportFilter;
import org.springframework.boot.autoconfigure.AutoConfigurationMetadata;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class MyExclusionFilter
        implements AutoConfigurationImportFilter {

    private static final Set<String> SHOULD_SKIP = new HashSet<>(
            Arrays.asList("org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration",
                    "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration",
                    "org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration",
                    "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                    "org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration",
                    "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration",
                    "org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration",
                    "org.springframework.boot.autoconfigure.quartz.QuartzAutoConfiguration",
                    "org.springframework.boot.autoconfigure.quartz.QuartzDataSourceInitializer"));


    @Override
    public boolean[] match(String[] classNames, AutoConfigurationMetadata metadata) {
        boolean[] matches = new boolean[classNames.length];

        for (int i = 0; i < classNames.length; i++) {
            matches[i] = !SHOULD_SKIP.contains(classNames[i]);
        }
        return matches;
    }
}