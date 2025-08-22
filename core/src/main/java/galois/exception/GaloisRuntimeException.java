package galois.exception;

public class GaloisRuntimeException extends RuntimeException {
    public GaloisRuntimeException(String message) {
        super(message);
    }

    public GaloisRuntimeException(Throwable cause) {
        super(cause);
    }
}
