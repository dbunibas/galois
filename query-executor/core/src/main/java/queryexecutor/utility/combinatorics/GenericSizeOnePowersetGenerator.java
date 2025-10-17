package queryexecutor.utility.combinatorics;

import java.util.ArrayList;
import java.util.List;

public class GenericSizeOnePowersetGenerator<T> {

    public List<List<T>> generatePermutationsOfSizeOne(List<T> inputList) {
        if (inputList.isEmpty()) {
            throw new IllegalArgumentException("Unable to generate permutations from an empty list");
        }
        List<List<T>> result = new ArrayList<List<T>>();
        for (int i = 0; i < inputList.size(); i++) {
            result.add(generateListWithoutElement(inputList, i));
        }
        return result;
    }

    private List<T> generateListWithoutElement(List<T> inputList, int indexToSkip) {
        List<T> result = new ArrayList<T>();
        for (int i = 0; i < inputList.size(); i++) {
            if (i == indexToSkip) {
                continue;
            }
            result.add(inputList.get(i));
        }
        return result;
    }
}
