package bsf.optimizer;

import java.util.List;
import net.sf.jsqlparser.expression.Expression;


public class QueryPlan {
    public static final String PUSHDOWN_ALL_CONDITION = "Pushdown-All";
    public static final String PUSHDOWN_SINGLE_CONDITION = "Pushdown-Single-";
    public static final String PUSHDOWN_UNOPTIMIZED = "Unoptimized";
    
    private List<Expression> expressionPushdown;
    private Double confidenceKeys;
    private int maxExpressionsInQuery;
    private List<Integer> indexesOptimize; // this is only for running with the current implementation

    public QueryPlan(List<Expression> expressionPushdown, Double confidenceKeys, int maxExpressionsInQuery, List<Integer> indexesOptimize) {
        this.expressionPushdown = expressionPushdown;
        this.confidenceKeys = confidenceKeys;
        this.maxExpressionsInQuery = maxExpressionsInQuery;
        this.indexesOptimize = indexesOptimize;
    }
    
    public String computePushdown() {
        if (maxExpressionsInQuery == 0) return PUSHDOWN_UNOPTIMIZED;
        if (expressionPushdown.size() == 1) {
            return PUSHDOWN_SINGLE_CONDITION + expressionPushdown.get(0).toString();
        }
        return PUSHDOWN_ALL_CONDITION;
    }

    public Double getConfidenceKeys() {
        return confidenceKeys;
    }
    
    public Integer getIndexPushDown() {
        if (expressionPushdown.size() == 1) {
            return indexesOptimize.get(0);
        }
        return null;
    }
     
    @Override
    public String toString() {
        return "Pushdown: " + computePushdown() + " Confidence: " + confidenceKeys;
    }
   
}
