package byzas.libs.flow.manager.async.model.exception;

import byzas.libs.flow.manager.async.model.event.Execution;

public class ExecutionFailedException extends RuntimeException {
    private Execution execution;

    public ExecutionFailedException(Execution execution, Throwable cause) {
        super(cause);
        this.execution = execution;
    }

    public Execution getExecution() {
        return execution;
    }
}
