package byzas.libs.flow.manager.async.model.exception;

public class JobNotFoundException extends RuntimeException {
    private String job;

    public JobNotFoundException() {
        super();
    }

    public JobNotFoundException(String jobName) {
        super();
        this.job = jobName;
    }

    public String getJob() {
        return job;
    }
}

