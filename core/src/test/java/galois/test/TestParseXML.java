package galois.test;

import galois.llm.algebra.LLMScan;
import galois.llm.database.LLMDB;
import galois.parser.postgresql.PostgresXMLParser;
import galois.test.experiments.json.parser.OperatorsConfigurationParser;
import lombok.extern.slf4j.Slf4j;
import org.jdom2.Document;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import speedy.model.algebra.IAlgebraOperator;
import speedy.model.algebra.OrderBy;
import speedy.model.algebra.Project;
import speedy.model.algebra.Select;
import speedy.model.database.IDatabase;
import speedy.model.expressions.Expression;
import speedy.persistence.relational.AccessConfiguration;

import java.util.List;

import static galois.test.utils.TestUtils.buildDOMFromString;

@Slf4j
public class TestParseXML {
    private IDatabase llmDB;

    @BeforeEach
    public void setUp() {
        AccessConfiguration accessConfiguration = new AccessConfiguration();
        String driver = "org.postgresql.Driver";
        String uri = "jdbc:postgresql:speedy_llm_actors";
        String schemaName = "target";
        String username = "pguser";
        String password = "pguser";
        accessConfiguration.setDriver(driver);
        accessConfiguration.setUri(uri);
        accessConfiguration.setSchemaName(schemaName);
        accessConfiguration.setLogin(username);
        accessConfiguration.setPassword(password);
        llmDB = new LLMDB(accessConfiguration);
    }

    @Test
    public void testSimpleSelect() {
        // SQL: select * from target.actor a
        String sql = "select * from target.actor a";
        String xml = """
                <explain xmlns="http://www.postgresql.org/2009/explain">
                  <Query>
                    <Plan>
                      <Node-Type>Seq Scan</Node-Type>
                      <Parallel-Aware>false</Parallel-Aware>
                      <Relation-Name>actor</Relation-Name>
                      <Schema>target</Schema>
                      <Alias>a</Alias>
                      <Startup-Cost>0.00</Startup-Cost>
                      <Total-Cost>17.80</Total-Cost>
                      <Plan-Rows>780</Plan-Rows>
                      <Plan-Width>76</Plan-Width>
                      <Output>
                        <Item>oid</Item>
                        <Item>name</Item>
                        <Item>gender</Item>
                        <Item>birth_year</Item>
                      </Output>
                    </Plan>
                  </Query>
                </explain>
                """;

        Document queryPlan = buildDOMFromString(xml);

        PostgresXMLParser parser = new PostgresXMLParser();
        IAlgebraOperator operator = parser.parse(queryPlan, llmDB, OperatorsConfigurationParser.getDefault(), sql);

        Assertions.assertNotNull(operator, "Operator is null!");
        Assertions.assertTrue(operator instanceof LLMScan);

        LLMScan scan = (LLMScan) operator;
        Assertions.assertTrue(scan.getChildren().isEmpty());
        Assertions.assertEquals(scan.getTableAlias().getAlias(), "a");
        Assertions.assertEquals(scan.getTableAlias().getTableName(), "actor");
    }

    @Test
    public void testSelectOrderBy() {
        // SQL: select * from target.actor a order by a.name
        String sql  = "select * from target.actor a order by a.name";
        String xml = """
                <explain xmlns="http://www.postgresql.org/2009/explain">
                  <Query>
                    <Plan>
                      <Node-Type>Sort</Node-Type>
                      <Parallel-Aware>false</Parallel-Aware>
                      <Startup-Cost>59.86</Startup-Cost>
                      <Total-Cost>61.98</Total-Cost>
                      <Plan-Rows>850</Plan-Rows>
                      <Plan-Width>68</Plan-Width>
                      <Sort-Key>
                        <Item>name</Item>
                      </Sort-Key>
                      <Plans>
                        <Plan>
                          <Node-Type>Seq Scan</Node-Type>
                          <Parent-Relationship>Outer</Parent-Relationship>
                          <Parallel-Aware>false</Parallel-Aware>
                          <Relation-Name>actor</Relation-Name>
                          <Alias>a</Alias>
                          <Startup-Cost>0.00</Startup-Cost>
                          <Total-Cost>18.50</Total-Cost>
                          <Plan-Rows>850</Plan-Rows>
                          <Plan-Width>68</Plan-Width>
                        </Plan>
                      </Plans>
                    </Plan>
                  </Query>
                </explain>
                """;

        Document queryPlan = buildDOMFromString(xml);

        PostgresXMLParser parser = new PostgresXMLParser();
        IAlgebraOperator operator = parser.parse(queryPlan, llmDB, OperatorsConfigurationParser.getDefault(), sql);

        Assertions.assertNotNull(operator, "Operator is null!");
        Assertions.assertTrue(operator instanceof OrderBy);

        Assertions.assertEquals(operator.getChildren().size(), 1);
        log.info("{}", operator.getChildren().get(0).getName());
        Assertions.assertTrue(operator.getChildren().get(0) instanceof LLMScan);
    }

