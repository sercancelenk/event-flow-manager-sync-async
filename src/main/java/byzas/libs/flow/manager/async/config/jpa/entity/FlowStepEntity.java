package byzas.libs.flow.manager.async.config.jpa.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import javax.persistence.*;
import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "flow_steps")
@ToString
@JsonInclude(JsonInclude.Include.NON_ABSENT)
public class FlowStepEntity extends BaseEntity {
    private static final String SEQUENCE_PREFIX = "flow_step";
    private static final String SEQUENCE_GENERATOR_NAME = SEQUENCE_PREFIX + "_gen";
    private static final String SEQUENCE_NAME = SEQUENCE_PREFIX + "_seq";

    @Id//TODO: Allocation size degistir 1 e cek
    @SequenceGenerator(name = SEQUENCE_GENERATOR_NAME, allocationSize = 100, sequenceName = SEQUENCE_NAME)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = SEQUENCE_GENERATOR_NAME)
    private Long id = 0L;

    @Column(name = "flow_step_id",nullable = false)
    private Integer stepId;

    @ManyToOne
    @JoinColumn(name = "flow_id")
    private FlowEntity flow;

    @Column(name = "status",nullable = false)
    @Enumerated(EnumType.ORDINAL)
    private FlowStepStatus status;

    @Column(name = "event_id",nullable = false)
    private String eventId;

    @Column(name = "transaction_id",nullable = false)
    private String transactionId;

    @Column(name="input",columnDefinition = "text")
    private String input;

    @Column(name="output",columnDefinition = "text")
    private String output;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FlowStepEntity that = (FlowStepEntity) o;
        return getId().equals(that.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }

}