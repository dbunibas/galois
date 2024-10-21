package engine.model.algebra.operators;

import engine.model.algebra.CartesianProduct;
import engine.model.algebra.CreateTableAs;
import engine.model.algebra.Difference;
import engine.model.algebra.Distinct;
import engine.model.algebra.ExtractRandomSample;
import engine.model.algebra.GroupBy;
import engine.model.algebra.Intersection;
import engine.model.algebra.Join;
import engine.model.algebra.Limit;
import engine.model.algebra.Offset;
import engine.model.algebra.OrderBy;
import engine.model.algebra.OrderByRandom;
import engine.model.algebra.Partition;
import engine.model.algebra.Project;
import engine.model.algebra.RestoreOIDs;
import engine.model.algebra.Scan;
import engine.model.algebra.Select;
import engine.model.algebra.SelectIn;
import engine.model.algebra.SelectNotIn;
import engine.model.algebra.Union;

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
    Object getResult();
}
