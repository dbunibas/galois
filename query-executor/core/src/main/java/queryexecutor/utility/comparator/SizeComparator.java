package queryexecutor.utility.comparator;

import java.util.Collection;
import java.util.Comparator;

public class SizeComparator implements Comparator<Collection> {

    public int compare(Collection o1, Collection o2) {
        return o1.size() - o2.size();
    }

}
