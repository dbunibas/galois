package galois.parser.postgresql;

import galois.parser.IQueryPlanParser;
import galois.parser.ParserException;
import org.jdom2.Document;
import org.jdom2.Element;
import speedy.model.algebra.IAlgebraOperator;

public class PostgresXMLParser implements IQueryPlanParser<Document> {
    @Override
    public IAlgebraOperator parse(Document queryPlan) {
        if (queryPlan == null) throw new ParserException("Query plan cannot be null!");
        Element root = parseRoot(queryPlan);
        return NodeParserFactory.getParserForNode(root).parse(root);
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
