package galois.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

import static galois.utils.FunctionalUtils.orElseThrow;

public class Mapper {
    private static final TypeReference<HashMap<String, Object>> JSON_REF = new TypeReference<>() {
    };

    private static final ObjectMapper mapper = new ObjectMapper();

    public static String asString(Object value) {
        return orElseThrow(
                () -> mapper.writeValueAsString(value),
                MapperException::new
        );
    }

    public static Map<String, Object> fromJSON(String value) {
        return orElseThrow(
                () -> value != null ?
                        mapper.readValue(value, JSON_REF) :
                        null,
                MapperException::new
        );
    }

    private static final class MapperException extends RuntimeException {
        public MapperException(Throwable cause) {
            super(cause);
        }
    }
}
