package engine.model.algebra.operators.sql.translator;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import engine.EngineConstants;
import engine.model.algebra.CartesianProduct;
import engine.model.algebra.CreateTableAs;
import engine.model.algebra.Difference;
import engine.model.algebra.Distinct;
import engine.model.algebra.ExtractRandomSample;
import engine.model.algebra.GroupBy;
import engine.model.algebra.IAlgebraOperator;
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
import engine.model.algebra.aggregatefunctions.AvgAggregateFunction;
import engine.model.algebra.aggregatefunctions.CountAggregateFunction;
import engine.model.algebra.aggregatefunctions.IAggregateFunction;
import engine.model.algebra.aggregatefunctions.MaxAggregateFunction;
import engine.model.algebra.aggregatefunctions.MinAggregateFunction;
import engine.model.algebra.aggregatefunctions.StdDevAggregateFunction;
import engine.model.algebra.aggregatefunctions.SumAggregateFunction;
import engine.model.algebra.aggregatefunctions.ValueAggregateFunction;
import engine.model.algebra.operators.IAlgebraTreeVisitor;
import engine.model.algebra.operators.sql.ExpressionToSQL;
import engine.model.database.Attribute;
import engine.model.database.AttributeRef;
import engine.model.database.IDatabase;
import engine.model.database.ITable;
import engine.model.database.TableAlias;
import engine.model.database.dbms.DBMSVirtualTable;
import engine.model.expressions.Expression;
import engine.utility.DBMSUtility;
import engine.utility.EngineUtility;

public class AlgebraTreeToSQLVisitor implements IAlgebraTreeVisitor {

    private final static Logger logger = LoggerFactory.getLogger(AlgebraTreeToSQLVisitor.class);
    private int counter = 0;
    private int indentLevel = 0;
    private boolean addOIDColumn = false;
    private ExpressionToSQL sqlGenerator = new ExpressionToSQL();
    private SQLQueryBuilder sqlQueryBuilder = new SQLQueryBuilder();
    private IDatabase source;
    private IDatabase target;
    private String initialIndent;
    private List<String> createTableQueries = new ArrayList<String>();
    private List<String> dropTempTableQueries = new ArrayList<String>();
    private List<AttributeRef> currentProjectionAttribute;
    private SelectNotIn currentSelectNotIn;
    private StringBuilder selectNotInBufferWhere = new StringBuilder();

    // TRANSLATOR
    private TranslateScan scanTranslator = new TranslateScan();
    private TranslateJoin joinTranslator = new TranslateJoin();
    private TranslateSelect selectTranslator = new TranslateSelect();
    private TranslateSelectIn selectInTranslator = new TranslateSelectIn();
    private TranslateSelectNotIn selectNotInTranslator = new TranslateSelectNotIn();
    private TranslateCartesianProduct cartesianProductTranslator = new TranslateCartesianProduct();
    private TranslateProject projectTranslator = new TranslateProject();
    private TranslateDifference differenceTranslator = new TranslateDifference();
    private TranslateUnion unionTranslator = new TranslateUnion();
    private TranslateOrderBy orderByTranslator = new TranslateOrderBy();
    private TranslateOrderByRandom orderByRandomTranslator = new TranslateOrderByRandom();
    private TranslateGroupBy groupByTranslator = new TranslateGroupBy();
    private TranslateLimit limitTranslator = new TranslateLimit();
    private TranslateOffset offsetTranslator = new TranslateOffset();
    private TranslateRestoreOIDs restoreOIDsTranslator = new TranslateRestoreOIDs();
    private TranslateCreateTableAs createTableAsTranslator = new TranslateCreateTableAs();
    private TranslateDistinct distinctTranslator = new TranslateDistinct();
    private TranslateIntersection intersectionTranslator = new TranslateIntersection();

    public AlgebraTreeToSQLVisitor(IDatabase source, IDatabase target, String initialIndent) {
        this.source = source;
        this.target = target;
        this.initialIndent = initialIndent;
    }

