package bsf.llm.models;

import java.util.Map;

public interface IModel {
    String text(String prompt);

    default Map<String, Object> json(String prompt, String schema) {
        throw new UnsupportedOperationException("JSON-based structured generation is not supported for current model!");
    }

    default String regex(String prompt, String regex) {
        throw new UnsupportedOperationException("Regex-based structured generation is not supported for current model!");
    }
}
