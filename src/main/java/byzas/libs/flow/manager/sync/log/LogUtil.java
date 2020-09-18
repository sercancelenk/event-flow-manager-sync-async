package byzas.libs.flow.manager.sync.log;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class LogUtil {
    private final boolean disableLogging;

    public LogUtil(boolean disableLogging) {this.disableLogging = disableLogging;}


    private String format(String msg, Object... params) {
        return String.format(msg.replaceAll("\\{}", "%s"), params);
    }

    public void info(String msg, Object... params) {
        if (!disableLogging)
            log.info(format(msg, params));
    }

    public void error(String msg, Throwable t, Object... params) {
        log.error(format(msg, params), t);
    }

    public void error(String msg, Object... params) {
        log.error(format(msg, params));
    }
}