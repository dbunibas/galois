package queryexecutor.model.algebra.operators.sql.translator;

import queryexecutor.model.algebra.IAlgebraOperator;
import queryexecutor.model.database.TableAlias;

public class NestedOperator {

    private IAlgebraOperator operator;
    private TableAlias alias;

    public NestedOperator(IAlgebraOperator operator, TableAlias alias) {
        this.operator = operator;
        this.alias = alias;
    }

    @Override
    public String toString() {
        return alias.toString();
    }

    public IAlgebraOperator getOperator() {
        return operator;
    }

    public TableAlias getAlias() {
        return alias;
    }
    
    

}
