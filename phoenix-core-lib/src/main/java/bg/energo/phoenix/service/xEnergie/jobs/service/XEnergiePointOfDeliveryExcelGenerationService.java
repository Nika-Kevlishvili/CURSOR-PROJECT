package bg.energo.phoenix.service.xEnergie.jobs.service;

import bg.energo.phoenix.model.customAnotations.aspects.ExecutionTimeLogger;
import bg.energo.phoenix.model.process.ProductContractPodExportData;
import bg.energo.phoenix.model.response.communication.xEnergie.AdditionalInformationForPointOfDeliveries;
import bg.energo.phoenix.model.response.communication.xEnergie.XEnergieEmptySpaceInfo;
import bg.energo.phoenix.process.model.entity.Process;
import bg.energo.phoenix.repository.contract.billing.ContractPodRepository;
import bg.energo.phoenix.repository.pod.pod.PointOfDeliveryDetailsRepository;
import bg.energo.phoenix.service.contract.product.ProductContractPodExportService;
import bg.energo.phoenix.service.xEnergie.XEnergieRepository;
import bg.energo.phoenix.service.xEnergie.jobs.enums.XEnergieJobType;
import bg.energo.phoenix.service.xEnergie.jobs.model.XEnergieJobProperties;
import bg.energo.phoenix.service.xEnergie.jobs.model.xEnergieExcelGeneration.ExcelGenerationDataModel;
import bg.energo.phoenix.service.xEnergie.jobs.model.xEnergieExcelGeneration.ExcelGenerationFetchDataModel;
import bg.energo.phoenix.service.xEnergie.jobs.model.xEnergieExcelGeneration.ExcelGenerationLatestPointOfDeliveryDetailsDataModel;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
@Profile({"dev","test"})
@DependsOn({"XEnergieSchedulerErrorHandler"})
@ConditionalOnExpression("${app.cfg.schedulers.enabled:true}")
public class XEnergiePointOfDeliveryExcelGenerationService extends AbstractXEnergieService {
    public XEnergiePointOfDeliveryExcelGenerationService(XEnergieSchedulerErrorHandler xEnergieSchedulerErrorHandler,
                                                         XEnergieRepository xEnergieRepository,
                                                         ContractPodRepository contractPodRepository,
                                                         ProductContractPodExportService productContractPodExportService,
                                                         PointOfDeliveryDetailsRepository pointOfDeliveryDetailsRepository,
                                                         XEnergieSplitCreationCommitmentService xEnergieSplitCreationCommitmentService) {
        super(xEnergieSchedulerErrorHandler);
        this.xEnergieRepository = xEnergieRepository;
        this.contractPodRepository = contractPodRepository;
        this.productContractPodExportService = productContractPodExportService;
        this.pointOfDeliveryDetailsRepository = pointOfDeliveryDetailsRepository;
        this.xEnergieSplitCreationCommitmentService = xEnergieSplitCreationCommitmentService;
    }

    private final XEnergieRepository xEnergieRepository;
    private final ContractPodRepository contractPodRepository;
    private final ProductContractPodExportService productContractPodExportService;
    private final PointOfDeliveryDetailsRepository pointOfDeliveryDetailsRepository;
    private final XEnergieSplitCreationCommitmentService xEnergieSplitCreationCommitmentService;

    @Override
    protected XEnergieJobType getJobType() {
        return XEnergieJobType.X_ENERGIE_POINT_OF_DELIVERY_EXCEL_GENERATION;
    }

    @Override
    protected AbstractXEnergieService getNextJobInChain() {
        return xEnergieSplitCreationCommitmentService;
    }

    @Override
    protected XEnergieJobProperties getProperties() {
        return new XEnergieJobProperties(
                batchSize,
                20,
                numberOfThreads
        );
    }

