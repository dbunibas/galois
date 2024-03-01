package galois.test;

import galois.parser.postgresql.PostgresXMLParser;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.input.sax.XMLReaders;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import speedy.exceptions.DAOException;
import speedy.model.algebra.IAlgebraOperator;
import speedy.model.algebra.Scan;
import speedy.model.algebra.Select;
import speedy.persistence.xml.DAOXmlUtility;

import java.io.ByteArrayInputStream;
import java.io.IOException;

public class TestParseXML {

    private static final Logger logger = LoggerFactory.getLogger(TestParseXML.class);

    private final DAOXmlUtility daoUtility = new DAOXmlUtility();

    @Test
    public void testSimpleSelect() {
        // SQL: select * from city c
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
                      <Plan-Width>26</Plan-Width>
                      <Output>
                        <Item>city_name</Item>
                        <Item>population</Item>
                        <Item>country_name</Item>
                        <Item>state_name</Item>
                      </Output>
                    </Plan>
                  </Query>
                </explain>""";

        Document queryPlan = buildDOMFromString(xml);

        PostgresXMLParser parser = new PostgresXMLParser();
        IAlgebraOperator operator = parser.parse(queryPlan);

        Assertions.assertNotNull(operator, "Operator is null!");
        Assertions.assertTrue(operator instanceof Scan);

        Scan scan = (Scan) operator;
        Assertions.assertTrue(scan.getChildren().isEmpty());
        Assertions.assertEquals(scan.getTableAlias().getAlias(), "c");
        Assertions.assertEquals(scan.getTableAlias().getTableName(), "city");
    }

    @Test
    public void testSelectWhere() {
        // SQL: select * from city c where c.population > 50000
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
                      <Plan-Width>26</Plan-Width>
                      <Output>
                        <Item>city_name</Item>
                        <Item>population</Item>
                        <Item>country_name</Item>
                        <Item>state_name</Item>
                      </Output>
                      <Filter>(c.population &gt; 50000)</Filter>
                    </Plan>
                  </Query>
                </explain>""";

        Document queryPlan = buildDOMFromString(xml);

        PostgresXMLParser parser = new PostgresXMLParser();
        IAlgebraOperator operator = parser.parse(queryPlan);

        Assertions.assertNotNull(operator, "Operator is null!");
        Assertions.assertTrue(operator instanceof Select);
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
            logger.error(ex.toString());
            throw new DAOException(ex.getMessage());
        }
    }

}
