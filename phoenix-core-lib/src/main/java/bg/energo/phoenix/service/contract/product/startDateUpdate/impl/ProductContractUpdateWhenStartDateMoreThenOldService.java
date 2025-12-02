package bg.energo.phoenix.service.contract.product.startDateUpdate.impl;

import bg.energo.phoenix.model.customAnotations.aspects.ExecutionTimeLogger;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.contract.billing.ContractPods;
import bg.energo.phoenix.model.entity.contract.product.ProductContractDetails;
import bg.energo.phoenix.model.entity.pod.pod.PointOfDeliveryDetails;
import bg.energo.phoenix.model.enums.contract.products.ActivationDateComparisonResult;
import bg.energo.phoenix.model.enums.contract.products.ProductContractStartDateUpdateRouteTypes;
import bg.energo.phoenix.model.process.ProductContractPodExportData;
import bg.energo.phoenix.repository.contract.billing.ContractPodRepository;
import bg.energo.phoenix.repository.contract.product.ProductContractDetailsRepository;
import bg.energo.phoenix.repository.customer.CustomerDetailsRepository;
import bg.energo.phoenix.repository.customer.CustomerRepository;
import bg.energo.phoenix.repository.pod.pod.PointOfDeliveryDetailsRepository;
import bg.energo.phoenix.repository.pod.pod.PointOfDeliveryRepository;
import bg.energo.phoenix.repository.product.product.ProductDetailsRepository;
import bg.energo.phoenix.service.contract.product.ProductContractPodExportService;
import bg.energo.phoenix.service.xEnergie.XEnergieCommunicationService;
import bg.energo.phoenix.service.xEnergie.XEnergieRepository;
import bg.energo.phoenix.util.epb.EPBChainedExceptionTriggerUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

@Slf4j
@Service
@Deprecated
public class ProductContractUpdateWhenStartDateMoreThenOldService extends AbstractProductContractUpdateStartDate {

    public ProductContractUpdateWhenStartDateMoreThenOldService(ProductContractDetailsRepository productContractDetailsRepository, ContractPodRepository contractPodRepository, XEnergieCommunicationService xEnergieCommunicationService, XEnergieRepository xEnergieRepository, ProductDetailsRepository productDetailsRepository, PointOfDeliveryRepository pointOfDeliveryRepository, PointOfDeliveryDetailsRepository pointOfDeliveryDetailsRepository, ProductContractPodExportService exportService, CustomerDetailsRepository customerDetailsRepository, CustomerRepository customerRepository) {
        super(productContractDetailsRepository, contractPodRepository, xEnergieCommunicationService, xEnergieRepository, productDetailsRepository, pointOfDeliveryRepository, pointOfDeliveryDetailsRepository, exportService, customerDetailsRepository, customerRepository);
    }

    @Override
    public ProductContractStartDateUpdateRouteTypes getRoute() {
        return ProductContractStartDateUpdateRouteTypes.MORE_THEN_OLD_START_DATE;
    }

