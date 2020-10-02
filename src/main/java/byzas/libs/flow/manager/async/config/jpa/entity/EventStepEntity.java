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
@Table(name = "event_steps")
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@ToString(exclude = {"eventFlow"})
public class EventStepEntity extends BaseEntity {
    private static final String SEQUENCE_PREFIX = "event_steps";
    private static final String SEQUENCE_GENERATOR_NAME = SEQUENCE_PREFIX + "_gen";
    private static final String SEQUENCE_NAME = SEQUENCE_PREFIX + "_seq";

    @Id//TODO: Allocation size degistir 1 e cek
    @SequenceGenerator(name = SEQUENCE_GENERATOR_NAME, allocationSize = 1, sequenceName = SEQUENCE_NAME)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = SEQUENCE_GENERATOR_NAME)
    private Long id = 0L;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "event_flow_id", referencedColumnName = "id")
    private EventEntity eventFlow;

    @Column(name = "transaction_id", nullable = false)
    private String transactionId;

    @Column(name = "step_id", nullable = false)
    private Integer stepId;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.ORDINAL)
    private EventStepStatus status;

    @Column(name = "input", columnDefinition = "text")
    private String input;

    @Column(name = "output", columnDefinition = "text")
    private String output;

    @Column(name = "error_message")
    private String errorMessage;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EventStepEntity that = (EventStepEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}