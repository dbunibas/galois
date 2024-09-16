package galois.test;

import galois.parser.ParserProvenance;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import speedy.model.database.IDatabase;
import speedy.model.database.mainmemory.MainMemoryDB;
import speedy.model.database.mainmemory.MainMemoryVirtualDB;
import speedy.persistence.DAOMainMemoryDatabase;

@Slf4j
public class TestProvenance {

    @Test
    public void testProvenance1() {
        String sql = "SELECT area_squared_miles FROM target.usa_state WHERE state_name = 'new mexico'";
        ParserProvenance parser = new ParserProvenance(null);
        parser.parse(sql);
        Set<String> tables = parser.getTablesProvenance();
        Set<String> attributes = parser.getAttributeProvenance();
        log.info("SQL: {}", sql);
        log.info("Tables: {}", tables);
        log.info("Attributes: {}", attributes);
        Assertions.assertTrue(tables.size() == 1);
        Assertions.assertTrue(tables.contains("usa_state"));
        Assertions.assertTrue(attributes.size() == 2);
        Assertions.assertTrue(attributes.contains("area_squared_miles"));
        Assertions.assertTrue(attributes.contains("state_name"));
    }

    @Test
    public void testProvenance2() {
        String sql = "SELECT area_squared_miles, min(population), max(population), avg(population) FROM target.usa_state WHERE state_name = 'new mexico'";
        ParserProvenance parser = new ParserProvenance(null);
        parser.parse(sql);
        Set<String> tables = parser.getTablesProvenance();
        Set<String> attributes = parser.getAttributeProvenance();
        log.info("SQL: {}", sql);
        log.info("Tables: {}", tables);
        log.info("Attributes: {}", attributes);
        Assertions.assertTrue(tables.size() == 1);
        Assertions.assertTrue(tables.contains("usa_state"));
        Assertions.assertTrue(attributes.size() == 3);
        Assertions.assertTrue(attributes.contains("area_squared_miles"));
        Assertions.assertTrue(attributes.contains("population"));
        Assertions.assertTrue(attributes.contains("state_name"));
    }

    @Test
    public void testProvenance3() {
        String sql = "SELECT area_squared_miles, min(population), max(population), avg(population) FROM target.usa_state WHERE state_name = 'new mexico' GROUP BY state ORDER BY density ASC";
        ParserProvenance parser = new ParserProvenance(null);
        parser.parse(sql);
        Set<String> tables = parser.getTablesProvenance();
        Set<String> attributes = parser.getAttributeProvenance();
        log.info("SQL: {}", sql);
        log.info("Tables: {}", tables);
        log.info("Attributes: {}", attributes);
        Assertions.assertTrue(tables.size() == 1);
        Assertions.assertTrue(tables.contains("usa_state"));
        Assertions.assertTrue(attributes.size() == 5);
        Assertions.assertTrue(attributes.contains("area_squared_miles"));
        Assertions.assertTrue(attributes.contains("population"));
        Assertions.assertTrue(attributes.contains("state_name"));
        Assertions.assertTrue(attributes.contains("state"));
        Assertions.assertTrue(attributes.contains("density"));
    }

    @Test
    public void testProvenance4() {
        String sql = "SELECT us.area_squared_miles, min(us.population), max(us.population), avg(us.population) FROM target.usa_state us WHERE us.state_name = 'new mexico' GROUP BY us.state ORDER BY us.density ASC";
        ParserProvenance parser = new ParserProvenance(null);
        parser.parse(sql);
        Set<String> tables = parser.getTablesProvenance();
        Set<String> attributes = parser.getAttributeProvenance();
        log.info("SQL: {}", sql);
        log.info("Tables: {}", tables);
        log.info("Attributes: {}", attributes);
        Assertions.assertTrue(tables.size() == 1);
        Assertions.assertTrue(tables.contains("usa_state"));
        Assertions.assertTrue(attributes.size() == 5);
        Assertions.assertTrue(attributes.contains("area_squared_miles"));
        Assertions.assertTrue(attributes.contains("population"));
        Assertions.assertTrue(attributes.contains("state_name"));
        Assertions.assertTrue(attributes.contains("state"));
        Assertions.assertTrue(attributes.contains("density"));
    }

    @Test
    public void testProvenance5() {
        String sql = "SELECT t2.capital FROM usa_city AS t1 JOIN usa_state AS t2 ON t1.city_name=t2.capital WHERE t1.population <= 150000";
        ParserProvenance parser = new ParserProvenance(null);
        parser.parse(sql);
        Set<String> tables = parser.getTablesProvenance();
        Set<String> attributes = parser.getAttributeProvenance();
        log.info("SQL: {}", sql);
        log.info("Tables: {}", tables);
        log.info("Attributes: {}", attributes);
        Assertions.assertTrue(tables.size() == 2);
        Assertions.assertTrue(tables.contains("usa_state"));
        Assertions.assertTrue(tables.contains("usa_city"));
        Assertions.assertTrue(attributes.size() == 3);
        Assertions.assertTrue(attributes.contains("capital"));
        Assertions.assertTrue(attributes.contains("city_name"));
        Assertions.assertTrue(attributes.contains("population"));
    }

    @Test
    public void testProvencance6() {
        // Here we ignore the * and the num alias since we don't have the IDatabase database.
        // The num will be ignored since it isn't an attribute name, and thus it shoul be an attribute in the select
        // The * will discover all the attributes in all the tables in the query
        String sqlQueries[] = {
            "SELECT count(p.party) from target.world_presidents p WHERE p.country='United States' AND p.party='Republican'",
            "SELECT count(p.party) as party from target.world_presidents p WHERE p.country='United States' AND p.party='Republican'",
            "SELECT p.party, count(p.party) num from target.world_presidents p WHERE p.country='United States' group by p.party order by num desc limit 1",
            "SELECT count(*) from target.world_presidents p where p.country='United States' AND p.start_year >= 1990  AND p.start_year < 2000",
            "select distinct a.name from target.airports a where a.elevation >= -50 AND a.elevation <= 50",
            "select max(elevation) from target.airports where country = 'Iceland'",
            "SELECT avg(population) FROM usa_state",
            "SELECT count(distinct river_name) FROM usa_river where length_in_km > 400",
            "SELECT COUNT ( DISTINCT state_name ) FROM usa_city WHERE city_name = 'springfield' AND population > 72000"
        };
        List<List<String>> expectedAttributes = new ArrayList<>();
        expectedAttributes.add(List.of("party", "country"));
        expectedAttributes.add(List.of("party", "country"));
        expectedAttributes.add(List.of("party", "country"));
        expectedAttributes.add(List.of("country", "start_year"));
        expectedAttributes.add(List.of("name", "elevation"));
        expectedAttributes.add(List.of("country", "elevation"));
        expectedAttributes.add(List.of("population"));
        expectedAttributes.add(List.of("river_name", "length_in_km"));
        expectedAttributes.add(List.of("state_name", "city_name", "population"));        
        for (int i = 0; i < sqlQueries.length; i++) {
            String sql = sqlQueries[i];
            List<String> assertionAttributes = expectedAttributes.get(i);
            ParserProvenance parser = new ParserProvenance(null);
            parser.parse(sql);
            Set<String> tables = parser.getTablesProvenance();
            Set<String> attributes = parser.getAttributeProvenance();
            log.info("SQL: {}", sql);
            log.info("Attributes: {}", attributes);
            Assertions.assertTrue(attributes.size() == assertionAttributes.size());
            for (String assertionAttribute : assertionAttributes) {
                Assertions.assertTrue(attributes.contains(assertionAttribute));
            }
        }
    }
}
