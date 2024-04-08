package galois.parser.postgresql.nodeparsers;

import galois.llm.algebra.config.OperatorsConfiguration;
import org.jdom2.Element;
import speedy.model.algebra.IAlgebraOperator;
import speedy.model.database.IDatabase;
import speedy.model.database.TableAlias;

public interface INodeParser {
    IAlgebraOperator parse(Element node, IDatabase database, OperatorsConfiguration configuration);

    TableAlias getTableAlias();
}
