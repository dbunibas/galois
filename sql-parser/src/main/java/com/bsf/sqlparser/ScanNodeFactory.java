package com.bsf.sqlparser;

import queryexecutor.model.algebra.Scan;
import queryexecutor.model.database.AttributeRef;
import queryexecutor.model.database.TableAlias;

import java.util.List;

@FunctionalInterface
public interface ScanNodeFactory {
    Scan createScanNode(TableAlias tableAlias, List<AttributeRef> attributes);
}
