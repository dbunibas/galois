package bsf.llm.models.togetherai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class StreamChoice {
    public long index;
    public String text;
    public String finish_reason;
    public StreamDelta delta;
}