    public String getResult() {
        StringBuilder resultQuery = new StringBuilder();
        for (String query : createTableQueries) {
            resultQuery.append(query);
        }
        resultQuery.append(sqlQueryBuilder);
        for (String query : dropTempTableQueries) {
            resultQuery.append(query);
        }
        return resultQuery.toString();
    }

    public void visitScan(Scan operator) {
        scanTranslator.translate(operator, this);
    }

    public void visitSelect(Select operator) {
        selectTranslator.translate(operator, this);
    }

    public void visitSelectIn(SelectIn operator) {
        selectInTranslator.translate(operator, this);
    }

    public void visitSelectNotIn(SelectNotIn operator) {
        selectNotInTranslator.translate(operator, this);
    }

    public void visitJoin(Join operator) {
        joinTranslator.translate(operator, this);
    }

    public void visitCartesianProduct(CartesianProduct operator) {
        cartesianProductTranslator.translate(operator, this);
    }

    public void visitProject(Project operator) {
        projectTranslator.translate(operator, this);
    }

    public void visitDifference(Difference operator) {
        differenceTranslator.translate(operator, this);
    }

    public void visitUnion(Union operator) {
        unionTranslator.translate(operator, this);
    }

    public void visitOrderBy(OrderBy operator) {
        orderByTranslator.translate(operator, this);
    }

    public void visitOrderByRandom(OrderByRandom operator) {
        orderByRandomTranslator.translate(operator, this);
    }

    public void visitGroupBy(GroupBy operator) {
        groupByTranslator.translate(operator, this);
    }

    public void visitLimit(Limit operator) {
        limitTranslator.translate(operator, this);
    }

    public void visitOffset(Offset operator) {
        offsetTranslator.translate(operator, this);
    }

    public void visitRestoreOIDs(RestoreOIDs operator) {
        restoreOIDsTranslator.translate(operator, this);
    }

    public void visitCreateTable(CreateTableAs operator) {
        createTableAsTranslator.translate(operator, this);
    }

    public void visitDistinct(Distinct operator) {
        distinctTranslator.translate(operator, this);
    }

    public void visitPartition(Partition operator) {
        throw new UnsupportedOperationException("Not supported yet."); //TODO Implement method
    }

    public void visitIntersection(Intersection operator) {
        intersectionTranslator.translate(operator, this);
    }
    
    ///////////////////////////////////////////////////////////
    protected void visitChildren(IAlgebraOperator operator) {
        List<IAlgebraOperator> listOfChildren = operator.getChildren();
        if (listOfChildren != null) {
            for (IAlgebraOperator child : listOfChildren) {
                child.accept(this);
            }
        }
    }

    protected StringBuilder indentString() {
        StringBuilder indent = new StringBuilder(initialIndent);
        for (int i = 0; i < this.indentLevel; i++) {
            indent.append("    ");
        }
        return indent;
    }

    protected void incrementIndentLevel() {
        indentLevel++;
    }

    protected void reduceIndentLevel() {
        indentLevel--;
    }

    protected IDatabase getSource() {
        return source;
    }

    protected IDatabase getTarget() {
        return target;
    }

    protected List<String> getCreateTableQueries() {
        return createTableQueries;
    }

    protected void setCreateTableQueries(List<String> createTableQueries) {
        this.createTableQueries = createTableQueries;
    }

    protected TranslateCreateTableAs getCreateTableAsTranslator() {
        return createTableAsTranslator;
    }

    protected void setCreateTableAsTranslator(TranslateCreateTableAs createTableAsTranslator) {
        this.createTableAsTranslator = createTableAsTranslator;
    }

    protected List<AttributeRef> getCurrentProjectionAttribute() {
        return currentProjectionAttribute;
    }

    protected void setCurrentProjectionAttribute(List<AttributeRef> currentProjectionAttribute) {
        this.currentProjectionAttribute = currentProjectionAttribute;
    }

    protected SQLQueryBuilder getSQLQueryBuilder() {
        return this.sqlQueryBuilder;
    }

    protected void setSQLQueryBuilder(SQLQueryBuilder sqlQueryBuilder) {
        this.sqlQueryBuilder = sqlQueryBuilder;
    }

    public int getCounter() {
        return counter;
    }

    public void setCounter(int counter) {
        this.counter = counter;
    }

