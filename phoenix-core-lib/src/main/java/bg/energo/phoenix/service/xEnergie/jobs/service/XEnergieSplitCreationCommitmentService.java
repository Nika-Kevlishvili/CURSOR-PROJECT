package bg.energo.phoenix.service.xEnergie.jobs.service;

import bg.energo.phoenix.model.customAnotations.aspects.ExecutionTimeLogger;
import bg.energo.phoenix.process.model.entity.Process;
import bg.energo.phoenix.repository.contract.billing.ContractPodRepository;
import bg.energo.phoenix.service.xEnergie.XEnergieRepository;
import bg.energo.phoenix.service.xEnergie.jobs.enums.XEnergieJobType;
import bg.energo.phoenix.service.xEnergie.jobs.model.xEnergieSplitCreationCommitment.SplitCreationCommitmentModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
@Profile({"dev","test"})
@DependsOn({"XEnergieSchedulerErrorHandler"})
@ConditionalOnExpression("${app.cfg.schedulers.enabled:true}")
public class XEnergieSplitCreationCommitmentService extends AbstractXEnergieService {
    private final ContractPodRepository contractPodRepository;
    private final XEnergieRepository xEnergieRepository;

    public XEnergieSplitCreationCommitmentService(XEnergieSchedulerErrorHandler xEnergieSchedulerErrorHandler,
                                                  ContractPodRepository contractPodRepository,
                                                  XEnergieRepository xEnergieRepository) {
        super(xEnergieSchedulerErrorHandler);
        this.contractPodRepository = contractPodRepository;
        this.xEnergieRepository = xEnergieRepository;
    }

    @Override
    protected XEnergieJobType getJobType() {
        return XEnergieJobType.X_ENERGIE_SPLIT_CREATION_COMMITMENT;
    }

    @Override
    protected AbstractXEnergieService getNextJobInChain() {
        return null;
    }

    @Override
    @Transactional
    @ExecutionTimeLogger
    public void execute(Process process) {
        LocalDate now = LocalDate.now();
        LocalDateTime lastYearStart = now.minusYears(1).atStartOfDay();
        try {
            ExecutorService executorService = Executors.newFixedThreadPool(getProperties().numberOfThreads());
            List<Callable<Boolean>> callableQueue = new ArrayList<>();

            Long nonSynchronizedContractPodCount = contractPodRepository.countAllNonSynchronizedContractPods(lastYearStart);

            Long fetchedContractPodsCount = 0L;
            int offset = 0;
            Integer queryBatchSize = getProperties().queryBatchSize();

            while (nonSynchronizedContractPodCount > fetchedContractPodsCount) {
                List<SplitCreationCommitmentModel> nonSynchronizedContractPods =
                        contractPodRepository
                                .findAllNonSynchronizedContractPods(lastYearStart, PageRequest.of(offset, queryBatchSize));

                callableQueue.add(() -> processBatch(process, nonSynchronizedContractPods));

                fetchedContractPodsCount += queryBatchSize;
                offset++;
            }

            executorService.invokeAll(callableQueue);
        } catch (Exception e) {
            handleException(process, "Unexpected exception handled while trying to commit");
        } finally {
            finishProcess(process);
        }
    }

    private Boolean processBatch(Process process, List<SplitCreationCommitmentModel> batch) {
        for (SplitCreationCommitmentModel model : batch) {
            try {
                String identifier = model.getIdentifier();
                Optional<Long> splitExistanceOptional = handleException(process, () -> xEnergieRepository.getSplitIdByDealNumberIdentifierAndActivationDates(
                        Long.valueOf(model.getDealNumber()),
                        identifier,
                        model.getActivationDate(),
                        Objects.requireNonNullElse(model.getDeactivationDate(), LocalDate.of(2030, Month.DECEMBER, 31))
                ), "Exception handled while trying to fetch split existence from xEnergie database, split not found for point of delivery with: Deal[%s], Identifier[%s],Activations[%s-%s]".formatted(model.getDealNumber(), identifier, model.getActivationDate(), model.getDeactivationDate()));

                if (splitExistanceOptional.isEmpty()) {
                    continue;
                }

                Long splitId = splitExistanceOptional.get();
                contractPodRepository.updateSplitId(model.getId(), splitId);
            } catch (Exception e) {
                handleException(process, e.getMessage());
            }
        }
        return true;
    }
}
