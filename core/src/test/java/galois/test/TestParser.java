package galois.test;

import com.galois.sqlparser.SQLQueryParser;
import galois.llm.algebra.LLMScan;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import speedy.model.algebra.IAlgebraOperator;
import speedy.model.algebra.Join;
import speedy.model.algebra.Project;
import speedy.model.database.ITable;
import speedy.model.database.mainmemory.MainMemoryDB;
import speedy.persistence.DAOMainMemoryDatabase;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.*;

@Slf4j
public class TestParser {
    private static final String TABLE_NAME = "EmpTable";

    private MainMemoryDB db;

    @BeforeEach
    public void setUp() {
        String schema = requireNonNull(TestParser.class.getResource("/employees/mainmemory/schema.xsd")).getFile();
        String instance = requireNonNull(TestParser.class.getResource("/employees/mainmemory/50_emp.xml")).getFile();
        db = new DAOMainMemoryDatabase().loadXMLDatabase(schema, instance);
    }

    @Test
    public void testSelectStarLLM() {
        String sql = String.format("select * from %s t", TABLE_NAME);
        IAlgebraOperator root = new SQLQueryParser().parse(sql, (tableAlias, attributes) -> new LLMScan(tableAlias, null, attributes, null));
        log.info("root is {}", root);
        assertNotNull(root);
        assertInstanceOf(LLMScan.class, root);
        LLMScan llmScan = (LLMScan) root;
        // If the attributes are empty, everything is fetched
        assertEquals(0, llmScan.getAttributesSelect().size());
    }

    @Test
    public void testSelectAliasStarLLM() {
        String sql = String.format("select t.* from %s t", TABLE_NAME);
        IAlgebraOperator root = new SQLQueryParser().parse(sql, (tableAlias, attributes) -> new LLMScan(tableAlias, null, attributes, null));
        log.info("root is {}", root);
        assertNotNull(root);
        assertInstanceOf(LLMScan.class, root);
        LLMScan llmScan = (LLMScan) root;
        // If the attributes are empty, everything is fetched
        assertEquals(0, llmScan.getAttributesSelect().size());
    }

    @Test
    public void testSelectCountStarLLM() {
        String sql = String.format("select count(*) from %s", TABLE_NAME);
        ITable table = db.getTable(TABLE_NAME);

        IAlgebraOperator root = new SQLQueryParser().parse(sql, (tableAlias, attributes) -> new LLMScan(tableAlias, null, attributes, null));
        log.info("root is {}", root);
        assertNotNull(root);

        assertInstanceOf(Project.class, root);
        Project project = (Project) root;
        assertEquals(1, project.getAggregateFunctions().size());
        assertTrue(project.isAggregative());
        assertEquals(1, project.getChildren().size());
        assertInstanceOf(LLMScan.class, project.getChildren().getFirst());

        LLMScan llmScan = (LLMScan) project.getChildren().getFirst();
        // If the attributes are empty, everything is fetched
        assertEquals(0, llmScan.getAttributesSelect().size());
    }

    @Test
    public void testSelectCountStarAliasLLM() {
        String sql = String.format("select count(t.*) from %s t", TABLE_NAME);
        ITable table = db.getTable(TABLE_NAME);

        IAlgebraOperator root = new SQLQueryParser().parse(sql, (tableAlias, attributes) -> new LLMScan(tableAlias, null, attributes, null));
        log.info("root is {}", root);
        assertNotNull(root);

        assertInstanceOf(Project.class, root);
        Project project = (Project) root;
        assertEquals(1, project.getAggregateFunctions().size());
        assertTrue(project.isAggregative());
        assertEquals(1, project.getChildren().size());
        assertInstanceOf(LLMScan.class, project.getChildren().getFirst());

        LLMScan llmScan = (LLMScan) project.getChildren().getFirst();
        // If the attributes are empty, everything is fetched
        assertEquals(0, llmScan.getAttributesSelect().size());
    }

