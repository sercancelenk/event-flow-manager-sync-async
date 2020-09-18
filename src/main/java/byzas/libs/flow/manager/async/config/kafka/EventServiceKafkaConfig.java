package byzas.libs.flow.manager.async.config.kafka;

import byzas.libs.flow.manager.async.config.kafka.props.Consumer;
import byzas.libs.flow.manager.async.config.kafka.props.EventsConfig;
import byzas.libs.flow.manager.async.config.kafka.props.Step;
import byzas.libs.flow.manager.async.model.event.EventDto;
import byzas.libs.flow.manager.util.extensions.MapSupport;
import com.google.common.collect.ImmutableMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.MapUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.SeekToCurrentBatchErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;

import static java.util.Optional.ofNullable;

@Configuration
@EnableKafka
@RequiredArgsConstructor
@Log4j2
public class EventServiceKafkaConfig implements MapSupport {

    private final EventsConfig eventsConfig;

    private final ApplicationContext applicationContext;

    private ImmutableMap<String, ImmutableMap<Integer, KafkaTemplate<String, EventDto>>> producers;

    private ImmutableMap<String, ImmutableMap<Integer, ConcurrentMessageListenerContainer>> consumerContainers;

    public KafkaTemplate<String, EventDto> getProducer(String eventName, int step) {
        return producers.get(eventName).get(step);
    }

    public ConcurrentMessageListenerContainer getConsumerContainer(String eventName, int step) {
        return consumerContainers.get(eventName).get(step);
    }

    @PostConstruct
    private void init() {
        log.info("Kafka Config initialized {}", eventsConfig);
        consumerContainers = createAndStartConsumers();
        producers = createProducers();
    }

    private ImmutableMap<String, ImmutableMap<Integer, KafkaTemplate<String, EventDto>>> createProducers() {
        if (MapUtils.isNotEmpty(eventsConfig.getEvents())) {
            return eventsConfig.getEvents()
                    .entrySet().stream()
                    .reduce(ImmutableMap.of(),
                            (acc1, entry) -> {
                                String eventName = entry.getKey();
                                Map<Integer, Step> stepMap = entry.getValue();
                                ImmutableMap<Integer, KafkaTemplate<String, EventDto>> eventProducers = stepMap.values().stream().reduce(ImmutableMap.of(),
                                        (acc2, step) ->
                                                ofNullable(step.getProducer()).map(p -> {
                                                    DefaultKafkaProducerFactory<String, EventDto> kafkaProducerFactory =
                                                            new DefaultKafkaProducerFactory<>(mergeKafkaProps(p.getProps(), eventsConfig.getProducerPropsDefaults()));
                                                    KafkaTemplate<String, EventDto> kafkaTemplate = new KafkaTemplate<>(kafkaProducerFactory);
                                                    return new ImmutableMap.Builder<Integer, KafkaTemplate<String, EventDto>>()
                                                            .putAll(acc2)
                                                            .put(step.getId(), kafkaTemplate)
                                                            .build();
                                                }).orElse(ImmutableMap.<Integer, KafkaTemplate<String, EventDto>>builder().putAll(acc2).build())
                                        , (map1, map2) -> ImmutableMap.<Integer, KafkaTemplate<String, EventDto>>builder().putAll(map1).putAll(map2).build());
                                return ImmutableMap.<String, ImmutableMap<Integer, KafkaTemplate<String, EventDto>>>builder()
                                        .putAll(acc1).put(eventName, eventProducers).build();
                            }, (map1, map2) -> ImmutableMap.<String, ImmutableMap<Integer, KafkaTemplate<String, EventDto>>>builder().putAll(map1).putAll(map2).build());
        }

        return ImmutableMap.<String, ImmutableMap<Integer, KafkaTemplate<String, EventDto>>>builder().build();
    }

    private ImmutableMap<String, ImmutableMap<Integer, ConcurrentMessageListenerContainer>> createAndStartConsumers() {
        if (MapUtils.isNotEmpty(eventsConfig.getEvents())) {
            return eventsConfig.getEvents()
                    .entrySet().stream()
                    .reduce(ImmutableMap.of(),
                            (acc1, entry) -> {
                                String eventName = entry.getKey();
                                Map<Integer, Step> stepMap = entry.getValue();
                                ImmutableMap<Integer, ConcurrentMessageListenerContainer> eventContainers = stepMap.values().stream().reduce(ImmutableMap.of(),
                                        (acc2, step) -> {
                                            ConcurrentMessageListenerContainer container = createMessageListenerContainer(step);
                                            return ImmutableMap.<Integer, ConcurrentMessageListenerContainer>builder()
                                                    .putAll(acc2).put(step.getId(), container).build();
                                        }, (map1, map2) -> ImmutableMap.<Integer, ConcurrentMessageListenerContainer>builder()
                                                .putAll(map1).putAll(map2).build());
                                return ImmutableMap.<String, ImmutableMap<Integer, ConcurrentMessageListenerContainer>>builder()
                                        .putAll(acc1).put(eventName, eventContainers).build();
                            }, (map1, map2) -> ImmutableMap.<String, ImmutableMap<Integer, ConcurrentMessageListenerContainer>>builder()
                                    .putAll(map1).putAll(map2).build());
        }

        return ImmutableMap.<String, ImmutableMap<Integer, ConcurrentMessageListenerContainer>>builder().build();


    }

    private ConcurrentMessageListenerContainer createMessageListenerContainer(Step step) {
        Consumer consumer = step.getConsumer();
        DefaultKafkaConsumerFactory<String, EventDto> kafkaConsumerFactory =
                new DefaultKafkaConsumerFactory<>(mergeKafkaProps(consumer.getProps(), eventsConfig.getConsumerPropsDefaults()));
        ContainerProperties containerProperties = new ContainerProperties(consumer.getTopic());
        containerProperties.setMissingTopicsFatal(false);
        containerProperties.setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        containerProperties.setSyncCommits(consumer.isSyncCommit());
        containerProperties.setSyncCommitTimeout(Duration.ofSeconds(consumer.getSyncCommitTimeoutSecond()));
        containerProperties.setMessageListener(applicationContext.getBean(consumer.getEventListenerBeanName()));
        ConcurrentMessageListenerContainer container =
                new ConcurrentMessageListenerContainer<>(kafkaConsumerFactory, containerProperties);
        SeekToCurrentBatchErrorHandler errorHandler = new SeekToCurrentBatchErrorHandler();
        FixedBackOff fixedBackOff =
                new FixedBackOff(consumer.getBackoffIntervalMillis(), Long.MAX_VALUE);
        errorHandler.setBackOff(fixedBackOff);
        container.setBatchErrorHandler(errorHandler);
        container.setConcurrency(step.getConsumer().getConcurrency());
        container.start();
        return container;
    }


    @PreDestroy
    public void shutdownHook() {
        consumerContainers
                .forEach((key, value) -> value.forEach((key1, value1) -> {
                    try {
                        if (Optional.ofNullable(value1).isPresent()) {
                            value1.stop();
                            log.warn("Consumer stopped. {}", value1);
                        }
                    } catch (Exception ex) {
                        log.warn("Consumer can not stopping on shutdown hook.", ex);
                    }
                }));
        producers
                .forEach((key, value) -> value.forEach((key1, value1) -> {
                    try {
                        if (Optional.ofNullable(value1).isPresent()) {
                            value1.flush();
                            log.warn("Producer flushed. {}", value1);
                        }
                    } catch (Exception ex) {
                        log.warn("Producer can not flush on shutdown hook", ex);
                    }
                }));
    }

}