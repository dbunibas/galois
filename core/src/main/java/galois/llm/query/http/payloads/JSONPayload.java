package galois.llm.query.http.payloads;

import lombok.Value;

@Value
public class JSONPayload {
    String prompt;
    String jsonSchema;
}
