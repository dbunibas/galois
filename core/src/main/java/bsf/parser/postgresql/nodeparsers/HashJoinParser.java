package bsf.parser.postgresql.nodeparsers;

import bsf.llm.algebra.config.OperatorsConfiguration;
import bsf.parser.postgresql.NodeParserFactory;
import org.jdom2.Element;
import queryexecutor.model.algebra.IAlgebraOperator;
import queryexecutor.model.algebra.Join;
import queryexecutor.model.database.AttributeRef;
import queryexecutor.model.database.IDatabase;

import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

public class HashJoinParser extends AbstractNodeParser {
    @Override
    public IAlgebraOperator parse(Element node, IDatabase database, OperatorsConfiguration configuration) {
        // TODO: Handle join type and other specific nodes?
        // TODO: Add to AbstractNodeParser
        Element plans = node.getChild("Plans", node.getNamespace());
        List<Element> subPlans = plans.getChildren("Plan", plans.getNamespace());
        List<INodeParser> parsers = subPlans.stream()
                .map(NodeParserFactory::getParserForNode)
                .toList();
        List<IAlgebraOperator> operators = IntStream.range(0, subPlans.size())
                .mapToObj(i -> parsers.get(i).parse(subPlans.get(i), database, configuration))
                .toList();

        // TODO: Handle multiple attributes
        String condition = node.getChild("Hash-Cond", node.getNamespace()).getText();
        List<String> attributes = Arrays.stream(condition.split("="))
                .map(a -> a.replaceAll("[()]", ""))
                .map(String::trim)
                .toList();

        Join join = getJoin(parsers, attributes);
        join.addChild(operators.get(0));
        join.addChild(operators.get(1));
        return join;
    }

    private static Join getJoin(List<INodeParser> parsers, List<String> attributes) {
        INodeParser leftParser = parsers.get(0);
        String leftAttribute = attributes.get(0).replace(leftParser.getTableAlias().getAlias() + ".", "");
        List<AttributeRef> leftAttributes = List.of(
                new AttributeRef(parsers.get(0).getTableAlias(), leftAttribute)
        );

        INodeParser rightParser = parsers.get(1);
        String rightAttribute = attributes.get(1).replace(rightParser.getTableAlias().getAlias() + ".", "");
        List<AttributeRef> rightAttributes = List.of(
                new AttributeRef(parsers.get(1).getTableAlias(), rightAttribute)
        );

        return new Join(leftAttributes, rightAttributes);
    }
}
