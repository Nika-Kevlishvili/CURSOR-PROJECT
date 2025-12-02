package bg.energo.phoenix.service.xEnergie.jobs.service;

import bg.energo.phoenix.process.model.entity.Process;
import bg.energo.phoenix.service.xEnergie.jobs.enums.XEnergieJobType;
import bg.energo.phoenix.service.xEnergie.jobs.exception.ExceptionSupplier;
import bg.energo.phoenix.service.xEnergie.jobs.model.XEnergieJobProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
public abstract class AbstractXEnergieService {
    @Value("${xEnergie.batch-size}")
    protected Integer batchSize;

    @Value("${xEnergie.query-batch-size}")
    protected Integer queryBatchSize;

    @Value("${xEnergie.number-of-threads}")
    protected Integer numberOfThreads;

    protected final XEnergieSchedulerErrorHandler xEnergieSchedulerErrorHandler;

    protected abstract XEnergieJobType getJobType();

    protected XEnergieJobProperties getProperties() {
        return new XEnergieJobProperties(
                batchSize,
                queryBatchSize,
                numberOfThreads
        );
    }

    protected abstract AbstractXEnergieService getNextJobInChain();

    protected abstract void execute(Process process);

    protected void executeNextJobInChain(Process process) {
        log.debug("Executing next job in chain if required");
        AbstractXEnergieService nextJobInChain = getNextJobInChain();
        if (nextJobInChain != null) {
            log.debug("Next job in chain: [%s]".formatted(nextJobInChain.getJobType()));
            new Thread(() -> nextJobInChain.execute(process)).start();
            log.debug("Next job execution started");
        } else {
            log.debug("Next job in chain is null, finishing process");
        }
    }

    protected <E> Optional<E> handleException(Process process,
                                              ExceptionSupplier<E> exceptionThrow,
                                              String message) {
        try {
            E e = exceptionThrow.get();
            return Optional.of(e);
        } catch (Exception e) {
            handleException(process, message);
            return Optional.empty();
        }
    }

    protected void handleException(Process process,
                                   String message) {
        String exceptionMessage = "(%s) [%s]: %s".formatted(LocalDateTime.now(), getJobType().getJobExceptionMessageHeader(), message);
        log.error(exceptionMessage);
        xEnergieSchedulerErrorHandler.handleException(process, exceptionMessage);
    }

    protected void finishProcess(Process process) {
        xEnergieSchedulerErrorHandler.finishProcess(process);
    }
}
