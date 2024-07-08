package galois.parser.postgresql;

import galois.llm.algebra.LLMScan;
import galois.llm.algebra.config.OperatorsConfiguration;
import galois.parser.IQueryPlanParser;
import galois.parser.ParserException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import lombok.extern.slf4j.Slf4j;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.filter.ElementFilter;
import org.jdom2.util.IteratorIterable;
import speedy.model.algebra.IAlgebraOperator;
import speedy.model.database.IDatabase;
import speedy.model.database.TableAlias;

import java.util.stream.StreamSupport;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.SelectItem;
import speedy.model.algebra.Project;
import speedy.model.algebra.ProjectionAttribute;
import speedy.model.database.AttributeRef;

@Slf4j
public class PostgresXMLParser implements IQueryPlanParser<Document> {

    @Override
    public IAlgebraOperator parse(Document queryPlan, IDatabase database, OperatorsConfiguration configuration, String sqlQuery) {
        if (queryPlan == null) {
            throw new ParserException("Query plan cannot be null!");
        }
        Element root = parseRoot(queryPlan);

        if (configuration.getScan().getQueryExecutor().ignoreTree()) {
            return parseIgnoreTree(root, configuration);
        }

        IAlgebraOperator operator = NodeParserFactory.getParserForNode(root).parse(root, database, configuration);
        if (operator instanceof Project) {
            return operator;
        }
        if (sqlQuery != null && !sqlQuery.isEmpty()) {
            try {
                PlainSelect select = (PlainSelect) CCJSqlParserUtil.parse(sqlQuery);
                List<SelectItem<?>> selectItems = select.getSelectItems();
                List<String> attributeSelect = new ArrayList<>();
                for (SelectItem<?> selectItem : selectItems) {
                    StringTokenizer tokenizer = new StringTokenizer(selectItem.toString(), ".");
                    String attribute = tokenizer.nextToken();
                    if (tokenizer.hasMoreTokens()) {
                        attribute = tokenizer.nextToken();
                    }
                    attributeSelect.add(attribute);
                }
                if (attributeSelect.size() == 1 && attributeSelect.get(0).equals("*")) {
                    return operator;
                }
                FromItem fromItem = select.getFromItem();
                String alias = fromItem.getAlias().getName();
                String tableName = database.getFirstTable().getName(); // TODO: solve in case of multiple tables
                TableAlias tableAlias = new TableAlias(tableName, alias);
                List<ProjectionAttribute> projectionAttributes = new ArrayList<>();
                for (String attributeName : attributeSelect) {
                    AttributeRef aRef = new AttributeRef(tableAlias, attributeName);
                    ProjectionAttribute pa = new ProjectionAttribute(aRef);
                    projectionAttributes.add(pa);
                }
                IAlgebraOperator projection = new Project(projectionAttributes);
                projection.addChild(operator);
                operator = projection;
            } catch (JSQLParserException ex) {

            }
        }
        log.debug("Operator: " + operator.toString("\t"));
        return operator;
    }

    private Element parseRoot(Document queryPlan) {
        // TODO: Use XPath?
        Element root = queryPlan.getRootElement();
        if (root == null) {
            throw new ParserException("Invalid XML query plan!");
        }
        Element query = root.getChild("Query", root.getNamespace());
        if (query == null) {
            throw new ParserException("Invalid XML query plan!");
        }
        Element plan = query.getChild("Plan", query.getNamespace());
        if (plan == null) {
            throw new ParserException("Invalid XML query plan!");
        }
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
