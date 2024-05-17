package galois.prompt.key;

import java.util.Set;

@FunctionalInterface
public interface IKeyResponseParser {
    Set<String> parse(String response);
}
