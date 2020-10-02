package byzas.libs.flow.manager.async.config.jpa.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import javax.persistence.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "events")
@ToString
@JsonInclude(JsonInclude.Include.NON_ABSENT)
/* CREATE INDEX flows_index_key_flow_type_flow_key_type ON flows(key,flow_type,flow_key_type); */
public class EventEntity extends BaseEntity {
    private static final String SEQUENCE_PREFIX = "events";
    private static final String SEQUENCE_GENERATOR_NAME = SEQUENCE_PREFIX + "_gen";
    private static final String SEQUENCE_NAME = SEQUENCE_PREFIX + "_seq";

    @Id//TODO: Allocation size degistir 1 e cek
    @SequenceGenerator(name = SEQUENCE_GENERATOR_NAME, allocationSize = 1, sequenceName = SEQUENCE_NAME)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = SEQUENCE_GENERATOR_NAME)
    private Long id = 0L;

    @Column(name = "transaction_id", nullable = false)
    private String transactionId;

    @Column(name = "event_name", nullable = false)
    private String eventName;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    // required unique key
    @Column(name = "key", nullable = false)
    private String key;

    @Column(name = "start_topic", nullable = false)
    private String startTopic;

    @OneToMany(
            mappedBy = "eventFlow",
            cascade = CascadeType.ALL,
            fetch = FetchType.EAGER
    )
    private Set<EventStepEntity> steps = new HashSet<>();

    public boolean isCompleted() {
        long completedStepCount = steps.stream()
                .filter(s -> s.getStatus() == EventStepStatus.COMPLETED).count();
        return (int) completedStepCount == steps.size();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EventEntity that = (EventEntity) o;
        return getId().equals(that.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }

    public Optional<EventStepEntity> stepFromMap(int stepId) {
        Optional<Map<Integer, EventStepEntity>> stepMap = Optional.ofNullable(steps)
                .map(steps -> steps.stream().collect(Collectors.toMap(EventStepEntity::getStepId, Function.identity(), (s1, s2) -> s2)));

        return stepMap.isPresent() && stepMap.get().containsKey(stepId) ? Optional.of(stepMap.get().get(stepId)) : Optional.empty();
    }
}