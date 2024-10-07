package galois.test;

import galois.utils.Mapper;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@Slf4j
public class TestParserJSONResponse {

    @Test
    public void testParseIncompleteResponse1() {
        String response = "[\n"
                + "  \"The Shawshank Redemption\",\n"
                + "  \"The Dark\",\n"
                + "  \"The Godfather\",\n"
                + "  \"The Dark";
//        String cleaned = Mapper.toCleanJsonList(response);
//        System.out.println("Cleaned: " + cleaned);
        List<String> parsed = Mapper.fromJsonListToList(response);
        log.debug("Parsed: {}", parsed);
        Assertions.assertEquals(3, parsed.size());
    }

    @Test
    public void testParseIncompleteResponse2() {
        String response = "[\n"
                + "  \"The Shawshank Redemption\",\n"
                + "  \"The Godfather\",\n"
                + "  \"The Dark\", ";
//        String cleaned = Mapper.toCleanJsonList(response);
//        System.out.println("Cleaned: " + cleaned);
        List<String> parsed = Mapper.fromJsonListToList(response);
        log.debug("Parsed: {}", parsed);
        Assertions.assertEquals(3, parsed.size());

    }

    @Test
    public void testParseCompleteResponseList() {
        String response = "[\n"
                + "  \"The Shawshank Redemption\",\n"
                + "  \"The Godfather\",\n"
                + "  \"The Dark\" \n]";
//        String cleaned = Mapper.toCleanJsonList(response);
//        System.out.println("Cleaned: " + cleaned);
        List<String> parsed = Mapper.fromJsonListToList(response);
        log.debug("Parsed: {}", parsed);
        Assertions.assertEquals(3, parsed.size());
    }

    @Test
    public void testParseCompleteResponseList2() {
        String response = "[\n"
                + "	{\n"
                + "		\"name\": \"n1\",\n"
                + "	 	\"value\": 1\n"
                + "	 },\n"
                + "	 {\n"
                + "		\"name\": \"n2\",\n"
                + "	 	\"value\": 2\n"
                + "	 },\n"
                + "	 {\n"
                + "		\"name\": \"n3\",\n"
                + "	 	\"value\": 3\n"
                + "	 }\n"
                + "]";
        List<Map<String, Object>> parsed = Mapper.fromJsonToListOfMaps(response);
        log.debug("Parsed: {}", parsed);
        Assertions.assertEquals(3, parsed.size());

    }

    @Test
    public void testParseIncompleteResponseList3() {
        String response = "[\n"
                + "	{\n"
                + "		\"name\": \"n1\",\n"
                + "	 	\"value\": 1\n"
                + "	 },\n"
                + "	 {\n"
                + "		\"name\": \"n2\",\n"
                + "	 	\"value\": 2\n"
                + "	 },\n"
                + "	 {\n"
                + "		\"name\": \"n3\",\n"
                + "	 	\"value\": 3\n"
                + "	 }\n";
        List<Map<String, Object>> parsed = Mapper.fromJsonToListOfMaps(response);
        log.debug("Parsed: {}", parsed);
        Assertions.assertEquals(3, parsed.size());
    }

    @Test
    public void testParseIncompleteResponseList4() {
        String response = "[\n"
                + "	{\n"
                + "		\"name\": \"n1\",\n"
                + "	 	\"value\": 1\n"
                + "	 },\n"
                + "	 {\n"
                + "		\"name\": \"n2\",\n"
                + "	 	\"value\": 2\n"
                + "	 },\n"
                + "	 {\n"
                + "		\"name\": \"n3\",\n"
                + "	 	\"value\": 3\n";
        List<Map<String, Object>> parsed = Mapper.fromJsonToListOfMaps(response);
        log.debug("Parsed: {}", parsed);
        Assertions.assertEquals(2, parsed.size());
    }

