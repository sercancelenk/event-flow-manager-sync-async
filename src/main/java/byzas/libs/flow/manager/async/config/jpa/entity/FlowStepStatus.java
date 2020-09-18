package byzas.libs.flow.manager.async.config.jpa.entity;

import com.fasterxml.jackson.annotation.JsonValue;

public enum FlowStepStatus {
    ERROR,
    COMPLETED;

    private FlowStepStatus() {
    }

    @JsonValue
    public int toValue() {
        return this.ordinal();
    }
}
