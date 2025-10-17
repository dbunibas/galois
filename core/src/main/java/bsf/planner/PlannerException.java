package bsf.planner;

public class PlannerException extends RuntimeException {

    public PlannerException() {
        super();
    }

    public PlannerException(String message) {
        super(message);
    }

    public PlannerException(String message, Throwable cause) {
        super(message, cause);
    }

    public PlannerException(Throwable cause) {
        super(cause);
    }
}