    @Test
    public void testParseIncompleteResponseList5() {
        String response = "[\n"
                + "	{\n"
                + "		\"name\": \"n1\",\n"
                + "	 	\"value\": 1\n"
                + "	 },\n"
                + "	 {\n"
                + "		\"name\": \"n2\",\n"
                + "	 	\"value\": 2\n"
                + "	 },\n";
        List<Map<String, Object>> parsed = Mapper.fromJsonToListOfMaps(response);
        log.debug("Parsed: {}", parsed);
        Assertions.assertEquals(2, parsed.size());

    }

    @Test
    public void testParseIncompleteAndWithDuplicatesResponseList(){
        String response = """
                [
                  {
                    "opponent_team": "Wolves",
                    "match_date_year": 2024,
                    "match_date_month": 8,
                    "match_date_day": 12
                  },
                  {
                    "opponent_team": "Leicester",
                    "match_date_year": 2024,
                    "match_date_month": 8,
                    "match_date_day": 26
                  },
                  {
                    "opponent_team": "Leicester",
                    "match_date_year": 2024,
                    "match_date_month": 8,
                    "match_date_day": 26
                  },
                  {
                    "opponent_team": "Leicester",
                    "match_date_year": 2024,
                    "match_date_month": 8,
                    "match_date_day": 26
                  },
                  {
                    "opponent_team": "Leicester",
                    "match_date_year": 2024,
                    "match_date_month": 8,
                    "match_date_day": 26
                  },
                  {
                    "opponent_team": "Leicester",
                    "match_date_year": 2024,
                    "match_date_month": 8,
                    "match_date_day": 26
                  },
                  {
                    "opponent_team": "Leicester",
                    "match_date_year": 2024,
                    "match_date_month": 8,
                    "match_date_day": 26
                  },
                  {
                    "opponent_team": "Leicester",
                    "match_date_year": 2024,
                    "match_date_month": 8,
                    "match_date_day": 26
                  },
                  {
                    "opponent_team": "Leicester",
                    "match_date_year": 2024,
                    "match_date_month": 8,
                    "match_date_day": 26
                  },
                  {
                    "opponent_team": "Leicester",
                    "match_date_year": 2024,
                    "match_date_month": 8,
                    "match_date_day": 26
                  },
                  {
                    "opponent_team": "Leicester",
                    "match_date_year": 2024,
                    "match_date_month": 8,
                    "match_date_day": 26
                  },
                  {
                    "opponent_team": "Leicester",
                    "match_date_year": 2024,
                    "match_date_month": 8,
                    "match_date_day": 26
                  },
                  {
                    "opponent_team": "Leicester",
                    "match_date_year": 2024,
                    "match_date_month": 8,
                    "match_date_day": 26
                  },
                  {
                    "opponent_team": "Leicester",
                    "match_date_year": 2024,
                    "match_date_month": 8,
                    "match_date_day": 26
                  },
                  {
                    "opponent_team": "Leicester",
                    "match_date_year": 2024,
                    "match_date_month": 8,
                    "match_date_day": 26
                  },
                  {
                    "opponent_team": "Leicester",
                    "match_date_year": 2024,
                    "match_date_month": 8,
                    "match_date_day": 26
                  },
                  {
                    "opponent_team": "Leicester",
                    "match_date_year": 2024,
                    "match_date_month": 8,
                    "match_date_day": 26
                  },
                  {
                    "opponent_team": "Leicester",
                    "match_date_year": 2024,
                    "match_date_month": 8,
                    "match_date_day": 26
                  },
                  {
                    "opponent_team": "Leicester",
                    "match_date_year": 2024,
                    "match_date_month": 8,
                    "match_date_day": 26
                  },
                  {
                    "opponent_team": "Leicester",
                    "match_date_year": 2024,
                    "match_date_month": 8,
                    "match_date_day": 26
                  },
                  {
                    "opponent_team": "Leicester",
                    "match_date_year": 2024,
                    "match_date_month": 8,
                    "match_date_day": 26
                  },
                  {
                    "opponent_team": "Leicester",
                    "match_date_year": 2024,
                    "match_date_month": 8,
                    "match_date_day": 26
                  },
                  {
                    "opponent_team": "Leicester",
                    "match_date_year": 2024,
                    "match_date_month": 8,
                    "match_date_day": 26
                  },
                  {
                    "opponent_team": "Leicester",
                    "match_date_year": 2024,
                    "match_date_month": 8,
                    "match_date_day": 26
                  },
                  {
                    "opponent_team": "Leicester",
                    "match_date_year": 2024,
                    "match_date_month": 8,
                    "match_date_day": 26
                  },
                  {
                    "opponent_team": "Leicester",
                    "match_date_year": 2024,
                    "match_date_month": 8,
                    "match_date_day": 26
                  },
                  {
                    "opponent_team": "Leicester",
                    "match_date_year": 2024,
                    "match_date_month": 8,
                    "match_date_day": 26
                  },
                  {
                    "opponent_team": "Leicester",
                    "match_date_year": 2024,
                    "match_date_month": 8,
                    "match_date_day": 26
                  },
                  {
                    "opponent_team": "Leicester",
                    "match_date_year": 2024,
                    "match_date_month": 8,
                    "match_date_day": 26
                  },
                  {
                    "opponent_team": "Leicester",
                    "match_date_year": 2024,
                    "match_date_month": 8,
                    "match_date_day": 26
                  },
                  {
                    "opponent_team": "Leicester",
                    "match_date_year": 2024,
                    "match_date_month": 8,
                    "match_date_day": 26
                  },
                  {
                    "opponent_team": "Leicester",
                    "match_date_year": 2024,
                    "match_date_month": 8,
                    "match_date_day": 26
                  },
                  {
                    "opponent_team": "Leicester",
                    "match_date_year": 2024,
                    "match_date_month": 8,
                    "match_date_day": 26
                  },
                  {
                    "opponent_team": "Leicester",
                    "match_date_year": 2024,
                    "match_date_month": 8,
                    "match_date_day": 26
                  },
                  {
                    "opponent_team": "Leicester",
                    "match_date_year": 2024,
                    "match_date_month": 8,
                    "match_date_day": 26
                  },
                  {
                    "opponent_team": "Leicester",
                    "match_date_year": 2024,
                    "match_date_month": 8,
                    "match_date_day": 26
                  },
                  {
                    "opponent_team": "Leicester",
                    "match_date_year": 2024,
                    "match_date_month": 8,
                    "match_date_day": 26
                  },
                  {
                    "opponent_team": "Leicester",
                    "match_date_year": 2024,
                    "match_date_month": 8,
                    "match_date_day": 26
                  },
                  {
                    "opponent_team": "Leicester",
                    "match_date_year": 2024,
                    "match_date_month": 8,
                    "match_date_day": 26
                  },
                  {
                    "opponent_team": "Leicester",
                    "match_date_year": 2024,
                    "match_date_month": 8,
                    "match_date_day": 26
                  },
                  {
                    "opponent_team": "Leicester",
                    "match_date_year": 2024,
                    "match_date_month": 8,
                    "match_date_day": 26
                  },
                  {
                    "opponent_team": "Leicester",
                    "match_date_year": 2024,
                    "match_date_month": 8,
                    "match_date_day": 26
                  },
                  {
                    "opponent_team": "Leicester",
                    "match_date_year": 2024,
                    "match_date_month": 8,
                    "match_date_day": 26
                  },
                  {
                    "opponent_team": "Leicester",
                    "match_date_year": 2024,
                    "match_date_month": 8,
                    "match_date_day": 26
                  },
                  {
                    "opponent_team": "Leicester",
                    "match_date_year": 2024,
                    "match_date_month": 8,
                    "match_date_day": 26
                  },
                  {
                    "opponent_team": "Leicester",
                    "match_date_year": 2024,
                    "match_date_month": 8,
                    "match_date_day": 26
                  },
                  {
                    "opponent_team": "Leicester",
                    "match_date_year": 2024,
                    "match_date_month": 8,
                    "match_date_day": 26
                  },
                  {
                    "opponent_team": "Leicester",
                    "match_date_year": 2024,
                    "match_date_month": 8,
                    "match_date_day": 26
                  },
                  {
                    "opponent_team": "Leicester",
                    "match_date_year": 2024,
                    "match_date_month": 8,
                    "match_date_day": 26
                  },
                  {
                    "opponent_team": "Leicester",
                    "match_date_year": 2024,
                    "match_date_month": 8,
                    "match_date_day": 26
                  },
                  {
                    "opponent_team": "Leicester",
                    "match_date_year": 2024,
                    "match_date_month": 8,
                    "match_date_day": 26
                  },
                  {
                    "opponent_team": "Leicester",
                    "match_date_year": 2024,
                    "match_date_month": 8,
                    "match_date_day": 26
                  },
                  {
                    "opponent_team": "Leicester",
                    "match_date_year": 2024,
                    "match_date_month": 8,
                    "match_date_day": 26
                  },
                  {
                    "opponent_team": "Leicester",
                    "match_date_year": 2024,
                    "match_date_month": 8,
                    "match_date_day": 26
                  },
                  {
                    "opponent_team": "Leicester",
                    "match_date_year": 2024,
                    "match_date_month": 8,
                    "match_date_day": 26
                  },
                  {
                    "opponent_team": "Leicester",
                    "match_date_year": 2024,
                    "match_date_month": 8,
                    "match_date_day": 26
                  },
                  {
                    "opponent_team": "Leicester",
                    "match_date_year": 2024,
                    "match_date_month": 8,
                    "match_date_day": 26
                  },
                  {
                    "opponent_team": "Leicester",
                    "match_date_year": 2024,
                    "match_date_month": 8,
                    "match_date_day": 26
                  },
                  {
                    "opponent_team": "Leicester",
                    "match_date_year": 2024,
                    "match_date_month": 8,
                    "match_date_day": 26
                  },
                  {
                    "opponent_team": "Leicester",
                    "match_date_year": 2024,
                    "match_date_month": 8,
                    "match_date_day": 26
                  },
                  {
                    "opponent_team": "Leicester",
                    "match_date_year": 2024,
                    "match_date_month": 8,
                    "match_date_day": 26
                  },
                  {
                    "opponent_team": "Leicester",
                    "match_date_year": 2024,
                    "match_date_month": 8,
                    "match_date_day": 26
                  },
                  {
                    "opponent_team": "Leicester",
                    "match_date_year": 2024,
                    "match_date_month": 8,
                    "match_date_day": 26
                  },
                  {
                    "opponent_team": "Leicester",
                    "match_date_year": 2024,
                    "match_date_month": 8,
                    "match_date_day": 26
                  },
                  {
                    "opponent_team": "Leicester",
                    "match_date_year": 2024,
                    "match_date_month": 8,
                    "match_date_day": 26
                  },
                  {
                    "opponent_team": "Leicester",
                    "match_date_year": 2024,
                    "match_date_month": 8,
                    "match_date_day": 26
                  },
                  {
                    "opponent_team": "Leicester",
                    "match_date_year": 2024,
                    "match_date_month": 8,
                    "match_date_day": 26
                  },
                  {
                    "opponent_team": "Leicester",
                    "match_date_year": 2024,
                    "match_date_month": 8,
                    "match_date_day": 26
                  },
                  {
                    "opponent_team": "Leicester",
                    "match_date_year": 2024,
                    "match_date_month": 8,
                    "match_date_day": 26
                  },
                  {
                    "opponent_team": "Leicester",
                    "match_date_year": 2024,
                    "match_date_month": 8,
                    "match_date_day": 26
                  },
                  {
                    "opponent_team": "Leicester",
                    "match_date_year": 2024,
                    "match_date_month": 8,
                    "match_date_day": 26
                  },
                  {
                    "opponent_team": "Leicester",
                    "match_date_year": 2024,
                    "match_date_month": 8,
                    "match_date_day": 26
                  },
                  {
                    "opponent_team": "Leicester",
                    "match_date_year": 2024,
                    "match_date_month": 8,
                    "match_date_day": 26
                  },
                  {
                    "opponent_team": "Leicester",
                    "match_date_year": 2024,
                    "match_date_month": 8,
                    "match_date_day": 26
                  },
                  {
                    "opponent_team": "Leicester",
                    "match_date_year": 2024,
                    "match_date_month": 8,
                    "match_date_day": 26
                  },
                  {
                    "opponent_team": "Leicester",
                    "match_date_year": 2024,
                    "match_date_month": 8,
                    "match_date_day": 26
                  },
                  {
                    "opponent_team": "Leicester",
                    "match_date_year": 2024,
                    "match_date_month": 8,
                    "match_date_day": 26
                  },
                  {
                    "opponent_team": "Leicester",
                    "match_date_year": 2024,
                    "match_date_month": 8,
                    "match_date_day": 26
                  },
                  {
                    "opponent_team": "Leicester",
                    "match_date_year": 2024,
                    "match_date_month": 8,
                    "match_date_day": 26
                  },
                  {
                    "opponent_team": "Leicester",
                    "match_date_year": 2024,
                    "match_date_month": 8,
                    "match_date_day": 26
                  },
                  {
                    "opponent_team": "Leicester",
                    "match_date_year": 2024,
                    "match_date_month": 8,
                    "match_date_day": 26
                  },
                  {
                    "opponent_team": "Leicester",
                    "match_date_year": 2024,
                    "match_date_month": 8,
                    "match_date_day": 26
                  },
                  {
                    "opponent_team": "Leicester",
                    "match_date_year": 2024,
                    "match_date_month": 8,
                    "match_date_day": 26
                  },
                  {
                    "opponent_team": "Leicester",
                    "match_date_year": 2024,
                    "match_date_month": 8,
                    "match_date_day": 26
                  },
                  {
                    "opponent_team": "Leicester",
                    "match_date_year": 2024,
                    "match_date_month": 8,
                    "match_date_day": 26
                  },
                  {
                    "opponent_team": "Leicester",
                    "match_date_year": 2024,
                    "match_date_month": 8,
                    "match_date_day": 26
                  },
                  {
                    "opponent_team": "Leicester",
                    "match_date_year": 2024,
                    "match_date_month": 8,
                    "match_date_day": 26
                  },
                  {
                    "opponent_team": "Leicester",
                    "match_date_year": 2024,
                    "match_date_month": 8,
                    "match_date_day": 26
                  },
                  {
                    "opponent_team": "Leicester",
                    "match_date_year": 2024,
                    "match_date_month": 8,
                    "match_date_day": 26
                  },
                  {
                    "opponent_team": "Leicester",
                    "match_date_year": 2024,
                    "match_date_month": 8,
                    "match_date_day": 26
                  },
                  {
                    "opponent_team": "Leicester",
                    "match_date_year": 2024,
                    "match_date_month": 8,
                    "match_date_day": 26
                  },
                  {
                    "opponent_team": "Leicester",
                    "match_date_year": 2024,
                    "match_date_month": 8,
                    "match_date_day": 26
                  },
                  {
                    "opponent_team": "Leicester",
                    "match_date_year": 2024,
                    "match_date_month": 8,
                    "match_date_day": 26
                  },
                  {
                    "opponent_team": "Leicester",
                    "match_date_year": 2024,
                    "match_date_month": 8,
                    "match_date_day": 26
                  },
                  {
                    "opponent_team": "Leicester",
                    "match_date_year": 2024,
                    "match_date_month": 8,
                    "match_date_day": 26
                  },
                  {
                    "opponent_team": "Leicester",
                    "match_date_year": 2024,
                    "match_date_month": 8,
                    "match_date_day": 26
                  },
                  {
                    "opponent_team": "Leicester",
                    "match_date_year": 2024,
                    "match_date_month": 8,
                    "match_date_day": 26
                  },
                  {
                    "opponent_team": "Leicester",
                    "match_date_year": 2024
                """;
        List<Map<String, Object>> parsed = Mapper.fromJsonToListOfMaps(response);
        log.debug("Parsed: {}", parsed);
        Assertions.assertEquals(2, parsed.size());
    }

}