    @Test
    public void testSelect() {
        // SQL: select a.name from target.actor a
        String sql = "select a.name from target.actor a";
        String xml = """
                <explain xmlns="http://www.postgresql.org/2009/explain">
                  <Query>
                    <Plan>
                      <Node-Type>Seq Scan</Node-Type>
                      <Parallel-Aware>false</Parallel-Aware>
                      <Relation-Name>actor</Relation-Name>
                      <Schema>target</Schema>
                      <Alias>a</Alias>
                      <Startup-Cost>0.00</Startup-Cost>
                      <Total-Cost>18.50</Total-Cost>
                      <Plan-Rows>850</Plan-Rows>
                      <Plan-Width>32</Plan-Width>
                      <Output>
                        <Item>name</Item>
                      </Output>
                    </Plan>
                  </Query>
                </explain>
                """;

        Document queryPlan = buildDOMFromString(xml);

        PostgresXMLParser parser = new PostgresXMLParser();
        IAlgebraOperator operator = parser.parse(queryPlan, llmDB, OperatorsConfigurationParser.getDefault(), sql);

        Assertions.assertNotNull(operator, "Operator is null!");
        Assertions.assertTrue(operator instanceof Project);

        Assertions.assertEquals(operator.getChildren().size(), 1);
        Assertions.assertTrue(operator.getChildren().get(0) instanceof LLMScan);
    }

    @Test
    public void testWhere() {
        // SQL: select * from actor a where a.gender = 'Female'
        String sql = "select * from actor a where a.gender = 'Female'";
        String xml = """
                  <explain xmlns="http://www.postgresql.org/2009/explain">
                    <Query>
                      <Plan>
                        <Node-Type>Seq Scan</Node-Type>
                        <Parallel-Aware>false</Parallel-Aware>
                        <Relation-Name>actor</Relation-Name>
                        <Schema>target</Schema>
                        <Alias>a</Alias>
                        <Startup-Cost>0.00</Startup-Cost>
                        <Total-Cost>19.75</Total-Cost>
                        <Plan-Rows>4</Plan-Rows>
                        <Plan-Width>76</Plan-Width>
                        <Output>
                          <Item>oid</Item>
                          <Item>name</Item>
                          <Item>gender</Item>
                          <Item>birth_year</Item>
                        </Output>
                        <Filter>(a.gender = 'Female'::text)</Filter>
                      </Plan>
                    </Query>
                  </explain>
                """;

        Document queryPlan = buildDOMFromString(xml);

        PostgresXMLParser parser = new PostgresXMLParser();
        IAlgebraOperator operator = parser.parse(queryPlan, llmDB, OperatorsConfigurationParser.getDefault(), sql);

        Assertions.assertNotNull(operator, "Operator is null!");
        Assertions.assertTrue(operator instanceof Select, operator.getName());

        Select select = (Select) operator;
        List<Expression> selections = select.getSelections();
        Assertions.assertEquals(1, selections.size());

        Expression expression = selections.get(0);
        Assertions.assertEquals(1, expression.getVariables().size());
        Assertions.assertEquals("actor_a.gender", expression.getVariables().get(0));
    }

    @Test
    public void testWhereWithBooleanOperations() {
        // SQL: select * from actor a where a.birth_year > 1990 and (a.gender = 'Female' or a.gender = 'Male')
        String sql = "select * from actor a where a.birth_year > 1990 and (a.gender = 'Female' or a.gender = 'Male')";
        String xml = """
                <explain xmlns="http://www.postgresql.org/2009/explain">
                  <Query>
                    <Plan>
                      <Node-Type>Seq Scan</Node-Type>
                      <Parallel-Aware>false</Parallel-Aware>
                      <Relation-Name>actor</Relation-Name>
                      <Schema>target</Schema>
                      <Alias>a</Alias>
                      <Startup-Cost>0.00</Startup-Cost>
                      <Total-Cost>23.65</Total-Cost>
                      <Plan-Rows>3</Plan-Rows>
                      <Plan-Width>76</Plan-Width>
                      <Output>
                        <Item>oid</Item>
                        <Item>name</Item>
                        <Item>gender</Item>
                        <Item>birth_year</Item>
                      </Output>
                      <Filter>((a.birth_year &gt; 1990) AND ((a.gender = 'Female'::text) OR (a.gender = 'Male'::text)))</Filter>
                    </Plan>
                  </Query>
                </explain>
                """;

        Document queryPlan = buildDOMFromString(xml);

        PostgresXMLParser parser = new PostgresXMLParser();
        IAlgebraOperator operator = parser.parse(queryPlan, llmDB, OperatorsConfigurationParser.getDefault(), sql);

        Assertions.assertNotNull(operator, "Operator is null!");
        Assertions.assertTrue(operator instanceof Select, operator.getName());

        Select select = (Select) operator;
        List<Expression> selections = select.getSelections();
        Assertions.assertEquals(1, selections.size());

        Expression expression = selections.get(0);
        Assertions.assertEquals(2, expression.getVariables().size());
        Assertions.assertEquals("actor_a.birth_year", expression.getVariables().get(0));
        Assertions.assertEquals("actor_a.gender", expression.getVariables().get(1));
    }

