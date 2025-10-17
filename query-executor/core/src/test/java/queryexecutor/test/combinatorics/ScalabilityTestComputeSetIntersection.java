package queryexecutor.test.combinatorics;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import junit.framework.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import queryexecutor.utility.StopWatch;
import queryexecutor.utility.collection.ComputeSetIntersection;
import queryexecutor.utility.collection.ComputeSetIntersectionMultiple;

public class ScalabilityTestComputeSetIntersection extends TestCase {

    private final static Logger logger = LoggerFactory.getLogger(ScalabilityTestComputeSetIntersection.class);
    private ComputeSetIntersection intersector = new ComputeSetIntersection<Integer>();
    private ComputeSetIntersectionMultiple intersectorMultiple = new ComputeSetIntersectionMultiple<Integer>();

    private List<Set<Integer>> sets;

    @Override
    public void setUp() {
        sets = new ArrayList<Set<Integer>>();
        addSequence(sets, 0, 5000000);
        addSequence(sets, 100000, 2000000);
        addSequence(sets, 100000, 3000000);
        addSequence(sets, 100000, 11000000);
        addSequence(sets, 100000, 20000000);
        addSequence(sets, 100000, 30000000);
        addSequence(sets, 100000, 40000000);
        addSequence(sets, 100000, 50000000);
        if (logger.isDebugEnabled()) logger.debug(printSets(sets));
    }

//    @SuppressWarnings("unchecked")
//    public void testSequence() {
//        //ALG 1
//        StopWatch stopWatch = new StopWatch();
//        Set<Integer> result = intersector.computeIntersection(sets);
//        if (logger.isDebugEnabled()) logger.debug("Intersection size: " + result.size());
//        if (logger.isDebugEnabled()) logger.debug("ALG1 time: " + stopWatch.stop() + " ms");
//    }

    @SuppressWarnings("unchecked")
    public void testMultiple() {
        //ALG 2
        StopWatch stopWatch = new StopWatch();
        Set<Integer> resultM = intersectorMultiple.computeIntersection(sets);
        if (logger.isDebugEnabled()) logger.debug("IntersectionM size: " + resultM.size());
        if (logger.isDebugEnabled()) logger.debug("ALG2 time: " + stopWatch.stop() + " ms");
    }

    private void addSequence(List<Set<Integer>> sets, int start, int end) {
        Set<Integer> result = new HashSet<Integer>();
        for (int i = start; i < end; i++) {
            result.add(i);
        }
        sets.add(result);
    }

    private String printSets(List<Set<Integer>> sets) {
        StringBuilder sb = new StringBuilder();
        int totalSize = 0;
        int count = 1;
        sb.append("\n");
        for (Set<Integer> set : sets) {
            sb.append("- Set ").append(count).append(" size: ").append(set.size()).append("\n");
            totalSize += set.size();
            count++;
        }
        sb.append("* Total size: ").append(totalSize);
        return sb.toString();
    }

}
