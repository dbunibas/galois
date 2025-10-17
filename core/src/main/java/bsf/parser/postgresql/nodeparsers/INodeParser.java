package bsf.parser.postgresql.nodeparsers;

import bsf.llm.algebra.config.OperatorsConfiguration;
import org.jdom2.Element;
import queryexecutor.model.algebra.IAlgebraOperator;
import queryexecutor.model.database.IDatabase;
import queryexecutor.model.database.TableAlias;

public interface INodeParser {
    IAlgebraOperator parse(Element node, IDatabase database, OperatorsConfiguration configuration);

    TableAlias getTableAlias();
}
