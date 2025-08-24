package galois.test.experiments.json.parser.operators;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.HuggingFaceTokenizer;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.chroma.ChromaEmbeddingStore;
import galois.llm.models.LMFactory;
import galois.llm.models.TogetherAIEmbeddingModel;
import galois.test.experiments.json.config.ContentRetrieverConfigurationJSON;
import galois.utils.Configuration;
import lombok.extern.slf4j.Slf4j;
import speedy.model.database.IDatabase;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static galois.llm.query.utils.QueryUtils.generateJsonSchemaListDatabase;

@Slf4j
public class ContentRetrieverConfigurationParser {

    public ContentRetriever loadContentRetriever(ContentRetrieverConfigurationJSON contentRetrieverConf, galois.test.experiments.Query query) {
        if (contentRetrieverConf == null) {
            return null;
        }
        EmbeddingModel embeddingModel = buildEmbeddingModel(contentRetrieverConf);
        EmbeddingStore<TextSegment> embeddingStore = buildEmbeddingStore(contentRetrieverConf);
        EmbeddingStoreIngestor embeddingStoreIngestor = buildEmbeddingStoreIngestor(contentRetrieverConf, embeddingModel, embeddingStore);
        ContentRetriever contentRetriever = buildContentRetriver(contentRetrieverConf, embeddingModel, embeddingStore);
//        //TODO: THE FOLLOW LINE IS FOR DEBUG ONLY
//        embeddingStore.removeAll(); //TO FORCE IMPORT
        if (isEmpty(embeddingStore, embeddingModel)) {
            log.debug("The embedding store is empty. Loading content...");
            loadDocuments(contentRetrieverConf, embeddingStoreIngestor, query.getDatabase());
        } else {
            log.debug("The contents have already been loaded into the embedding store. Skipping ingestion...");
        }
        return contentRetriever;
    }

    private boolean isEmpty(EmbeddingStore<TextSegment> embeddingStore, EmbeddingModel embeddingModel) {
        EmbeddingSearchResult<TextSegment> result = embeddingStore.search(EmbeddingSearchRequest.builder()
                .queryEmbedding(embeddingModel.embed("*").content())
                .minScore(0.0)
                .maxResults(1)
                .build());
        return result.matches().isEmpty();
    }

    private EmbeddingModel buildEmbeddingModel(ContentRetrieverConfigurationJSON contentRetrieverConf) {
        if (contentRetrieverConf.getEmbeddingModelEngine().equalsIgnoreCase("ollama")) {
            return OllamaEmbeddingModel.builder()
                    .baseUrl(Configuration.getInstance().getOllamaUrl())
                    .modelName(contentRetrieverConf.getEmbeddingModel())
                    .build();
        } else if (contentRetrieverConf.getEmbeddingModelEngine().equalsIgnoreCase("togetherai")) {
            return TogetherAIEmbeddingModel.builder()
                    .toghetherAiAPI(Configuration.getInstance().getTogetheraiApiKey())
                    .modelName(contentRetrieverConf.getEmbeddingModel())
                    .build();
        }
        throw new IllegalArgumentException("Unknown EmbeddingModelEngine " + contentRetrieverConf.getEmbeddingModelEngine());
    }

    private EmbeddingStore<TextSegment> buildEmbeddingStore(ContentRetrieverConfigurationJSON contentRetrieverConf) {
        EmbeddingStore<TextSegment> embeddingStore = ChromaEmbeddingStore
                .builder()
                .baseUrl(Configuration.getInstance().getChromaUrl())
                .collectionName(contentRetrieverConf.getEmbeddingStoreCollectionName())
                .logRequests(true)
                .logResponses(true)
                .build();
        return embeddingStore;
    }

    private EmbeddingStoreIngestor buildEmbeddingStoreIngestor(ContentRetrieverConfigurationJSON contentRetrieverConf,
                                                               EmbeddingModel embeddingModel,
                                                               EmbeddingStore<TextSegment> embeddingStore) {
        return EmbeddingStoreIngestor.builder()
                .documentSplitter(DocumentSplitters.recursive(contentRetrieverConf.getMaxSegmentSizeInTokens(),
                        contentRetrieverConf.getMaxOverlapSizeInTokens(),
                        new HuggingFaceTokenizer()))
//                .documentSplitter(new DocumentByParagraphSplitter(512, 128))
                .documentTransformer(document -> {
                    document.metadata().put("ingested_data", LocalDateTime.now().toString());
                    return document;
                })
                .textSegmentTransformer(textSegment -> TextSegment.from(
                        textSegment.metadata("file_name") + "\n" + textSegment.text(),
                        textSegment.metadata()
                ))
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .build();
    }

