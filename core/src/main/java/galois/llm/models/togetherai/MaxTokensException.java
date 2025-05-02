package galois.llm.models.togetherai;

import java.io.IOException;

public class MaxTokensException extends IOException {

    public MaxTokensException() {
    }

    public MaxTokensException(String message) {
        super(message);
    }

    public MaxTokensException(String message, Throwable cause) {
        super(message, cause);
    }
  
}
