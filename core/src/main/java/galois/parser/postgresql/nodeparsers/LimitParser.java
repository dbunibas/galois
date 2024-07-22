package galois.parser.postgresql.nodeparsers;

import galois.llm.algebra.config.OperatorsConfiguration;
import galois.parser.postgresql.NodeParserFactory;
import org.jdom2.Element;
import speedy.model.algebra.IAlgebraOperator;
import speedy.model.algebra.Limit;
import speedy.model.database.IDatabase;
import speedy.model.database.TableAlias;

public class LimitParser extends AbstractNodeParser {

    @Override
    public IAlgebraOperator parse(Element node, IDatabase database, OperatorsConfiguration configuration) {
        IAlgebraOperator subtree = parseSubtree(node, database, configuration);
        Element planRows = node.getChild("Plan-Rows", node.getNamespace());
        String sValue = planRows.getValue().trim();
        Long value = Long.valueOf(sValue);
        Limit limit = new Limit(value);
        limit.addChild(subtree);
        return limit;
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
    
}
