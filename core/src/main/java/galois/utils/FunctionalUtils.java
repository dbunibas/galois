package galois.utils;

import lombok.extern.slf4j.Slf4j;

import java.util.function.Function;

@Slf4j
public class FunctionalUtils {
    public static <T> T orElse(T value, T defaultValue) {
        return value != null ? value : defaultValue;
    }

    public static <T, E extends Exception> T orElseThrow(
            Executable<T, E> executable,
            Function<Exception, ? extends RuntimeException> exceptionSupplier
    ) {
        try {
            return executable.execute();
        } catch (Exception e) {
            log.warn("Exception during process", e);
            throw exceptionSupplier.apply(e);
        }
    }

    @FunctionalInterface
    public interface Executable<T, E extends Throwable> {
        T execute() throws E;
    }
}
