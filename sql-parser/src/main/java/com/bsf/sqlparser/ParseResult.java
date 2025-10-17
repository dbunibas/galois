package com.bsf.sqlparser;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import queryexecutor.model.database.AttributeRef;

import java.util.List;

@Data
@RequiredArgsConstructor
@AllArgsConstructor
public class ParseResult<T> {
    private final T result;
    private List<AttributeRef> attributeRefs = List.of();
}
