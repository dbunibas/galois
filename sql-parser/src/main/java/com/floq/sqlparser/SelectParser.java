package com.floq.sqlparser;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.*;
import engine.EngineConstants;
import engine.model.algebra.Distinct;
import engine.model.algebra.Join;
import engine.model.algebra.Limit;
import engine.model.algebra.Select;
import engine.model.algebra.*;
import engine.model.algebra.aggregatefunctions.CountAggregateFunction;
import engine.model.algebra.aggregatefunctions.IAggregateFunction;
import engine.model.algebra.aggregatefunctions.ValueAggregateFunction;
import engine.model.database.AttributeRef;
import engine.model.database.TableAlias;
import engine.model.database.VirtualAttributeRef;
import engine.model.expressions.ExpressionAttributeRef;
import engine.persistence.Types;

import java.util.*;
import java.util.stream.Stream;

import static com.floq.sqlparser.ParseUtils.contextToParseContext;

@Slf4j
@RequiredArgsConstructor
public class SelectParser extends SelectVisitorAdapter<IAlgebraOperator> {
    private final ScanNodeFactory scanNodeFactory;

    @Override
    public <S> IAlgebraOperator visit(PlainSelect plainSelect, S context) {
        ParseContext parseContext = new ParseContext();

        // From
        FromParser fromParser = new FromParser();
        FromItem fromItem = plainSelect.getFromItem();
        TableAlias tableAlias = fromItem.accept(fromParser, null);
        parseContext.addTableAlias(tableAlias);

        // Join
        Join join = null;
        TableAlias joinRightTableAlias;
        if (plainSelect.getJoins() != null && !plainSelect.getJoins().isEmpty()) {
            if (plainSelect.getJoins().size() > 1) {
                throw new UnsupportedOperationException("Join with more than two tables are unsupported!");
            }
            JoinParser joinParser = new JoinParser();
            net.sf.jsqlparser.statement.select.Join firstJoin = plainSelect.getJoins().getFirst();
            joinRightTableAlias = firstJoin.getFromItem().accept(fromParser, null);
            parseContext.addTableAlias(joinRightTableAlias);
            join = joinParser.parseJoin(firstJoin, tableAlias, joinRightTableAlias);
        } else {
            joinRightTableAlias = null;
        }

        // Where
        WhereParser.WhereParseResult whereParseResult = null;
        Select select = null;
        if (plainSelect.getWhere() != null) {
            WhereParser whereParser = new WhereParser();
            whereParseResult = plainSelect.getWhere().accept(whereParser, parseContext);
            if (whereParseResult == null) {
                throw new UnsupportedOperationException(String.format("Expression %s is unsupported!", plainSelect.getWhere().getClass()));
            }
            select = new Select(whereParseResult.expression());
        }

        // Group by
        GroupBy groupBy = null;
        if (plainSelect.getGroupBy() != null) {
            groupBy = plainSelect.getGroupBy().accept(new GroupByParser(), parseContext);
        }

        // Select
        Project project = null;
        List<ProjectionAttribute> projectionAttributes = List.of();
        if (hasSelectItems(plainSelect)) {
            SelectItemParser selectItemParser = new SelectItemParser();
            projectionAttributes = plainSelect.getSelectItems().stream()
                    .map(i -> i.accept(selectItemParser, parseContext))
                    .toList();

            // TODO: Refactor this (handles the compatibility with group by)
            if (groupBy != null && projectionAttributes.stream().anyMatch(p -> p.isAggregative() && p.getAggregateFunction() instanceof CountAggregateFunction)) {
                ProjectionAttribute groupByProjectionAttribute = projectionAttributes.stream()
                        .filter(p -> p.isAggregative() && p.getAggregateFunction() instanceof CountAggregateFunction)
                        .findFirst()
                        .orElseThrow();
                AttributeRef countRef = new VirtualAttributeRef(parseContext.getFromItemTableAlias(), EngineConstants.COUNT, Types.INTEGER);
                List<IAggregateFunction> aggregateFunctions = Stream.concat(
                        groupBy.getAggregateFunctions().stream(),
                        Stream.of(new CountAggregateFunction(countRef))
                ).toList();
                groupBy = new GroupBy(groupBy.getGroupingAttributes(), aggregateFunctions);
                projectionAttributes = projectionAttributes.stream()
                        .map(p -> p == groupByProjectionAttribute ? new ProjectionAttribute(countRef) : p)
                        .toList();
            }

            // TODO: Refactor this (this makes pure attributes and aggregative functions compatible)
            if (projectionAttributes.stream().anyMatch(ProjectionAttribute::isAggregative)) {
                projectionAttributes = projectionAttributes.stream()
                        .map(p -> !p.isAggregative() ? new ProjectionAttribute(new ValueAggregateFunction(p.getAttributeRef())) : p)
                        .toList();
            }

            // TODO: Refactor this (this handles aliases)
            List<AttributeRef> aliasAttributes = new ArrayList<>();
            for (int i = 0; i < projectionAttributes.size(); i++) {
                SelectItem<?> item = plainSelect.getSelectItems().get(i);
                AttributeRef attributeRef = projectionAttributes.get(i).getAttributeRef();
                AttributeRef newAttribute = attributeRef;
                if (item.getAlias() != null) {
                    if (attributeRef instanceof ExpressionAttributeRef expressionAttributeRef) {
                        newAttribute = new ExpressionAttributeRef(expressionAttributeRef.getExpression(), expressionAttributeRef.getTableAlias(), item.getAlias().getName(), expressionAttributeRef.getType());
                    } else if (attributeRef instanceof VirtualAttributeRef virtualAttributeRef) {
                        newAttribute = new VirtualAttributeRef(virtualAttributeRef.getTableAlias(), item.getAlias().getName(), virtualAttributeRef.getType());
                    } else {
                        newAttribute = new AttributeRef(attributeRef.getTableAlias(), item.getAlias().getName());
                    }
                }
                aliasAttributes.add(newAttribute);
            }

            log.trace("Parsed projection attributes: {} - aliases: {}", projectionAttributes, aliasAttributes);

            project = new Project(projectionAttributes, aliasAttributes, false);
        }

        // Order by
        OrderBy orderBy = null;
        if (plainSelect.getOrderByElements() != null && !plainSelect.getOrderByElements().isEmpty()) {
            OrderByElementParser orderByElementParser = new OrderByElementParser();
            List<AttributeRef> orderByRefs = plainSelect.getOrderByElements().stream()
                    .map(e -> e.accept(orderByElementParser, parseContext))
                    .toList();
            orderBy = new OrderBy(orderByRefs);
            if (!plainSelect.getOrderByElements().getFirst().isAsc()) {
                orderBy.setOrder(OrderBy.ORDER_DESC);
            }
        }

        // Limit
        Limit limit = null;
        if (plainSelect.getLimit() != null) {
            limit = new Limit(plainSelect.getLimit().getRowCount(LongValue.class).getValue());
        }

        // Distinct
        Distinct distinct = null;
        if (plainSelect.getDistinct() != null) {
            distinct = new Distinct();
        }

        IAlgebraOperator currentRoot;

        // TODO: Refactor by adding attributes in each method
        Set<AttributeRef> attributeRefSet = new HashSet<>();
        if (join != null) {
            attributeRefSet.addAll(join.getLeftAttributes());
            attributeRefSet.addAll(join.getRightAttributes());
        }
        if (whereParseResult != null) {
            attributeRefSet.addAll(whereParseResult.variableDescriptions().stream()
                    .map(WhereParser.VariableDescription::attributeRef)
                    .toList()
            );
        }
        if (groupBy != null) {
            attributeRefSet.addAll(groupBy.getGroupingAttributes());
        }
        if (!projectionAttributes.isEmpty()) {
            attributeRefSet.addAll(projectionAttributes.stream()
                    .map(ProjectionAttribute::getAttributeRef)
                    .toList()
            );
        }
        if (orderBy != null) {
            attributeRefSet.addAll(orderBy.getAttributes(null, null));
        }

        // If there is no projection, always empty the scan attributes
        if (project == null) {
            attributeRefSet.clear();
        }

        // Check for $all special projection attribute
        AttributeRef allAttribute = attributeRefSet.stream()
                .filter(a -> a.getName().equals(ParseConstants.ALL_ATTRIBUTES))
                .findFirst()
                .orElse(null);
        if (project != null && allAttribute != null) {
            // FIXME: Add support for multiple table project: (ex. t1.*, t2.attribute)
            List<ProjectionAttribute> cleanedProjectionAttributes = projectionAttributes.stream()
                    .filter(pa -> !pa.getAttributeRef().getName().equals(ParseConstants.ALL_ATTRIBUTES))
                    .toList();
            List<AttributeRef> cleanedAttributeRef = project.getNewAttributes().stream()
                    .filter(a -> !a.getName().equals(ParseConstants.ALL_ATTRIBUTES))
                    .toList();
            project = cleanedProjectionAttributes.isEmpty() ?
                    null :
                    new Project(cleanedProjectionAttributes, cleanedAttributeRef, false);

            if (allAttribute.getTableAlias() != null) {
                attributeRefSet.removeIf(a -> a.getTableAlias().equals(allAttribute.getTableAlias()));
            } else {
                attributeRefSet.clear();
            }
        }

        // Check for count(*) projection attribute
        ProjectionAttribute countAttribute = projectionAttributes.stream()
                .filter(pa -> pa.getAggregateFunction() instanceof CountAggregateFunction)
                .findFirst()
                .orElse(null);
        if (project != null && countAttribute != null && countAttribute.getAttributeRef().getName().equals(EngineConstants.COUNT)) {
            attributeRefSet.clear();
        }

        List<AttributeRef> attributeRef = attributeRefSet.stream()
                .filter(a -> a.getTableAlias().equals(tableAlias))
                .toList();
        // TODO: Refactor
        currentRoot = scanNodeFactory.createScanNode(parseContext.getFromItemTableAlias(), attributeRef);
        if (select != null) {
            select.addChild(currentRoot);
            currentRoot = select;
        }
        if (join != null) {
            join.addChild(currentRoot);
            List<AttributeRef> joinRightAttributes = attributeRefSet.stream()
                    .filter(a -> a.getTableAlias().equals(joinRightTableAlias))
                    .toList();
            Scan joinRightScan = scanNodeFactory.createScanNode(joinRightTableAlias, joinRightAttributes);
            join.addChild(joinRightScan);
            currentRoot = join;
        }
        if (groupBy != null) {
            groupBy.addChild(currentRoot);
            currentRoot = groupBy;
        }
        boolean shouldAddFullProject = true;
        if (orderBy != null) {
            // Check if orderBy needs partial aliased projection attributes
            if (project != null) {
                boolean isProjectionAggregative = projectionAttributes.stream().anyMatch(ProjectionAttribute::isAggregative);
                List<AttributeRef> projectAttributes = project.getAttributes(null, null);
                List<AttributeRef> newAttributes = project.getNewAttributes();

                List<AttributeRef> orderByExclusiveAttributes = orderBy.getAttributes(null, null).stream()
                        .filter(a -> projectAttributes.stream().noneMatch(pa -> pa.equalsModuloClass(a)) && newAttributes.stream().noneMatch(na -> na.equalsModuloClass(a)))
                        .toList();
                log.trace("Order by exclusive attributes {}", orderByExclusiveAttributes);

                if (!orderByExclusiveAttributes.isEmpty()) {
                    List<ProjectionAttribute> partialProjectionAttributes = Stream.concat(
                            projectionAttributes.stream(),
                            orderByExclusiveAttributes.stream().map(a -> isProjectionAggregative ? new ProjectionAttribute(new ValueAggregateFunction(a)) : new ProjectionAttribute(a))
                    ).distinct().toList();
                    List<AttributeRef> partialAliases = Stream.concat(
                            project.getNewAttributes().stream(),
                            orderByExclusiveAttributes.stream()
                    ).distinct().toList();
                    log.trace("Partial projection attributes {}", partialProjectionAttributes);
                    log.trace("Partial aliases {}", partialAliases);

                    Project partialProject = new Project(partialProjectionAttributes, partialAliases, false);
                    partialProject.addChild(currentRoot);
                    currentRoot = partialProject;
                } else {
                    shouldAddFullProject = false;
                    project.addChild(currentRoot);
                    currentRoot = project;
                }
            }

            orderBy.addChild(currentRoot);
            currentRoot = orderBy;
        }
        if (project != null && shouldAddFullProject) {
            project.addChild(currentRoot);
            currentRoot = project;
        }
        if (limit != null) {
            limit.addChild(currentRoot);
            currentRoot = limit;
        }
        if (distinct != null) {
            distinct.addChild(currentRoot);
            currentRoot = distinct;
        }

        return currentRoot;
    }

