package floq.parser.postgresql.nodeparsers;

import floq.llm.algebra.MockOperator;
import floq.llm.algebra.config.OperatorsConfiguration;
import org.jdom2.Element;
import engine.model.algebra.IAlgebraOperator;
import engine.model.database.IDatabase;

public class MockParser extends AbstractNodeParser{

    @Override
    public IAlgebraOperator parse(Element node, IDatabase database, OperatorsConfiguration configuration) {
        return new MockOperator();
    }
    
}
