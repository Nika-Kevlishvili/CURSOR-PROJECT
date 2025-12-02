package bg.energo.phoenix.service.contract.product.startDateUpdate.impl;

import bg.energo.phoenix.model.customAnotations.aspects.ExecutionTimeLogger;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.contract.billing.ContractPods;
import bg.energo.phoenix.model.entity.contract.product.ProductContractDetails;
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

@Slf4j
@Service
@Deprecated
public class ProductContractUpdateWhenStartDateLessThenOldService extends AbstractProductContractUpdateStartDate {

    public ProductContractUpdateWhenStartDateLessThenOldService(ProductContractDetailsRepository productContractDetailsRepository, ContractPodRepository contractPodRepository, XEnergieCommunicationService xEnergieCommunicationService, XEnergieRepository xEnergieRepository, ProductDetailsRepository productDetailsRepository, PointOfDeliveryRepository pointOfDeliveryRepository, PointOfDeliveryDetailsRepository pointOfDeliveryDetailsRepository, ProductContractPodExportService exportService, CustomerDetailsRepository customerDetailsRepository, CustomerRepository customerRepository) {
        super(productContractDetailsRepository, contractPodRepository, xEnergieCommunicationService, xEnergieRepository, productDetailsRepository, pointOfDeliveryRepository, pointOfDeliveryDetailsRepository, exportService, customerDetailsRepository, customerRepository);
    }

    @Override
    public ProductContractStartDateUpdateRouteTypes getRoute() {
        return ProductContractStartDateUpdateRouteTypes.LESS_THEN_OLD_START_DATE;
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
            return;
        }

        List<ContractPods> updatedVersionActivePods = contractPodRepository
                .findAllByContractDetailIdAndStatusIn(currentContractDetails.getId(), List.of(EntityStatus.ACTIVE));

        List<ContractPods> previousVersionActivePods = contractPodRepository
                .findAllByContractDetailIdAndStatusIn(previousVersionDetails.getId(), List.of(EntityStatus.ACTIVE));