    /**
     * Executes the specified logic, which includes fetching data from repositories, processing the data, and exporting it.
     * This method is transactional, meaning that changes in the database will be committed or rolled back as a unit of work.
     */
    @Transactional
    @ExecutionTimeLogger
    public void execute(Process process) {
        List<ProductContractPodExportData> exportData = new ArrayList<>();

        try {
            ExecutorService executorService = Executors.newFixedThreadPool(getProperties().numberOfThreads());
            List<Callable<Boolean>> callableQueue = new ArrayList<>();

            LocalDateTime dateBefore = LocalDateTime.now().minusYears(1);

            Long countData = contractPodRepository
                    .countAllNonSynchronizedProductContractPointOfDeliveriesWithSplitsFromXEnergieBeforeDate(dateBefore);
            Long fetchedDataCount = 0L;
            int dataOffset = 0;
            Integer queryBatchSize = getProperties().queryBatchSize();

            while (countData > fetchedDataCount) {
                List<ExcelGenerationFetchDataModel> allNonSynchronizedProductContractPointOfDeliveriesWithSplitsFromXEnergieBeforeDate =
                        contractPodRepository
                                .findAllNonSynchronizedProductContractPointOfDeliveriesWithSplitsFromXEnergieBeforeDate(dateBefore, PageRequest.of(dataOffset, queryBatchSize));

                callableQueue.add(() ->
                        processBatch(
                                dateBefore,
                                process,
                                exportData,
                                allNonSynchronizedProductContractPointOfDeliveriesWithSplitsFromXEnergieBeforeDate
                        )
                );

                fetchedDataCount += queryBatchSize;
                dataOffset++;
            }

            executorService.invokeAll(callableQueue);
        } catch (Exception e) {
            handleException(process, e.getMessage());
        }

        try {
            productContractPodExportService.process(exportData);
        } catch (Exception e) {
            handleException(process, e.getMessage());
        }

        executeNextJobInChain(process);
    }

