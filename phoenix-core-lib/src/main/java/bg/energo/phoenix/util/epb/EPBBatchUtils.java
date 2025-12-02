package bg.energo.phoenix.util.epb;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
public class EPBBatchUtils {

    private static final int BATCH_PROCESSOR_DEFAULT_THREAD_SIZE = 10;
    private static final int AWAIT_TERMINATION_TIME_IN_MILLIS = 200;


    public static <T> void processItemsInBatches(List<T> items, BatchHandler<T> handler) {
        processItemsInBatches(items, BATCH_PROCESSOR_DEFAULT_THREAD_SIZE, handler);
    }

    /**
     * Processes the given list of items in batches, distributing the work across multiple threads.
     *
     * <p>This method partitions the input list into batches and processes each batch in parallel using
     * a fixed thread pool. The number of threads is determined by the provided {@code threadCount}.
     * Each batch is passed to the provided {@link BatchHandler#handleBatch(List)} method for processing.</p>
     *
     * @param <T> The type of items in the list.
     * @param items A list of items to be processed in batches. This must not be {@code null} or empty.
     * @param threadCount The number of threads to use for processing. Must be a positive integer.
     * @param handler A {@link BatchHandler} implementation that handles each batch of items.
     *
     * @throws IllegalArgumentException if {@code items} is {@code null} or empty.
     * @throws RuntimeException if an exception occurs during batch processing or thread execution.
     */
    public static <T> void processItemsInBatches(List<T> items, int threadCount, BatchHandler<T> handler) {
        if (CollectionUtils.isEmpty(items)) {
            throw new IllegalArgumentException("Items for processing in batch must not be null or empty.");
        }

        if (threadCount <= 0) {
            throw new IllegalArgumentException("Thread count must be greater than 0.");
        }

        log.debug("Starting batch processing. Total items: {}, thread count: {}", items.size(), threadCount);

        int batchSize = (items.size() + threadCount - 1) / threadCount;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);

