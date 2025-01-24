package floq.llm.query;

import dev.langchain4j.chain.ConversationalRetrievalChain;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import lombok.Getter;

@Getter
public class CustomConversionalRetrievalChain extends ConversationalRetrievalChain {

    private ContentRetriever contentRetriever;
    private ChatLanguageModel chatLanguageModel;


    public CustomConversionalRetrievalChain(ChatLanguageModel chatLanguageModel, ContentRetriever contentRetriever,  RetrievalAugmentor retrievalAugmentor) {
        super(chatLanguageModel, null, retrievalAugmentor);
        this.chatLanguageModel = chatLanguageModel;
        this.contentRetriever = contentRetriever;
    }

}
