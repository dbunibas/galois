package engine.utility.combinatorics;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GenericPowersetGenerator<T> {

    private final static Logger logger = LoggerFactory.getLogger(GenericPowersetGenerator.class);
    private List<T> list;
    private long totalPowersets;
    private long currentCombination;

    public GenericPowersetGenerator(List<T> list) {
        this.list = list;
        totalPowersets = (long) Math.pow(2, list.size());
        currentCombination = 0;
    }

    public boolean hasNext() {
        return currentCombination < totalPowersets;
    }

    public List<T> next() {
        List<T> result = new ArrayList<T>();
        String binary = Long.toBinaryString(currentCombination);
        binary = padding(binary, list.size());
        if (logger.isDebugEnabled()) logger.debug("Num: " + currentCombination + " - Binary: " + binary);
        for (int i = 0; i < binary.length(); i++) {
            if (binary.charAt(i) == '0') { //Using 0, we'll start from the powerset with all elements
                result.add(list.get(i));
            }
        }
        currentCombination++;
        return result;
    }

    private String padding(String binary, int size) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < (size - binary.length()); i++) {
            sb.append('0');
        }
        sb.append(binary);
        return sb.toString();
    }

    public long numberOfPowersets() {
        return totalPowersets;
    }
}
