package engine.utility.comparator;

import java.util.Comparator;
import engine.model.database.Tuple;

public class TupleComparatorOIDs implements Comparator<Tuple> {

    public int compare(Tuple o1, Tuple o2) {
        Integer oid1 = Integer.parseInt(o1.getOid().toString());
        Integer oid2 = Integer.parseInt(o2.getOid().toString());
        return oid1.compareTo(oid2);
    }
}
