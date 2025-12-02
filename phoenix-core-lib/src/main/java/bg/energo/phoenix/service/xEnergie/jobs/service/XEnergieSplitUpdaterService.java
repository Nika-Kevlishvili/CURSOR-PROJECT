package bg.energo.phoenix.service.xEnergie.jobs.service;

import bg.energo.phoenix.model.customAnotations.aspects.ExecutionTimeLogger;
import bg.energo.phoenix.model.entity.contract.billing.ContractPods;
import bg.energo.phoenix.model.response.communication.xEnergie.XEnergieSplitInfo;
import bg.energo.phoenix.process.model.entity.Process;
import bg.energo.phoenix.repository.contract.billing.ContractPodRepository;
import bg.energo.phoenix.repository.nomenclature.product.GridOperatorRepository;
import bg.energo.phoenix.service.xEnergie.XEnergieRepository;
import bg.energo.phoenix.service.xEnergie.jobs.enums.XEnergieJobType;
import bg.energo.phoenix.service.xEnergie.jobs.model.xEnergieSplitUpdate.UpdateJobModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

@Slf4j
@Service
@Profile({"dev","test"})
@ConditionalOnExpression("${app.cfg.schedulers.enabled:true}")
public class XEnergieSplitUpdaterService extends AbstractXEnergieService {
    public XEnergieSplitUpdaterService(XEnergieSchedulerErrorHandler xEnergieSchedulerErrorHandler,
                                       XEnergieRepository xEnergieRepository,
                                       ContractPodRepository contractPodRepository,
                                       GridOperatorRepository gridOperatorRepository,
                                       XEnergieDealDatesUpdaterService dealDatesUpdaterService) {
        super(xEnergieSchedulerErrorHandler);
        this.xEnergieRepository = xEnergieRepository;
        this.contractPodRepository = contractPodRepository;
        this.gridOperatorRepository = gridOperatorRepository;
        this.dealDatesUpdaterService = dealDatesUpdaterService;
    }

    private final XEnergieRepository xEnergieRepository;

    private final ContractPodRepository contractPodRepository;

    private final GridOperatorRepository gridOperatorRepository;

    private List<Long> energoProGridOperatorIds = new ArrayList<>();

    private final XEnergieDealDatesUpdaterService dealDatesUpdaterService;

    @Override
    protected XEnergieJobType getJobType() {
        return XEnergieJobType.X_ENERGIE_SPLIT_UPDATER;
    }

    @Override
    protected AbstractXEnergieService getNextJobInChain() {
        return dealDatesUpdaterService;
    }