    @Override
    @ExecutionTimeLogger
    public void recalculateDates(LocalDate requestedStartDate,
                                 ProductContractDetails currentContractDetails,
                                 ProductContractDetails previousVersionDetails,
                                 ProductContractDetails nextVersionDetails,
                                 List<String> exceptionMessages) {
        List<ContractPods> uncommittedEntities = new ArrayList<>();
        List<Runnable> uncommittedActions = new ArrayList<>();
        List<ProductContractPodExportData> uncommittedExportData = new ArrayList<>();

        if (Objects.isNull(previousVersionDetails)) {
            EPBChainedExceptionTriggerUtil.addExceptionAndTrigger("Previous Product Contract Version not found, a first version may have been provided", exceptionMessages, log);
        }

        List<ContractPods> updatedVersionActivePods = contractPodRepository
                .findAllByContractDetailIdAndStatusIn(currentContractDetails.getId(), List.of(EntityStatus.ACTIVE));

        List<ContractPods> previousVersionActivePods = contractPodRepository
                .findAllByContractDetailIdAndStatusIn(previousVersionDetails.getId(), List.of(EntityStatus.ACTIVE));

        for (ContractPods updatedPod : updatedVersionActivePods) {
            Optional<ContractPods> previosPodOptional = previousVersionActivePods
                    .stream()
                    .filter(pod -> pod.getPodDetailId().equals(updatedPod.getPodDetailId()))
                    .findAny();

            if (previosPodOptional.isEmpty()) {
                if (Stream.of(updatedPod.getActivationDate(), updatedPod.getDeactivationDate())
                        .allMatch(Objects::nonNull) && updatedPod.getActivationDate().isAfter(requestedStartDate)) {
                    continue;
                }
                EPBChainedExceptionTriggerUtil.addExceptionAndTrigger("POD with detail id [%s] found in current Product Contract Version but not found in previous Product Contract Version;".formatted(updatedPod.getPodDetailId()), exceptionMessages, log);
            }
        }

        for (ContractPods previousPod : previousVersionActivePods) {
            Optional<ContractPods> updatedVersionPodOptional = updatedVersionActivePods
                    .stream()
                    .filter(pod -> pod.getPodDetailId().equals(previousPod.getPodDetailId()))
                    .findAny();

            if (updatedVersionPodOptional.isEmpty()) {
                continue;
            }

            ContractPods updatedVersionPod = updatedVersionPodOptional.get();

            switch (ActivationDateComparisonResult.compareActivationDates(previousPod, updatedVersionPod)) {
                case HAS_ACTIVATION_IN_BOTH_VERSION -> {
                    if (existsGapBetweenPreviousAndUpdatedVersion(previousPod, updatedVersionPod)) {
                        if (updatedVersionPod.getActivationDate().isBefore(requestedStartDate)) {
                            EPBChainedExceptionTriggerUtil.addExceptionAndTrigger("Activation date in Updated Version POD [%s] is less than Requested Start Date [%s]".formatted(updatedVersionPod.getActivationDate(), requestedStartDate), exceptionMessages, log);
                        }
                    } else {
                        if (isPodDeactivationDateLessThanRequestedStartDate(updatedVersionPod, requestedStartDate)) {
                            LocalDate previousPodActivationDate = previousPod.getActivationDate();

                            previousPod.setDeactivationDate(updatedVersionPod.getDeactivationDate());
                            updatedVersionPod.setActivationDate(null);
                            updatedVersionPod.setDeactivationDate(null);

                            switch (checkDealNumber(currentContractDetails, previousVersionDetails)) {
                                case BOTH_VERSIONS -> {
                                    if (isDealNumbersSame(currentContractDetails, previousVersionDetails)) {
                                        continue;
                                    } else {
                                        String updatedVersionPodIdentifier = fetchIdentifierOrThrowException(exceptionMessages, updatedVersionPod);

//                                        addNewUncommittedActionToList(uncommittedActions, () -> removePodFromVersion(updatedVersionPodIdentifier, updatedVersionPod));

                                        String previousPodIdentifier = fetchIdentifierOrThrowException(exceptionMessages, previousPod);

//                                        addNewUncommittedActionToList(uncommittedActions, () -> editPointOfDeliveryStartAndEndDates(previousPodIdentifier, previousPod.getActivationDate(), null, updatedVersionPod.getDeactivationDate()););
                                    }
                                }
                                case UPDATED_VERSION -> {
                                    String identifier = fetchIdentifierOrThrowException(exceptionMessages, previousPod);

//                                    addNewUncommittedActionToList(uncommittedActions, () -> removePodFromVersion(identifier, updatedVersionPod));
                                }
                                case PREVIOUS_VERSION -> {
                                    String identifier = fetchIdentifierOrThrowException(exceptionMessages, previousPod);

//                                    addNewUncommittedActionToList(uncommittedActions, () -> editPointOfDeliveryStartAndEndDates(identifier, previousPodActivationDate, null, previousPod.getDeactivationDate()));
                                }
                            }
                        } else {
                            LocalDate updatedVersionPodActivationDate = updatedVersionPod.getActivationDate();
                            LocalDate previousVersionPodActivationDate = previousPod.getActivationDate();

                            updatedVersionPod.setActivationDate(requestedStartDate);

                            previousPod.setDeactivationDate(requestedStartDate.minusDays(1));

                            switch (checkDealNumber(currentContractDetails, previousVersionDetails)) {
                                case BOTH_VERSIONS -> {
                                    if (isDealNumbersSame(currentContractDetails, previousVersionDetails)) {
                                        continue;
                                    } else {
                                        String previousPodIdentifier = fetchIdentifierOrThrowException(exceptionMessages, previousPod);
                                        String updatedPodIdentifier = fetchIdentifierOrThrowException(exceptionMessages, updatedVersionPod);

//                                        addNewUncommittedActionToList(uncommittedActions, () -> editPointOfDeliveryStartAndEndDates(previousPodIdentifier, previousPod.getActivationDate(), null, previousPod.getDeactivationDate()));
//                                        addNewUncommittedActionToList(uncommittedActions, () -> editPointOfDeliveryStartAndEndDates(updatedPodIdentifier, updatedVersionPodActivationDate, updatedVersionPod.getActivationDate(), null));

                                        // TODO: 24.10.23 update start date of the deal if current start date of the deal is more than new activation date - red flagged
                                    }
                                }
                                case UPDATED_VERSION -> {
                                    String updatedPodIdentifier = fetchIdentifierOrThrowException(exceptionMessages, updatedVersionPod);

//                                    addNewUncommittedActionToList(uncommittedActions, () -> editPointOfDeliveryStartAndEndDates(updatedPodIdentifier, updatedVersionPodActivationDate, updatedVersionPod.getActivationDate(), null));
                                }
                                case PREVIOUS_VERSION -> {
                                    String previousPodIdentifier = fetchIdentifierOrThrowException(exceptionMessages, previousPod);

//                                    addNewUncommittedActionToList(uncommittedActions, () -> editPointOfDeliveryStartAndEndDates(previousPodIdentifier, previousVersionPodActivationDate, null, previousPod.getDeactivationDate()));
                                }
                            }
                        }
                    }
                }
                case HAS_ACTIVATION_IN_PREVIOUS_VERSION, DOES_NOT_HAVE_ACTIVATION_DATES -> {
                    continue;
                }
                case HAS_ACTIVATION_IN_UPDATED_VERSION -> {
                    if (isPodActivationDateLessThanRequestedStartDate(updatedVersionPod, requestedStartDate)) {
                        if (isPodDeactivationDateMoreThanRequestedStartDate(updatedVersionPod, requestedStartDate)) {
                            LocalDate previousPodActivationDate = previousPod.getActivationDate();

                            previousPod.setActivationDate(updatedVersionPod.getActivationDate());
                            previousPod.setDeactivationDate(requestedStartDate.minusDays(1));

                            LocalDate updatedVersionPodActivationDate = updatedVersionPod.getActivationDate();

                            updatedVersionPod.setActivationDate(requestedStartDate);

                            switch (checkDealNumber(currentContractDetails, previousVersionDetails)) {
                                case BOTH_VERSIONS -> {
                                    if (isDealNumbersSame(currentContractDetails, previousVersionDetails)) {
                                        continue;
                                    } else {
                                        String previousPodIdentifier = fetchIdentifierOrThrowException(exceptionMessages, previousPod);
                                        String updatedPodIdentifier = fetchIdentifierOrThrowException(exceptionMessages, updatedVersionPod);

//                                        addNewUncommittedActionToList(uncommittedActions, () -> editPointOfDeliveryStartAndEndDates(previousPodIdentifier, previousPodActivationDate, null, previousPod.getDeactivationDate()));
//                                        addNewUncommittedActionToList(uncommittedActions, () -> editPointOfDeliveryStartAndEndDates(updatedPodIdentifier, updatedVersionPodActivationDate, updatedVersionPod.getActivationDate(), null));
                                    }
                                }
                                case UPDATED_VERSION -> {
                                    String identifier = fetchIdentifierOrThrowException(exceptionMessages, updatedVersionPod);

//                                    addNewUncommittedActionToList(uncommittedActions, () -> editPointOfDeliveryStartAndEndDates(identifier, updatedVersionPodActivationDate, updatedVersionPod.getActivationDate(), null));
                                }
                                case PREVIOUS_VERSION -> {
                                    String identifier = fetchIdentifierOrThrowException(exceptionMessages, previousPod);

//                                    AdditionalInformationForPointOfDeliveriesOfElectroDistributionNorth additionalInformation = fetchAdditionalInformation(identifier, exceptionMessages);
//
//                                    PointOfDeliveryDetails pointOfDeliveryDetails = fetchPointOfDeliveryDetailsOrElseThrowException(previousPod, exceptionMessages);
//
//                                    addNewPodExportDataToList(uncommittedExportData, identifier, additionalInformation, previousPod, pointOfDeliveryDetails);
                                }
                            }
                        } else {
                            switch (checkDealNumber(currentContractDetails, previousVersionDetails)) {
                                case BOTH_VERSIONS -> {
                                    if (isDealNumbersSame(currentContractDetails, previousVersionDetails)) {
                                        continue;
                                    } else {
                                        // TODO: 20.10.23 Check if deal end date in previous deal is less than deactivation date in previous version POD
                                    }
                                }
                                case UPDATED_VERSION -> {
                                    String identifier = fetchIdentifierOrThrowException(exceptionMessages, previousPod);

//                                    addNewUncommittedActionToList(uncommittedActions, () -> removePodFromVersion(identifier, updatedVersionPod));
                                }
                                case PREVIOUS_VERSION -> {
                                    String identifier = fetchIdentifierOrThrowException(exceptionMessages, previousPod);

                                    PointOfDeliveryDetails pointOfDeliveryDetails = fetchPointOfDeliveryDetailsOrElseThrowException(previousPod, exceptionMessages);

//                                    AdditionalInformationForPointOfDeliveriesOfElectroDistributionNorth additionalInformation = fetchAdditionalInformation(identifier, exceptionMessages);

                                    previousPod.setActivationDate(updatedVersionPod.getActivationDate());
                                    previousPod.setDeactivationDate(updatedVersionPod.getDeactivationDate().minusDays(1));

                                    updatedVersionPod.setActivationDate(null);
                                    updatedVersionPod.setDeactivationDate(null);

//                                    addNewPodExportDataToList(uncommittedExportData, identifier, additionalInformation, previousPod, pointOfDeliveryDetails);
                                }
                            }
                        }
                    }

                    uncommittedEntities.addAll(List.of(updatedVersionPod, previousPod));
                }
            }
        }

        commit(uncommittedEntities, uncommittedActions, uncommittedExportData, exceptionMessages);
    }
}
