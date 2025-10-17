package queryexecutor.model.algebra.operators;

import queryexecutor.model.algebra.*;
import queryexecutor.model.algebra.udf.UserDefinedFunction;

public interface IAlgebraTreeVisitor {

    void visitScan(Scan operator);
    void visitSelect(Select operator);
    void visitDistinct(Distinct operator);
    void visitSelectIn(SelectIn operator);
    void visitSelectNotIn(SelectNotIn operator);
    void visitJoin(Join operator);
    void visitCartesianProduct(CartesianProduct operator);
    void visitProject(Project operator);
    void visitDifference(Difference operator);
    void visitUnion(Union operator);
    void visitGroupBy(GroupBy operator);
    void visitPartition(Partition operator);
    void visitOrderBy(OrderBy operator);
    void visitOrderByRandom(OrderByRandom operator);
    void visitLimit(Limit operator);
    void visitOffset(Offset operator);
    void visitRestoreOIDs(RestoreOIDs operator);
    void visitCreateTable(CreateTableAs operator);
    void visitExtractRandomSample(ExtractRandomSample operator);
    void visitIntersection(Intersection operator);
    void visitUserDefinedFunction(UserDefinedFunction operator);
    Object getResult();
}
