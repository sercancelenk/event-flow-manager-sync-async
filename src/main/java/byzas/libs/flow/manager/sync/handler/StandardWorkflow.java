package byzas.libs.flow.manager.sync.handler;

import java.util.List;

public class StandardWorkflow extends Workflow {
    public StandardWorkflow(String workFlowName, List<Step> steps) {
        super(workFlowName, steps);
    }

    public StandardWorkflow(String workFlowName, List<Step> steps, boolean disableLogging) {
        super(workFlowName, steps, disableLogging);
    }
}