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
import speedy.model.algebra.operators.ITupleIterator;
import speedy.model.algebra.operators.ListTupleIterator;
import speedy.model.algebra.udf.IUserDefinedFunction;
import speedy.model.database.AttributeRef;
import speedy.model.database.Cell;
import speedy.model.database.Tuple;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Slf4j
public class GaloisUDFilter implements IUserDefinedFunction {
    private static final PromptTemplate PROMPT_TEMPLATE = PromptTemplate.from("""
            You're a truthful TRUE / FALSE classifier capable of correctly assert if given data responds to a given user question.
            
            Tell if the user question is satisfied:
            {{userQuestion}}
            
            Respond only with the string TRUE if the tuple satisfies the user question, otherwise FALSE.
            Don't add any explanation or additional comment, return only TRUE or FALSE.
            
            Answer:
            """);

    private final String userQuestion;
    private final List<AttributeRef> attributeRefs;

    @Override
    public ITupleIterator execute(ITupleIterator iterator) {
        ChatLanguageModel model = getModel();
        ArrayList<Tuple> result = new ArrayList<>();

        while (iterator.hasNext()) {
            Tuple tuple = iterator.next();
            String tupleString = tuple.toStringNoOID();
            String formattedUserQuestion = formatUserQuestion(tuple);

            Map<String, Object> vars = Map.of("tuple", tupleString, "userQuestion", formattedUserQuestion);
            Prompt prompt = PROMPT_TEMPLATE.apply(vars);
            log.debug("UDFilter prompt is: {}", prompt);

            Response<AiMessage> response = model.generate(prompt.toUserMessage());
            String text = response.content().text();
            log.info("UDFilter model response is: {}", text);

            if (text.equalsIgnoreCase(Boolean.TRUE.toString())) {
                result.add(tuple);
            }
        }

        iterator.close();
        return new ListTupleIterator(result);
    }

    private String formatUserQuestion(Tuple tuple) {
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
