package queryexecutor.utility.comparator;

import java.util.Comparator;
import queryexecutor.model.database.IDatabase;
import queryexecutor.model.database.ITable;

public class TableComparatorBySizeAndName implements Comparator<String> {

    private IDatabase db;

    public TableComparatorBySizeAndName(IDatabase db) {
        this.db = db;
    }

    public int compare(String tn1, String tn2) {
        ITable t1 = db.getTable(tn1);
        ITable t2 = db.getTable(tn2);
        if(t1.getSize() == t2.getSize()){
            return tn1.compareTo(tn2);
        }
        Long l1 = t1.getSize();
        Long l2 = t2.getSize();
        return l1.compareTo(l2);
    }

}
