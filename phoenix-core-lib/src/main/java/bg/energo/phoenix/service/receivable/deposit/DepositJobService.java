package bg.energo.phoenix.service.receivable.deposit;

import bg.energo.phoenix.model.entity.receivable.customerLiability.CustomerLiability;
import bg.energo.phoenix.model.entity.receivable.deposit.Deposit;
import bg.energo.phoenix.repository.receivable.customerLiability.CustomerLiabilityRepository;
import bg.energo.phoenix.repository.receivable.deposit.DepositRepository;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
// todo change to cron after testing
public class DepositJobService {
    private final DepositRepository depositRepository;
    private final CustomerLiabilityRepository customerLiabilityRepository;
    private final TransactionTemplate template;

    private final ExecutorService executorService = Executors.newFixedThreadPool(10);

    @PreDestroy
    public void cleanup() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public void execute() {
        List<Object[]> customerLiabilities = depositRepository.liabilitiesForDepositJob();
        List<List<Object[]>> batches = partitionList(customerLiabilities, 1000);

        List<CompletableFuture<Void>> futures = batches.stream()
                .map(batch -> CompletableFuture.runAsync(
                        () -> processInTransaction(batch),
                        executorService
                ))
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }

    private void processInTransaction(List<Object[]> batch) {
        template.execute(status -> {
            List<Deposit> depositsToUpdate = new ArrayList<>();
            List<CustomerLiability> liabilitiesToUpdate = new ArrayList<>();

            batch.forEach(liability -> {
                Deposit deposit = (Deposit) liability[1];
                CustomerLiability customerLiability = (CustomerLiability) liability[0];

                deposit.setCurrentAmount(deposit.getCurrentAmount()
                        .add(customerLiability.getInitialAmount()));
                depositsToUpdate.add(deposit);

                customerLiability.setAddedToDeposit(true);
                liabilitiesToUpdate.add(customerLiability);
            });

            saveInBatch(depositsToUpdate, liabilitiesToUpdate);
            return null;
        });
    }

    private <T> List<List<T>> partitionList(List<T> list, int chunkSize) {
        List<List<T>> chunks = new ArrayList<>();
        for (int i = 0; i < list.size(); i += chunkSize) {
            chunks.add(list.subList(i, Math.min(i + chunkSize, list.size())));
        }
        return chunks;
    }

    private void saveInBatch(List<Deposit> deposits, List<CustomerLiability> liabilities) {
        depositRepository.saveAll(deposits);
        customerLiabilityRepository.saveAll(liabilities);
    }


}
