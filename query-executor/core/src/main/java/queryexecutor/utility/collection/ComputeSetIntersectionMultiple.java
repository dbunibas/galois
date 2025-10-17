package queryexecutor.utility.collection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import queryexecutor.utility.QueryExecutorUtility;
import queryexecutor.utility.StopWatch;
import queryexecutor.utility.comparator.SizeComparator;

public class ComputeSetIntersectionMultiple<T> {

    private final static Logger logger = LoggerFactory.getLogger(ComputeSetIntersectionMultiple.class);

    public Set<T> computeIntersection(List<Set<T>> setToIntersect) {
        if (setToIntersect == null || setToIntersect.isEmpty()) {
            throw new IllegalArgumentException("Unable to intersect an empty collection");
        }
        StopWatch stopWatch = new StopWatch();
        List<Set<T>> orderedSets = new ArrayList<Set<T>>(setToIntersect);
        Collections.sort(orderedSets, new SizeComparator());
        if (logger.isTraceEnabled()) logger.trace("Computing intersection btw:\n" + QueryExecutorUtility.printCollection(orderedSets, "\t"));
        Set<T> result = new HashSet<T>();
        Set<T> firstSet = orderedSets.get(0);
        logger.error("First element size: " + firstSet.size());
        for (T element : firstSet) {
            boolean toAdd = true;
            for (int i = 1; i < orderedSets.size(); i++) {
                if (!orderedSets.get(i).contains(element)) {
                    toAdd = false;
                    break;
                }
            }
            if (toAdd) {
                result.add(element);
            }
        }
        if (logger.isDebugEnabled()) logger.debug("Intersection computed in " + stopWatch.stop() + " ms");
        return result;
    }

}
