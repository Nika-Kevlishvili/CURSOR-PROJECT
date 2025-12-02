package bg.energo.phoenix.service.xEnergie.jobs.exception;

@FunctionalInterface
public interface ExceptionSupplier<T> {
    T get() throws Exception;
}
