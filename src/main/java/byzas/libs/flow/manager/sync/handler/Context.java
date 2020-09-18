package byzas.libs.flow.manager.sync.handler;

public interface Context {
    Object getParametersAttribute(String name);

    void setParametersAttribute(String name, Object value);

    Object getLoggingParametersAttribute(String name);

    void setLoggingParametersAttribute(String name, Object value);

    void dumpLogs();

    void dumpContextParameters();
}