    private ContentRetriever buildContentRetriver(ContentRetrieverConfigurationJSON contentRetrieverConf,
                                                  EmbeddingModel embeddingModel,
                                                  EmbeddingStore<TextSegment> embeddingStore) {
        EmbeddingStoreContentRetriever baseRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(contentRetrieverConf.getMaxResults())
                .minScore(contentRetrieverConf.getMinScore())
                .build();
        return new CustomEmbeddingStoreContentRetriever(baseRetriever);
    }

    private void loadDocuments(ContentRetrieverConfigurationJSON contentRetrieverConf, EmbeddingStoreIngestor embeddingStoreIngestor, IDatabase database) {
        File folder = new File(contentRetrieverConf.getDocumentsToLoad());
        if (!folder.exists() || !folder.isDirectory()) {
            throw new IllegalArgumentException("Unable to read files from path. " + contentRetrieverConf.getDocumentsToLoad() + " - Base path: " + new File(".").getAbsoluteFile().getPath());
        }
        for (File file : folder.listFiles()) {
            if (file.isDirectory() || file.isHidden()) continue;
            log.info("Reading file {}", file.getName());
            Document document = FileSystemDocumentLoader.loadDocument(file.getPath());
            if (contentRetrieverConf.isProcessDocuments()) {
                document = processDocument(document, database);
            }
            log.info("Document Metadata {}", document.metadata());
            embeddingStoreIngestor.ingest(document);
        }
    }

    private Document processDocument(Document document, IDatabase database) {
        ChatLanguageModel model = LMFactory.getLMModel();
        StringBuilder processPrompt = new StringBuilder();
        processPrompt.append("# Prompt: \nYou are an AI assistant tasked with summarizing documents into a concise textual description based on specific schema requirements. Given the following document:");
        processPrompt.append(document.text());
        processPrompt.append("\n------------\n");
        processPrompt.append("# Generate a summarized textual description containing the following information:\n");
        processPrompt.append(generateJsonSchemaListDatabase(database));
        processPrompt.append("\nDo not add any extra information or instruction.\n");
        processPrompt.append("\n------------\n");
        processPrompt.append("# Answer:\n");
        log.info("Processing document with prompt {}", processPrompt);
        String response = model.generate(processPrompt.toString());
        log.info("Processed document {}", response);
        return Document.document(response, document.metadata());
    }

    @Slf4j
    static class CustomEmbeddingStoreContentRetriever implements ContentRetriever {

        EmbeddingStoreContentRetriever baseRetriever;

        public CustomEmbeddingStoreContentRetriever(EmbeddingStoreContentRetriever baseRetriever) {
            this.baseRetriever = baseRetriever;
        }

        @Override
        public List<Content> retrieve(Query query) {
            Query cleanedQuery = cleanQuery(query);
            log.warn("# Cleaned SQL query {}", cleanedQuery);
            List<Content> results = baseRetriever.retrieve(cleanedQuery);
            String plainSQLQuery = getPlainSQLQuery(query);
            if (plainSQLQuery != null && plainSQLQuery.toLowerCase().contains("where")) {
                String whereClause = plainSQLQuery.substring(plainSQLQuery.toLowerCase().indexOf("where") + 6);
                List<Content> whereResults = baseRetriever.retrieve(Query.from(whereClause, query.metadata()));
                for (Content whereResult : whereResults) {
                    if (!results.contains(whereResult)) {
                        log.warn("Adding specific result extracted for the where clause");
                        results.addFirst(whereResult);
                    }
                }
            }
            return results;
        }

        private Query cleanQuery(Query query) {
            String plainSQLQuery = getPlainSQLQuery(query);
            if (plainSQLQuery == null) {
                if (query.text().contains("Use the following JSON schema")) { //To avoid Input validation error: `inputs` tokens + `max_new_tokens` must be <= 532
                    String simplifiedQuery = query.text().substring(0, query.text().indexOf("Use the following JSON schema"));
                    return Query.from(simplifiedQuery, query.metadata());
                }
                return query;
            }
            return Query.from(plainSQLQuery, query.metadata());
        }

        private String getPlainSQLQuery(Query query) {
            Pattern pattern = Pattern.compile(".*(select .*)\\.\n", Pattern.CASE_INSENSITIVE);
            String originalQuery = query.text().replace("\\n", "\n");
            Matcher matcher = pattern.matcher(originalQuery);
            boolean matchFound = matcher.find();
            if (!matchFound) {
                return null;
            }
            return matcher.group(1);
        }
    }

}
