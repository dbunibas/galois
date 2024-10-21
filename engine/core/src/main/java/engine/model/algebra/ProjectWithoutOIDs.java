package engine.model.algebra;

import engine.EngineConstants;
import java.util.Iterator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import engine.model.database.Cell;
import engine.model.database.Tuple;

public class ProjectWithoutOIDs extends Project {

    private static Logger logger = LoggerFactory.getLogger(ProjectWithoutOIDs.class);

    private List<ProjectionAttribute> attributes;

    public ProjectWithoutOIDs(List<ProjectionAttribute> attributes) {
        super(attributes);
        this.attributes = attributes;
    }

    public String getName() {
        return "PROJECT-NO-OIDs-" + attributes;
    }

    @Override
    protected Tuple projectTuple(Tuple originalTuple) {
        Tuple tuple = originalTuple.clone();
        if (logger.isDebugEnabled()) logger.debug("Tuple before projection: " + tuple);
        List<Cell> cells = tuple.getCells();
        for (Iterator<Cell> it = cells.iterator(); it.hasNext();) {
            Cell cell = it.next();
            if (cell.getAttribute().equals(EngineConstants.OID)) {
                it.remove();
            } else if (!isToProject(cell.getAttributeRef(), this.attributes)) {
                it.remove();
            }
        }
        if (logger.isDebugEnabled()) logger.debug("Tuple after projection: " + tuple);
        sortTupleAttributes(tuple, attributes);
        return tuple;
    }
}
