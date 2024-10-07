package galois.test.experiments.json.config;

import lombok.Data;

@Data
public class ContentRetrieverConfigurationJSON {
    private String embeddingStoreCollectionName;
    private String embeddingModelEngine;
    private String embeddingModel;
    private int maxResults;
    private double minScore;
    private int maxSegmentSizeInTokens;
    private int maxOverlapSizeInTokens;
    private String documentsToLoad;
}
