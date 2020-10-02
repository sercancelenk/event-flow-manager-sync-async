package byzas.libs.flow.manager.async.config.jpa.entity;

import com.fasterxml.jackson.annotation.JsonValue;

public enum EventStepStatus {
    INITIAL,
    RUNNING,
    ERROR,
    COMPLETED;

    private EventStepStatus() {
    }

    @JsonValue
    public int toValue() {
        return this.ordinal();
    }
}
