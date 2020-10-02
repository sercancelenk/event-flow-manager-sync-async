package byzas.libs.flow.manager.async.config.jpa.repository;

import byzas.libs.flow.manager.async.config.jpa.entity.EventStepEntity;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface FlowStepRepository extends PagingAndSortingRepository<EventStepEntity, Long> {
}
