package galois.http.payloads;

import lombok.Value;

@Value
public class JSONPayload {
    String model;
    String prompt;
    String jsonSchema;
}
