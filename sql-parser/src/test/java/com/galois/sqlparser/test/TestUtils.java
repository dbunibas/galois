package com.galois.sqlparser.test;

import speedy.model.algebra.operators.ITupleIterator;
import speedy.model.database.Tuple;

import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class TestUtils {
    public static Stream<Tuple> toTupleStream(ITupleIterator iterator) {
        Iterable<Tuple> iterable = () -> iterator;
        return StreamSupport.stream(iterable.spliterator(), false);
    }

    public static List<Tuple> toTupleList(ITupleIterator iterator) {
        return toTupleStream(iterator).toList();
    }
}
