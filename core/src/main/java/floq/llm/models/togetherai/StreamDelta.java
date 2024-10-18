package floq.llm.models.togetherai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class StreamDelta {
    public String role;
    public String content;
}
