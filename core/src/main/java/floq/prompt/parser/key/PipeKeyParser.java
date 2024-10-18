package floq.prompt.parser.key;

import java.util.Arrays;
import java.util.List;

public class PipeKeyParser {
    public static List<String> parse(String response) {
        return Arrays.stream(response.split("\\|"))
                .map(String::trim)
                .filter(k -> !k.isBlank())
                .toList();
    }
}
