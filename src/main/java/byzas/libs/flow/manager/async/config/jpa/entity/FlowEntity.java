package byzas.libs.flow.manager.async.config.jpa.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "flows")
@ToString
@JsonInclude(JsonInclude.Include.NON_ABSENT)
/* CREATE INDEX flows_index_key_flow_type_flow_key_type ON flows(key,flow_type,flow_key_type); */
public class FlowEntity extends BaseEntity {
    private static final String SEQUENCE_PREFIX = "flow";
    private static final String SEQUENCE_GENERATOR_NAME = SEQUENCE_PREFIX + "_gen";
    private static final String SEQUENCE_NAME = SEQUENCE_PREFIX + "_seq";

    @Id//TODO: Allocation size degistir 1 e cek
    @SequenceGenerator(name = SEQUENCE_GENERATOR_NAME, allocationSize = 100, sequenceName = SEQUENCE_NAME)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = SEQUENCE_GENERATOR_NAME)
    private Long id = 0L;

    @Column(name = "name",nullable = false)
    private String name;

    @Column(name = "topic",nullable = false)
    private String topic;

    @Column(name = "key")
    private String key;

    @Column(name = "flow_type")
    private String type;

    @Column(name = "flow_key_type")
    private String keyType;

    @OneToMany(mappedBy = "flow",fetch = FetchType.EAGER,
            cascade = CascadeType.REMOVE,orphanRemoval = true)
    private Set<FlowStepEntity> steps = new HashSet<>();

    @Column(name = "step_count",nullable = false)
    private int stepCount;

    @Column(name = "param")
    private String param;

    public boolean isCompleted(){
        long completedStepCount = steps.stream()
                .filter(s -> s.getStatus() == FlowStepStatus.COMPLETED).count();
        return (int)completedStepCount == stepCount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FlowEntity that = (FlowEntity) o;
        return getId().equals(that.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }

}