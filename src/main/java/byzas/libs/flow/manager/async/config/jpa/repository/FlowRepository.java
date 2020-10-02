package byzas.libs.flow.manager.async.config.jpa.repository;

import byzas.libs.flow.manager.async.config.jpa.entity.EventEntity;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;

public interface FlowRepository extends PagingAndSortingRepository<EventEntity, Long> {
    List<EventEntity> findAllByIdIn(List<Long> ids);

    List<EventEntity> findAllByKeyAndEventType(String key, String eventType);
}
