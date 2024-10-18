package floq.parser.postgresql;

import floq.parser.ParserException;
import floq.parser.postgresql.nodeparsers.*;
import org.jdom2.Element;

import java.util.Map;

public class NodeParserFactory {
    private static final Map<String, INodeParserGenerator> parserMap = Map.ofEntries(
            Map.entry("Seq Scan", ScanParser::new),
            Map.entry("Bitmap Heap Scan", () -> new ScanParser("Recheck-Cond")),
            Map.entry("Index Scan", () -> new ScanParser("Index-Cond")),
            Map.entry("Sort", SortParser::new),
            Map.entry("Hash", HashParser::new),
            Map.entry("Aggregate", AggregateParser::new),
            Map.entry("Gather Merge", MockParser::new),
            Map.entry("Hash Join", HashJoinParser::new),
            Map.entry("Limit", LimitParser::new)
    );

    public static INodeParser getParserForNode(Element node) {
        Element nodeType = node.getChild("Node-Type", node.getNamespace());
        if (nodeType == null) throw new ParserException("Invalid node!");

        String type = nodeType.getTextTrim();
        if (type == null || type.isEmpty() || !parserMap.containsKey(type))
            throw new ParserException("Invalid node type: " + type + "!");

        INodeParserGenerator parserGenerator = parserMap.get(type);
        return parserGenerator.create();
    }

    @FunctionalInterface
    private interface INodeParserGenerator {
        INodeParser create();
    }
}
