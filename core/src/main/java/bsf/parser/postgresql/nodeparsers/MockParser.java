package bsf.parser.postgresql.nodeparsers;

import bsf.llm.algebra.MockOperator;
import bsf.llm.algebra.config.OperatorsConfiguration;
import org.jdom2.Element;
import queryexecutor.model.algebra.IAlgebraOperator;
import queryexecutor.model.database.IDatabase;

public class MockParser extends AbstractNodeParser{

    @Override
    public IAlgebraOperator parse(Element node, IDatabase database, OperatorsConfiguration configuration) {
        return new MockOperator();
    }
    
}
