package galois.llm.models;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.StreamReadFeature;
import galois.llm.models.togetherai.CellProb;
import galois.llm.models.togetherai.Logprobs;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * This class compute the probabilities of the extracted data from open LLM
 * models using the logprobs
 *
 */
@Slf4j
public class DataProb {

    public List<CellProb> computeProbabilities(Logprobs logprobs) {
        JsonFactory jfactory = new JsonFactory();
        List<CellProb> cellProbs = new ArrayList<>();
        List<String> tokenLines = logprobs.getTrimmedTokens();
        List<Double> tokenProbs = logprobs.getTokenProbs();
        String currentAttribute = null;
        Double currentAttributeProb = null;
        try {
            String jsonString = String.join("", tokenLines);
            List<Double> probsForChars = buildProbsForChars(tokenLines, tokenProbs);
            log.debug("JSON String: '{}'", jsonString);
            log.trace("Probs for chars: {}", probsForChars);
            if (jsonString.length() != probsForChars.size()) {
                log.error("JSON String Length: {}", jsonString.length());
                log.error("Probs for chars Length: {}", probsForChars.size());
                throw new IllegalArgumentException("Wrong probs for chars");
            }
            JsonParser jParser = jfactory.createParser(jsonString);
            jParser.enable(StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION.mappedFeature());
            while (jParser.nextToken() != JsonToken.END_ARRAY) {
                if (jParser.getCurrentToken() == JsonToken.VALUE_STRING) {
                    String value = jParser.getValueAsString();
                    int endToken = jParser.currentLocation().getColumnNr() - 2;
                    int startToken = endToken - value.length();
                    double probs = getProbs(startToken, endToken, probsForChars);
                    String check = jsonString.substring(startToken, endToken);
                    log.debug("CHECK {}: {}-{}", value.equals(check), value, check);
                    log.debug("Value: {} - Probs: {}", value, probs);
                    CellProb cp = new CellProb(currentAttribute, value, currentAttributeProb, probs);
                    cellProbs.add(cp);
                } else if (jParser.getCurrentToken() == JsonToken.VALUE_NUMBER_INT) {
                    Integer value = jParser.getValueAsInt();
//                    double probs = probsDataInput.getMeasuredProbs();
                    int endToken = jParser.currentLocation().getColumnNr() - 1;
                    int startToken = endToken - (value + "").length();
                    double probs = getProbs(startToken, endToken, probsForChars);
                    String check = jsonString.substring(startToken, endToken);
                    log.debug("CHECK {}: {}-{}", (value + "").equals(check), value, check);
                    log.debug("Value: {} - Probs: {}", value, probs);
                    CellProb cp = new CellProb(currentAttribute, value, currentAttributeProb, probs);
                    cellProbs.add(cp);
                } else if (jParser.getCurrentToken() == JsonToken.VALUE_NUMBER_FLOAT) {
                    Double value = jParser.getValueAsDouble();
//                    double probs = probsDataInput.getMeasuredProbs();
                    int endToken = jParser.currentLocation().getColumnNr() - 1;
                    int startToken = endToken - (value + "").length();
                    double probs = getProbs(startToken, endToken, probsForChars);
                    String check = jsonString.substring(startToken, endToken);
                    log.debug("CHECK {}: {}-{}", (value + "").equals(check), value, check);
                    log.debug("Value: {} - Probs: {}", value, probs);
                    CellProb cp = new CellProb(currentAttribute, value, currentAttributeProb, probs);
                    cellProbs.add(cp);
                } else if (jParser.getCurrentToken() == JsonToken.VALUE_TRUE || jParser.getCurrentToken() == JsonToken.VALUE_FALSE) {
                    boolean value = jParser.getValueAsBoolean();
                    int endToken = jParser.currentLocation().getColumnNr() - 1;
                    int startToken = endToken - (value + "").length();
                    double probs = getProbs(startToken, endToken, probsForChars);
                    String check = jsonString.substring(startToken, endToken);
                    log.debug("CHECK {}: {}-{}", (value + "").equals(check), value, check);
                    log.debug("Value: {} - Probs: {}", value, probs);
                    CellProb cp = new CellProb(currentAttribute, value, currentAttributeProb, probs);
                    cellProbs.add(cp);
                } else if (jParser.getCurrentToken() == JsonToken.VALUE_NULL) {
                    Object value = null;
                    int endToken = jParser.currentLocation().getColumnNr() - 1;
                    int startToken = endToken - (value + "").length();
                    double probs = getProbs(startToken, endToken, probsForChars);
                    String check = jsonString.substring(startToken, endToken);
                    log.debug("CHECK {}: {}-{}", (value + "").equals(check), value, check);
                    log.debug("Value: {} - Probs: {}", value, probs);
                    CellProb cp = new CellProb(currentAttribute, value, currentAttributeProb, probs);
                    cellProbs.add(cp);
                } else if (jParser.getCurrentToken() == JsonToken.VALUE_EMBEDDED_OBJECT) {
                    throw new UnsupportedOperationException("Unsupported VALUE_EMBEDDED_OBJECT");
                } else if (jParser.getCurrentToken() == JsonToken.FIELD_NAME) {
                    String value = jParser.getValueAsString();
//                    double probs = 0;
                    int endToken = jParser.currentLocation().getColumnNr() - 1;
                    int startToken = jsonString.substring(0, endToken).lastIndexOf(value);
                    endToken = startToken + value.length();
                    double probs = getProbs(startToken, endToken, probsForChars);
                    String check = jsonString.substring(startToken, endToken);
                    log.debug("CHECK {}: {}-{}", value.equals(check), value, check);
                    log.debug("Field Name: {} - Probs: {}", value, probs);
                    currentAttribute = value;
                    currentAttributeProb = probs;
                }
            }
        } catch (Exception e) {
            //log.error("Unable to parse document", e);
            //throw new RuntimeException(e);
        }
        return cellProbs;
    }

    private double getProbs(int startToken, int endToken, List<Double> probsForChars) {
        return probsForChars.subList(startToken, endToken).stream().filter(Objects::nonNull).mapToDouble(i -> i).summaryStatistics().getAverage();
    }

    private List<Double> buildProbsForChars(List<String> tokenLines, List<Double> probLines) {
        List<Double> probsForChars = new ArrayList<>();
        for (int i = 0; i < tokenLines.size(); i++) {
            String token = tokenLines.get(i);
            Double prob = probLines.get(i);
            for (int c = 0; c < token.length(); c++) {
                if (c == 0) {
                    probsForChars.add(prob);
                } else {
//                probsForChars.add(prob); //strategy: each char of the token gets the same prob  - Example: Token 'ABC' Prob: 10 -> probsForChars = 10, 10, 10
                    probsForChars.add(null); //strategy: only the first char of the token get the probs, others null  - Example: Token 'ABC' Prob: 10 -> probsForChars = 10, null, null
                }
            }
        }
        return probsForChars;
    }

}
