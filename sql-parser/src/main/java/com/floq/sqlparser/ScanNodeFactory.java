package com.floq.sqlparser;

import engine.model.algebra.Scan;
import engine.model.database.AttributeRef;
import engine.model.database.TableAlias;

import java.util.List;

@FunctionalInterface
public interface ScanNodeFactory {
    Scan createScanNode(TableAlias tableAlias, List<AttributeRef> attributes);
}
