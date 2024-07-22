package galois.parser.postgresql.nodeparsers;

import galois.llm.algebra.MockOperator;
import galois.llm.algebra.config.OperatorsConfiguration;
import org.jdom2.Element;
import speedy.model.algebra.IAlgebraOperator;
import speedy.model.database.IDatabase;

public class MockParser extends AbstractNodeParser{

    @Override
    public IAlgebraOperator parse(Element node, IDatabase database, OperatorsConfiguration configuration) {
        return new MockOperator();
    }
    
}
