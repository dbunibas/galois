package galois.parser.postgresql.nodeparsers;

import org.jdom2.Element;
import speedy.model.algebra.IAlgebraOperator;
import speedy.model.database.TableAlias;

public interface INodeParser {
    IAlgebraOperator parse(Element node);

    TableAlias getTableAlias();
}
