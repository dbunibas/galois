package galois.prompt.key;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class CommaKeyParser {
    public static Set<String> parse(String response) {
        return Arrays.stream(response.split(",")).map(String::trim).collect(Collectors.toUnmodifiableSet());
    }
}
