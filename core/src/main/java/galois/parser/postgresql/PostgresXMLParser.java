package galois.parser.postgresql;

import galois.llm.algebra.config.OperatorsConfiguration;
import galois.parser.IQueryPlanParser;
import galois.parser.ParserException;
import org.jdom2.Document;
import org.jdom2.Element;
import speedy.model.algebra.IAlgebraOperator;
import speedy.model.database.IDatabase;

public class PostgresXMLParser implements IQueryPlanParser<Document> {
    @Override
    public IAlgebraOperator parse(Document queryPlan, IDatabase database, OperatorsConfiguration configuration) {
        if (queryPlan == null) throw new ParserException("Query plan cannot be null!");
        Element root = parseRoot(queryPlan);
        return NodeParserFactory.getParserForNode(root).parse(root, database, configuration);
    }

    private Element parseRoot(Document queryPlan) {
        // TODO: Use XPath?
        Element root = queryPlan.getRootElement();
        if (root == null) throw new ParserException("Invalid XML query plan!");
        Element query = root.getChild("Query", root.getNamespace());
        if (query == null) throw new ParserException("Invalid XML query plan!");
        Element plan = query.getChild("Plan", query.getNamespace());
        if (plan == null) throw new ParserException("Invalid XML query plan!");
        return plan;
    }

}
