package galois.planner.postgresql.xml;

import galois.planner.IQueryPlanner;
import galois.planner.PlannerException;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.input.sax.XMLReaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import speedy.persistence.relational.AccessConfiguration;
import speedy.persistence.relational.QueryManager;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

public class PostgresXMLPlanner implements IQueryPlanner<Document> {
    private static final Logger logger = LoggerFactory.getLogger(PostgresXMLPlanner.class);

    private final AccessConfiguration accessConfiguration;

    public PostgresXMLPlanner(AccessConfiguration configuration) {
        this.accessConfiguration = configuration;
    }

    @Override
    public Document planFrom(String sql) {
        if (accessConfiguration == null) {
            throw new PlannerException("Cannot establish DB connection: access configuration is null!");
        }
        // TODO: Infer schema configuration?
        Connection connection = QueryManager.getConnection(accessConfiguration);
        String query = "explain (verbose, format xml) " + sql;
        try (ResultSet result = QueryManager.executeQuery(query, connection, accessConfiguration)) {
            result.next();
            String xmlString = result.getString("QUERY PLAN");
            return buildDOMFromString(xmlString);
        } catch (SQLException ex) {
            throw new PlannerException(ex);
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    logger.error("Cannot close non-null connection! {}", connection);
                }
            }
        }
    }

    private Document buildDOMFromString(String content) throws PlannerException {
        if (content == null || content.isEmpty()) {
            throw new PlannerException("Unable to load xml from empty content.");
        }
        SAXBuilder builder = new SAXBuilder();
        builder.setXMLReaderFactory(XMLReaders.NONVALIDATING);
        Document document;
        try {
            document = builder.build(new ByteArrayInputStream(content.getBytes()));
            return document;
        } catch (JDOMException | IOException ex) {
            logger.error(ex.toString());
            throw new PlannerException(ex.getMessage());
        }
    }
}