    public List<String> getDropTempTableQueries() {
        return dropTempTableQueries;
    }

    public void setDropTempTableQueries(List<String> dropTempTableQueries) {
        this.dropTempTableQueries = dropTempTableQueries;
    }

    public boolean isAddOIDColumn() {
        return addOIDColumn;
    }

    public void setAddOIDColumn(boolean addOIDColumn) {
        this.addOIDColumn = addOIDColumn;
    }

    public SelectNotIn getCurrentSelectNotIn() {
        return currentSelectNotIn;
    }

    public void setCurrentSelectNotIn(SelectNotIn currentSelectNotIn) {
        this.currentSelectNotIn = currentSelectNotIn;
    }

    public StringBuilder getSelectNotInBufferWhere() {
        return selectNotInBufferWhere;
    }

    public void setSelectNotInBufferWhere(StringBuilder selectNotInBufferWhere) {
        this.selectNotInBufferWhere = selectNotInBufferWhere;
    }

    protected void generateNestedSelect(IAlgebraOperator operator) {
        this.incrementIndentLevel();
        sqlQueryBuilder.append("(\n");
        operator.accept(this);
        sqlQueryBuilder.append("\n").append(this.indentString()).append(") AS ");
        sqlQueryBuilder.append("Nest_").append(operator.hashCode());
        this.reduceIndentLevel();
    }

    protected void createSQLSelectClause(IAlgebraOperator operator, List<NestedOperator> nestedSelect, boolean useTableName) {
        sqlQueryBuilder.append(this.indentString());
        sqlQueryBuilder.append("SELECT ");
        if (sqlQueryBuilder.isDistinct()) {
            sqlQueryBuilder.append("DISTINCT ");
            sqlQueryBuilder.setDistinct(false);
        }
        this.incrementIndentLevel();
        List<AttributeRef> attributes = operator.getAttributes(source, target);
        List<IAggregateFunction> aggregateFunctions = null;
        List<AttributeRef> newAttributes = null;
        IAlgebraOperator father = operator.getFather();
        if (father != null && (father instanceof Select)) {
            father = father.getFather();
        }
        if (father != null && (father instanceof Project)) {
            Project project = (Project) father;
            if (!project.isAggregative()) {
                attributes = project.getAttributes(source, target);
                aggregateFunctions = null;
            } else {
                attributes = null;
                aggregateFunctions = project.getAggregateFunctions();
            }
            newAttributes = project.getNewAttributes();
        }
        if (logger.isDebugEnabled()) logger.debug("Setting current projection attribute for operator " + operator + "\n: *** " + currentProjectionAttribute);
        this.currentProjectionAttribute = attributes;
        sqlQueryBuilder.append("\n").append(this.indentString());
        if (this.addOIDColumn) {
            sqlQueryBuilder.append("row_number() OVER () as oid, ");
            this.addOIDColumn = false;
        }
        sqlQueryBuilder.append(attributesToSQL(attributes, aggregateFunctions, newAttributes, nestedSelect, useTableName));
        this.reduceIndentLevel();
        sqlQueryBuilder.append("\n").append(this.indentString());
    }

