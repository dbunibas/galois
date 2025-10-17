package bsf.prompt.parser.entities;

import queryexecutor.model.database.ITable;

import java.util.List;
import java.util.Map;

import static bsf.utils.Mapper.fromJsonToListOfMaps;

public class JSONEntitiesParser {

    public static List<Map<String, Object>> parse(String response, ITable table) {
        return fromJsonToListOfMaps(response, false);
    }

    public static List<Map<String, Object>> parseAndRemoveDuplicates(String response, ITable table) {
        return fromJsonToListOfMaps(response, true);
    }
}
