package floq.llm.models.togetherai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class StreamResponse {
    public String id;
    public List<StreamChoice> choices;
    public String model;
    public Usage usage;

}