    protected void createJoinClausePart(IAlgebraOperator operator, List<NestedOperator> nestedSelect) {
        if ((operator instanceof Join)) {
            IAlgebraOperator leftChild = operator.getChildren().get(0);
            IAlgebraOperator rightChild = operator.getChildren().get(1);
            createJoinClause((Join) operator, leftChild, rightChild, nestedSelect);
        } else if ((operator instanceof Scan)) {
            TableAlias tableAlias = ((Scan) operator).getTableAlias();
            sqlQueryBuilder.append(tableAliasToSQL(tableAlias));
        } else if ((operator instanceof Select)) {
            IAlgebraOperator child = operator.getChildren().get(0);
            createJoinClausePart(child, nestedSelect);
//            Select select = (Select) operator;
//            createWhereClause(select);
        } else if ((operator instanceof Project)) {
            sqlQueryBuilder.append("(\n");
            this.incrementIndentLevel();
            operator.accept(this);
            this.reduceIndentLevel();
            sqlQueryBuilder.append("\n").append(this.indentString()).append(") AS ");
            sqlQueryBuilder.append(generateNestedAlias(operator));
        } else if ((operator instanceof GroupBy)) {
            sqlQueryBuilder.append("(\n");
            this.incrementIndentLevel();
            operator.accept(this);
            this.reduceIndentLevel();
            sqlQueryBuilder.append("\n").append(this.indentString()).append(") AS ");
            sqlQueryBuilder.append(generateNestedAlias(operator));
        } else if ((operator instanceof Difference)) {
            sqlQueryBuilder.append("(\n");
            this.incrementIndentLevel();
            operator.accept(this);
            this.reduceIndentLevel();
            sqlQueryBuilder.append("\n").append(this.indentString()).append(") AS ");
            sqlQueryBuilder.append("Nest_").append(operator.hashCode());
        } else if ((operator instanceof Limit)) {
            sqlQueryBuilder.append("(\n");
            this.incrementIndentLevel();
            operator.accept(this);
            this.reduceIndentLevel();
            sqlQueryBuilder.append("\n").append(this.indentString()).append(") AS ");
            sqlQueryBuilder.append("Nest_").append(operator.hashCode());
        } else if ((operator instanceof CreateTableAs)) {
            this.incrementIndentLevel();
            operator.accept(this);
            this.reduceIndentLevel();
        } else {
            throw new IllegalArgumentException("Join not supported: " + operator);
        }
    }

    protected void createJoinClause(Join operator, IAlgebraOperator leftOperator, IAlgebraOperator rightOperator, List<NestedOperator> nestedSelects) {
        createJoinClausePart(leftOperator, nestedSelects);
        sqlQueryBuilder.append(" JOIN ");
        createJoinClausePart(rightOperator, nestedSelects);
        sqlQueryBuilder.append(" ON ");
        List<AttributeRef> leftAttributes = operator.getLeftAttributes();
        List<AttributeRef> rightAttributes = operator.getRightAttributes();
        for (int i = 0; i < leftAttributes.size(); i++) {
            AttributeRef leftAttributeRef = leftAttributes.get(i);
            AttributeRef rightAttributeRef = rightAttributes.get(i);
            Attribute leftAttribute = EngineUtility.getAttribute(leftAttributeRef, EngineUtility.getDatabase(leftAttributeRef, source, target));
            Attribute rightAttribute = EngineUtility.getAttribute(rightAttributeRef, EngineUtility.getDatabase(rightAttributeRef, source, target));
            boolean castNeeded = isCastNeeded(leftAttribute, rightAttribute);
            sqlQueryBuilder.append(getJoinAttributeSQL(leftAttributeRef, castNeeded, leftOperator, nestedSelects));
            sqlQueryBuilder.append(" = ");
            sqlQueryBuilder.append(getJoinAttributeSQL(rightAttributeRef, castNeeded, rightOperator, nestedSelects));
            sqlQueryBuilder.append(" AND ");
        }
        EngineUtility.removeChars(" AND ".length(), sqlQueryBuilder.getStringBuilder());
        while (leftOperator instanceof Select) {
            createWhereClause((Select) leftOperator, true);
            leftOperator = leftOperator.getChildren().get(0);
        }
        while (rightOperator instanceof Select) {
            createWhereClause((Select) rightOperator, true);
            rightOperator = rightOperator.getChildren().get(0);
        }
    }

    private boolean isCastNeeded(Attribute leftAttribute, Attribute rightAttribute) {
        logger.debug("leftAttribute:" + leftAttribute.getName() + "- " + leftAttribute.getType());
        logger.debug("rightAttribute:" + rightAttribute.getName() + "- " + rightAttribute.getType());
        String leftType = leftAttribute.getType();
        String rightType = rightAttribute.getType();
        if (EngineUtility.isNumeric(leftType) && EngineUtility.isNumeric(rightType)) {
            return false;
        }
        return !leftAttribute.getType().equals(rightAttribute.getType());
    }

