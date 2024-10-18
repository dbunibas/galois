package floq.llm.query.exception;

public class LLMQueryException extends RuntimeException {

    public LLMQueryException(String message) {
        super(message);
    }

    public LLMQueryException(Throwable cause) {
        super(cause);
    }
}
