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
    
    @Test
    public void testProvenance4() {
        String sql = "SELECT us.area_squared_miles, min(us.population), max(us.population), avg(us.population) FROM target.usa_state us WHERE us.state_name = 'new mexico' GROUP BY us.state ORDER BY us.density ASC";
        ParserProvenance parser = new ParserProvenance();
        parser.parse(sql);
        Set<String> tables = parser.getTablesProvenance();
        Set<String> attributes = parser.getAttributeProvenance();
        log.info("SQL: {}", sql);
        log.info("Tables: {}", tables);
        log.info("Attributes: {}", attributes);
    }
    
    @Test
    public void testProvenance5() {
        String sql = "SELECT t2.capital FROM usa_city AS t1 JOIN usa_state AS t2 ON t1.city_name=t2.capital WHERE t1.population <= 150000";
        ParserProvenance parser = new ParserProvenance();
        parser.parse(sql);
        Set<String> tables = parser.getTablesProvenance();
        Set<String> attributes = parser.getAttributeProvenance();
        log.info("SQL: {}", sql);
        log.info("Tables: {}", tables);
        log.info("Attributes: {}", attributes);
    }
}
