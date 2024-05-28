package galois.prompt.parser.key;

import java.util.Arrays;
import java.util.List;

public class CommaKeyParser {
    public static List<String> parse(String response) {
        return Arrays.stream(response.split(","))
                .map(String::trim)
                .filter(k -> !k.isBlank())
                .toList();
    }
}
