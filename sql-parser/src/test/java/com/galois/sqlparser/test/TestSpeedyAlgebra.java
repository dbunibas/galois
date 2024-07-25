package com.galois.sqlparser.test;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import speedy.SpeedyConstants;
import speedy.model.algebra.GroupBy;
import speedy.model.algebra.Project;
import speedy.model.algebra.ProjectionAttribute;
import speedy.model.algebra.Scan;
import speedy.model.algebra.aggregatefunctions.CountAggregateFunction;
import speedy.model.algebra.aggregatefunctions.IAggregateFunction;
import speedy.model.algebra.aggregatefunctions.ValueAggregateFunction;
import speedy.model.database.AttributeRef;
import speedy.model.database.TableAlias;
import speedy.model.database.Tuple;
import speedy.model.database.mainmemory.MainMemoryDB;
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
        tupleList.forEach(t -> log.info("Tuple {}", t));
    }
}
