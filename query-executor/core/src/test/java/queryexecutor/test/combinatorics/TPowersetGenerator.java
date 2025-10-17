package queryexecutor.test.combinatorics;

import java.util.Arrays;
import java.util.List;
import junit.framework.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import queryexecutor.utility.combinatorics.GenericPowersetGenerator;
import queryexecutor.utility.combinatorics.GenericSizeOnePowersetGenerator;

public class TPowersetGenerator extends TestCase {

    private static Logger logger = LoggerFactory.getLogger(TPowersetGenerator.class);

    public TPowersetGenerator(String testName) {
        super(testName);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void test() {
        List<Integer> list = Arrays.asList(new Integer[]{1, 2, 3, 4, 5});
        GenericPowersetGenerator<Integer> generator = new GenericPowersetGenerator<Integer>(list);
        double pow = Math.pow(2, list.size());
        System.out.println("*** " + pow);
        if (logger.isInfoEnabled()) logger.info("Number of powersets: " + generator.numberOfPowersets());
        while (generator.hasNext()) {
            System.out.println(generator.next());
        }
    }

    public void testSinglePowerset() {
        List<Integer> list = Arrays.asList(new Integer[]{1, 2, 3, 4, 5});
        GenericSizeOnePowersetGenerator<Integer> generator = new GenericSizeOnePowersetGenerator<Integer>();
        double pow = Math.pow(2, list.size());
        System.out.println("*** " + pow);
        if (logger.isInfoEnabled()) logger.info("Number of powersets: " + generator.generatePermutationsOfSizeOne(list));
    }
}