    private boolean hasSelectItems(PlainSelect plainSelect) {
        List<SelectItem<?>> selectItems = plainSelect.getSelectItems();
        return selectItems.size() > 1 || !Objects.equals(selectItems.getFirst().toString(), "*");
    }

    private static class SelectItemParser extends SelectItemVisitorAdapter<ProjectionAttribute> {
        @Override
        public <S> ProjectionAttribute visit(SelectItem<? extends Expression> item, S context) {
            ProjectionAttribute projectionAttribute = item.getExpression().accept(new ProjectExpressionParser(), context);
            if (projectionAttribute == null) {
                throw new UnsupportedOperationException(String.format("Expression %s is unsupported!", item.getExpression().getClass()));
            }
            return projectionAttribute;
        }
    }

    private static class OrderByElementParser extends OrderByVisitorAdapter<AttributeRef> {
        @Override
        public <S> AttributeRef visit(OrderByElement orderBy, S context) {
            ParseContext parseContext = contextToParseContext(context);
            Expression expression = orderBy.getExpression();

            if (expression instanceof Column column) {
                return new AttributeRef(parseContext.getTableAliasFromColumn(column), column.getColumnName());
            }

            throw new UnsupportedOperationException(String.format("Order by element %s is unsupported!", expression.getClass()));
        }
    }
}
