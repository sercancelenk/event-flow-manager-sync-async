package byzas.libs.flow.manager.async.config.jpa.repository;

import byzas.libs.flow.manager.async.config.jpa.entity.FlowStepEntity;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface FlowStepRepository extends PagingAndSortingRepository<FlowStepEntity, Long> {
}
