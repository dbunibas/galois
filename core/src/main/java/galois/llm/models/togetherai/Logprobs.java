package galois.llm.models.togetherai;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.ArrayList;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "token_ids",
    "tokens",
    "token_logprobs"
})
public class Logprobs {

    @JsonProperty("token_ids")
    private List<Integer> tokenIds;
    @JsonProperty("tokens")
    private List<String> tokens;
    @JsonProperty("token_logprobs")
    private List<Double> tokenLogprobs;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();

    @JsonProperty("token_ids")
    public List<Integer> getTokenIds() {
        return tokenIds;
    }

    @JsonProperty("token_ids")
    public void setTokenIds(List<Integer> tokenIds) {
        this.tokenIds = tokenIds;
    }

    @JsonProperty("tokens")
    public List<String> getTokens() {
        return tokens;
    }

    @JsonProperty("tokens")
    public void setTokens(List<String> tokens) {
        this.tokens = tokens;
    }

    @JsonProperty("token_logprobs")
    public List<Double> getTokenLogprobs() {
        return tokenLogprobs;
    }

    @JsonProperty("token_logprobs")
    public void setTokenLogprobs(List<Double> tokenLogprobs) {
        this.tokenLogprobs = tokenLogprobs;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }
    
    public List<String> getTrimmedTokens() {
        List<String> trimmedTokens = new ArrayList<>();
        for (String token : tokens) {
            trimmedTokens.add(token.replace("\n", ""));
        }
        return trimmedTokens;
    }
    
    public List<Double> getTokenProbs() {
        List<Double> logProbs = getTokenLogprobs();
        List<Double> probs = new ArrayList<>();
        for (Double logProb : logProbs) {
            probs.add(Math.exp(logProb));
        }
        return probs;
    }
    
    public Double getAverageProb() {
        List<Double> tokenProbs = getTokenProbs();
        return getAverage(tokenProbs);
    }
    
    public Double getAverage(List<Double> list) {
        if (list.isEmpty()) return null;
        Double sum = 0.0;
        for (Double prob : list) {
            sum += prob;
        }
        return sum/list.size();
    }
    
    public Double getMaxProb() {
        List<Double> tokenProbs = getTokenProbs();
        if (tokenProbs.isEmpty()) return null;
        Double max = tokenProbs.get(0);
        for (Double tokenProb : tokenProbs) {
            if (tokenProb > max) {
                max = tokenProb;
            }
        }
        return max;
    }
    
    public Double getPerplexity() {
        Double average = getAverage(tokenLogprobs);
        return Math.exp(-average);
    }
    
    public String toLongString() {
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < this.tokens.size(); i++) {
            Integer tokenID = this.tokenIds.get(i);
            String token = this.tokens.get(i);
            Double prob = this.getTokenProbs().get(i);
            sb.append(token).append("\t").append(prob).append("\n");
//            sb.append(token).append("\n");
        }
        return sb.toString();
    }

}
