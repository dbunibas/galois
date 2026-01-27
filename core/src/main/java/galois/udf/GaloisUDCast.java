package galois.udf;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.output.TokenUsage;
import galois.Constants;
import galois.llm.models.TogetherAIModel;
import galois.llm.models.togetherai.TogetherAIConstants;
import galois.llm.query.LLMQueryStatManager;
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
public class GaloisUDCast implements IUserDefinedFunction {

    private final String attributeName;

    @Override
    public Object execute(Tuple tuple) {
            
        Cell sentimentCell = tuple.getCell(new AttributeRef("reviews_r", attributeName));
        
        String sentiment = sentimentCell.getValue().toString();
        
        if (sentiment.equalsIgnoreCase("POSITIVE")) {
            return 1;
        }  
        
        return 0;
    }

    
}