    private String getJoinAttributeSQL(AttributeRef attribute, boolean castNeeded, IAlgebraOperator operator, List<NestedOperator> nestedSelects) {
        boolean useAlias = false;
        if (operator instanceof CreateTableAs) {
            useAlias = true;
        }
//            if (operator instanceof Join && (operator.getChildren().get(0) instanceof CreateTableAs)
//                    && (operator.getChildren().get(1) instanceof CreateTableAs)) {
        if (isJoinBetweenCreateTables(operator)) {
            useAlias = true;
        }
        IAlgebraOperator nestedOperator = findNestedOperator(nestedSelects, operator, attribute.getTableAlias());
        if (nestedOperator != null) {
            useAlias = true;
        }
        String attributeResult;
        if (useAlias) {
            attributeResult = DBMSUtility.attributeRefToAliasSQL(attribute);
        } else {
            attributeResult = DBMSUtility.attributeRefToSQLDot(attribute);
        }
        if (castNeeded) {
            attributeResult = "CAST(" + attributeResult + " as text)";
        }
//            if (attributeResult.equals("")) {
//                logger.error(" Attribute: " + attribute);
//                logger.error(" Operator: " + operator);
//                logger.error(" NestedOperator: " + nestedOperator);
//                logger.error(" Result: " + attributeResult);
//            }
        return attributeResult;
    }

    private boolean isJoinBetweenCreateTables(IAlgebraOperator operator) {
        if (!(operator instanceof Join)) {
            return false;
        }
        IAlgebraOperator firstChild = operator.getChildren().get(0);
        IAlgebraOperator secondChild = operator.getChildren().get(1);
        if ((firstChild instanceof CreateTableAs) && (secondChild instanceof CreateTableAs)) {
            return true;
        }
        boolean isFirstChildJoinBtwCreateTables = isJoinBetweenCreateTables(firstChild);
        boolean isSecondChildJoinBtwCreateTables = isJoinBetweenCreateTables(secondChild);
        if (isFirstChildJoinBtwCreateTables && (secondChild instanceof CreateTableAs)
                || ((firstChild instanceof CreateTableAs) && isSecondChildJoinBtwCreateTables)
                || (isFirstChildJoinBtwCreateTables && isSecondChildJoinBtwCreateTables)) {
            return true;
        }
        return false;
    }

    protected void createWhereClause(Select operator, boolean append) {
        if (operator.getChildren() != null && operator.getChildren().get(0) instanceof GroupBy) {
            return; //HAVING
        }
        sqlQueryBuilder.append("\n").append(this.indentString());
        if (append || operator.getChildren() != null
                && (operator.getChildren().get(0) instanceof Select
                || operator.getChildren().get(0) instanceof Join)) {
            sqlQueryBuilder.append(" AND ");
        } else {
            sqlQueryBuilder.append(" WHERE ");
        }
        this.incrementIndentLevel();
        sqlQueryBuilder.append("\n").append(this.indentString());
        for (Expression condition : operator.getSelections()) {
            boolean useAlias = true;
            if (isInACartesianProduct(operator)) {
                useAlias = false;
            }
            if (!operator.getChildren().isEmpty()) {
                IAlgebraOperator firstChild = operator.getChildren().get(0);
                if (firstChild instanceof Difference) {
                    Difference diff = (Difference) operator.getChildren().get(0);
                    if (diff.getChildren().get(0) instanceof Difference || diff.getChildren().get(1) instanceof Difference) {
                        useAlias = false;
                    }
                }
            }
            String expressionSQL = sqlGenerator.expressionToSQL(condition, useAlias, source, target);
            sqlQueryBuilder.append(expressionSQL);
            sqlQueryBuilder.append(" AND ");
        }
        EngineUtility.removeChars(" AND ".length(), sqlQueryBuilder.getStringBuilder());
        this.reduceIndentLevel();
    }

    private boolean isInACartesianProduct(IAlgebraOperator operator) {
        if (operator.getChildren().isEmpty()) {
            return false;
        }
        IAlgebraOperator firstChild = operator.getChildren().get(0);
        if (firstChild instanceof CartesianProduct) {
            return true;
        }
        if (firstChild instanceof Select) {
            return isInACartesianProduct(firstChild);
        }
        return false;
    }

