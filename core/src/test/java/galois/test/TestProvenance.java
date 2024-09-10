package galois.test;

import galois.parser.ParserProvenance;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;


@Slf4j
public class TestProvenance {
    
    @Test
    public void testProvenance1() {
        String sql = "SELECT area_squared_miles FROM target.usa_state WHERE state_name = 'new mexico'";
        ParserProvenance parser = new ParserProvenance();
        parser.parse(sql);
        Set<String> tables = parser.getTablesProvenance();
        Set<String> attributes = parser.getAttributeProvenance();
        log.info("SQL: {}", sql);
        log.info("Tables: {}", tables);
        log.info("Attributes: {}", attributes);
    }
    
    @Test
    public void testProvenance2() {
        String sql = "SELECT area_squared_miles, min(population), max(population), avg(population) FROM target.usa_state WHERE state_name = 'new mexico'";
        ParserProvenance parser = new ParserProvenance();
        parser.parse(sql);
        Set<String> tables = parser.getTablesProvenance();
        Set<String> attributes = parser.getAttributeProvenance();
        log.info("SQL: {}", sql);
        log.info("Tables: {}", tables);
        log.info("Attributes: {}", attributes);
    }
    
    @Test
    public void testProvenance3() {
        String sql = "SELECT area_squared_miles, min(population), max(population), avg(population) FROM target.usa_state WHERE state_name = 'new mexico' GROUP BY state ORDER BY density ASC";
        ParserProvenance parser = new ParserProvenance();
        parser.parse(sql);
        Set<String> tables = parser.getTablesProvenance();
        Set<String> attributes = parser.getAttributeProvenance();
        log.info("SQL: {}", sql);
        log.info("Tables: {}", tables);
        log.info("Attributes: {}", attributes);
    }
}
