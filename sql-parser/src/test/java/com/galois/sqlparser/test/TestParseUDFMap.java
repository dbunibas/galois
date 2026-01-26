package com.galois.sqlparser.test;

import com.galois.sqlparser.IUserDefinedFunctionFactory;
import com.galois.sqlparser.ParseContext;
import com.galois.sqlparser.SQLQueryParser;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import speedy.model.algebra.*;
import speedy.model.algebra.aggregatefunctions.AvgAggregateFunction;
import speedy.model.algebra.aggregatefunctions.CountAggregateFunction;
import speedy.model.algebra.aggregatefunctions.ValueAggregateFunction;
import speedy.model.algebra.udf.IUserDefinedFunction;
import speedy.model.database.AttributeRef;
import speedy.model.database.Tuple;
import speedy.model.database.mainmemory.MainMemoryDB;
import speedy.persistence.DAOMainMemoryDatabase;

import java.util.List;
import java.util.Random;

import static com.galois.sqlparser.test.TestUtils.toTupleList;
import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.*;

@Slf4j
public class TestParseUDFMap {
    private static final String TABLE_NAME = "EmpTable";
    private static final IUserDefinedFunctionFactory UDF_FACTORY = new MockUDMapFactory();

    private MainMemoryDB db;

    @BeforeEach
    public void setUp() {
        String schema = requireNonNull(TestParseUDFMap.class.getResource("/employees/schema.xsd")).getFile();
        String instance = requireNonNull(TestParseUDFMap.class.getResource("/employees/50_emp.xml")).getFile();
        db = new DAOMainMemoryDatabase().loadXMLDatabase(schema, instance);
    }

    @Test
    public void testParseUserDefinedSelectMap() {
        String sql = String.format("select udmap() from %s e", TABLE_NAME);

        IAlgebraOperator root = new SQLQueryParser().parse(sql, UDF_FACTORY);
        assertNotNull(root);

        assertInstanceOf(Project.class, root);
        Project project = (Project) root;

        assertEquals(1, project.getChildren().size());
        assertInstanceOf(Scan.class, project.getChildren().getFirst());

        List<Tuple> result = toTupleList(root.execute(db, db));
        assertEquals(52, result.size());
        assertEquals(2, result.getFirst().getCells().size());
        assertEquals("udmap", result.getFirst().getCells().get(1).getAttribute());
        assertEquals("extra_attribute", result.getFirst().getCells().get(1).getValue().getPrimitiveValue());
    }

    @Test
    public void testParseUserDefinedSelectMapWithAlias() {
        String sql = String.format("select udmap() as extra_attribute from %s e", TABLE_NAME);

        IAlgebraOperator root = new SQLQueryParser().parse(sql, UDF_FACTORY);
        assertNotNull(root);

        assertInstanceOf(Project.class, root);
        Project project = (Project) root;

        assertEquals(1, project.getChildren().size());
        assertInstanceOf(Scan.class, project.getChildren().getFirst());

        List<Tuple> result = toTupleList(root.execute(db, db));
        assertEquals(52, result.size());
        assertEquals(2, result.getFirst().getCells().size());
        assertEquals("extra_attribute", result.getFirst().getCells().get(1).getAttribute());
        assertEquals("extra_attribute", result.getFirst().getCells().get(1).getValue().getPrimitiveValue());
    }

    @Test
    public void testParseUserDefinedSelectMapWithWhere() {
        String sql = String.format("select e.name, udmap() from %s e where e.name = 'RafaelNadal'", TABLE_NAME);

        IAlgebraOperator root = new SQLQueryParser().parse(sql, UDF_FACTORY);
        assertNotNull(root);

        assertInstanceOf(Project.class, root);
        Project project = (Project) root;

        assertEquals(1, project.getChildren().size());
        assertInstanceOf(Select.class, project.getChildren().getFirst());

        Select select = (Select) project.getChildren().getFirst();
        assertEquals(1, select.getChildren().size());
        assertInstanceOf(Scan.class, select.getChildren().getFirst());

        List<Tuple> result = toTupleList(root.execute(db, db));
        assertEquals(1, result.size());
        assertEquals(3, result.getFirst().getCells().size());
        assertEquals("name", result.getFirst().getCells().get(1).getAttribute());
        assertEquals("udmap", result.getFirst().getCells().get(2).getAttribute());
    }