    @Test
    @Disabled
    public void testWhereWithBooleanOperationsNot() {
        // SQL: select * from actor a where not a.birth_year > 1990 and (a.gender != 'Female' or a.gender = 'Male')
        String sql = "select * from actor a where not a.birth_year > 1990 and (a.gender != 'Female' or a.gender = 'Male')";
        String xml = """
                <explain xmlns="http://www.postgresql.org/2009/explain">
                  <Query>
                    <Plan>
                      <Node-Type>Seq Scan</Node-Type>
                      <Parallel-Aware>false</Parallel-Aware>
                      <Relation-Name>actor</Relation-Name>
                      <Schema>target</Schema>
                      <Alias>a</Alias>
                      <Startup-Cost>0.00</Startup-Cost>
                      <Total-Cost>23.65</Total-Cost>
                      <Plan-Rows>259</Plan-Rows>
                      <Plan-Width>76</Plan-Width>
                      <Output>
                        <Item>oid</Item>
                        <Item>name</Item>
                        <Item>gender</Item>
                        <Item>birth_year</Item>
                      </Output>
                      <Filter>((a.birth_year &lt;= 1990) AND ((a.gender &lt;&gt; 'Female'::text) OR (a.gender = 'Male'::text)))</Filter>
                    </Plan>
                  </Query>
                </explain>
                """;

        Document queryPlan = buildDOMFromString(xml);

        PostgresXMLParser parser = new PostgresXMLParser();
        IAlgebraOperator operator = parser.parse(queryPlan, llmDB, OperatorsConfigurationParser.getDefault(), sql);

        Assertions.assertNotNull(operator, "Operator is null!");
        Assertions.assertTrue(operator instanceof Select, operator.getName());

        Select select = (Select) operator;
        List<Expression> selections = select.getSelections();
        Assertions.assertEquals(1, selections.size());

        Expression expression = selections.get(0);
        Assertions.assertEquals(2, expression.getVariables().size());
        Assertions.assertEquals("actor_a.birth_year", expression.getVariables().get(0));
        Assertions.assertEquals("actor_a.gender", expression.getVariables().get(1));
    }

    @Test
    public void testSelectWhere() {
        // SQL: select a.name from actor a where a.birth_year > 1990
        String sql = "select a.name from actor a where a.birth_year > 1990";
        String xml = """
                <explain xmlns="http://www.postgresql.org/2009/explain">
                  <Query>
                    <Plan>
                      <Node-Type>Seq Scan</Node-Type>
                      <Parallel-Aware>false</Parallel-Aware>
                      <Relation-Name>actor</Relation-Name>
                      <Schema>target</Schema>
                      <Alias>a</Alias>
                      <Startup-Cost>0.00</Startup-Cost>
                      <Total-Cost>19.75</Total-Cost>
                      <Plan-Rows>260</Plan-Rows>
                      <Plan-Width>32</Plan-Width>
                      <Output>
                        <Item>name</Item>
                      </Output>
                      <Filter>(a.birth_year &gt; 1990)</Filter>
                    </Plan>
                  </Query>
                </explain>
                """;

        Document queryPlan = buildDOMFromString(xml);

        PostgresXMLParser parser = new PostgresXMLParser();
        IAlgebraOperator operator = parser.parse(queryPlan, llmDB, OperatorsConfigurationParser.getDefault(), sql);

        Assertions.assertNotNull(operator, "Operator is null!");
        Assertions.assertTrue(operator instanceof Project, operator.getName());

        Project project = (Project) operator;
        Assertions.assertFalse(project.isAggregative());
        Assertions.assertEquals(1, project.getAttributes(null, llmDB).size());
        Assertions.assertEquals("name", project.getAttributes(null, llmDB).get(0).getName());
        Assertions.assertEquals(1, project.getChildren().size());

        IAlgebraOperator child = project.getChildren().get(0);
        Assertions.assertTrue(child instanceof Select);

        Select select = (Select) child;
        List<Expression> selections = select.getSelections();
        Assertions.assertEquals(1, selections.size());

        Expression expression = selections.get(0);
        Assertions.assertEquals(1, expression.getVariables().size());
        Assertions.assertEquals("actor_a.birth_year", expression.getVariables().get(0));
    }

}
