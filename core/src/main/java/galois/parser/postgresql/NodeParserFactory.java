package galois.parser.postgresql;

import galois.parser.ParserException;
import galois.parser.postgresql.nodeparsers.*;
import org.jdom2.Element;

import java.util.Map;

public class NodeParserFactory {
    private static final Map<String, INodeParserGenerator> parserMap = Map.ofEntries(
            Map.entry("Seq Scan", ScanParser::new),
            Map.entry("Sort", SortParser::new),
            Map.entry("Hash", HashParser::new),
            Map.entry("Hash Join", HashJoinParser::new)
    );

    public static INodeParser getParserForNode(Element node) {
        Element nodeType = node.getChild("Node-Type", node.getNamespace());
        if (nodeType == null) throw new ParserException("Invalid node!");

        String type = nodeType.getTextTrim();
        if (type == null || type.isEmpty() || !parserMap.containsKey(type))
            throw new ParserException("Invalid node type!");

        INodeParserGenerator parserGenerator = parserMap.get(type);
        return parserGenerator.create();
    }

    @FunctionalInterface
    private interface INodeParserGenerator {
        INodeParser create();
    }
}
