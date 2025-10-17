package bsf.prompt.parser;

import java.util.List;

@FunctionalInterface
public interface IKeyResponseParser {
    List<String> parse(String response);
}
