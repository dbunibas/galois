package floq.optimizer;

public class OptimizerException extends RuntimeException {
    public OptimizerException() {
        super();
    }

    public OptimizerException(String message) {
        super(message);
    }

    public OptimizerException(Throwable cause) {
        super(cause);
    }
}
