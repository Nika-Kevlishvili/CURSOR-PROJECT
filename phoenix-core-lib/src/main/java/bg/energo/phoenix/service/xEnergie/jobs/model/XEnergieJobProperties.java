package bg.energo.phoenix.service.xEnergie.jobs.model;

public record XEnergieJobProperties(
        Integer batchSize,
        Integer queryBatchSize,
        Integer numberOfThreads
) {
}
