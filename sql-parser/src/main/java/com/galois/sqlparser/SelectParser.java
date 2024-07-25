package com.galois.sqlparser;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.*;
import speedy.SpeedyConstants;
import speedy.model.algebra.Limit;
import speedy.model.algebra.Select;
import speedy.model.algebra.*;
import speedy.model.algebra.aggregatefunctions.CountAggregateFunction;
import speedy.model.algebra.aggregatefunctions.IAggregateFunction;
import speedy.model.algebra.aggregatefunctions.ValueAggregateFunction;
import speedy.model.database.AttributeRef;
import speedy.model.database.TableAlias;
import speedy.model.database.VirtualAttributeRef;
import speedy.persistence.Types;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@AllArgsConstructor
public class SelectParser extends SelectVisitorAdapter<IAlgebraOperator> {
    ScanNodeFactory scanNodeFactory;

    @Override
    public <S> IAlgebraOperator visit(PlainSelect plainSelect, S context) {
        // From
        FromParser fromParser = new FromParser();
        FromItem fromItem = plainSelect.getFromItem();
        TableAlias tableAlias = fromItem.accept(fromParser, null);
//        currentRoot = scanNodeFactory.createScanNode(tableAlias, List.of());

        // Where
        WhereParser.WhereParseResult whereParseResult = null;
        Select select = null;
        if (plainSelect.getWhere() != null) {
            WhereParser whereParser = new WhereParser();
            whereParseResult = plainSelect.getWhere().accept(whereParser, tableAlias);
            if (whereParseResult == null) {
                throw new UnsupportedOperationException(String.format("Expression %s is unsupported!", plainSelect.getWhere().getClass()));
            }
            select = new Select(whereParseResult.expression());
//            select.addChild(currentRoot);
//            currentRoot = select;
        }

        // Group by
        GroupBy groupBy = null;
        if (plainSelect.getGroupBy() != null) {
            groupBy = plainSelect.getGroupBy().accept(new GroupByParser(), tableAlias);
//            groupBy.addChild(currentRoot);
//            currentRoot = groupBy;
        }

        // Select
        Project project = null;
        List<ProjectionAttribute> projectionAttributes = List.of();
        if (hasSelectItems(plainSelect)) {
            SelectItemParser selectItemParser = new SelectItemParser();
            projectionAttributes = plainSelect.getSelectItems().stream()
                    .map(i -> i.accept(selectItemParser, tableAlias))
                    .toList();

            // TODO: Refactor this (handles the compatibility with group by)
            if (groupBy != null && projectionAttributes.stream().anyMatch(p -> p.isAggregative() && p.getAggregateFunction() instanceof CountAggregateFunction)) {
                ProjectionAttribute groupByProjectionAttribute = projectionAttributes.stream()
                        .filter(p -> p.isAggregative() && p.getAggregateFunction() instanceof CountAggregateFunction)
                        .findFirst()
                        .orElseThrow();
                AttributeRef countRef = new VirtualAttributeRef(tableAlias, SpeedyConstants.COUNT, Types.INTEGER);
                List<IAggregateFunction> aggregateFunctions = Stream.concat(
                        groupBy.getAggregateFunctions().stream(),
                        Stream.of(new CountAggregateFunction(countRef))
                ).toList();
                groupBy = new GroupBy(groupBy.getGroupingAttributes(), aggregateFunctions);
//                groupBy.addChild(currentRoot.getChildren().getFirst());
//                currentRoot = groupBy;
                projectionAttributes = projectionAttributes.stream()
                        .map(p -> p == groupByProjectionAttribute ? new ProjectionAttribute(countRef) : p)
                        .toList();
            }

            // TODO: Refactor this (this makes pure attributes and aggregative functions compatible
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
                if(item.getAlias() != null){
                    if(attributeRef instanceof VirtualAttributeRef){
                        newAttribute = new VirtualAttributeRef(tableAlias, item.getAlias().getName(), ((VirtualAttributeRef) attributeRef).getType());
                    }else {
                        newAttribute = new AttributeRef(tableAlias, item.getAlias().getName());
                    }
                }
                aliasAttributes.add(newAttribute);
            }
            log.trace("Parsed projection attributes: {} - aliases: {}", projectionAttributes, aliasAttributes);

            project = new Project(projectionAttributes, aliasAttributes, false);
//            project.addChild(currentRoot);
//            currentRoot = project;
        }

        // Order by
        OrderBy orderBy = null;
        if (plainSelect.getOrderByElements() != null && !plainSelect.getOrderByElements().isEmpty()) {
            OrderByElementParser orderByElementParser = new OrderByElementParser();
            List<AttributeRef> orderByRefs = plainSelect.getOrderByElements().stream()
                    .map(e -> e.accept(orderByElementParser, tableAlias))
                    .toList();
            orderBy = new OrderBy(orderByRefs);
            if (!plainSelect.getOrderByElements().getFirst().isAsc()) {
                orderBy.setOrder(OrderBy.ORDER_DESC);
            }
//            orderBy.addChild(currentRoot);
//            currentRoot = orderBy;
        }

        // Limit
        Limit limit = null;
        if (plainSelect.getLimit() != null) {
            limit = new Limit(plainSelect.getLimit().getRowCount(LongValue.class).getValue());
//            limit.addChild(currentRoot);
//            currentRoot = limit;
        }

        IAlgebraOperator currentRoot;

        // TODO: Refactor by adding attributes in each method
        Set<AttributeRef> attributeRefSet = new HashSet<>();
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

        attributeRefSet = attributeRefSet.stream()
                .filter(a -> !a.isAliased())
                .collect(Collectors.toUnmodifiableSet());

        // TODO: Refactor
        currentRoot = scanNodeFactory.createScanNode(tableAlias, attributeRefSet.stream().toList());
        if (select != null) {
            select.addChild(currentRoot);
            currentRoot = select;
        }
        if (groupBy != null) {
            groupBy.addChild(currentRoot);
            currentRoot = groupBy;
        }
        if (project != null) {
            project.addChild(currentRoot);
            currentRoot = project;
        }
        if (orderBy != null) {
            orderBy.addChild(currentRoot);
            currentRoot = orderBy;
        }
        if (limit != null) {
            limit.addChild(currentRoot);
            currentRoot = limit;
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
            Expression expression = orderBy.getExpression();

            if (expression instanceof Column) {
                return new AttributeRef((TableAlias) context, ((Column) expression).getColumnName());
            }

            throw new UnsupportedOperationException(String.format("Order by element %s is unsupported!", expression.getClass()));
        }
    }
}
