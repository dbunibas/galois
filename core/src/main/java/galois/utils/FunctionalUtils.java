package galois.utils;

import lombok.extern.slf4j.Slf4j;

import java.util.function.Function;
import java.util.function.Supplier;

@Slf4j
public class FunctionalUtils {
    public static <T, E extends Exception> T orElseThrow(
            Executable<T, E> executable,
            Function<Exception, ? extends RuntimeException> exceptionSupplier
    ) {
        try {
            return executable.execute();
        } catch (Exception e) {
            throw exceptionSupplier.apply(e);
        }
    }

    @FunctionalInterface
    public interface Executable<T, E extends Throwable> {
        T execute() throws E;
    }
}
