package galois.parser.postgresql.nodeparsers;

import org.jdom2.Element;
import speedy.model.algebra.IAlgebraOperator;

public interface INodeParser {
    IAlgebraOperator parse(Element node);
}
