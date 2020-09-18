package byzas.libs.flow.manager.sync.handler;


import lombok.ToString;

import java.util.Map;

@ToString
public class ParameterContext extends AbstractContext {
    public ParameterContext() {}

    public ParameterContext(Map<String, Object> parameters) {
        super(parameters);
    }
}