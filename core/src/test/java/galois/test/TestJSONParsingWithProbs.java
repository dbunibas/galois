package galois.test;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.StreamReadFeature;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
public class TestJSONParsingWithProbs {


    @Test
    public void testReadJSON() {
        JsonFactory jfactory = new JsonFactory();
        try {
            InputStream tokensIS = new FileInputStream("/Users/donatello/Projects/research/galois/core/src/test/resources/tokens-raw.txt");
            InputStream probsIS = new FileInputStream("/Users/donatello/Projects/research/galois/core/src/test/resources/tokens-probs.txt");
            List<String> tokenLines = IOUtils.readLines(new InputStreamReader(tokensIS));
            List<String> probsLines = IOUtils.readLines(new InputStreamReader(probsIS));
            String jsonString = String.join("", tokenLines);
            List<Integer> probsForChars = buildProbsForChars(tokenLines, probsLines);
            log.info("JSON String: '{}'", jsonString);
            log.info("Probs for chars: {}", probsForChars);
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
                    log.info("CHECK {}: {}-{}", value.equals(check), value, check);
                    log.info("Value: {} - Probs: {}", value, probs);
                } else if (jParser.getCurrentToken() == JsonToken.VALUE_NUMBER_INT) {
                    Integer value = jParser.getValueAsInt();
//                    double probs = probsDataInput.getMeasuredProbs();
                    int endToken = jParser.currentLocation().getColumnNr() - 1;
                    int startToken = endToken - (value + "").length();
                    double probs = getProbs(startToken, endToken, probsForChars);
                    String check = jsonString.substring(startToken, endToken);
                    log.info("CHECK {}: {}-{}", (value + "").equals(check), value, check);
                    log.info("Value: {} - Probs: {}", value, probs);
                } else if (jParser.getCurrentToken() == JsonToken.VALUE_NUMBER_FLOAT) {
                    Double value = jParser.getValueAsDouble();
//                    double probs = probsDataInput.getMeasuredProbs();
                    int endToken = jParser.currentLocation().getColumnNr() - 1;
                    int startToken = endToken - (value + "").length();
                    double probs = getProbs(startToken, endToken, probsForChars);
                    String check = jsonString.substring(startToken, endToken);
                    log.info("CHECK {}: {}-{}", (value + "").equals(check), value, check);
                    log.info("Value: {} - Probs: {}", value, probs);
                } else if (jParser.getCurrentToken() == JsonToken.VALUE_TRUE || jParser.getCurrentToken() == JsonToken.VALUE_FALSE) {
                    boolean value = jParser.getValueAsBoolean();
                    int endToken = jParser.currentLocation().getColumnNr() - 1;
                    int startToken = endToken - (value + "").length();
                    double probs = getProbs(startToken, endToken, probsForChars);
                    String check = jsonString.substring(startToken, endToken);
                    log.info("CHECK {}: {}-{}", (value + "").equals(check), value, check);
                    log.info("Value: {} - Probs: {}", value, probs);
                } else if (jParser.getCurrentToken() == JsonToken.VALUE_NULL) {
                    Object value = null;
                    int endToken = jParser.currentLocation().getColumnNr() - 1;
                    int startToken = endToken - (value + "").length();
                    double probs = getProbs(startToken, endToken, probsForChars);
                    String check = jsonString.substring(startToken, endToken);
                    log.info("CHECK {}: {}-{}", (value + "").equals(check), value, check);
                    log.info("Value: {} - Probs: {}", value, probs);
                } else if (jParser.getCurrentToken() == JsonToken.VALUE_EMBEDDED_OBJECT) {
                    throw new UnsupportedOperationException("Unsupported VALUE_EMBEDDED_OBJECT");
                } else if (jParser.getCurrentToken() == JsonToken.FIELD_NAME) {
                    String value = jParser.getValueAsString();
//                    double probs = probsDataInput.getMeasuredProbs();
                    double probs = 0;
                    log.info("Field Name: {} - Probs: {}", value, probs);
                }
            }
        } catch (Exception e) {
            log.error("Unable to parse document", e);
            throw new RuntimeException(e);
        }

    }

    private double getProbs(int startToken, int endToken, List<Integer> probsForChars) {
        return probsForChars.subList(startToken, endToken).stream().filter(Objects::nonNull).mapToInt(i -> i).summaryStatistics().getAverage();
    }

    private List<Integer> buildProbsForChars(List<String> tokenLines, List<String> probsLines) {
        List<Integer> probsForChars = new ArrayList<>();
        for (int i = 0; i < tokenLines.size(); i++) {
            String token = tokenLines.get(i);
            Integer prob = Integer.parseInt(probsLines.get(i));
//            probsForChars.add(prob);
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