        try {
            log.debug("Partitioning items into batches of size: {}", batchSize);
            List<List<T>> partitions = ListUtils.partition(items, batchSize);

            log.debug("Number of batches created: {}", partitions.size());

            List<Callable<Void>> callables = createCallables(handler, partitions);

            try {
                log.debug("Invoking all tasks to process batches...");
                executorService.invokeAll(callables);
            } catch (InterruptedException e) {
                log.error("Batch processing was interrupted.", e);
                throw new RuntimeException("Batch processing was interrupted.", e);
            }

        } finally {
            executorService.shutdown();
            try {
                log.debug("Shutting down executor service.");
                if (!executorService.awaitTermination(AWAIT_TERMINATION_TIME_IN_MILLIS, TimeUnit.MILLISECONDS)) {
                    log.warn("Executor service did not terminate in the expected time, forcing shutdown.");
                    executorService.shutdown();
                }
            } catch (InterruptedException e) {
                log.error("Interrupted while waiting for executor service shutdown.", e);
                executorService.shutdownNow();
            }
            log.debug("Batch processing completed.");
        }
    }

    /**
     * Processes a list of items in batches concurrently using multiple threads. The items are partitioned into smaller
     * batches of a specified size, and each batch is processed by a separate thread from a fixed thread pool.
     * The specified {@link BatchHandler} is used to process each batch.
     * If the list of items is empty or if invalid parameters are provided (such as zero or negative thread count or
     * batch size), an {@link IllegalArgumentException} is thrown.
     * The method submits each batch for processing asynchronously, waits for the threads to finish, and shuts down the
     * executor service gracefully. If the executor does not terminate within a specified time, it is forcibly shut down.
     *
     * @param <T> The type of items in the list to be processed.
     * @param items The list of items to be processed. It must not be null or empty.
     * @param threadCount The number of threads to use for processing the batches. Must be greater than 0.
     * @param maxBatchSize The maximum size of each batch. Must be greater than 0.
     * @param handler The {@link BatchHandler} used to process each batch of items. It should implement the logic for
     *                handling the items in each batch.
     * @throws IllegalArgumentException if {@code items} is null or empty, {@code threadCount} is less than or equal
     *                                  to 0, or {@code maxBatchSize} is less than or equal to 0.
     */
    public static <T> void submitItemsInBatches(List<T> items, int threadCount, int maxBatchSize, BatchHandler<T> handler) {
        if (CollectionUtils.isEmpty(items)) {
            throw new IllegalArgumentException("Items for processing in batch must not be null or empty.");
        }

        if (threadCount <= 0) {
            throw new IllegalArgumentException("Thread count must be greater than 0.");
        }

        if (maxBatchSize <= 0) {
            throw new IllegalArgumentException("maxBatchSize must be greater than 0.");
        }

        log.debug("Starting batch processing. Total items: {}, thread count: {}, max batch size: {}", items.size(), threadCount, maxBatchSize);

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);

        try {
            // Partition the items into batches of size maxBatchSize (or fewer for the last batch)
            log.debug("Partitioning items into batches of size: {}", maxBatchSize);
            List<List<T>> partitions = ListUtils.partition(items, maxBatchSize);

            log.debug("Number of batches created: {}", partitions.size());

            // Submit each partition to the executor service one by one
            for (List<T> partition : partitions) {
                Callable<Void> callable = createCallable(handler, partition);
                executorService.submit(callable);
            }

        } finally {
            executorService.shutdown();
            try {
                log.debug("Shutting down executor service.");
                if (!executorService.awaitTermination(AWAIT_TERMINATION_TIME_IN_MILLIS, TimeUnit.MILLISECONDS)) {
                    log.warn("Executor service did not terminate in the expected time, forcing shutdown.");
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                log.error("Interrupted while waiting for executor service shutdown.", e);
                executorService.shutdownNow();
            }
            log.debug("Batch processing completed.");
        }
    }

    /**
     * Creates a list of {@link Callable} objects, each responsible for processing a single batch of items using the provided
     * {@link BatchHandler}. Each {@link Callable} will execute the {@code handleBatch} method of the {@link BatchHandler}
     * for its corresponding batch and handle any exceptions that occur by logging the error.
     *
     * @param handler The {@link BatchHandler} that will be used to process each batch of items. It is responsible for
     *                implementing the logic to handle individual batches.
     * @param partitions A list of batches, where each batch is a {@link List} of items to be processed. These batches are
     *                   partitions of the original collection of items.
     * @param <T> The type of the items in each batch.
     * @return A list of {@link Callable} objects, each responsible for processing a batch of items.
     */
    private static <T> List<Callable<Void>> createCallables(BatchHandler<T> handler, List<List<T>> partitions) {
        List<Callable<Void>> callables = new ArrayList<>(partitions.size());
        for (List<T> partition : partitions) {
            callables.add(() -> {
                        try {
                            handler.handleBatch(partition);
                        } catch (Exception e) {
                            log.error("Error processing batch: {}", partition, e);
                        }
                        return null;
                    }
            );
        }
        return callables;
    }

    /**
     * Creates a {@link Callable} that processes a single batch of items using the provided {@link BatchHandler}.
     * The {@link Callable} will execute the {@code handleBatch} method of the {@link BatchHandler} and handle any
     * exceptions that occur during processing by logging the error.
     *
     * @param handler The {@link BatchHandler} used to process the batch of items. It should implement the logic for
     *                handling each batch of items.
     * @param partition The list of items that represents a batch to be processed by the handler.
     *                  This list is a partition of the original collection of items.
     * @param <T> The type of items in the batch.
     * @return A {@link Callable} that processes the given batch of items.
     */
    private static <T> Callable<Void> createCallable(BatchHandler<T> handler, List<T> partition) {
        return () -> {
            try {
                handler.handleBatch(partition);
            } catch (Exception e) {
                log.error("Error processing batch: {}", partition, e);
            }
            return null;
        };
    }

    /**
     * A functional interface for handling a batch of items.
     * This interface defines a single method for processing a collection of items in a batch.
     *
     * @param <T> the type of items to be handled
     */
    @FunctionalInterface
    public interface BatchHandler<T> {
        /**
         * Handles a batch of items.
         *
         * @param batch the batch of items to be handled
         */
        void handleBatch(List<T> batch);
    }
}