    @Test
    public void testParseUserDefinedAggregativeSelectMap() {
        String sql = String.format("select avg(udrank()) as avg from %s e", TABLE_NAME);

        IAlgebraOperator root = new SQLQueryParser().parse(sql, UDF_FACTORY);
        assertNotNull(root);

        assertInstanceOf(Project.class, root);
        assertEquals(1, root.getChildren().size());
        assertInstanceOf(Scan.class, root.getChildren().getFirst());

        List<Tuple> result = toTupleList(root.execute(db, db));
        assertEquals(1, result.size());
        assertEquals(1, result.getFirst().getCells().size());
        assertEquals("avg", result.getFirst().getCells().getFirst().getAttribute());
    }

    @Test
    public void testParseUserDefinedMapWithAggregativeGroupBy() {
        String sql = String.format("select count(*), avg(udrank()) as avg from %s e group by e.dept", TABLE_NAME);

        IAlgebraOperator root = new SQLQueryParser().parse(sql, UDF_FACTORY);
        assertNotNull(root);
        assertInstanceOf(Project.class, root);

        Project project = (Project) root;
        assertFalse(project.isAggregative());
        assertEquals(2, project.getNewAttributes().size());
        assertInstanceOf(AttributeRef.class, project.getNewAttributes().getFirst());
        assertEquals("count", project.getNewAttributes().getFirst().getName());
        assertInstanceOf(AttributeRef.class, project.getNewAttributes().getLast());
        assertEquals("avg", project.getNewAttributes().getLast().getName());

        assertEquals(1, project.getChildren().size());
        assertInstanceOf(GroupBy.class, project.getChildren().getFirst());
        GroupBy groupBy = (GroupBy) project.getChildren().getFirst();
        assertEquals(1, groupBy.getGroupingAttributes().size());
        assertEquals(3, groupBy.getAggregateFunctions().size());
        assertInstanceOf(ValueAggregateFunction.class, groupBy.getAggregateFunctions().getFirst());
        assertInstanceOf(CountAggregateFunction.class, groupBy.getAggregateFunctions().get(1));
        assertInstanceOf(AvgAggregateFunction.class, groupBy.getAggregateFunctions().getLast());

        log.info("{}", toTupleList(root.execute(db, db)));
    }

    @Test
    public void testParseUserDefinedMapWithGroupByAndWhere() {
        String sql = String.format("select udsentiment(), count(*) from %s where salary > 0 group by udsentiment", TABLE_NAME);

        IAlgebraOperator root = new SQLQueryParser().parse(sql, UDF_FACTORY);
        assertNotNull(root);
        assertInstanceOf(Project.class, root);

        log.info("{}", toTupleList(root.execute(db, db)));
    }

    private static final class ExtraAttributeUDMap implements IUserDefinedFunction {
        @Override
        public Object execute(Tuple iterator) {
            return "extra_attribute";
        }
    }

    private static final class ExtraAttributeUDRank implements IUserDefinedFunction {
        private final Random random = new Random();

        @Override
        public Object execute(Tuple iterator) {
            return random.nextInt(100);
        }
    }

    private static final class ExtraAttributeUDSentiment implements IUserDefinedFunction {
        private final Random random = new Random();

        @Override
        public Object execute(Tuple iterator) {
            return random.nextBoolean() ? "POSITIVE" : "NEGATIVE";
        }
    }

    private static final class MockUDMapFactory implements IUserDefinedFunctionFactory {
        @Override
        public IUserDefinedFunction getUserDefinedFunction(String name, ExpressionList<? extends Expression> expressions, ParseContext parseContext) {
            if (name.equalsIgnoreCase("udmap")) return new ExtraAttributeUDMap();
            if (name.equalsIgnoreCase("udrank")) return new ExtraAttributeUDRank();
            if (name.equalsIgnoreCase("udsentiment")) return new ExtraAttributeUDSentiment();
            throw new UnsupportedOperationException();
        }
    }
}
