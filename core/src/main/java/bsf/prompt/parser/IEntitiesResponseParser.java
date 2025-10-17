package bsf.prompt.parser;

import queryexecutor.model.database.ITable;

import java.util.List;
import java.util.Map;

@FunctionalInterface
public interface IEntitiesResponseParser {
    List<Map<String, Object>> parse(String response, ITable table);
}
