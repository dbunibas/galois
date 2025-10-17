package bsf.parser.postgresql.nodeparsers;

import bsf.llm.algebra.config.OperatorsConfiguration;
import bsf.parser.postgresql.NodeParserFactory;
import org.jdom2.Element;
import queryexecutor.model.algebra.IAlgebraOperator;
import queryexecutor.model.database.IDatabase;

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
