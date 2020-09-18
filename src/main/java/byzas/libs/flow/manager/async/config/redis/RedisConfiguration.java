package byzas.libs.flow.manager.async.config.redis;

import byzas.libs.flow.manager.util.YamlPropertySourceFactory;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.ReadFrom;
import io.lettuce.core.SocketOptions;
import io.lettuce.core.TimeoutOptions;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStaticMasterReplicaConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Configuration
@RequiredArgsConstructor
@Getter
@Setter
@Log4j2
@PropertySource(value = "classpath:event-handler-redis-config.yml", factory = YamlPropertySourceFactory.class)
public class RedisConfiguration {

    private final Environment env;

    @PostConstruct
    public void postConstruct() {
        log.info("Kutla redis support loaded successfully.");
    }

    @Bean("reactiveRedisConnectionFactory")
    public LettuceConnectionFactory redisConnectionFactory() {
        ClientOptions clientOptions = ClientOptions.builder()
                .timeoutOptions(TimeoutOptions.enabled())
                .autoReconnect(true)
                .socketOptions(SocketOptions.create())
                .build();
        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                .clientOptions(clientOptions)
                .commandTimeout(Duration.ofSeconds(env.getProperty("redis.command.timeoutSeconds", Integer.class, 10)))
                .readFrom(ReadFrom.REPLICA_PREFERRED)
                .build();
        RedisStaticMasterReplicaConfiguration staticMasterReplicaConfiguration =
                new RedisStaticMasterReplicaConfiguration(env.getRequiredProperty("redis.master.url", String.class).split(":")[0], Integer.parseInt(env.getRequiredProperty("redis.master.url", String.class).split(":")[1]));
        Optional.ofNullable(env.getProperty("redis.master.password")).ifPresent(p -> {
            staticMasterReplicaConfiguration.setPassword(RedisPassword.of(p));
        });
        if (env.containsProperty("redis.slaves")) {
            List<String> slaves = Arrays.asList(env.getRequiredProperty("redis.slaves").replace(" ", "").split(","));
            slaves.stream().forEach(slave -> staticMasterReplicaConfiguration.addNode(slave.split(":")[0], Integer.parseInt(slave.split(":")[1])));
        }


        return new LettuceConnectionFactory(staticMasterReplicaConfiguration, clientConfig);
    }

    @Primary
    @Bean
    public ReactiveRedisTemplate<String, String> reactiveRedisTemplate(@Qualifier("reactiveRedisConnectionFactory") ReactiveRedisConnectionFactory reactiveRedisConnectionFactory) {
        RedisSerializationContext.RedisSerializationContextBuilder<String, String> builder =
                RedisSerializationContext.newSerializationContext(new StringRedisSerializer());

        return new ReactiveRedisTemplate<>(reactiveRedisConnectionFactory, builder.value(new StringRedisSerializer()).build());
    }

    @Getter
    @Setter
    private static class RedisInstance {
        private String host;
        private int port;
    }

}