    @Test
    public void testSelectCountAttributeLLM() {
        String attribute = "name";
        String sql = String.format("select count(%s) from %s", attribute, TABLE_NAME);

        IAlgebraOperator root = new SQLQueryParser().parse(sql, (tableAlias, attributes) -> new LLMScan(tableAlias, null, attributes, null));
        log.info("root is {}", root);
        assertNotNull(root);

        assertInstanceOf(Project.class, root);
        Project project = (Project) root;
        assertEquals(1, project.getAggregateFunctions().size());
        assertTrue(project.isAggregative());
        assertEquals(1, project.getChildren().size());
        assertInstanceOf(LLMScan.class, project.getChildren().getFirst());

        LLMScan llmScan = (LLMScan) project.getChildren().getFirst();
        assertEquals(1, llmScan.getAttributesSelect().size());
        assertEquals(attribute, llmScan.getAttributesSelect().getFirst().getName());
    }

    @Test
    public void testSelectCountAttributeAliasLLM() {
        String attribute = "name";
        String sql = String.format("select count(t.%s) from %s t", attribute, TABLE_NAME);

        IAlgebraOperator root = new SQLQueryParser().parse(sql, (tableAlias, attributes) -> new LLMScan(tableAlias, null, attributes, null));
        log.info("root is {}", root);
        assertNotNull(root);

        assertInstanceOf(Project.class, root);
        Project project = (Project) root;
        assertEquals(1, project.getAggregateFunctions().size());
        assertTrue(project.isAggregative());
        assertEquals(1, project.getChildren().size());
        assertInstanceOf(LLMScan.class, project.getChildren().getFirst());

        LLMScan llmScan = (LLMScan) project.getChildren().getFirst();
        assertEquals(1, llmScan.getAttributesSelect().size());
        assertEquals(attribute, llmScan.getAttributesSelect().getFirst().getName());
    }

    @Test
    public void testSimpleJoin() {
        String sql = String.format("select * from %s t1 join %s t2 on t1.manager = t2.name", TABLE_NAME, TABLE_NAME);

        IAlgebraOperator root = new SQLQueryParser().parse(sql, (tableAlias, attributes) -> new LLMScan(tableAlias, null, attributes, null));
        log.info("root is {}", root);
        assertNotNull(root);
        assertInstanceOf(Join.class, root);

        Join join = (Join) root;
        assertEquals(2, join.getChildren().size());
        assertInstanceOf(LLMScan.class, join.getChildren().getFirst());
        assertInstanceOf(LLMScan.class, join.getChildren().getLast());
        LLMScan first = (LLMScan) join.getChildren().getFirst();
        LLMScan last = (LLMScan) join.getChildren().getLast();

        assertEquals(1, join.getLeftAttributes().size());
        assertEquals("manager", join.getLeftAttributes().getFirst().getName());

        assertEquals(1, join.getRightAttributes().size());
        assertEquals("name", join.getRightAttributes().getFirst().getName());

        assertEquals(0, first.getAttributesSelect().size());
        assertEquals(0, last.getAttributesSelect().size());
    }

    @Test
    public void testJoinStarLeft() {
        String sql = String.format("select t1.*, t2.name from %s t1 join %s t2 on t1.manager = t2.name", TABLE_NAME, TABLE_NAME);

        IAlgebraOperator root = new SQLQueryParser().parse(sql, (tableAlias, attributes) -> new LLMScan(tableAlias, null, attributes, null));
        log.info("root is {}", root);
        assertNotNull(root);
//        assertInstanceOf(Join.class, root);
//
//        Join join = (Join) root;
//        assertEquals(2, join.getChildren().size());
//        assertInstanceOf(LLMScan.class, join.getChildren().getFirst());
//        assertInstanceOf(LLMScan.class, join.getChildren().getLast());
//        LLMScan first = (LLMScan) join.getChildren().getFirst();
//        LLMScan last = (LLMScan) join.getChildren().getLast();
//
//        assertEquals(1, join.getLeftAttributes().size());
//        assertEquals("manager", join.getLeftAttributes().getFirst().getName());
//
//        assertEquals(1, join.getRightAttributes().size());
//        assertEquals("name", join.getRightAttributes().getFirst().getName());
//
//        assertEquals(0, first.getAttributesSelect().size());
//        assertEquals(0, last.getAttributesSelect().size());
    }
}
