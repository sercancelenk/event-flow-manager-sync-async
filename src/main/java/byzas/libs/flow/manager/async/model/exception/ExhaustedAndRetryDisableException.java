package byzas.libs.flow.manager.async.model.exception;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ExhaustedAndRetryDisableException extends RuntimeException {

    private Boolean doActionInScheduledRetriesEnd;
    private Boolean doActionInInstantRetriesEnd;
    private Boolean cancelInstantRetry;
    private Boolean logFatalInScheduledRetriesEnd;
    private Boolean logFatalInInstantRetriesEnd;
}
