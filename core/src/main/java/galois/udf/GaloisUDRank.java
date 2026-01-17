package galois.udf;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.output.Response;
import galois.Constants;
import galois.llm.models.TogetherAIModel;
import galois.llm.models.togetherai.TogetherAIConstants;
import galois.utils.Configuration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import speedy.model.algebra.udf.IUserDefinedFunction;
import speedy.model.database.AttributeRef;
import speedy.model.database.Cell;
import speedy.model.database.Tuple;

import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Slf4j
public class GaloisUDRank implements IUserDefinedFunction {
    private static final PromptTemplate PROMPT_TEMPLATE = PromptTemplate.from("""
            You're a feature numeric generator, capable of truthfully and precisely answer a user question.
            
            Answer the user question:
            {{userQuestion}}
            
            Respond only with the result, don't add any explanation or additional comment. The result MUST be a number. If you don't know respond with -1.
            
            Answer:
            """);

    private final String userQuestion;
    private final List<AttributeRef> attributeRefs;

    @Override
    public Object execute(Tuple tuple) {
        ChatLanguageModel model = getModel();
        String tupleString = tuple.toStringNoOID();
        String formattedUserQuestion = formatUserQuestion(tuple);

        Map<String, Object> vars = Map.of("tuple", tupleString, "userQuestion", formattedUserQuestion);
        Prompt prompt = PROMPT_TEMPLATE.apply(vars);
        log.debug("UDMap prompt is: {}", prompt);

        Response<AiMessage> response = model.generate(prompt.toUserMessage());
        String text = response.content().text();
        log.info("UDMap model response is: {}", text);
        if (text != null && !text.isBlank()) {
            try {
                return Integer.parseInt(text.trim());
            } catch (NumberFormatException e) {
                log.warn("The LLM couldn't extract a number from {}", text);
            }
        }

        return -1;

    }

    private String formatUserQuestion(Tuple tuple) {
        log.trace("Formatting user question {} with attribute refs {} and tuple {}", userQuestion, attributeRefs, tuple);
        String result = userQuestion;
        // replace {i} placeholder with attribute value
        for (int i = 0; i < attributeRefs.size(); i++) {
            AttributeRef attributeRef = attributeRefs.get(i);
            Cell cell = tuple.getCell(attributeRef);
            result = result.replace(String.format("{%d}", i + 1), cell.getValue().getPrimitiveValue().toString());
        }
        return result;
    }

    private ChatLanguageModel getModel() {
        if (Configuration.getInstance().getLLMProvider().equals(Constants.PROVIDER_OPENAI)) {
            return OpenAiChatModel.builder()
                    .apiKey(Configuration.getInstance().getOpenaiApiKey())
                    .modelName(Configuration.getInstance().getOpenaiModelName())
                    .build();
        }
        return new TogetherAIModel(
                Configuration.getInstance().getTogetheraiApiKey(),
                Configuration.getInstance().getTogetheraiModel(),
                TogetherAIConstants.STREAM_MODE
        );
    }

}
