package byzas.libs.flow.manager.async.model.exception;

public class JobAlreadyExistsException extends RuntimeException{
    private String job;

    public JobAlreadyExistsException(){
        super();
    }

    public JobAlreadyExistsException(String jobName){
        super();
        this.job = jobName;
    }

    public String getJob() {
        return job;
    }
}