    protected List<NestedOperator> findNestedTablesForJoin(IAlgebraOperator operator) {
        List<NestedOperator> attributes = new ArrayList<NestedOperator>();
        IAlgebraOperator leftChild = operator.getChildren().get(0);
        for (AttributeRef nestedAttribute : getNestedAttributes(leftChild)) {
            NestedOperator nestedOperator = new NestedOperator(leftChild, nestedAttribute.getTableAlias());
            attributes.add(nestedOperator);
        }
        IAlgebraOperator rightChild = operator.getChildren().get(1);
        for (AttributeRef nestedAttribute : getNestedAttributes(rightChild)) {
            NestedOperator nestedOperator = new NestedOperator(rightChild, nestedAttribute.getTableAlias());
            attributes.add(nestedOperator);
        }
        List<NestedOperator> tableAliases = new ArrayList<NestedOperator>();
        for (NestedOperator nestedOperator : attributes) {
            if (containsAlias(tableAliases, nestedOperator.getAlias())) {
                continue;
            }
            tableAliases.add(nestedOperator);
        }
        if (logger.isDebugEnabled()) logger.debug("Nested tables for operator:\n" + operator + "\n" + tableAliases);
        return tableAliases;
    }

    protected List<AttributeRef> getNestedAttributes(IAlgebraOperator operator) {
        List<AttributeRef> attributes = new ArrayList<AttributeRef>();
        if (operator instanceof Difference) {
            attributes.addAll(operator.getAttributes(source, target));
        }
        if (operator instanceof GroupBy) {
            attributes.addAll(operator.getAttributes(source, target));
        }
        if (operator instanceof Project) {
            attributes.addAll(operator.getAttributes(source, target));
        }
        if (operator instanceof Limit) {
            attributes.addAll(operator.getAttributes(source, target));
        }
        if (operator instanceof Join) {
            IAlgebraOperator leftChild = operator.getChildren().get(0);
            attributes.addAll(getNestedAttributes(leftChild));
            IAlgebraOperator rightChild = operator.getChildren().get(1);
            attributes.addAll(getNestedAttributes(rightChild));
//            attributes.addAll(operator.getAttributes(source, target));
        }
        if (operator instanceof CreateTableAs) {
            attributes.addAll(operator.getAttributes(source, target));
//            CreateTableAs createTable = (CreateTableAs)operator;
//            for (AttributeRef attributeRef : operator.getAttributes(source, target)) {
//                attributes.add(new AttributeRef(createTable.getTableAlias(), attributeRef.getName()));
//            }
        }
        if (operator instanceof Join) {
            IAlgebraOperator leftChild = operator.getChildren().get(0);
            attributes.addAll(getNestedAttributes(leftChild));
            IAlgebraOperator rightChild = operator.getChildren().get(1);
            attributes.addAll(getNestedAttributes(rightChild));
//            attributes.addAll(operator.getAttributes(source, target));
        }
        return attributes;
    }

    private String generateNestedAlias(IAlgebraOperator operator) {
        if (operator instanceof Scan) {
            TableAlias tableAlias = ((Scan) operator).getTableAlias();
            return tableAlias.getTableName();
        } else if (operator instanceof Select) {
            Select select = (Select) operator;
            operator = select.getChildren().get(0);
            if (operator instanceof Scan) {
                TableAlias tableAlias = ((Scan) operator).getTableAlias();
                return tableAlias.getTableName();
            }
        }
        IAlgebraOperator child = operator.getChildren().get(0);
        if (child != null) {
            return generateNestedAlias(child);
        }
        return "Nest_" + operator.hashCode();
    }

