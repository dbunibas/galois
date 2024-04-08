package galois.parser.postgresql.nodeparsers;

import galois.llm.algebra.config.OperatorsConfiguration;
import galois.parser.postgresql.NodeParserFactory;
import org.jdom2.Element;
import speedy.model.algebra.IAlgebraOperator;
import speedy.model.algebra.OrderBy;
import speedy.model.database.AttributeRef;
import speedy.model.database.IDatabase;
import speedy.model.database.TableAlias;

import java.util.List;

public class SortParser extends AbstractNodeParser {
    @Override
    public IAlgebraOperator parse(Element node, IDatabase database, OperatorsConfiguration configuration) {
        IAlgebraOperator subTree = parseSubtree(node, database, configuration);

        // TODO: Handle more than one key / handle order
        Element sortKeyElement = node.getChild("Sort-Key", node.getNamespace());
        String sortKey = getSortKey(sortKeyElement);
        AttributeRef attributeRef = new AttributeRef(getTableAlias(), sortKey);

        OrderBy orderBy = new OrderBy(List.of(attributeRef));
        orderBy.addChild(subTree);
        return orderBy;
    }

    private IAlgebraOperator parseSubtree(Element node, IDatabase database, OperatorsConfiguration configuration) {
        Element plans = node.getChild("Plans", node.getNamespace());
        // TODO: Does the plans node have always one and only one plan child?
        Element subPlan = plans.getChild("Plan", plans.getNamespace());
        INodeParser parser = NodeParserFactory.getParserForNode(subPlan);

        IAlgebraOperator subTree = parser.parse(subPlan, database, configuration);
        TableAlias tableAlias = parser.getTableAlias();
        setTableAlias(tableAlias);

        return subTree;
    }

    private String getSortKey(Element element) {
        String sortKey = element.getChild("Item", element.getNamespace()).getText();
        return sortKey.replace(getTableAlias().getAlias() + ".", "");
    }
}
