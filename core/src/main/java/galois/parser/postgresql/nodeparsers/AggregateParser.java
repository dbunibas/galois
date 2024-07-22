package galois.parser.postgresql.nodeparsers;

import galois.llm.algebra.LLMScan;
import galois.llm.algebra.config.OperatorsConfiguration;
import galois.parser.postgresql.NodeParserFactory;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import lombok.extern.slf4j.Slf4j;
import org.jdom2.Element;
import speedy.model.algebra.IAlgebraOperator;
import speedy.model.algebra.Project;
import speedy.model.algebra.ProjectionAttribute;
import speedy.model.algebra.Select;
import speedy.model.algebra.aggregatefunctions.AvgAggregateFunction;
import speedy.model.algebra.aggregatefunctions.CountAggregateFunction;
import speedy.model.algebra.aggregatefunctions.IAggregateFunction;
import speedy.model.algebra.aggregatefunctions.MaxAggregateFunction;
import speedy.model.algebra.aggregatefunctions.MinAggregateFunction;
import speedy.model.algebra.aggregatefunctions.StdDevAggregateFunction;
import speedy.model.algebra.aggregatefunctions.SumAggregateFunction;
import speedy.model.algebra.aggregatefunctions.ValueAggregateFunction;
import speedy.model.database.AttributeRef;
import speedy.model.database.IDatabase;
import speedy.model.database.Key;
import speedy.model.database.TableAlias;
import speedy.model.expressions.Expression;

@Slf4j
public class AggregateParser extends AbstractNodeParser {

    public static final String COUNT = "count";
    public static final String AVG = "avg";
    public static final String MIN = "min";
    public static final String MAX = "max";
    public static final String STDDEV = "stddev";
    public static final String SUM = "stddev";

    public static final String[] functions = {COUNT, AVG, MIN, MAX, STDDEV, SUM};

    @Override
    public IAlgebraOperator parse(Element node, IDatabase database, OperatorsConfiguration configuration) {
        log.debug("Aggregate Parser!");
        IAlgebraOperator subTree = parseSubtree(node, database, configuration);
        log.debug("Subtree: " + subTree.getName());
        // TODO: Handle more than one key / handle order
        Element output = node.getChild("Output", node.getNamespace());
        List<ProjectionAttribute> projectionAttributes = new ArrayList<>();
        for (Element item : output.getChildren()) {
            IAggregateFunction aggregate = getAggregate(item);
            if (aggregate != null) {
                log.debug("Add aggregate: " + aggregate.getName() + " on " + aggregate.getAttributeRef());
                projectionAttributes.add(new ProjectionAttribute(aggregate));
            } else {
                String attributeName = item.getValue().trim();
                ValueAggregateFunction vaf = new ValueAggregateFunction(new AttributeRef(getTableAlias(), attributeName));
                projectionAttributes.add(new ProjectionAttribute(vaf));
            }
        }
        Project project = new Project(projectionAttributes);
        project.addChild(subTree);
        List<AttributeRef> attributesForCleaning = cleanAttributesLLMScan(project);
        addKeys(database, attributesForCleaning);
        log.debug("Attributes: " + attributesForCleaning);
        updateLLMScan(project, attributesForCleaning);
//        Limit limit1 = new Limit(1);
//        limit1.addChild(project);

        return project;
    }

    private void addKeys(IDatabase database, List<AttributeRef> attributesForCleaning) {
        List<Key> primaryKeys = database.getPrimaryKeys();
        if (primaryKeys.size() == 1) {
            Key key = primaryKeys.get(0);
            List<AttributeRef> attributesKey = key.getAttributes();
            for (AttributeRef aKey : attributesKey) {
                AttributeRef aRef = new AttributeRef(super.getTableAlias(), aKey.getName());
                if (!attributesForCleaning.contains(aRef)) {
                    attributesForCleaning.add(aRef);
                }
            }
        }
    }

    private IAlgebraOperator parseSubtree(Element node, IDatabase database, OperatorsConfiguration configuration) {
        Element plans = node.getChild("Plans", node.getNamespace());
        // TODO: Does the plans node have always one and only one plan child?
        Element subPlan = plans.getChild("Plan", plans.getNamespace());
        INodeParser parser = NodeParserFactory.getParserForNode(subPlan);

        IAlgebraOperator subTree = parser.parse(subPlan, database, configuration);
        TableAlias tableAlias = parser.getTableAlias();
        setTableAlias(tableAlias);

        return subTree;
    }

    private IAggregateFunction getAggregate(Element item) {
        String text = item.getTextTrim();
        if (!text.contains("(") && !text.contains(")")) return null;
        String attributeName = text.substring(text.indexOf("(") + 1, text.indexOf(")")).trim();
        String functionName = text.substring(0, text.indexOf("(")).trim();
        log.debug("Function: " + functionName + " over " + attributeName);
        AttributeRef attributeRef = new AttributeRef(getTableAlias(), attributeName);
        if (functionName.equalsIgnoreCase(AVG)) {
            return new AvgAggregateFunction(attributeRef);
        }
        if (functionName.equalsIgnoreCase(COUNT)) {
            return new CountAggregateFunction(attributeRef);
        }
        if (functionName.equalsIgnoreCase(MIN)) {
            return new MinAggregateFunction(attributeRef);
        }
        if (functionName.equalsIgnoreCase(MAX)) {
            return new MaxAggregateFunction(attributeRef);
        }
        if (functionName.equalsIgnoreCase(STDDEV)) {
            return new StdDevAggregateFunction(attributeRef);
        }
        if (functionName.equalsIgnoreCase(SUM)) {
            return new SumAggregateFunction(attributeRef);
        }
        log.error("Unknown aggregate function: " + functionName);
        return null;
    }

    private List<AttributeRef> cleanAttributesLLMScan(IAlgebraOperator operator) {
        Set<AttributeRef> attributes = new HashSet<>();
        if (operator == null) {
            return new ArrayList<>();
        }
        if (operator instanceof Project) {
            List<IAggregateFunction> aggregateFunctions = ((Project) operator).getAggregateFunctions();
            log.debug("Aggregate Functions: " + aggregateFunctions.size());
            for (IAggregateFunction aggregateFunction : aggregateFunctions) {
                log.debug("Aggregate: " + aggregateFunction);
                if (aggregateFunction != null) attributes.add(aggregateFunction.getAttributeRef());
            }
        }
        if (operator instanceof Select) {
            Select s = (Select) operator;
            List<Expression> selections = s.getSelections();
            for (Expression selection : selections) {
                List<String> variables = selection.getVariables();
                for (String variable : variables) {
                    StringTokenizer tokenizer = new StringTokenizer(variable, ".");
                    tokenizer.nextToken();
                    String attributeName = tokenizer.nextToken();
                    AttributeRef aRef = new AttributeRef(super.getTableAlias(), attributeName);
                    if (!containsAttributeByName(attributes, aRef)) {
                        attributes.add(aRef);
                    }
                }
            }
        }
        for (IAlgebraOperator children : operator.getChildren()) {
            attributes.addAll(cleanAttributesLLMScan(children));
        }
        return new ArrayList<>(attributes);
    }

    private void updateLLMScan(IAlgebraOperator operator, List<AttributeRef> attributesLLMScan) {
        if (operator == null) {
            return;
        }
        if (operator instanceof LLMScan) {
            LLMScan llmScan = (LLMScan) operator;
            llmScan.setAttributesSelect(attributesLLMScan);
            return;
        }
        for (IAlgebraOperator children : operator.getChildren()) {
            updateLLMScan(children, attributesLLMScan);
        }

    }

}