    @Transactional
    @ExecutionTimeLogger
    public void execute(Process process) {
        try {
            log.debug("Fetching current Energo Pro grid operators");
            energoProGridOperatorIds = new ArrayList<>(gridOperatorRepository.fetchEnergoProGridOperators());
            log.debug("Energo Pro grid operators fetched, %s".formatted(Arrays.toString(energoProGridOperatorIds.toArray())));

            log.debug("Creating executor service with number of threads: [%s]".formatted(getProperties().numberOfThreads()));
            ExecutorService executorService = Executors.newFixedThreadPool(getProperties().numberOfThreads());
            Integer queryBatchSize = getProperties().queryBatchSize();

            List<Callable<Boolean>> callableQueue = new ArrayList<>();

            log.debug("Fetching count of non synchronized splits");
            Long nonSyncedSplitCount =
                    contractPodRepository.countAllNonSynchronizedProductContractPointOfDeliveriesWithSplitsFromXEnergie();
            log.debug("Non synchronized splits count is: [%s]".formatted(nonSyncedSplitCount));
            Long fetchedNonSynchronizedDataCount = 0L;
            int nonSynchronizedDataOffset = 0;

            while (nonSyncedSplitCount > fetchedNonSynchronizedDataCount) {
                log.debug("Fetching non synchronized product point of deliveries batch with properties: offset-[%s], size-[%s]".formatted(nonSynchronizedDataOffset, queryBatchSize));
                List<UpdateJobModel> firstJobContractPods =
                        contractPodRepository
                                .findAllNonSynchronizedProductContractPointOfDeliveriesWithSplitsFromXEnergie(
                                        PageRequest.of(nonSynchronizedDataOffset, queryBatchSize)
                                );
                log.debug("Non synchronized product point of deliveries batch with properties: offset-[%s], size-[%s], fetched, data count: [%s], adding to execution queue".formatted(nonSynchronizedDataOffset, queryBatchSize, firstJobContractPods.size()));

                for (UpdateJobModel firstJobContractPod : firstJobContractPods) {
                    callableQueue.add(() -> synchronizeProductContractPointOfDeliveryWithXEnergieSplit(process, firstJobContractPod));
                }
                log.debug("Non synchronized product point of deliveries batch execution added in queue successfully");

                fetchedNonSynchronizedDataCount += queryBatchSize;
                nonSynchronizedDataOffset++;
            }

            log.debug("Calculating query data ranges");
            LocalDateTime yesterdayStart = LocalDateTime.now().minusDays(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
            log.debug("Calculated query range start is: [%s]".formatted(yesterdayStart));
            LocalDateTime yesterdayEnd = yesterdayStart.plusDays(1).minusSeconds(1);
            log.debug("Calculated query range end is: [%s]".formatted(yesterdayEnd));

            log.debug("Fetching count of updated product contract point of deliveries count in range: [%s]-[%s]".formatted(yesterdayStart, yesterdayEnd));
            Long updatedProductContractPointOfDeliveriesCount = contractPodRepository
                    .countAllUpdatedProductContractPointOfDeliveriesThatSynchronizedInXEnergie(yesterdayStart, yesterdayEnd);
            log.debug("Updated product contract point of deliveries count is: [%s]".formatted(updatedProductContractPointOfDeliveriesCount));
            Long fetchedUpdatedDataCount = 0L;
            int updatedDataOffset = 0;

            while (updatedProductContractPointOfDeliveriesCount > fetchedUpdatedDataCount) {
                log.debug("Fetching updated product contract point of deliveries batch with properties: offset-[%s], size-[%s]".formatted(updatedDataOffset, queryBatchSize));
                List<UpdateJobModel> secondJobContractPods =
                        contractPodRepository
                                .findAllUpdatedProductContractPointOfDeliveriesThatSynchronizedInXEnergie(
                                        yesterdayStart,
                                        yesterdayEnd,
                                        PageRequest.of(updatedDataOffset, queryBatchSize));
                log.debug("Updated product contract point of deliveries batch with properties: offset-[%s], size-[%s], fetched, data count: [%s], adding to execution queue".formatted(updatedDataOffset, queryBatchSize, secondJobContractPods.size()));

                for (UpdateJobModel secondJobContractPod : secondJobContractPods) {
                    callableQueue.add(() -> updateXEnergieSplitByProductContractPointOfDelivery(process, secondJobContractPod));
                }
                log.debug("Updated product contract point of deliveries batch execution added in queue successfully");

                fetchedUpdatedDataCount += queryBatchSize;
                updatedDataOffset++;
            }

            log.debug("Invoking all queued runnable");
            executorService.invokeAll(callableQueue);
            log.debug("XEnergieSplitUpdaterService job execution finished");
        } catch (Exception e) {
            handleException(process, e.getMessage());
        } finally {
            executeNextJobInChain(process);
        }
    }

    private Boolean synchronizeProductContractPointOfDeliveryWithXEnergieSplit(Process process, UpdateJobModel contractPods) {
        log.debug("Synchronizing Point Of Delivery with identifier: [%s]".formatted(contractPods.getPodIdentifier()));
        if (energoProGridOperatorIds.contains(contractPods.getPodGridOperator())) {
            log.debug("Point Of Delivery with identifier: [%s] is owned by Energo PRO".formatted(contractPods.getPodIdentifier()));
            synchronizeForEnergoProPods(process, contractPods);
        } else {
            log.debug("Point Of Delivery with identifier: [%s] is not owned by Energo PRO, skipping process".formatted(contractPods.getPodIdentifier()));
        }
        return true;
    }

    private Boolean updateXEnergieSplitByProductContractPointOfDelivery(Process process, UpdateJobModel contractPods) {
        log.debug("Updating XEnergie split by Product Contract Point Of Delivery with identifier: [%s]".formatted(contractPods.getPodIdentifier()));
        if (energoProGridOperatorIds.contains(contractPods.getPodGridOperator())) {
            log.debug("Point Of Delivery with identifier: [%s] is owned by Energo Pro");
            updateEnergoProPointOfDelivery(process, contractPods);
        } else {
            log.debug("Point Of Delivery with identifier: [%s] is not owned by Energo Pro");
            synchronizeForNonEnergoProPods(process, contractPods);
        }
        return true;
    }

    private void synchronizeForNonEnergoProPods(Process process, UpdateJobModel contractPods) {
        try {
            log.debug("Trying to synchronize non Energo Pro Point Of Delivery with split id: [%s]".formatted(contractPods.getSplitId()));
            log.debug("Trying to fetch xEnergie split info for split with id: [%s]".formatted(contractPods.getSplitId()));
            Optional<XEnergieSplitInfo> splitInfoOptional = handleException(process, () -> xEnergieRepository.getSplitInfo(contractPods.getSplitId()), "xEnergie-Split not found with id: %s!;".formatted(contractPods.getSplitId()));
            if (splitInfoOptional.isEmpty()) {
                return;
            }
            XEnergieSplitInfo xEnergieSplitInfo = splitInfoOptional.get();
            log.debug("XEnergie split info was fetched successfully, [%s]".formatted(xEnergieSplitInfo));
            if (contractPods.getActivationDate() == null && contractPods.getDeactivationDate() == null) {
                log.debug("Activation and Deactivation was removed from Phoenix side, split deletion needed");
                handleException(process, () -> {
                    xEnergieRepository.updateSplitStartDate(xEnergieSplitInfo.getId(), contractPods.getActivationDate());
                    return Boolean.TRUE;
                }, "xEnergie-Split can not be deleted with id: %s!;".formatted(xEnergieSplitInfo.getId()));
                deleteSplitId(process, contractPods);
                return;
            }
            if (!Objects.equals(contractPods.getActivationDate(), xEnergieSplitInfo.getDateFrom())) {
                log.debug("Product Contract Point Of Delivery activation date was changed, trying to synchronize with xEnergie");
                handleException(process, () -> {
                    xEnergieRepository.updateSplitStartDate(xEnergieSplitInfo.getId(), contractPods.getActivationDate());
                    return Boolean.TRUE;
                }, "xEnergie-Split with id: %s Activation date can not be updated!;".formatted(xEnergieSplitInfo.getId()));
            }
            if (!Objects.equals(contractPods.getDeactivationDate(), xEnergieSplitInfo.getDateTo())) {
                log.debug("Product Contract Point Of Delivery deactivation date was changed, trying to synchronize with xEnergie");
                handleException(process, () -> {
                    xEnergieRepository.updateSplitEndDate(xEnergieSplitInfo.getId(), contractPods.getDeactivationDate());
                    return Boolean.TRUE;
                }, "xEnergie-Split with id: %s Deactivation date can not be updated!;".formatted(xEnergieSplitInfo.getId()));
            }
            if (!Objects.equals(contractPods.getDealNumber(), String.valueOf(xEnergieSplitInfo.getDealId()))) {
                log.debug("Deal id for Product Contract Point Of Delivery with id: [%s] was changed, trying to synchronize with xEnergie".formatted(contractPods.getContractPodId()));
                updateDealNumber(process, contractPods);
            }
        } catch (Exception e) {
            handleException(process, e.getMessage());
        }
    }

    private void updateDealNumber(Process process, UpdateJobModel contractPods) {
        handleException(process, () -> {
            xEnergieRepository.updateSplitDealNumber(contractPods.getSplitId(), Long.valueOf(contractPods.getDealNumber()));
            return Boolean.TRUE;
        }, "xEnergie-Split with id %s Deal number date can not be updated!;".formatted(contractPods.getSplitId()));
    }

    private void synchronizeForEnergoProPods(Process process, UpdateJobModel contractPods) {
        try {
            log.debug("Trying to delete empty splits before date: [%s] for Point Of Delivery with identifier: [%s]".formatted(contractPods.getActivationDate(), contractPods.getPodIdentifier()));
            handleException(process, () -> {
                xEnergieRepository.deleteEmptySplitsBeforeDate(contractPods.getPodIdentifier(), contractPods.getActivationDate());
                return Boolean.TRUE;
            }, "xEnergie-Empty splits before %s date can not be deleted for pod: %s;".formatted(contractPods.getActivationDate(), contractPods.getPodIdentifier()));

            log.debug("Trying to delete empty splits after date: [%s] for Point Of Delivery with identifier: [%s]".formatted(contractPods.getActivationDate(), contractPods.getPodIdentifier()));
            handleException(process, () -> {
                xEnergieRepository.deleteEmptySplitsAfterDate(contractPods.getPodIdentifier(), contractPods.getActivationDate());
                return Boolean.TRUE;
            }, "xEnergie-Empty splits after %s date can not be deleted for pod: %s;".formatted(contractPods.getActivationDate(), contractPods.getPodIdentifier()));
        } catch (Exception e) {
            handleException(process, e.getMessage());
        }
    }

    private void updateEnergoProPointOfDelivery(Process process, UpdateJobModel contractPods) {
        try {
            log.debug("Fetching split info with split id: [%s],from XEnergie".formatted(contractPods.getSplitId()));
            Optional<XEnergieSplitInfo> splitInfoOptional = handleException(process, () -> xEnergieRepository.getSplitInfo(contractPods.getSplitId()), "xEnergie-Split not found with id: %s!;".formatted(contractPods.getSplitId()));
            if (splitInfoOptional.isEmpty()) {
                log.debug("Cannot found valid split with id: [%s] in XEnergie".formatted(contractPods.getSplitId()));
                return;
            }
            log.debug("Split info with id: [%s], successfully fetched from XEnergie".formatted(contractPods.getSplitId()));
            XEnergieSplitInfo xEnergieSplitInfo = splitInfoOptional.get();
            log.debug("Split info: %s".formatted(xEnergieSplitInfo));

            log.debug("Trying to update split with id: [%s]".formatted(contractPods.getSplitId()));
            updateSpecificSplit(process, contractPods, xEnergieSplitInfo);
        } catch (Exception e) {
            handleException(process, e.getMessage());
        }
    }

    private void updateSpecificSplit(Process process, UpdateJobModel contractPods, XEnergieSplitInfo xEnergieSplitInfo) {
        log.debug("Checking changes properties for split with id: [%s]".formatted(contractPods.getSplitId()));
        if (contractPods.getActivationDate() == null && contractPods.getDeactivationDate() == null) {
            log.debug("Activation and Deactivation was removed from Phoenix side, split deletion needed");
            handleException(process, () -> {
                xEnergieRepository.deleteEmptySplitsBeforeDate(contractPods.getPodIdentifier(), xEnergieSplitInfo.getDateFrom());
                return Boolean.TRUE;
            }, "xEnergie-Empty splits before %s date can not be deleted for pod: %s;".formatted(xEnergieSplitInfo.getDateFrom(), contractPods.getPodIdentifier()));

            handleException(process, () -> {
                xEnergieRepository.deleteEmptySplitsAfterDate(contractPods.getPodIdentifier(), xEnergieSplitInfo.getDateFrom());
                return Boolean.TRUE;
            }, "xEnergie-Empty splits after %s date can not be deleted for pod: %s;".formatted(xEnergieSplitInfo.getDateFrom(), contractPods.getPodIdentifier()));
            handleException(process, () -> {
                xEnergieRepository.deleteSplit(xEnergieSplitInfo.getId());
                return Boolean.TRUE;
            }, "xEnergie-Split can not be deleted for pod: %s;".formatted(contractPods.getPodIdentifier()));
            log.debug("Deleting split id for Product Contract Point Of Delivery with id: [%s]".formatted(contractPods.getContractPodId()));
            deleteSplitId(process, contractPods);
            return;
        }

        if (!Objects.equals(contractPods.getActivationDate(), xEnergieSplitInfo.getDateFrom())) {
            log.debug("Product Contract Point Of Delivery activation date was changed, trying to synchronize with xEnergie");
            deleteEmptySplitsBeforeAndAfter(process, contractPods, xEnergieSplitInfo);
            handleException(process, () -> {
                xEnergieRepository.updateSplitStartDate(xEnergieSplitInfo.getId(), contractPods.getActivationDate());
                return Boolean.TRUE;
            }, "xEnergie-Split with id: %s Activation date can not be updated!;".formatted(xEnergieSplitInfo.getId()));
        }
        if (!Objects.equals(contractPods.getDeactivationDate(), xEnergieSplitInfo.getDateTo())) {
            log.debug("Product Contract Point Of Delivery deactivation date was changed, trying to synchronize with xEnergie");
            deleteEmptySplitsBeforeAndAfter(process, contractPods, xEnergieSplitInfo);
            handleException(process, () -> {
                xEnergieRepository.updateSplitEndDate(xEnergieSplitInfo.getId(), contractPods.getDeactivationDate());
                return Boolean.TRUE;
            }, "xEnergie-Split with id: %s Activation date can not be updated!;".formatted(xEnergieSplitInfo.getId()));
        }
        if (!Objects.equals(contractPods.getDealNumber(), String.valueOf(xEnergieSplitInfo.getDealId()))) {
            updateDealNumber(process, contractPods);
        }
    }

    /**
     * Deletes empty splits before and after a specified date for a given contract pod.
     *
     * @param process           the process associated with the delete operation
     * @param contractPods      the contract pods for which the empty splits will be deleted
     * @param xEnergieSplitInfo the XEnergieSplitInfo containing the date range for deletion
     */
    private void deleteEmptySplitsBeforeAndAfter(Process process, UpdateJobModel contractPods, XEnergieSplitInfo xEnergieSplitInfo) {
        handleException(process, () -> {
            xEnergieRepository.deleteEmptySplitsBeforeDate(contractPods.getPodIdentifier(), xEnergieSplitInfo.getDateFrom());
            return Boolean.TRUE;
        }, "xEnergie-Empty splits before %s date can not be deleted for pod: %s;".formatted(xEnergieSplitInfo.getDateFrom(), contractPods.getPodIdentifier()));
        handleException(process, () -> {
            xEnergieRepository.deleteEmptySplitsAfterDate(contractPods.getPodIdentifier(), xEnergieSplitInfo.getDateTo());
            return Boolean.TRUE;
        }, "xEnergie-Empty splits after %s date can not be deleted for pod: %s;".formatted(xEnergieSplitInfo.getDateTo(), contractPods.getPodIdentifier()));
    }

    private void deleteSplitId(Process process, UpdateJobModel contractPods) {
        log.debug("Fetching Product Contract Point Of Delivery with id: [%s]".formatted(contractPods.getContractPodId()));
        Optional<ContractPods> productContractPointOfDelivery = contractPodRepository.findById(contractPods.getContractPodId());
        if (productContractPointOfDelivery.isEmpty()) {
            handleException(process, "contract pod with id %s do not exist!;".formatted(contractPods.getContractPodId()));
            return;
        }
        log.debug("Product Contract Point Of Delivery was fetched successfully");
        ContractPods contractPodToSave = productContractPointOfDelivery.get();
        log.debug("Product Contract Point Of Delivery: [%s]".formatted(contractPodToSave));
        contractPodToSave.setSplitId(null);
        log.debug("Old split id was: [%s], clearing splitId".formatted(contractPodToSave.getId()));
        contractPodRepository.save(contractPodToSave);
    }

    public List<UpdateJobModel> fetchNonSynchronizedData() {
        List<UpdateJobModel> firstJobContractPods =
                contractPodRepository.findAllNonSynchronizedProductContractPointOfDeliveriesWithSplitsFromXEnergie(PageRequest.of(0, Integer.MAX_VALUE));

        LocalDateTime yesterdayStart = LocalDateTime.now().minusDays(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime yesterdayEnd = yesterdayStart.plusDays(1).minusSeconds(1);

        List<UpdateJobModel> secondJobContractPods =
                contractPodRepository.findAllUpdatedProductContractPointOfDeliveriesThatSynchronizedInXEnergie(yesterdayStart, yesterdayEnd, PageRequest.of(0, Integer.MAX_VALUE));
        return Stream.of(firstJobContractPods,
                        secondJobContractPods
                )
                .flatMap(Collection::stream)
                .toList();
    }
}
