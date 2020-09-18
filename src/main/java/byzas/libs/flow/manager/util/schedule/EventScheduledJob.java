package byzas.libs.flow.manager.util.schedule;

import byzas.libs.flow.manager.async.config.kafka.props.EventsConfig;
import byzas.libs.flow.manager.async.config.redis.RedisCacheService;
import byzas.libs.flow.manager.async.model.event.EventDto;
import byzas.libs.flow.manager.async.service.EventSender;
import byzas.libs.flow.manager.util.extensions.JsonSupport;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.TimeUnit;

@DisallowConcurrentExecution
@Log4j2
public class EventScheduledJob<T, V> implements Job, JsonSupport {

    @Autowired
    private EventSender eventSender;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private RedisCacheService cacheService;
    @Autowired
    private EventsConfig eventsConfig;


    public EventScheduledJob() {
    }

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        String eventJson = "";
        try {
            JobDataMap jobDataMap = jobExecutionContext.getMergedJobDataMap();
            eventJson = jobDataMap.getString("event");
            EventDto event = fromJson(objectMapper, EventDto.class, eventJson);
            String topic = eventsConfig.getEvents().get(event.getName()).get(event.getStep()).getConsumer().getTopic();
            cacheService.remove(String.format("%s.%s", topic, event.getId()))
                    .thenCompose(any -> eventSender.send(event))
                    .get(20000, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            log.fatal("Resending Event For Long Term Scheduled Retry Alarm, event : {}", eventJson, e);
        }
    }
}