    private String attributesToSQL(List<AttributeRef> attributes, List<IAggregateFunction> aggregateFunctions,
            List<AttributeRef> newAttributes, List<NestedOperator> nestedSelect, boolean useTableName) {
        if (logger.isDebugEnabled()) logger.debug("Generating SQL for attributes\n\nAttributes: " + attributes + "\n\t" + newAttributes + "\n\tNested Select: " + nestedSelect + "\n\tuseTableName: " + useTableName);
        StringBuilder sb = new StringBuilder();
        if (attributes != null) {
            List<String> sqlAttributes = new ArrayList<String>();
            for (int i = 0; i < attributes.size(); i++) {
                AttributeRef newAttributeRef = null;
                if (newAttributes != null) {
                    newAttributeRef = newAttributes.get(i);
                }
                String attribute = attributeToSQL(attributes.get(i), useTableName, nestedSelect, newAttributeRef);
                if (!sqlAttributes.contains(attribute)) {
                    sqlAttributes.add(attribute);
                }
            }
            for (String attribute : sqlAttributes) {
                sb.append(attribute).append(",\n").append(this.indentString());
            }
            EngineUtility.removeChars(",\n".length() + this.indentString().length(), sb);
        } else {
            boolean hasAttributesInAggregates = false;
            for (int i = 0; i < aggregateFunctions.size(); i++) {
                AttributeRef newAttributeRef = null;
                if (newAttributes != null) {
                    newAttributeRef = newAttributes.get(i);
                }
                IAggregateFunction aggregateFunction = aggregateFunctions.get(i);
                AttributeRef attributeRef = aggregateFunction.getAttributeRef();
                if (attributeRef.toString().contains(EngineConstants.AGGR + "." + EngineConstants.COUNT)) {
                    continue;
                }
                if (newAttributeRef == null) {
                    newAttributeRef = aggregateFunction.getAttributeRef();
                }
                sb.append(aggregateFunctionToString(aggregateFunction, newAttributeRef, nestedSelect));
                sb.append(", ");
                hasAttributesInAggregates = true;
            }
            if (hasAttributesInAggregates) {
                EngineUtility.removeChars(", ".length(), sb);
            }
        }
        return sb.toString();
    }

    public String attributeToSQL(AttributeRef attributeRef, boolean useTableName, List<NestedOperator> nestedSelects, AttributeRef newAttributeRef) {
        StringBuilder sb = new StringBuilder();
        String attributeName;
//            if (!useTableName || containsAlias(nestedSelects, attributeRef.getTableAlias())) {
        if (!useTableName || containsNestedAttribute(nestedSelects, attributeRef)) {
            attributeName = DBMSUtility.attributeRefToAliasSQL(attributeRef);
        } else {
            attributeName = DBMSUtility.attributeRefToSQLDot(attributeRef);
        }
        sb.append(attributeName);
        if (newAttributeRef != null) {
            sb.append(" AS ");
            String alias = newAttributeRef.getName();
            sb.append(alias);
//            } else if (!(containsAlias(nestedSelects, attributeRef.getTableAlias()))) {
        } else if (!(containsNestedAttribute(nestedSelects, attributeRef))) {
            sb.append(" AS ");
            String alias = DBMSUtility.attributeRefToAliasSQL(attributeRef);
            sb.append(alias);
        }
        return sb.toString();
    }

    private boolean containsNestedAttribute(List<NestedOperator> nestedSelects, AttributeRef attribute) {
        for (NestedOperator nestedSelect : nestedSelects) {
            if (!nestedSelect.getAlias().equals(attribute.getTableAlias())) {
                continue;
            }
            IAlgebraOperator operator = nestedSelect.getOperator();
            return operator.getAttributes(source, target).contains(attribute);
        }
        return false;
    }

    protected boolean containsAlias(List<NestedOperator> nestedSelects, TableAlias alias) {
        for (NestedOperator nestedSelect : nestedSelects) {
            if (nestedSelect.getAlias().equals(alias)) {
                return true;
            }
        }
        return false;
    }

    private IAlgebraOperator findNestedOperator(List<NestedOperator> nestedSelects, IAlgebraOperator operator, TableAlias alias) {
        for (NestedOperator nestedSelect : nestedSelects) {
            if (nestedSelect.getAlias().equals(alias) && nestedSelect.getOperator().equals(operator)) {
//                if (nestedSelect.operator.equals(operator)) {
                return nestedSelect.getOperator();
            }
        }
        return null;
    }

