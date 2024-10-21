package engine.utility.combinatorics;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GenericListGeneratorIterator<T> {

    private final static Logger logger = LoggerFactory.getLogger(GenericListGeneratorIterator.class);

    private final List<List<T>> inputLists;
    private final int[] sizes;
    private final int[] currentIndexes;
    private final int numberOfLists;
    private boolean starting;

    public GenericListGeneratorIterator(List<List<T>> inputLists) {
        checkInputLists(inputLists);
        this.inputLists = inputLists;
        this.numberOfLists = inputLists.size();
        this.currentIndexes = new int[numberOfLists];
        this.sizes = new int[numberOfLists];
        for (int i = 0; i < sizes.length; i++) {
            sizes[i] = inputLists.get(i).size() - 1;
        }
        this.starting = true;
    }

    public boolean hasNext() {
        if (starting) {
            return true;
        }
        for (int currentIndex : currentIndexes) {
            if (currentIndex != 0) {
                return true;
            }
        }
        return false;
    }

    public List<T> next() {
        if (starting) {
            starting = false;
        }
        List<T> newList = generateListFromCurrentIndexes();
        rotateCurrentIndexes();
        return newList;
    }

    private void checkInputLists(List<List<T>> inputLists) {
        for (List<T> inputList : inputLists) {
            if (inputList.isEmpty()) {
                throw new IllegalArgumentException("Unable to combine elements of lists containing an empty list...");
            }
        }
    }

    private List<T> generateListFromCurrentIndexes() {
        List<T> result = new ArrayList<T>();
        for (int i = 0; i < numberOfLists; i++) {
            result.add(inputLists.get(i).get(currentIndexes[i]));
        }
        return result;
    }

    private void rotateCurrentIndexes() {
        for (int i = numberOfLists - 1; i >= 0; i--) {
            currentIndexes[i]++;
            if (currentIndexes[i] > sizes[i]) {
                currentIndexes[i] = 0;
            } else {
                break;
            }
        }
    }

    public long numberOfCombination() {
        long sum = 1;
        for (List<T> inputList : inputLists) {
            sum *= inputList.size();
        }
        return sum;
    }

}
