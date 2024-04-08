package galois.test;

import galois.llm.algebra.LLMScan;
import galois.llm.database.LLMDB;
import galois.parser.postgresql.PostgresXMLParser;
import galois.test.experiments.json.parser.OperatorsConfigurationParser;
import lombok.extern.slf4j.Slf4j;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.input.sax.XMLReaders;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import speedy.exceptions.DAOException;
import speedy.model.algebra.IAlgebraOperator;
import speedy.model.algebra.OrderBy;
import speedy.model.algebra.Project;
import speedy.model.algebra.Select;
import speedy.model.database.IDatabase;
import speedy.persistence.relational.AccessConfiguration;

import java.io.ByteArrayInputStream;
import java.io.IOException;

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
                      <Plan-Width>68</Plan-Width>
                      <Output>
                        <Item>oid</Item>
                        <Item>name</Item>
                        <Item>sex</Item>
                      </Output>
                    </Plan>
                  </Query>
                </explain>""";

        Document queryPlan = buildDOMFromString(xml);

        PostgresXMLParser parser = new PostgresXMLParser();
        IAlgebraOperator operator = parser.parse(queryPlan, llmDB, OperatorsConfigurationParser.parseJSON(null));

        Assertions.assertNotNull(operator, "Operator is null!");
        Assertions.assertTrue(operator instanceof LLMScan);

        LLMScan scan = (LLMScan) operator;
        Assertions.assertTrue(scan.getChildren().isEmpty());
        Assertions.assertEquals(scan.getTableAlias().getAlias(), "a");
        Assertions.assertEquals(scan.getTableAlias().getTableName(), "actor");
    }

    @Test
    @Disabled
    public void testSelectWhere() {
        // SQL: select c.city_name from city c where c.population > 50000
        String xml = """
                <explain xmlns="http://www.postgresql.org/2009/explain">
                  <Query>
                    <Plan>
                      <Node-Type>Seq Scan</Node-Type>
                      <Parallel-Aware>false</Parallel-Aware>
                      <Relation-Name>city</Relation-Name>
                      <Schema>public</Schema>
                      <Alias>c</Alias>
                      <Startup-Cost>0.00</Startup-Cost>
                      <Total-Cost>7.83</Total-Cost>
                      <Plan-Rows>383</Plan-Rows>
                      <Plan-Width>9</Plan-Width>
                      <Output>
                        <Item>city_name</Item>
                      </Output>
                      <Filter>(c.population &gt; 50000)</Filter>
                    </Plan>
                  </Query>
                </explain>""";

        Document queryPlan = buildDOMFromString(xml);

        PostgresXMLParser parser = new PostgresXMLParser();
        IAlgebraOperator operator = parser.parse(queryPlan, llmDB, OperatorsConfigurationParser.parseJSON(null));

        Assertions.assertNotNull(operator, "Operator is null!");
        Assertions.assertTrue(operator instanceof Select);
    }

    @Test
    public void testSelectOrderBy() {
        // SQL: select * from target.actor a order by a.name
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
        IAlgebraOperator operator = parser.parse(queryPlan, llmDB, OperatorsConfigurationParser.parseJSON(null));

        Assertions.assertNotNull(operator, "Operator is null!");
        Assertions.assertTrue(operator instanceof OrderBy);

        Assertions.assertEquals(operator.getChildren().size(), 1);
        log.info("{}", operator.getChildren().get(0).getName());
        Assertions.assertTrue(operator.getChildren().get(0) instanceof LLMScan);
    }

    @Test
    public void testSelect() {
        // SQL: select a.name from target.actor a
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
        IAlgebraOperator operator = parser.parse(queryPlan, llmDB, OperatorsConfigurationParser.parseJSON(null));

        Assertions.assertNotNull(operator, "Operator is null!");
        Assertions.assertTrue(operator instanceof Project);

        Assertions.assertEquals(operator.getChildren().size(), 1);
        Assertions.assertTrue(operator.getChildren().get(0) instanceof LLMScan);
    }

    private Document buildDOMFromString(String content) throws DAOException {
        if (content == null || content.isEmpty()) {
            throw new DAOException("Unable to load xml from empty content.");
        }
        SAXBuilder builder = new SAXBuilder();
        builder.setXMLReaderFactory(XMLReaders.NONVALIDATING); // builder.setValidation(false); is deprecated
        Document document = null;
        try {
            document = builder.build(new ByteArrayInputStream(content.getBytes()));
            return document;
        } catch (JDOMException | IOException ex) {
            log.error(ex.toString());
            throw new DAOException(ex.getMessage());
        }
    }

}
