package queryexecutor.utility.comparator;

import java.util.Comparator;
import queryexecutor.model.database.Tuple;

public class TupleComparatorWithoutOIDs implements Comparator<Tuple> {
    
    @Override
    public int compare(Tuple o1, Tuple o2) {
        return o1.toStringNoOID().compareTo(o2.toStringNoOID());
    }
    
}
