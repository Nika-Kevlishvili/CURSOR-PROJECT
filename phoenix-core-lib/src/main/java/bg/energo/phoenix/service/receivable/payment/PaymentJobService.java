package bg.energo.phoenix.service.receivable.payment;

import bg.energo.phoenix.model.entity.receivable.payment.Payment;
import bg.energo.phoenix.model.enums.receivable.DirectOffsettingSourceType;
import bg.energo.phoenix.model.enums.receivable.OperationContext;
import bg.energo.phoenix.repository.receivable.payment.PaymentRepository;
import bg.energo.phoenix.security.PermissionService;
import bg.energo.phoenix.service.receivable.customerReceivables.CustomerReceivableService;
import bg.energo.phoenix.service.receivable.customerReceivables.ReceivableDirectOffsettingService;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentJobService {

    private final PaymentRepository paymentRepository;
    private final ReceivableDirectOffsettingService receivableDirectOffsettingService;
    private final CustomerReceivableService customerReceivableService;
    private final PermissionService permissionService;
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);
    private final TransactionTemplate transactionTemplate;


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

    @SneakyThrows
    public void execute() {
        List<Payment> paymentsWithCurrentAmountMoreThanZero = paymentRepository.findPaymentsWithCurrentAmountMoreThanZero();
        List<List<Payment>> batchPayments = partitionList(paymentsWithCurrentAmountMoreThanZero, 1000);

        List<Callable<Void>> callables = new ArrayList<>();

        for (List<Payment> paymentList : batchPayments) {
            for (Payment payment : paymentList) {
                callables.add(() -> {
                    transactionTemplate.executeWithoutResult(x -> {
                        Long receivable = customerReceivableService.createFromPayment(payment, payment.getCurrentAmount(), true);
                        if (receivable != null) {
                            receivableDirectOffsettingService.directOffsetting(
                                    DirectOffsettingSourceType.PAYMENT,
                                    payment.getId(),
                                    receivable,
                                    permissionService.getLoggedInUserId() == null ? "system.admin" : permissionService.getLoggedInUserId(),
                                    permissionService.getLoggedInUserId() == null ? "system.admin" : permissionService.getLoggedInUserId(),
                                    OperationContext.APO.name()
                            );
                        }
                    });
                    return null;
                });
            }
        }
        executorService.invokeAll(callables);
    }

    private <T> List<List<T>> partitionList(List<T> list, int chunkSize) {
        List<List<T>> chunks = new ArrayList<>();
        for (int i = 0; i < list.size(); i += chunkSize) {
            chunks.add(list.subList(i, Math.min(i + chunkSize, list.size())));
        }
        return chunks;
    }
}
