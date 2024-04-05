package galois.test;

import galois.planner.IQueryPlanner;
import galois.planner.postgresql.xml.PostgresXMLPlanner;
import org.jdom2.Document;
import org.jdom2.Element;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import speedy.persistence.relational.AccessConfiguration;

public class TestPostgresXMLPlanner {
    private static AccessConfiguration accessConfiguration;

    @BeforeAll
    public static void beforeAll() {
        String driver = "org.postgresql.Driver";
        String uri = "jdbc:postgresql:speedy_llm_actors";
        String schemaName = "target";
        String username = "pguser";
        String password = "pguser";

        accessConfiguration = new AccessConfiguration();
        accessConfiguration.setDriver(driver);
        accessConfiguration.setUri(uri);
        accessConfiguration.setSchemaName(schemaName);
        accessConfiguration.setLogin(username);
        accessConfiguration.setPassword(password);
    }

    @Test
    public void testPlanSimpleSelect() {
        String sql = "select * from target.actor a";
        IQueryPlanner<Document> planner = new PostgresXMLPlanner(accessConfiguration);
        Document plan = planner.planFrom(sql);
        Assertions.assertNotNull(plan);
        Element root = plan.getRootElement();
        Assertions.assertNotNull(root);
        Element query = root.getChild("Query", root.getNamespace());
        Assertions.assertNotNull(query);
        Element planNode = query.getChild("Plan", query.getNamespace());
        Assertions.assertNotNull(planNode);
    }
}
