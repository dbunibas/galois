package engine.utility.collection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import engine.utility.EngineUtility;
import engine.utility.comparator.SizeComparator;

public class ComputeSetIntersection<T> {

    private final static Logger logger = LoggerFactory.getLogger(ComputeSetIntersection.class);

    public Set<T> computeIntersection(List<Set<T>> setToIntersect) {
        if (setToIntersect == null || setToIntersect.isEmpty()) {
            throw new IllegalArgumentException("Unable to intersect an empty collection");
        }
        long start = new Date().getTime();
        List<Set<T>> orderedSets = new ArrayList<Set<T>>(setToIntersect);
        Collections.sort(orderedSets, new SizeComparator());
        if (logger.isDebugEnabled()) logger.debug("Computing intersection btw:\n" + EngineUtility.printCollection(orderedSets, "\t"));
        Set<T> result = new HashSet<T>(orderedSets.get(0));
        for (int i = 1; i < orderedSets.size(); i++) {
            if (result.isEmpty()) {
                break;
            }
            Set<T> nextSet = orderedSets.get(i);
            result.retainAll(nextSet);
        }
        long end = new Date().getTime();
        if (logger.isDebugEnabled()) logger.debug("Intersection computed in " + (end - start) + " ms");
        return result;
    }

}
