package com.galois.sqlparser.test;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import speedy.SpeedyConstants;
import speedy.model.algebra.*;
import speedy.model.algebra.aggregatefunctions.CountAggregateFunction;
import speedy.model.algebra.aggregatefunctions.IAggregateFunction;
import speedy.model.algebra.aggregatefunctions.ValueAggregateFunction;
import speedy.model.database.AttributeRef;
import speedy.model.database.TableAlias;
import speedy.model.database.Tuple;
import speedy.model.database.mainmemory.MainMemoryDB;
import speedy.model.expressions.Expression;
import speedy.persistence.DAOMainMemoryDatabase;

import java.util.List;

import static com.galois.sqlparser.test.TestUtils.toTupleList;
import static java.util.Objects.requireNonNull;

@Slf4j
public class TestSpeedyAlgebra {
    private static final String TABLE_NAME = "EmpTable";

    private static MainMemoryDB db;

    @BeforeAll
    public static void setUp() {
        String schema = requireNonNull(TestParseSelect.class.getResource("/employees/schema.xsd")).getFile();
        String instance = requireNonNull(TestParseSelect.class.getResource("/employees/50_emp.xml")).getFile();
        db = new DAOMainMemoryDatabase().loadXMLDatabase(schema, instance);
    }

    @Test
    public void testGroupBy() {
        // SQL: select t.dept, count(t.dept) as num from table t group by t.dept
        TableAlias tableAlias = new TableAlias(TABLE_NAME, "t");
        Scan scan = new Scan(tableAlias);

        AttributeRef deptRef = new AttributeRef(tableAlias, "dept");
        AttributeRef countRef = new AttributeRef(tableAlias, SpeedyConstants.COUNT);
        IAggregateFunction valueAggregate = new ValueAggregateFunction(deptRef);
        IAggregateFunction countAggregate = new CountAggregateFunction(countRef);
        GroupBy groupBy = new GroupBy(List.of(deptRef), List.of(valueAggregate, countAggregate));
        groupBy.addChild(scan);

        ProjectionAttribute deptAttribute = new ProjectionAttribute(deptRef);
        ProjectionAttribute countAttribute = new ProjectionAttribute(countRef);
        Project project = new Project(List.of(deptAttribute, countAttribute), List.of(deptRef, new AttributeRef(tableAlias, "num")), false);
        project.addChild(groupBy);

        List<Tuple> tupleList = toTupleList(project.execute(null, db));
        logTuples(tupleList);
    }

    @Test
    public void testSimpleJoin() {
        // SQL: select * from table t1 join table t2 on t1.manager = t2.name
        TableAlias leftTableAlias = new TableAlias(TABLE_NAME, "t1");
        TableAlias rightTableAlias = new TableAlias(TABLE_NAME, "t2");

        Scan leftScan = new Scan(leftTableAlias);
        Scan rightScan = new Scan(rightTableAlias);

        List<AttributeRef> leftAttributes = List.of(new AttributeRef(leftTableAlias, "manager"));
        List<AttributeRef> rightAttributes = List.of(new AttributeRef(rightTableAlias, "name"));
        Join join = new Join(leftAttributes, rightAttributes);
        join.addChild(leftScan);
        join.addChild(rightScan);

        List<Tuple> tupleList = toTupleList(join.execute(null, db));
        logTuples(tupleList);
    }

    @Test
    public void testJoinWithExpressions() {
        // SQL: select * from table t1 join table t2 on t1.manager = t2.name where t1.salary > 2000 and t2.salary > 2000
        TableAlias leftTableAlias = new TableAlias(TABLE_NAME, "t1");
        TableAlias rightTableAlias = new TableAlias(TABLE_NAME, "t2");

        Scan leftScan = new Scan(leftTableAlias);
        Expression leftExpression = new Expression("salary > 2000");
        leftExpression.setVariableDescription("salary", new AttributeRef(leftTableAlias, "salary"));
        Select leftSelect = new Select(leftExpression);
        leftSelect.addChild(leftScan);

        Scan rightScan = new Scan(rightTableAlias);
        Expression rightExpression = new Expression("salary > 2000");
        rightExpression.setVariableDescription("salary", new AttributeRef(rightTableAlias, "salary"));
        Select rightSelect = new Select(rightExpression);
        rightSelect.addChild(rightScan);

        List<AttributeRef> leftAttributes = List.of(new AttributeRef(leftTableAlias, "manager"));
        List<AttributeRef> rightAttributes = List.of(new AttributeRef(rightTableAlias, "name"));
        Join join = new Join(leftAttributes, rightAttributes);
        join.addChild(leftSelect);
        join.addChild(rightSelect);

        List<Tuple> tupleList = toTupleList(join.execute(null, db));
        logTuples(tupleList);
    }

    private void logTuples(List<Tuple> tuples) {
        tuples.forEach(t -> log.info("{}", t));
    }
}