    private Boolean processBatch(LocalDateTime dateBefore,
                                 Process process,
                                 List<ProductContractPodExportData> exportData,
                                 List<ExcelGenerationFetchDataModel> allNonSynchronizedProductContractPointOfDeliveriesWithSplitsFromXEnergieBeforeDate) {
        dataModelFlag:
        for (ExcelGenerationFetchDataModel excelGenerationFetchDataModel : allNonSynchronizedProductContractPointOfDeliveriesWithSplitsFromXEnergieBeforeDate) {
            try {
                Long id = excelGenerationFetchDataModel.id();
                String identifier = excelGenerationFetchDataModel.identifier();
                Boolean ownedByEnergoPro = excelGenerationFetchDataModel.ownedByEnergoPro();

                List<Runnable> uncommittedExportData = new ArrayList<>();
                if (ownedByEnergoPro) {
                    Optional<List<XEnergieEmptySpaceInfo>> xEnergieEmptySpaceInfosOptional =
                            handleException(process, () -> xEnergieRepository.getEmptySpaces(identifier), "Exception handled while trying to fetch empty spaces for Point Of Delivery with identifier: [%s]".formatted(identifier));
                    if (xEnergieEmptySpaceInfosOptional.isEmpty()) {
                        continue;
                    }

                    List<XEnergieEmptySpaceInfo> xEnergieEmptySpaceInfos = xEnergieEmptySpaceInfosOptional.get();
                    for (XEnergieEmptySpaceInfo xEnergieEmptySpaceInfo : xEnergieEmptySpaceInfos) {
                        List<ExcelGenerationDataModel> respectivePointOfDeliveriesForEmptySplit =
                                contractPodRepository
                                        .findAllNonSynchronizedProductContractPointOfDeliveriesWithSplitsFromXEnergieBeforeDateForEnergoProPointOfDelivery(
                                                id,
                                                dateBefore,
                                                xEnergieEmptySpaceInfo.getStartDate().atStartOfDay(),
                                                xEnergieEmptySpaceInfo.getEndDate().atStartOfDay()
                                        );

                        Pair<LocalDate, LocalDate> range = Pair.of(xEnergieEmptySpaceInfo.getStartDate(), xEnergieEmptySpaceInfo.getEndDate());
                        List<Pair<LocalDate, LocalDate>> nonUsedDatesBetweenDatesInList = findNonUsedDatesBetweenDatesInList(range, respectivePointOfDeliveriesForEmptySplit);

                        Optional<AdditionalInformationForPointOfDeliveries> additionalInformationForPointOfDeliveries =
                                handleException(process, () ->
                                                xEnergieRepository.retrieveAdditionalInformationForPointOfDelivery(identifier),
                                        "Additional Information Not Found for point of delivery with identifier: [%s]".formatted(identifier));

                        if (additionalInformationForPointOfDeliveries.isEmpty()) {
                            continue dataModelFlag;
                        }

                        AdditionalInformationForPointOfDeliveries additionalInformation = additionalInformationForPointOfDeliveries.get();
                        for (ExcelGenerationDataModel model : respectivePointOfDeliveriesForEmptySplit) {
                            uncommittedExportData.add(() ->
                                    exportData.add(
                                            new ProductContractPodExportData(
                                                    model.pointOfDeliveryIdentifier(),
                                                    model.pointOfDeliveryAdditionalIdentifier(),
                                                    model.pointOfDeliveryIdentifier(),
                                                    model.activationDate(),
                                                    model.deactivationDate(),
                                                    additionalInformation.getVoltageLevelOne(),
                                                    additionalInformation.getMeasurement(),
                                                    model.customerNumber(),
                                                    model.gridOperatorAdditionalCode(),
                                                    additionalInformation.getMeteringServicesProvider(),
                                                    additionalInformation.getAncillaryServicesProvider(),
                                                    additionalInformation.getLpRegion1(),
                                                    model.dealNumber(),
                                                    additionalInformation.getMeasurementSystem()
                                            )
                                    )
                            );
                        }

                        Optional<ExcelGenerationLatestPointOfDeliveryDetailsDataModel> lastVersionPointOfDeliveryDetailsByIdentifier = pointOfDeliveryDetailsRepository.findLastVersionPointOfDeliveryDetailsByIdentifier(identifier);
                        if (lastVersionPointOfDeliveryDetailsByIdentifier.isPresent()) {
                            ExcelGenerationLatestPointOfDeliveryDetailsDataModel excelGenerationLatestPointOfDeliveryDetailsDataModel = lastVersionPointOfDeliveryDetailsByIdentifier.get();
                            for (Pair<LocalDate, LocalDate> emptySpaceDate : nonUsedDatesBetweenDatesInList) {
                                uncommittedExportData.add(() ->
                                        exportData.add(
                                                new ProductContractPodExportData(
                                                        identifier,
                                                        excelGenerationLatestPointOfDeliveryDetailsDataModel.additionalIdentifier(),
                                                        identifier,
                                                        emptySpaceDate.getKey(),
                                                        emptySpaceDate.getValue(),
                                                        additionalInformation.getVoltageLevelOne(),
                                                        additionalInformation.getMeasurement(),
                                                        additionalInformation.getOwnerPDT(),
                                                        excelGenerationLatestPointOfDeliveryDetailsDataModel.gridOperatorAdditionalCode(),
                                                        additionalInformation.getMeteringServicesProvider(),
                                                        additionalInformation.getAncillaryServicesProvider(),
                                                        additionalInformation.getLpRegion1(),
                                                        "",
                                                        additionalInformation.getMeasurementSystem()
                                                )

                                        )
                                );
                            }
                        } else {
                            handleException(process, "Cannot create empty splits, latest version for point of delivery with identifier: [%s] not found".formatted(identifier));
                            continue dataModelFlag;
                        }
                    }
                } else {
                    List<ExcelGenerationDataModel> respectivePointOfDeliveriesForEmptySplit =
                            contractPodRepository
                                    .findAllNonSynchronizedProductContractPointOfDeliveriesWithSplitsFromXEnergieBeforeDateForEnergoProPointOfDelivery(
                                            id,
                                            dateBefore,
                                            null,
                                            null
                                    );

                    for (ExcelGenerationDataModel model : respectivePointOfDeliveriesForEmptySplit) {
                        uncommittedExportData.add(() ->
                                exportData.add(
                                        new ProductContractPodExportData(
                                                model.pointOfDeliveryIdentifier(),
                                                model.pointOfDeliveryAdditionalIdentifier(),
                                                model.pointOfDeliveryIdentifier(),
                                                model.activationDate(),
                                                model.deactivationDate(),
                                                "",
                                                model.gridOperatorAdditionalCode(),
                                                model.customerNumber(),
                                                model.gridOperatorAdditionalCode(),
                                                "",
                                                "",
                                                "",
                                                model.dealNumber(),
                                                ""
                                        )
                                )
                        );
                    }
                }

                uncommittedExportData.forEach(Runnable::run);
            } catch (Exception e) {
                handleException(process, e.getMessage());
            }
        }

        return true;
    }

