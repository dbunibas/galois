package floq.llm.models;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;

public class OllamaModel implements IModel {
    private final ChatLanguageModel model;

    public OllamaModel(String modelName) {
        this.model = OllamaChatModel.builder()
                .baseUrl("http://127.0.0.1:11434")
                .modelName(modelName)
                .temperature(0.0)
                .build();
    }

    @Override
    public String text(String prompt) {
        return model.generate(prompt);
    }
}
