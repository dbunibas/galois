package floq.parser.postgresql.nodeparsers;

import floq.llm.algebra.config.OperatorsConfiguration;
import floq.parser.postgresql.NodeParserFactory;
import org.jdom2.Element;
import engine.model.algebra.IAlgebraOperator;
import engine.model.database.IDatabase;

public class HashParser extends AbstractNodeParser {
    @Override
    public IAlgebraOperator parse(Element node, IDatabase database, OperatorsConfiguration configuration) {
        // TODO: Add to AbstractNodeParser?
        // TODO: Does the plans node have always one and only one plan child?
        // TODO: Should we always skip the Hash node?
        Element plans = node.getChild("Plans", node.getNamespace());
        Element subPlan = plans.getChild("Plan", plans.getNamespace());
        INodeParser parser = NodeParserFactory.getParserForNode(subPlan);
        IAlgebraOperator operator = parser.parse(subPlan, database, configuration);
        setTableAlias(parser.getTableAlias());
        return operator;
    }
}