    protected String tableAliasToSQL(TableAlias tableAlias) {
        StringBuilder sb = new StringBuilder();
        ITable table;
        if (tableAlias.isSource()) {
            table = source.getTable(tableAlias.getTableName());
        } else {
            table = target.getTable(tableAlias.getTableName());
        }
        sb.append(table.toShortString());
        if (tableAlias.isAliased() || tableAlias.isSource() || (table instanceof DBMSVirtualTable)) {
            sb.append(" AS ").append(DBMSUtility.tableAliasToSQL(tableAlias));
        }
        return sb.toString();
    }

    protected String aggregateFunctionToString(IAggregateFunction aggregateFunction, AttributeRef newAttribute, List<NestedOperator> nestedTables) {
        String aggregateAttribute;
        if (containsNestedAttribute(nestedTables, aggregateFunction.getAttributeRef())) {
            aggregateAttribute = DBMSUtility.attributeRefToAliasSQL(aggregateFunction.getAttributeRef());
        } else {
            aggregateAttribute = DBMSUtility.attributeRefToSQLDot(aggregateFunction.getAttributeRef());
        }
        if (aggregateFunction instanceof ValueAggregateFunction) {
            return aggregateAttribute + " as " + DBMSUtility.attributeRefToAliasSQL(newAttribute);
        }
        if (aggregateFunction instanceof MaxAggregateFunction) {
            return "max(" + aggregateAttribute + ") as " + DBMSUtility.attributeRefToAliasSQL(newAttribute);
        }
        if (aggregateFunction instanceof MinAggregateFunction) {
            return "min(" + aggregateAttribute + ") as " + DBMSUtility.attributeRefToAliasSQL(newAttribute);
        }
        if (aggregateFunction instanceof AvgAggregateFunction) {
            return "avg(" + aggregateAttribute + ") as " + DBMSUtility.attributeRefToAliasSQL(newAttribute);
        }
        if (aggregateFunction instanceof StdDevAggregateFunction) {
            return "stddev(" + aggregateAttribute + ") as " + DBMSUtility.attributeRefToAliasSQL(newAttribute);
        }
        if (aggregateFunction instanceof SumAggregateFunction) {
            return "sum(" + aggregateAttribute + ") as " + DBMSUtility.attributeRefToAliasSQL(newAttribute);
        }
        if (aggregateFunction instanceof CountAggregateFunction) {
            return "count(*) as " + DBMSUtility.attributeRefToAliasSQL(aggregateFunction.getAttributeRef());
        }
        throw new UnsupportedOperationException("Unable generate SQL for aggregate function" + aggregateFunction);
    }

    public void visitExtractRandomSample(ExtractRandomSample operator) {
        long rangeMin = operator.getFloor();
        long rangeMax = operator.getCeil();
        long sampleSize = operator.getSampleSize();
        sqlQueryBuilder.append(this.indentString());
        this.incrementIndentLevel();
        sqlQueryBuilder.append("SELECT * FROM (").append("\n");
        this.incrementIndentLevel();
        sqlQueryBuilder.append(this.indentString());
        sqlQueryBuilder.append("SELECT * FROM (").append("\n");
        this.incrementIndentLevel();
        sqlQueryBuilder.append(this.indentString());
        sqlQueryBuilder.append("SELECT DISTINCT ").append(rangeMin).append(" + floor(random() * ");
        sqlQueryBuilder.append(rangeMax).append(")::integer AS oid").append("\n");
        sqlQueryBuilder.append(this.indentString());
        sqlQueryBuilder.append("FROM generate_series(1," + sampleSize + ") g").append("\n");
        this.reduceIndentLevel();
        sqlQueryBuilder.append(this.indentString());
        sqlQueryBuilder.append(") as r").append("\n");
        sqlQueryBuilder.append(this.indentString());
        sqlQueryBuilder.append("JOIN (").append("\n");
        this.incrementIndentLevel();
        visitChildren(operator);
        sqlQueryBuilder.append("\n");
        this.reduceIndentLevel();
        sqlQueryBuilder.append(this.indentString());
        AttributeRef oidAttribute = EngineUtility.getFirstOIDAttribute(operator.getAttributes(source, target));
        if (oidAttribute == null) {
            throw new IllegalArgumentException("ExtractRandomSample operator has a child without OID." + operator);
        }
    }

}
