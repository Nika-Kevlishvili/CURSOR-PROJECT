package bg.energo.phoenix.service.contract.product.termination;

import bg.energo.phoenix.model.response.contract.productContract.terminations.ProductContractTerminationGenericModel;
import bg.energo.phoenix.model.response.contract.productContract.terminations.ProductContractTerminationProperties;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.ListUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@RequiredArgsConstructor
public class ProductContractTerminationManager {


    public void processContractsForTermination(ProductContractTerminator productContractTerminator) throws InterruptedException {
        ProductContractTerminationProperties properties = productContractTerminator.getProperties();
        ExecutorService executorService = Executors.newFixedThreadPool(properties.getNumberOfThreads());
        int batchSize = properties.getBatchSize();
        int queryBatchSize = properties.getQueryBatchSize();

        Set<Long> processedContractIds = new HashSet<>();
        List<Callable<Boolean>> callables = new ArrayList<>();
        int page=0;
        while (true) {
            List<ProductContractTerminationGenericModel> contractsToProcess = productContractTerminator.getContractData(queryBatchSize, page);
            page++;
            if (contractsToProcess.isEmpty()) {
                break;
            }

            List<List<ProductContractTerminationGenericModel>> partitions = ListUtils.partition(contractsToProcess, batchSize);

            for (List<ProductContractTerminationGenericModel> partition : partitions) {
                List<ProductContractTerminationGenericModel> filteredPartition = partition.stream().filter(x -> !processedContractIds.contains(x.getId())).toList();
                Callable<Boolean> callableTask = () -> {
                    for (ProductContractTerminationGenericModel curr : filteredPartition) {
                        productContractTerminator.terminate(curr);
                    }
                    return true;
                };
                callables.add(callableTask);
            }
            processedContractIds.addAll(
                    contractsToProcess
                            .stream()
                            .map(ProductContractTerminationGenericModel::getId)
                            .toList()
            );
        }

        executorService.invokeAll(callables);
    }

}