        for (ContractPods previousPod : previousVersionActivePods) {
            Optional<ContractPods> updatedVersionPodOptional = updatedVersionActivePods
                    .stream()
                    .filter(pod -> pod.getPodDetailId().equals(previousPod.getPodDetailId()))
                    .findAny();

            if (updatedVersionPodOptional.isEmpty()) {
                EPBChainedExceptionTriggerUtil.addExceptionAndTrigger("Previous Product Contract Version POD with detail id: [%s] not found in Updated Product Contract Version PODS".formatted(previousPod.getPodDetailId()), exceptionMessages, log);
                return;
            }

            if (Objects.isNull(previousPod.getActivationDate())) {
                continue;
            }

            ContractPods updatedPod = updatedVersionPodOptional.get();

            if (isPodActivationDateLessThanRequestedStartDate(previousPod, requestedStartDate)) {
                if (!isPodDeactivationDateLessThanRequestedStartDate(previousPod, requestedStartDate)) {
                    if (Objects.nonNull(updatedPod.getActivationDate())) {
                        if (existsGapBetweenPreviousAndUpdatedVersion(previousPod, updatedPod)) {
                            EPBChainedExceptionTriggerUtil.addExceptionAndTrigger("Found gap between Previous Contract Version POD with activation date:[%s] and Updated Product Contract version deactivation date:[%s]".formatted(previousPod.getDeactivationDate(), updatedPod.getActivationDate()), exceptionMessages, log);
                        } else {
                            LocalDate previousPodInitialActivationDate = previousPod.getActivationDate();
                            LocalDate updatedPodInitialActivationDate = updatedPod.getActivationDate();

                            previousPod.setDeactivationDate(requestedStartDate.minusDays(1));
                            updatedPod.setActivationDate(requestedStartDate);

                            uncommittedEntities.addAll(List.of(previousPod, updatedPod));

                            String previousPodIdentifier = fetchIdentifierOrThrowException(exceptionMessages, previousPod);
//                                    addNewUncommittedActionToList(uncommittedActions, () -> editPointOfDeliveryStartAndEndDates(previousPodIdentifier, previousPodInitialActivationDate, null, previousPod.getDeactivationDate()));

                            String updatedPodIdentifier = fetchIdentifierOrThrowException(exceptionMessages, updatedPod);
//                                    addNewUncommittedActionToList(uncommittedActions, () -> editPointOfDeliveryStartAndEndDates(updatedPodIdentifier, updatedPodInitialActivationDate, null, updatedPod.getActivationDate()));

                            // todo update start date of the deal if current start date of the deal is more than new activation date
                        }
                    } else {
                        LocalDate currentActivationDate = previousPod.getActivationDate();

                        updatedPod.setDeactivationDate(previousPod.getDeactivationDate());
                        updatedPod.setActivationDate(requestedStartDate);
                        previousPod.setDeactivationDate(requestedStartDate.minusDays(1));

                        uncommittedEntities.addAll(List.of(previousPod, updatedPod));

                        switch (checkDealNumber(currentContractDetails, previousVersionDetails)) {
                            case BOTH_VERSIONS -> {
                                if (isDealNumbersSame(currentContractDetails, previousVersionDetails)) {
                                    continue;
                                } else {
//                                            String identifier = fetchIdentifierOrThrowException(exceptionMessages, previousPod);
//
//                                            addNewUncommittedActionToList(uncommittedActions, () -> editPointOfDeliveryStartAndEndDates(identifier, currentActivationDate, null, previousPod.getDeactivationDate()));
                                }
                            }
                            case UPDATED_VERSION -> {
                                String updatedPodIdentifier = fetchIdentifierOrThrowException(exceptionMessages, updatedPod);

//                                        AdditionalInformationForPointOfDeliveriesOfElectroDistributionNorth additionalInformation = fetchAdditionalInformation(updatedPodIdentifier, exceptionMessages);
//
//                                        PointOfDeliveryDetails pointOfDeliveryDetails = fetchPointOfDeliveryDetailsOrElseThrowException(updatedPod, exceptionMessages);
//
//                                        addNewPodExportDataToList(uncommittedExportData, updatedPodIdentifier, additionalInformation, updatedPod, pointOfDeliveryDetails);
                            }
                            case PREVIOUS_VERSION -> {
//                                        String identifier = fetchIdentifierOrThrowException(exceptionMessages, previousPod);

//                                        addNewUncommittedActionToList(uncommittedActions, () -> editPointOfDeliveryStartAndEndDates(identifier, currentActivationDate, null, previousPod.getDeactivationDate()));

                                Optional<String> balancingProductNameOptional = getBalancingProductName(previousVersionDetails, exceptionMessages);
                                if (balancingProductNameOptional.isPresent()) {
//                                            createCustomerInXEnergieIfRequired(previousVersionDetails, exceptionMessages, log);
                                    // TODO: 16.10.23 xEnergie - Create Deal And Record in contract versions

//                                            AdditionalInformationForPointOfDeliveriesOfElectroDistributionNorth additionalInformation = fetchAdditionalInformation(identifier, exceptionMessages);
//                                            PointOfDeliveryDetails pointOfDeliveryDetails = fetchPointOfDeliveryDetailsOrElseThrowException(previousPod, exceptionMessages);
//                                            addNewPodExportDataToList(uncommittedExportData, identifier, additionalInformation, previousPod, pointOfDeliveryDetails);
                                } else {
                                    continue;
                                }
                            }
                        }
                    }
                }
            } else {
                if (existsGapBetweenPreviousAndUpdatedVersion(previousPod, updatedPod)) {
                    EPBChainedExceptionTriggerUtil.addExceptionAndTrigger("Previous Product Contract Version has POD: [%s/%s] with deactivation date more than Requested Start Date: [%s]".formatted(previousPod.getPodDetailId(), previousPod.getDeactivationDate(), requestedStartDate), exceptionMessages, log);
                } else {
                    LocalDate updatedPodActivationDate = updatedPod.getActivationDate();

                    updatedPod.setActivationDate(previousPod.getActivationDate());
                    updatedPod.setDeactivationDate(previousPod.getDeactivationDate());

                    previousPod.setActivationDate(null);
                    previousPod.setDeactivationDate(null);

                    uncommittedEntities.addAll(List.of(previousPod, updatedPod));

                    switch (checkDealNumber(currentContractDetails, previousVersionDetails)) {
                        case BOTH_VERSIONS -> {
                            if (isDealNumbersSame(currentContractDetails, previousVersionDetails)) {
                                continue;
                            } else {
                                String previousPodIdentifier = fetchIdentifierOrThrowException(exceptionMessages, previousPod);
                                String updatedPodIdentifier = fetchIdentifierOrThrowException(exceptionMessages, updatedPod);

//                                    addNewUncommittedActionToList(uncommittedActions, () -> removePodFromVersion(previousPodIdentifier, previousPod));
//                                    addNewUncommittedActionToList(uncommittedActions, () -> editPointOfDeliveryStartAndEndDates(updatedPodIdentifier, updatedPodActivationDate, updatedPod.getActivationDate(), null));
                            }
                        }
                        case UPDATED_VERSION -> {
//                                    String identifier = fetchIdentifierOrThrowException(exceptionMessages, updatedPod);
//                                    AdditionalInformationForPointOfDeliveriesOfElectroDistributionNorth additionalInformation = fetchAdditionalInformation(identifier, exceptionMessages);
//                                    PointOfDeliveryDetails pointOfDeliveryDetails = fetchPointOfDeliveryDetailsOrElseThrowException(updatedPod, exceptionMessages);
//                                    addNewPodExportDataToList(uncommittedExportData, identifier, additionalInformation, updatedPod, pointOfDeliveryDetails);
                        }
                        case PREVIOUS_VERSION -> {
                            String identifier = fetchIdentifierOrThrowException(exceptionMessages, previousPod);

//                                addNewUncommittedActionToList(uncommittedActions, () -> removePodFromVersion(identifier, previousPod));
//                                createCustomerInXEnergieIfRequired(previousVersionDetails, exceptionMessages, log);
//                                createCustomerInXEnergieIfRequired(currentContractDetails, exceptionMessages, log);
                            // TODO: 16.10.23 create Deal and record in contracts versions

//                                    AdditionalInformationForPointOfDeliveriesOfElectroDistributionNorth additionalInformation = fetchAdditionalInformation(identifier, exceptionMessages);
//                                    PointOfDeliveryDetails pointOfDeliveryDetails = fetchPointOfDeliveryDetailsOrElseThrowException(previousPod, exceptionMessages);
//                                    addNewPodExportDataToList(uncommittedExportData, identifier, additionalInformation, updatedPod, pointOfDeliveryDetails);
                        }
                    }
                }
            }
        }

        commit(uncommittedEntities, uncommittedActions, uncommittedExportData, exceptionMessages);
    }
}
