package com.galois.sqlparser;

import speedy.model.algebra.Scan;
import speedy.model.database.AttributeRef;
import speedy.model.database.TableAlias;

import java.util.List;

@FunctionalInterface
public interface ScanNodeFactory {
    Scan createScanNode(TableAlias tableAlias, List<AttributeRef> attributes);
}
