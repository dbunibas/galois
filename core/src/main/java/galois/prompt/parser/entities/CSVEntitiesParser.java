package galois.prompt.parser.entities;

import speedy.model.database.ITable;

import java.util.List;
import java.util.Map;

import static galois.utils.Mapper.fromCSVToListOfMaps;

public class CSVEntitiesParser {
    public static List<Map<String, Object>> parse(String response, ITable table) {
        return fromCSVToListOfMaps(response);
    }
}
