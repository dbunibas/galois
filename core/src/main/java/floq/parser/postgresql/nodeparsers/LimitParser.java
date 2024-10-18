package floq.parser.postgresql.nodeparsers;

import floq.llm.algebra.config.OperatorsConfiguration;
import floq.parser.postgresql.NodeParserFactory;
import org.jdom2.Element;
import engine.model.algebra.IAlgebraOperator;
import engine.model.algebra.Limit;
import engine.model.database.IDatabase;
import engine.model.database.TableAlias;

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
