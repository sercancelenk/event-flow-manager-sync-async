package byzas.libs.flow.manager.async.config.jpa.repository;

import byzas.libs.flow.manager.async.config.jpa.entity.FlowEntity;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;

public interface FlowRepository extends PagingAndSortingRepository<FlowEntity, Long> {
    List<FlowEntity> findByIdIn(List<Long> ids);

    List<FlowEntity> findAllByKeyAndTypeAndKeyType(String subscriptionId, String type, String keyType);
}
