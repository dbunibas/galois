package galois.parser.postgresql;

import galois.llm.algebra.LLMScan;
import galois.llm.algebra.config.OperatorsConfiguration;
import galois.parser.IQueryPlanParser;
import galois.parser.ParserException;
import lombok.extern.slf4j.Slf4j;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.filter.ElementFilter;
import org.jdom2.util.IteratorIterable;
import speedy.model.algebra.IAlgebraOperator;
import speedy.model.database.IDatabase;
import speedy.model.database.TableAlias;

import java.util.stream.StreamSupport;

@Slf4j
public class PostgresXMLParser implements IQueryPlanParser<Document> {
    @Override
    public IAlgebraOperator parse(Document queryPlan, IDatabase database, OperatorsConfiguration configuration) {
        if (queryPlan == null) throw new ParserException("Query plan cannot be null!");
        Element root = parseRoot(queryPlan);

        if (configuration.getScan().getQueryExecutor().ignoreTree()) {
            return parseIgnoreTree(root, configuration);
        }

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

    private IAlgebraOperator parseIgnoreTree(Element root, OperatorsConfiguration configuration) {
        Element relationNameElement = getDescendant(root, new ElementFilter("Relation-Name"));
        String relationName = relationNameElement.getText();

        Element aliasElement = getDescendant(root, new ElementFilter("Alias"));
        String alias = aliasElement.getText();

        log.debug("Table name {} - Alias {}", relationName, alias);
        TableAlias tableAlias = new TableAlias(relationName, alias);
        return new LLMScan(tableAlias, configuration.getScan().getQueryExecutor());
    }

    private Element getDescendant(Element element, ElementFilter filter) {
        IteratorIterable<Element> descendants = element.getDescendants(filter);
        return StreamSupport.stream(descendants.spliterator(), false)
                .findFirst()
                .orElseThrow();
    }

}
