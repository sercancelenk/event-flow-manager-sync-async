package byzas.libs.flow.manager.async.model.event;

public interface BackoffDto {
    long STOP = -1;
    long nextBackOff();
}
