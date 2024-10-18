package floq.parser.postgresql.nodeparsers;

import floq.llm.algebra.config.OperatorsConfiguration;
import org.jdom2.Element;
import engine.model.algebra.IAlgebraOperator;
import engine.model.database.IDatabase;
import engine.model.database.TableAlias;

public interface INodeParser {
    IAlgebraOperator parse(Element node, IDatabase database, OperatorsConfiguration configuration);

    TableAlias getTableAlias();
}
