package bg.energo.phoenix.service.contract.product.termination.serviceContract;

import bg.energo.phoenix.model.response.contract.productContract.terminations.serviceContract.ServiceContractTerminationGenericModel;
import bg.energo.phoenix.model.response.contract.productContract.terminations.serviceContract.ServiceContractTerminationProperties;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.ListUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@RequiredArgsConstructor
public class ServiceContractTerminationManager {
    @Async
    public void processServiceContractsForTermination(ServiceContractTerminator serviceContractTerminator) throws InterruptedException {
        ServiceContractTerminationProperties properties = serviceContractTerminator.getProperties();
        ExecutorService executorService = Executors.newFixedThreadPool(properties.getNumberOfThreads());
        int queryBatchSize = properties.getQueryBatchSize();
        int batchSize = properties.getBatchSize();

        List<Long> processedContractIds = new ArrayList<>();
        List<Callable<Boolean>> callables = new ArrayList<>();
        
        while (true) {
            List<ServiceContractTerminationGenericModel> contractsToProcess = serviceContractTerminator.getContractData(queryBatchSize, processedContractIds);

            if (contractsToProcess.isEmpty()) {
                break;
            }

            processedContractIds.addAll(
                    contractsToProcess
                            .stream()
                            .map(ServiceContractTerminationGenericModel::getId)
                            .toList()
            );

            List<List<ServiceContractTerminationGenericModel>> partitions = ListUtils.partition(contractsToProcess, batchSize);

            for (List<ServiceContractTerminationGenericModel> partition : partitions) {
                Callable<Boolean> callableTask = () -> {
                    for (ServiceContractTerminationGenericModel curr : partition) {
                        serviceContractTerminator.terminate(curr);
                    }
                    return true;
                };
                callables.add(callableTask);
            }
        }

        executorService.invokeAll(callables);
    }
}