    private List<ProductContractPodExportData> startUpdateProcess(Process process) {
        List<ProductContractPodExportData> exportData = new ArrayList<>();

        List<ExcelGenerationFetchDataModel> allNonSynchronizedProductContractPointOfDeliveriesWithSplitsFromXEnergieBeforeDate =
                contractPodRepository
                        .findAllNonSynchronizedProductContractPointOfDeliveriesWithSplitsFromXEnergieBeforeDate(LocalDateTime.now().minusYears(1), PageRequest.ofSize(Integer.MAX_VALUE));

        processBatch(LocalDateTime.now().minusYears(1), process, exportData, allNonSynchronizedProductContractPointOfDeliveriesWithSplitsFromXEnergieBeforeDate);
        return exportData;
    }

    /**
     * Test purposes only
     */
    public Workbook generateExcel(Process process) throws Exception {
        List<ProductContractPodExportData> exportData = startUpdateProcess(process);

        return productContractPodExportService.generateExportData(exportData);
    }

    /**
     * Finds the non-used dates between the given range of dates in the given list of ExcelGenerationDataModel objects.
     *
     * @param range The range of dates to search between (inclusive).
     * @param list  The list of ExcelGenerationDataModel objects to search within.
     * @return A list of Pair objects, each representing a non-used date range within the given range of dates.
     */
    private List<Pair<LocalDate, LocalDate>> findNonUsedDatesBetweenDatesInList(Pair<LocalDate, LocalDate> range, List<ExcelGenerationDataModel> list) {
        List<Pair<LocalDate, LocalDate>> context = new ArrayList<>();

        if (CollectionUtils.isEmpty(list)) {
            context.add(range); // full range is non used
        } else {
            for (int i = 0; i < list.size(); i++) {
                ExcelGenerationDataModel model = list.get(i);

                LocalDate currentSplitDeactivationDate = model.deactivationDate();
                if (i == 0) {
                    LocalDate emptySpaceStartDate = range.getKey();
                    LocalDate currentSplitActivationDate = model.activationDate();
                    if (!currentSplitActivationDate.equals(emptySpaceStartDate)) {
                        context.add(Pair.of(emptySpaceStartDate, currentSplitActivationDate.minusDays(1)));
                    }

                    if (i == (list.size() - 1)) {
                        if (!currentSplitDeactivationDate.equals(range.getValue())) {
                            context.add(Pair.of(currentSplitDeactivationDate.plusDays(1), range.getValue()));
                        }
                    }
                } else {
                    if (i == (list.size() - 1)) {
                        if (!currentSplitDeactivationDate.equals(range.getValue())) {
                            context.add(Pair.of(currentSplitDeactivationDate.plusDays(1), range.getValue()));
                        }
                    } else {
                        ExcelGenerationDataModel nextModel = list.get(i + 1);
                        LocalDate nextSplitActivationDate = nextModel.activationDate();
                        if (ChronoUnit.DAYS.between(currentSplitDeactivationDate, nextSplitActivationDate) > 1) {
                            context.add(Pair.of(currentSplitDeactivationDate.plusDays(1), nextSplitActivationDate.minusDays(1)));
                        }
                    }
                }
            }
        }

        return context;
    }

    public List<ExcelGenerationFetchDataModel> fetchNonSynchronizedData() {
        return contractPodRepository
                .findAllNonSynchronizedProductContractPointOfDeliveriesWithSplitsFromXEnergieBeforeDate(LocalDateTime.now().minusYears(1), PageRequest.ofSize(Integer.MAX_VALUE));
    }
}
