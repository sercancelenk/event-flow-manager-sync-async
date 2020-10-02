package byzas.libs.flow.manager.async.service;

import byzas.libs.flow.manager.async.config.jpa.entity.EventEntity;
import org.springframework.stereotype.Component;

import java.util.List;

public class EventStartDecider {
    public boolean decide(List<EventEntity> currentEvents){
        return true;
    }
}
