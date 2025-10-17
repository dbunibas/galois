package queryexecutor.test.combinatorics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import junit.framework.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import queryexecutor.utility.collection.ComputeSetIntersection;

public class TestComputeSetIntersection extends TestCase {

    private final static Logger logger = LoggerFactory.getLogger(TestComputeSetIntersection.class);
    private ComputeSetIntersection intersector = new ComputeSetIntersection<Integer>();

    @SuppressWarnings("unchecked")
    public void testOne() {
        List<Set<Integer>> sets = new ArrayList<Set<Integer>>();
        sets.add(new HashSet<Integer>(Arrays.asList(new Integer[]{0, 1, 2})));
        sets.add(new HashSet<Integer>(Arrays.asList(new Integer[]{0, 3, 4})));
        sets.add(new HashSet<Integer>(Arrays.asList(new Integer[]{0, 5, 6})));
        sets.add(new HashSet<Integer>(Arrays.asList(new Integer[]{0, 7, 8})));
        Set<Integer> result = intersector.computeIntersection(sets);
        String resultString = toStringResult(result);
        if (logger.isDebugEnabled()) logger.debug("Result: " + resultString);
        assertEquals("[0]", resultString);
    }

    @SuppressWarnings("unchecked")
    public void testTwo() {
        List<Set<Integer>> sets = new ArrayList<Set<Integer>>();
        sets.add(new HashSet<Integer>(Arrays.asList(new Integer[]{0, 1, 2})));
        sets.add(new HashSet<Integer>(Arrays.asList(new Integer[]{0, 3, 4})));
        sets.add(new HashSet<Integer>(Arrays.asList(new Integer[]{0, 5, 6})));
        sets.add(new HashSet<Integer>(Arrays.asList(new Integer[]{7, 8})));
        Set<Integer> result = intersector.computeIntersection(sets);
        String resultString = toStringResult(result);
        if (logger.isDebugEnabled()) logger.debug("Result: " + resultString);
        assertEquals("[]", resultString);
    }

    @SuppressWarnings("unchecked")
    public void testThree() {
        List<Set<Integer>> sets = new ArrayList<Set<Integer>>();
        sets.add(new HashSet<Integer>(Arrays.asList(new Integer[]{0, 1, 2})));
        sets.add(new HashSet<Integer>(Arrays.asList(new Integer[]{0, 1, 2})));
        Set<Integer> result = intersector.computeIntersection(sets);
        String resultString = toStringResult(result);
        if (logger.isDebugEnabled()) logger.debug("Result: " + resultString);
        assertEquals("[0, 1, 2]", resultString);
    }

    private String toStringResult(Set<Integer> result) {
        List<Integer> orderedSet = new ArrayList<Integer>(result);
        Collections.sort(orderedSet);
        return orderedSet.toString();
    }

}
