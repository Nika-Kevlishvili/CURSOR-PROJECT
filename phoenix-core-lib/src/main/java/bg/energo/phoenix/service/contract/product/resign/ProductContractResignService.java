package bg.energo.phoenix.service.contract.product.resign;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.exception.IllegalArgumentsProvidedException;
import bg.energo.phoenix.model.customAnotations.aspects.ExecutionTimeLogger;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.contract.billing.ContractPods;
import bg.energo.phoenix.model.entity.contract.product.*;
import bg.energo.phoenix.model.entity.customer.CustomerDetails;
import bg.energo.phoenix.model.entity.pod.pod.PointOfDelivery;
import bg.energo.phoenix.model.entity.product.product.ProductContractTerms;
import bg.energo.phoenix.model.enums.contract.products.ContractDetailsStatus;
import bg.energo.phoenix.model.enums.contract.products.ContractDetailsSubStatus;
import bg.energo.phoenix.model.enums.product.product.ProductTermType;
import bg.energo.phoenix.model.enums.product.term.terms.ContractEntryIntoForce;
import bg.energo.phoenix.model.enums.product.term.terms.StartOfContractInitialTerm;
import bg.energo.phoenix.model.enums.product.term.terms.SupplyActivation;
import bg.energo.phoenix.model.response.contract.productContract.ProductContractDataModificationResponse;
import bg.energo.phoenix.model.response.contract.productContract.ResigningDataMapModel;
import bg.energo.phoenix.repository.contract.billing.ContractPodRepository;
import bg.energo.phoenix.repository.contract.product.ProductContractDetailsRepository;
import bg.energo.phoenix.repository.contract.product.ProductContractRepository;
import bg.energo.phoenix.repository.contract.product.ProductContractResignedContractsRepository;
import bg.energo.phoenix.repository.customer.CustomerDetailsRepository;
import bg.energo.phoenix.repository.pod.pod.PointOfDeliveryRepository;
import bg.energo.phoenix.repository.product.product.ProductContractTermRepository;
import bg.energo.phoenix.repository.product.product.ProductDetailsRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@AllArgsConstructor
public class ProductContractResignService {
    private final ProductContractRepository productContractRepository;
    private final ProductContractDetailsRepository productContractDetailsRepository;
    private final ContractPodRepository contractPodRepository;
    private final CustomerDetailsRepository customerDetailsRepository;
    private final PointOfDeliveryRepository pointOfDeliveryRepository;
    private final ProductContractResignedContractsRepository productContractResignedContractsRepository;
    private final ProductDetailsRepository productDetailsRepository;
    private final ProductContractTermRepository productContractTermRepository;

    /**
     * Executes the resigning process for a product contract.
     *
     * @param signingDate                  The date of signing the new contract.
     * @param targetProductContract        The product contract to resign.
     * @param targetProductContractDetails The details of the product contract to resign.
     * @param resigningMessages            A list to store any resigning messages.
     * @return The response object containing the product contract ID and resigning messages
     */
    @ExecutionTimeLogger
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ProductContractDataModificationResponse executeResign(LocalDate signingDate, ProductContract targetProductContract, ProductContractDetails targetProductContractDetails, List<String> resigningMessages) {
        Long productContractId = targetProductContract.getId();

        Optional<ProductContractDetails> versionBeforeSigned = productContractDetailsRepository
                .findVersionBeforeSigned(productContractId, signingDate, PageRequest.of(0, 1))
                .stream()
                .findFirst();
        if (versionBeforeSigned.isEmpty()) {
            log.debug("Resigning can't be executed, contract version before signing not found;");
            newPopupMessage("Resigning can't be executed, contract version before signing not found;", resigningMessages);
            return new ProductContractDataModificationResponse(productContractId, resigningMessages);
        }

        LocalDate calculatedResigningDate;
        ProductContractDetails productContractDetailsBeforeSigned = versionBeforeSigned.get();
        SupplyActivation supplyActivationBeforeSigned = productContractDetailsBeforeSigned.getSupplyActivationAfterContractResigning();
        switch (supplyActivationBeforeSigned) {
            case FIRST_DAY_OF_MONTH ->
                    calculatedResigningDate = signingDate.with(TemporalAdjusters.firstDayOfNextMonth());
            case MANUAL -> {
                log.debug("Resigning can't be executed for current contract because resigning is MANUAL;");
                newPopupMessage("Resigning can't be executed for current contract because resigning is MANUAL;", resigningMessages);
                return new ProductContractDataModificationResponse(productContractId, resigningMessages);
            }
            case EXACT_DATE -> calculatedResigningDate = productContractDetailsBeforeSigned.getSupplyActivationDate();
            default -> calculatedResigningDate = signingDate;
        }

        Optional<CustomerDetails> customerDetailsOptional = customerDetailsRepository
                .findById(targetProductContractDetails.getCustomerDetailId());
        if (customerDetailsOptional.isEmpty()) {
            log.debug("Resigning can't be executed because customer with presented details id: [%s] not found;");
            newPopupMessage("Resigning can't be executed because customer with presented details id: [%s] not found;".formatted(targetProductContractDetails.getCustomerDetailId()), resigningMessages);
            return new ProductContractDataModificationResponse(productContractId, resigningMessages);
        }

        Map<PointOfDelivery, Map<ProductContractDetails, ContractPods>> productContractDetailsThatShouldToBeSignedInFuture = contractPodRepository
                .findAllByContractIdAndStatusInMergeWithPods(productContractId, List.of(EntityStatus.ACTIVE))
                .stream()
                .collect(
                        Collectors.groupingBy(
                                ProductContractDetailsMapWithPods::getPointOfDelivery,
                                Collectors.toMap(ProductContractDetailsMapWithPods::getProductContractDetails, ProductContractDetailsMapWithPods::getContractPods)
                        )
                );

        List<ProductContractResigningWithCustomerAndPointOfDeliveryIntersectionMiddleResponse> filteredListOfPodsIntersectionAndCustomer =
                productContractDetailsRepository
                        .findProductContractDetailsWithCustomerAndPointOfDeliveryIntersection(
                                productContractId,
                                customerDetailsOptional.get().getCustomerId(),
                                signingDate,
                                productContractDetailsThatShouldToBeSignedInFuture
                                        .keySet()
                                        .stream()
                                        .map(PointOfDelivery::getId)
                                        .map(String::valueOf)
                                        .collect(Collectors.joining(","))
                        );

        if (filteredListOfPodsIntersectionAndCustomer.isEmpty()) {
            log.debug("Resigning can not be executed for current contract because valid contract for signing isn't exists;");
            newPopupMessage("Resigning can not be executed for current contract because valid contract for signing isn't exists;", resigningMessages);
            return new ProductContractDataModificationResponse(productContractId, resigningMessages);
        }

        HashMap<ProductContract, ResigningDataMapModel> productContractThatShouldToBeResignedMap =
                mapFilteredListOfPodsIntersectionAndCustomer(calculatedResigningDate, filteredListOfPodsIntersectionAndCustomer);

        switch (productContractDetailsBeforeSigned.getWaitContractExpire()) {
            case YES -> productContractThatShouldToBeResignedMap
                    .entrySet()
                    .removeIf(productContractResigningDataMapModelEntry -> {
                        ProductContract resignedContract = productContractResigningDataMapModelEntry.getKey();
                        LocalDate perpetuityDate = resignedContract.getPerpetuityDate();
                        if (Objects.isNull(perpetuityDate) || perpetuityDate.isAfter(signingDate)) {
                            if (Objects.isNull(resignedContract.getContractTermEndDate())) {
                                return true;
                            } else {
                                productContractResigningDataMapModelEntry
                                        .getValue()
                                        .getPodsMap()
                                        .forEach((productContractDetails, pointOfDeliveryContractPodsMap) -> {
                                            LocalDate initialTermDate = resignedContract.getContractTermEndDate();

                                            if (!initialTermDate.isBefore(productContractResigningDataMapModelEntry.getValue().getCalculatedSigningDate())) {
                                                switch (productContractDetailsBeforeSigned.getSupplyActivationAfterContractResigning()) {
                                                    case FIRST_DAY_OF_MONTH ->
                                                            productContractResigningDataMapModelEntry.getValue().setCalculatedSigningDate(resignedContract.getContractTermEndDate().with(TemporalAdjusters.firstDayOfNextMonth()));
                                                    case EXACT_DATE ->
                                                            productContractResigningDataMapModelEntry.getValue().setCalculatedSigningDate(resignedContract.getContractTermEndDate().plusDays(1));
                                                }
                                            }
                                        });
                            }
                        }
                        return false;
                    });
            case NO -> {
                /**
                 * Just continue execution
                 */
            }
        }

        startResigningProcess(productContractId, productContractThatShouldToBeResignedMap, productContractDetailsThatShouldToBeSignedInFuture, resigningMessages);
        updateProductContractStatusAfterResigning(targetProductContract, targetProductContractDetails);

        return new ProductContractDataModificationResponse(productContractId, resigningMessages);
    }

    /**
     * Updates the contract status and details after resigning a product contract.
     *
     * @param productContract        The product contract to be updated.
     * @param productContractDetails The product contract details associated with the contract.
     */
    private void updateProductContractStatusAfterResigning(ProductContract productContract, ProductContractDetails productContractDetails) {
        LocalDate now = LocalDate.now();

        List<ContractPods> productContractDetailsContractPods = contractPodRepository
                .getPodsThatHaveActivationDateInContractAnyVersion(productContract.getId(), EntityStatus.ACTIVE);

        if (CollectionUtils.isNotEmpty(productContractDetailsContractPods)) {
            List<ContractPods> productContractPointOfDeliveriesEnteredIntoForce = productContractDetailsContractPods
                    .stream()
                    .filter(contractPods -> Objects.nonNull(contractPods.getActivationDate()))
                    .filter(contractPods -> (!contractPods.getActivationDate().isAfter(now)))
                    .toList();

            if (CollectionUtils.isNotEmpty(productContractPointOfDeliveriesEnteredIntoForce)) {
                productContract.setContractStatus(ContractDetailsStatus.ACTIVE_IN_TERM);
                productContract.setSubStatus(ContractDetailsSubStatus.DELIVERY);
            }

            ContractPods contractPodWithMinActivationDate =
                    productContractDetailsContractPods
                            .stream()
                            .min(Comparator.comparing(ContractPods::getActivationDate))
                            .orElseThrow(() -> new ClientException("Cannot calculate Product Contract Point Of Deliveries minimal activation date", ErrorCode.APPLICATION_ERROR));

            ContractEntryIntoForce entryIntoForce = productContractDetails.getEntryIntoForce();
            LocalDate minActivationDate = contractPodWithMinActivationDate.getActivationDate();
            if (entryIntoForce != null) {
                switch (entryIntoForce) {
                    case DATE_CHANGE_OF_CBG, FIRST_DELIVERY -> {
                        if (productContract.getEntryIntoForceDate() == null) {
                            productContract.setEntryIntoForceDate(minActivationDate);
                        }
                    }
                }
            }

            productContract.setActivationDate(minActivationDate);

            StartOfContractInitialTerm startInitialTerm = productContractDetails.getStartInitialTerm();
            if (startInitialTerm != null) {
                switch (startInitialTerm) {
                    case DATE_OF_CHANGE_OF_CBG, FIRST_DELIVERY -> {
                        if (productContract.getInitialTermDate() == null) {
                            productContract.setInitialTermDate(minActivationDate);

                            Long productContractTermId = productContractDetails.getProductContractTermId();

                            ProductContractTerms productContractTerms = productContractTermRepository
                                    .findById(productContractTermId)
                                    .orElseThrow(() -> new DomainEntityNotFoundException("Product Contract Term with id: [%s] not found;".formatted(productContractTermId)));

                            ProductTermType termType = productContractTerms.getType();
                            Integer period = productContractTerms.getValue();
                            if (period != null) {
                                switch (termType) {
                                    case DAY_DAYS -> {
                                        productContract.setContractTermEndDate(minActivationDate.plusDays(period));
                                    }
                                    case MONTH_MONTHS -> {
                                        productContract.setContractTermEndDate(minActivationDate.plusMonths(period));
                                    }
                                    case YEAR_YEARS -> {
                                        productContract.setContractTermEndDate(minActivationDate.plusYears(period));
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Starts the resigning process for a new product contract.
     *
     * @param newProductContractId      the ID of the new product contract to which resigning is being performed
     * @param oldProductContractsMap    a map of old product contracts and their corresponding resigning data
     * @param newProductContractDetails a map of new product contract details for each point of delivery
     * @param resigningMessages         a list to store any resigning messages
     * @throws IllegalArgumentsProvidedException if any illegal arguments are provided
     */
    public void startResigningProcess(Long newProductContractId, HashMap<ProductContract, ResigningDataMapModel> oldProductContractsMap, Map<PointOfDelivery, Map<ProductContractDetails, ContractPods>> newProductContractDetails, List<String> resigningMessages) throws IllegalArgumentsProvidedException {
        List<Long> resignedContractIds = new ArrayList<>();

        contractFlag:
        for (Map.Entry<ProductContract, ResigningDataMapModel> oldProductContracts : oldProductContractsMap.entrySet()) {
            ProductContract oldProductContract = oldProductContracts.getKey();
            ResigningDataMapModel oldProductContractResigningDataMapModel = oldProductContracts.getValue();
            LocalDate calculatedSigningDate = oldProductContractResigningDataMapModel.getCalculatedSigningDate();

            for (Map.Entry<ProductContractDetails, Map<PointOfDelivery, ContractPods>> oldProductContractsEntry : oldProductContractResigningDataMapModel.getPodsMap().entrySet()) {
                List<Runnable> uncommittedActionsContext = new ArrayList<>();

                Map<PointOfDelivery, ContractPods> resignedProductContractPointOfDeliveryContractPodsMap = oldProductContractsEntry.getValue();
                for (Map.Entry<PointOfDelivery, ContractPods> pointOfDeliveryContractPodsEntry : resignedProductContractPointOfDeliveryContractPodsMap.entrySet()) {
                    PointOfDelivery resignedPod = pointOfDeliveryContractPodsEntry.getKey();
                    ContractPods oldPod = pointOfDeliveryContractPodsEntry.getValue();
                    boolean isRespectiveOldPodActiveInPresent = Stream.of(oldPod.getActivationDate(), oldPod.getDeactivationDate()).allMatch(Objects::isNull);
                    if (isRespectiveOldPodActiveInPresent) {
                        /*
                          If point of delivery is not active in any version, skip this contract
                         */
                        continue contractFlag;
                    }

                    Optional<Map.Entry<PointOfDelivery, Map<ProductContractDetails, ContractPods>>> respectiveDetailsForPointOfDeliveryOptional =
                            findRespectiveDetailsForPointOfDelivery(resignedPod, newProductContractDetails);
                    if (respectiveDetailsForPointOfDeliveryOptional.isEmpty()) {
                        newPopupMessage("Resigning can not be executed for current contract because respective version not found for Point Of Delivery with identifier:[%s]".formatted(resignedPod.getIdentifier()), resigningMessages);
                        continue contractFlag;
                    }

                    Map.Entry<PointOfDelivery, Map<ProductContractDetails, ContractPods>> respectiveDetailsForPointOfDelivery = respectiveDetailsForPointOfDeliveryOptional.get();
                    Optional<Map.Entry<ProductContractDetails, ContractPods>> respectiveProductContractDetailOptional =
                            findRespectiveDetailsDependingOnCalculatedSigningDate(respectiveDetailsForPointOfDeliveryOptional.get(), calculatedSigningDate);
                    if (respectiveProductContractDetailOptional.isEmpty()) {
                        newPopupMessage("Resigning can not be executed for current contract because respective version not found for Point Of Delivery with identifier:[%s]".formatted(resignedPod.getIdentifier()), resigningMessages);
                        continue contractFlag;
                    }

                    Map.Entry<ProductContractDetails, ContractPods> respectiveProductContractDetail = respectiveProductContractDetailOptional.get();

                    if (!isPodRespectiveVersionForThisContract(oldPod, calculatedSigningDate)) {
                        newPopupMessage("Resigning can not be executed for current contract because respective version not found for Point Of Delivery with identifier:[%s]".formatted(resignedPod.getIdentifier()), resigningMessages);
                        continue contractFlag;
                    }

                    boolean isPointOfDeliveryActiveInAnyOtherDetailsInFuture =
                            contractPodRepository
                                    .havePointOfDeliveryFutureActivationDate(resignedPod.getId(), oldProductContract.getId(), calculatedSigningDate);
                    if (isPointOfDeliveryActiveInAnyOtherDetailsInFuture) {
                        newPopupMessage("Resigning can not be executed for current contract because Point Of Delivery with identifier:[%s] is activated in another contract".formatted(resignedPod.getIdentifier()), resigningMessages);
                        continue contractFlag;
                    }

                    ContractPods newContractPod = respectiveProductContractDetail.getValue();
                    uncommittedActionsContext.add(() -> newContractPod.setActivationDate(calculatedSigningDate));
                    uncommittedActionsContext.add(() -> newContractPod.setCustomModifyDate(LocalDateTime.now()));

                    boolean isPointOfDeliveryActiveInPerpetuity = oldPod.getDeactivationDate() == null;
                    if (isPointOfDeliveryActiveInPerpetuity) {
                        activatePointOfDeliveryInPerpetuitySplittedByNewProductContractVersions(uncommittedActionsContext, oldPod, calculatedSigningDate, respectiveDetailsForPointOfDelivery, newContractPod);
                    } else {
                        List<Map.Entry<ProductContractDetails, ContractPods>> newProductContractFutureVersionsFromSigningDate =
                                respectiveDetailsForPointOfDelivery
                                        .getValue()
                                        .entrySet()
                                        .stream()
                                        .filter(newProductContractDetail -> !newProductContractDetail.getKey().getStartDate().isBefore(calculatedSigningDate))
                                        .sorted(Comparator.comparing(productContractDetailsContractPodsEntry -> productContractDetailsContractPodsEntry.getKey().getStartDate()))
                                        .toList();

                        List<ContractPods> oldProductContractFuturePointOfDeliveries =
                                contractPodRepository
                                        .findFutureContractPodsForResigning(oldProductContract.getId(), resignedPod.getId(), calculatedSigningDate);

                        if (hasOldProductContractFuturePointOfDeliveriesGap(oldPod, oldProductContractFuturePointOfDeliveries)) {
                            newPopupMessage("Resigning can not be executed for current contract because Point Of Delivery with identifier:[%s] has gap in old contract".formatted(resignedPod.getIdentifier()), resigningMessages);
                            continue contractFlag;
                        }

                        boolean hasPointOfDeliveryAnyFutureVersions = CollectionUtils.isNotEmpty(oldProductContractFuturePointOfDeliveries);
                        if (hasPointOfDeliveryAnyFutureVersions) {
                            LocalDate maxDeactivationDate = calculateMaxDeactivationDateIncludingPerpetuity(oldProductContractFuturePointOfDeliveries);
                            if (CollectionUtils.isNotEmpty(newProductContractFutureVersionsFromSigningDate)) {
                                uncommittedActionsContext.add(() -> newContractPod.setDeactivationDate(newProductContractFutureVersionsFromSigningDate.get(0).getKey().getStartDate().minusDays(1)));
                                uncommittedActionsContext.add(() -> newContractPod.setCustomModifyDate(LocalDateTime.now()));
                            } else {
                                uncommittedActionsContext.add(() -> newContractPod.setDeactivationDate(maxDeactivationDate));
                                uncommittedActionsContext.add(() -> newContractPod.setCustomModifyDate(LocalDateTime.now()));
                            }

                            boolean isAnyFuturePodActive =
                                    oldProductContractFuturePointOfDeliveries
                                            .stream()
                                            .anyMatch(cp -> Objects.nonNull(cp.getActivationDate()));
                            if (!isAnyFuturePodActive) {
                            /*
                              If point of delivery is not active in any version, skip this contract
                             */
                                continue contractFlag;
                            }

                            boolean isPointOfDeliveryActiveInPerpetuityDependingOnFutureVersions = maxDeactivationDate == null;
                            if (isPointOfDeliveryActiveInPerpetuityDependingOnFutureVersions) {
                                splitPointOfDeliveryActivationDatesByNewProductContractDetails(newProductContractFutureVersionsFromSigningDate, uncommittedActionsContext);
                            } else {
                                splitPointOfDeliveryActivationDatesByNewProductContractDetailsDependingOnMaxDeactivationDate(newProductContractFutureVersionsFromSigningDate, maxDeactivationDate, uncommittedActionsContext);
                            }

                            oldProductContractFuturePointOfDeliveries.forEach(contractPods -> {
                                uncommittedActionsContext.add(() -> contractPods.setActivationDate(null));
                                uncommittedActionsContext.add(() -> contractPods.setDeactivationDate(null));
                                uncommittedActionsContext.add(() -> contractPods.setCustomModifyDate(LocalDateTime.now()));
                            });
                        } else {
                            activatePointOfDeliveryWithoutFutureVersionsIntoNewProductContractDetails(oldPod, newProductContractFutureVersionsFromSigningDate, uncommittedActionsContext, newContractPod);
                        }

                        uncommittedActionsContext.add(() -> oldPod.setDeactivationDate(calculatedSigningDate.minusDays(1)));
                        uncommittedActionsContext.add(() -> oldPod.setCustomModifyDate(LocalDateTime.now()));
                    }

                    uncommittedActionsContext.add(() -> oldProductContract.setResignedTo(newProductContractId));
                    uncommittedActionsContext.add(() -> resignedContractIds.add(oldProductContract.getId()));
                }

                uncommittedActionsContext.forEach(Runnable::run);
            }
        }

        if (CollectionUtils.isNotEmpty(resignedContractIds)) {
            List<ProductContractResignedContracts> resignedContractList = resignedContractIds
                    .stream()
                    .distinct()
                    .map(resignedContractId -> new ProductContractResignedContracts(null, newProductContractId, resignedContractId))
                    .toList();

            productContractResignedContractsRepository.saveAll(resignedContractList);
        }
    }

    /**
     * Splits the point of delivery activation dates by new product contract details depending on the maximum deactivation date.
     *
     * @param newProductContractFutureVersionsFromSigningDate a list of new product contract future versions from the signing date
     * @param maxDeactivationDate                             the maximum deactivation date
     * @param uncommittedActionsContext                       a list of uncommitted actions
     */
    private void splitPointOfDeliveryActivationDatesByNewProductContractDetailsDependingOnMaxDeactivationDate(List<Map.Entry<ProductContractDetails, ContractPods>> newProductContractFutureVersionsFromSigningDate, LocalDate maxDeactivationDate, List<Runnable> uncommittedActionsContext) {
        List<Map.Entry<ProductContractDetails, ContractPods>> newProductContractsWithinDeactivationDate =
                newProductContractFutureVersionsFromSigningDate
                        .stream()
                        .filter(productContractDetailsContractPodsEntry -> productContractDetailsContractPodsEntry.getKey().getStartDate().isBefore(maxDeactivationDate))
                        .toList();

        splitPointOfDeliveryActivationsBetweenNewProductContractDetails(maxDeactivationDate, uncommittedActionsContext, newProductContractsWithinDeactivationDate);
    }

    /**
     * Split point of delivery activations between new product contract details.
     *
     * @param maxDeactivationDate                       The maximum deactivation date.
     * @param uncommittedActionsContext                 The list to store the uncommitted actions.
     * @param newProductContractsWithinDeactivationDate The list of new product contract details along with their contract pods.
     */
    private void splitPointOfDeliveryActivationsBetweenNewProductContractDetails(LocalDate maxDeactivationDate, List<Runnable> uncommittedActionsContext, List<Map.Entry<ProductContractDetails, ContractPods>> newProductContractsWithinDeactivationDate) {
        for (int i = 0; i < newProductContractsWithinDeactivationDate.size(); i++) {
            Map.Entry<ProductContractDetails, ContractPods> newProductContractFutureVersionFromSigningDateEntry = newProductContractsWithinDeactivationDate.get(i);

            ProductContractDetails newProductContractFutureVersionFromSigningDate = newProductContractFutureVersionFromSigningDateEntry.getKey();
            ContractPods newProductContractFutureVersionFromSigningDatePointOfDelivery = newProductContractFutureVersionFromSigningDateEntry.getValue();
            boolean isLastElement = (i == newProductContractsWithinDeactivationDate.size() - 1);
            if (!isLastElement) {
                findNextVersionAndCalculateCurrentVersionsPointOfDeliveryDeactivationDate(uncommittedActionsContext, newProductContractsWithinDeactivationDate, i, newProductContractFutureVersionFromSigningDate, newProductContractFutureVersionFromSigningDatePointOfDelivery);
            } else {
                uncommittedActionsContext.add(() -> newProductContractFutureVersionFromSigningDatePointOfDelivery.setActivationDate(newProductContractFutureVersionFromSigningDate.getStartDate()));
                uncommittedActionsContext.add(() -> newProductContractFutureVersionFromSigningDatePointOfDelivery.setDeactivationDate(maxDeactivationDate));
                uncommittedActionsContext.add(() -> newProductContractFutureVersionFromSigningDatePointOfDelivery.setCustomModifyDate(LocalDateTime.now()));
            }
        }
    }

    /**
     * This method is used to find the next version of a product contract and calculate the deactivation date of the current versions point of delivery.
     *
     * @param uncommittedActionsContext                                     A list of Runnable objects representing the uncommitted actions context.
     * @param newProductContractsWithinDeactivationDate                     A list of Map.Entry objects representing the new product contracts within the deactivation date.
     * @param i                                                             The index of the current product contract within the list.
     * @param newProductContractFutureVersionFromSigningDate                The product contract for the future version from the signing date.
     * @param newProductContractFutureVersionFromSigningDatePointOfDelivery The point of delivery for the future version from the signing date.
     */
    private void findNextVersionAndCalculateCurrentVersionsPointOfDeliveryDeactivationDate(List<Runnable> uncommittedActionsContext, List<Map.Entry<ProductContractDetails, ContractPods>> newProductContractsWithinDeactivationDate, int i, ProductContractDetails newProductContractFutureVersionFromSigningDate, ContractPods newProductContractFutureVersionFromSigningDatePointOfDelivery) {
        Map.Entry<ProductContractDetails, ContractPods> newProductContractFutureVersionFromSigningDateNextVersionEntry = newProductContractsWithinDeactivationDate.get(i + 1);

        uncommittedActionsContext.add(() -> newProductContractFutureVersionFromSigningDatePointOfDelivery.setActivationDate(newProductContractFutureVersionFromSigningDate.getStartDate()));
        uncommittedActionsContext.add(() -> newProductContractFutureVersionFromSigningDatePointOfDelivery.setDeactivationDate(newProductContractFutureVersionFromSigningDateNextVersionEntry.getKey().getStartDate().minusDays(1)));
        uncommittedActionsContext.add(() -> newProductContractFutureVersionFromSigningDatePointOfDelivery.setCustomModifyDate(LocalDateTime.now()));
    }

    /**
     * Activates a point of delivery without future versions into new product contract details.
     *
     * @param oldPod                                          The old ContractPods instance.
     * @param newProductContractFutureVersionsFromSigningDate A list of new product contract details and associated ContractPods instances.
     * @param uncommittedActionsContext                       A list of uncommitted actions to be performed.
     * @param newContractPod                                  The new ContractPods instance.
     */
    private void activatePointOfDeliveryWithoutFutureVersionsIntoNewProductContractDetails(ContractPods oldPod, List<Map.Entry<ProductContractDetails, ContractPods>> newProductContractFutureVersionsFromSigningDate, List<Runnable> uncommittedActionsContext, ContractPods newContractPod) {
        LocalDate maxDeactivationDate = oldPod.getDeactivationDate();

        List<Map.Entry<ProductContractDetails, ContractPods>> newProductContractsWithinDeactivationDate = newProductContractFutureVersionsFromSigningDate
                .stream()
                .filter(productContractDetailsContractPodsEntry -> productContractDetailsContractPodsEntry.getKey().getStartDate().isBefore(maxDeactivationDate))
                .toList();

        Optional<Map.Entry<ProductContractDetails, ContractPods>> newProductContractsFirstVersionWithingDeactivationDate = newProductContractsWithinDeactivationDate
                .stream()
                .min(Comparator.comparing(productContractDetailsContractPodsEntry -> productContractDetailsContractPodsEntry.getKey().getStartDate()));
        if (newProductContractsFirstVersionWithingDeactivationDate.isPresent()) {
            uncommittedActionsContext.add(() -> newContractPod.setDeactivationDate(newProductContractsFirstVersionWithingDeactivationDate.get().getKey().getStartDate().minusDays(1)));
            uncommittedActionsContext.add(() -> newContractPod.setCustomModifyDate(LocalDateTime.now()));
        } else {
            uncommittedActionsContext.add(() -> newContractPod.setDeactivationDate(oldPod.getDeactivationDate()));
            uncommittedActionsContext.add(() -> newContractPod.setCustomModifyDate(LocalDateTime.now()));
        }

        splitPointOfDeliveryActivationsBetweenNewProductContractDetails(maxDeactivationDate, uncommittedActionsContext, newProductContractsWithinDeactivationDate);
    }

    /**
     * Splits the point of delivery activation dates based on the new product contract details.
     *
     * @param newProductContractFutureVersionsFromSigningDate A list of map entries containing the new product contract versions and their associated contract pods.
     * @param uncommittedActionsContext                       A list of runnable actions to be executed based on the split point of delivery activation dates.
     */
    private void splitPointOfDeliveryActivationDatesByNewProductContractDetails(List<Map.Entry<ProductContractDetails, ContractPods>> newProductContractFutureVersionsFromSigningDate, List<Runnable> uncommittedActionsContext) {
        for (int i = 0; i < newProductContractFutureVersionsFromSigningDate.size(); i++) {
            Map.Entry<ProductContractDetails, ContractPods> newProductContractFutureVersionFromSigningDateEntry = newProductContractFutureVersionsFromSigningDate.get(i);

            ProductContractDetails newProductContractFutureVersionFromSigningDate = newProductContractFutureVersionFromSigningDateEntry.getKey();
            ContractPods newProductContractFutureVersionFromSigningDatePointOfDelivery = newProductContractFutureVersionFromSigningDateEntry.getValue();
            boolean isLastElement = (i == newProductContractFutureVersionsFromSigningDate.size() - 1);
            if (!isLastElement) {
                findNextVersionAndCalculateCurrentVersionsPointOfDeliveryDeactivationDate(uncommittedActionsContext, newProductContractFutureVersionsFromSigningDate, i, newProductContractFutureVersionFromSigningDate, newProductContractFutureVersionFromSigningDatePointOfDelivery);
            } else {
                uncommittedActionsContext.add(() -> newProductContractFutureVersionFromSigningDatePointOfDelivery.setActivationDate(newProductContractFutureVersionFromSigningDate.getStartDate()));
                uncommittedActionsContext.add(() -> newProductContractFutureVersionFromSigningDatePointOfDelivery.setCustomModifyDate(LocalDateTime.now()));
            }
        }
    }

    /**
     * Finds the respective details depending on the calculated signing date.
     *
     * @param newProductContractMap the map containing the product, contract details, and contract pods
     * @param calculatedSigningDate the calculated signing date
     * @return Optional containing the respective details, or empty if not found
     */
    private Optional<Map.Entry<ProductContractDetails, ContractPods>> findRespectiveDetailsDependingOnCalculatedSigningDate(Map.Entry<PointOfDelivery, Map<ProductContractDetails, ContractPods>> newProductContractMap, LocalDate calculatedSigningDate) {
        return newProductContractMap
                .getValue()
                .entrySet()
                .stream()
                .filter(newProductContractPods -> !newProductContractPods.getKey().getStartDate().isAfter(calculatedSigningDate))
                .max(Comparator.comparing((o -> o.getKey().getStartDate())));
    }

    /**
     * Finds the respective details for a given PointOfDelivery in the newProductContractDetails map.
     *
     * @param resignedPod               The PointOfDelivery for which details need to be found.
     * @param newProductContractDetails The map containing new product contract details for each PointOfDelivery.
     * @return An Optional containing the respective details of resignedPod if found, otherwise an empty Optional.
     */
    private Optional<Map.Entry<PointOfDelivery, Map<ProductContractDetails, ContractPods>>> findRespectiveDetailsForPointOfDelivery(
            PointOfDelivery resignedPod,
            Map<PointOfDelivery, Map<ProductContractDetails, ContractPods>> newProductContractDetails
    ) {
        List<Map.Entry<PointOfDelivery, Map<ProductContractDetails, ContractPods>>> newProductContractMapList = newProductContractDetails
                .entrySet()
                .stream()
                .filter(pointOfDeliveryMapEntry -> pointOfDeliveryMapEntry.getKey().getId().equals(resignedPod.getId()))
                .toList();
        if (newProductContractMapList.size() != 1) {
            return Optional.empty();
        }

        return Optional.of(newProductContractMapList.get(0));
    }

    /**
     * Activates a point of delivery in perpetuity, while splitting it into multiple
     * new product contract versions. This method updates the necessary attributes and
     * dates for the old point of delivery and the new contract pods.
     *
     * @param uncommittedActionsContext List of uncommitted actions to be performed
     * @param oldPod                    The old point of delivery
     * @param calculatedSigningDate     The calculated signing date
     * @param newProductContractMap     Entry containing the new product contract details and
     *                                  contract pods
     * @param newContractPod            The new contract pods
     */
    private void activatePointOfDeliveryInPerpetuitySplittedByNewProductContractVersions(
            List<Runnable> uncommittedActionsContext,
            ContractPods oldPod,
            LocalDate calculatedSigningDate,
            Map.Entry<PointOfDelivery, Map<ProductContractDetails, ContractPods>> newProductContractMap,
            ContractPods newContractPod) {
        uncommittedActionsContext.add(() -> oldPod.setDeactivationDate(calculatedSigningDate.minusDays(1)));
        uncommittedActionsContext.add(() -> oldPod.setCustomModifyDate(LocalDateTime.now()));

        List<Map.Entry<ProductContractDetails, ContractPods>> newProductContractFutureVersionsFromContractPodActivationDate =
                newProductContractMap
                        .getValue()
                        .entrySet()
                        .stream()
                        .filter(newProductContractDetail -> newProductContractDetail.getKey().getStartDate().isAfter(calculatedSigningDate))
                        .sorted(Comparator.comparing(productContractDetailsContractPodsEntry -> productContractDetailsContractPodsEntry.getKey().getStartDate()))
                        .toList();

        if (CollectionUtils.isNotEmpty(newProductContractFutureVersionsFromContractPodActivationDate)) {
            uncommittedActionsContext.add(() -> newContractPod.setDeactivationDate(newProductContractFutureVersionsFromContractPodActivationDate.get(0).getKey().getStartDate().minusDays(1)));
            uncommittedActionsContext.add(() -> newContractPod.setCustomModifyDate(LocalDateTime.now()));

            for (int i = 0; i < newProductContractFutureVersionsFromContractPodActivationDate.size(); i++) {
                Map.Entry<ProductContractDetails, ContractPods> newProductContractFutureVersionContractDetailsMap = newProductContractFutureVersionsFromContractPodActivationDate.get(i);
                ContractPods newProductContractFutureVersionContractPod = newProductContractFutureVersionContractDetailsMap.getValue();

                boolean isLastElement = i == (newProductContractFutureVersionsFromContractPodActivationDate.size() - 1);
                if (!isLastElement) {
                    Map.Entry<ProductContractDetails, ContractPods> nextProductContractFutureVersion =
                            newProductContractFutureVersionsFromContractPodActivationDate.get(i + 1);

                    boolean isFirstElement = i == 0;
                    if (isFirstElement) {
                        uncommittedActionsContext.add(() -> newProductContractFutureVersionContractPod.setActivationDate(newContractPod.getDeactivationDate().plusDays(1)));
                        uncommittedActionsContext.add(() -> newProductContractFutureVersionContractPod.setDeactivationDate(nextProductContractFutureVersion.getKey().getStartDate().minusDays(1)));
                        uncommittedActionsContext.add(() -> newProductContractFutureVersionContractPod.setCustomModifyDate(LocalDateTime.now()));
                    } else {
                        uncommittedActionsContext.add(() -> newProductContractFutureVersionContractPod.setActivationDate(newProductContractFutureVersionContractDetailsMap.getKey().getStartDate()));
                        uncommittedActionsContext.add(() -> newProductContractFutureVersionContractPod.setDeactivationDate(nextProductContractFutureVersion.getKey().getStartDate().minusDays(1)));
                        uncommittedActionsContext.add(() -> newProductContractFutureVersionContractPod.setCustomModifyDate(LocalDateTime.now()));
                    }
                } else {
                    uncommittedActionsContext.add(() -> newProductContractFutureVersionContractPod.setActivationDate(newProductContractFutureVersionContractDetailsMap.getKey().getStartDate()));
                    uncommittedActionsContext.add(() -> newProductContractFutureVersionContractPod.setCustomModifyDate(LocalDateTime.now()));
                }
            }
        }
    }

    /**
     * Calculate the maximum deactivation date, including perpetuity, from the list of old product contract future point of deliveries.
     *
     * @param oldProductContractFuturePointOfDeliveries a list of ContractPods representing the old product contract future point of deliveries
     * @return the maximum deactivation date, including perpetuity
     */
    private LocalDate calculateMaxDeactivationDateIncludingPerpetuity(List<ContractPods> oldProductContractFuturePointOfDeliveries) {
        if (CollectionUtils.isEmpty(oldProductContractFuturePointOfDeliveries)) {
            return null;
        }

        if (oldProductContractFuturePointOfDeliveries.stream().anyMatch(contractPods -> contractPods.getDeactivationDate() == null)) {
            return null;
        } else {
            return oldProductContractFuturePointOfDeliveries
                    .stream()
                    .max(Comparator.comparing(ContractPods::getDeactivationDate))
                    .get()
                    .getDeactivationDate();
        }
    }

    /**
     * Checks if there is a gap between the deactivation date and activation dates
     *
     * @param oldPod                                    the old pod to compare with
     * @param oldProductContractFuturePointOfDeliveries the list of old product contract future point of deliveries
     * @return true if there is a gap, false otherwise
     */
    private boolean hasOldProductContractFuturePointOfDeliveriesGap(ContractPods oldPod, List<ContractPods> oldProductContractFuturePointOfDeliveries) {
        if (CollectionUtils.isNotEmpty(oldProductContractFuturePointOfDeliveries)) {
            ContractPods firstFuturePointOfDelivery = oldProductContractFuturePointOfDeliveries.get(0);
            if (ChronoUnit.DAYS.between(oldPod.getDeactivationDate(), firstFuturePointOfDelivery.getActivationDate()) > 1) {
                return true;
            }

            for (int i = 0; i < oldProductContractFuturePointOfDeliveries.size() - 1; i++) {
                ContractPods current = oldProductContractFuturePointOfDeliveries.get(i);
                ContractPods next = oldProductContractFuturePointOfDeliveries.get(i + 1);

                if (ChronoUnit.DAYS.between(current.getDeactivationDate(), next.getActivationDate()) > 1) {
                    return true;
                }

                i++;
            }
        }
        return false;
    }

    /**
     * Checks if the given calculatedSigningDate is within the range specified by the activationDate and deactivationDate
     *
     * @param resignedContractPod   The resigned contract pod object
     * @param calculatedSigningDate The calculated signing date to be checked
     * @return true if the calculatedSigningDate is within the range specified by the activationDate and deactivationDate, false otherwise
     */
    private boolean isPodRespectiveVersionForThisContract(ContractPods resignedContractPod, LocalDate calculatedSigningDate) {
        LocalDate activationDate = resignedContractPod.getActivationDate();
        LocalDate deactivationDate = resignedContractPod.getDeactivationDate();

        if (Stream.of(activationDate, deactivationDate).noneMatch(Objects::isNull)) {
            return isDateInRange(calculatedSigningDate, activationDate, deactivationDate);
        } else {
            if (deactivationDate == null) {
                if (activationDate != null) {
                    return activationDate.isBefore(calculatedSigningDate);
                }
            }
        }

        return false;
    }

    /**
     * Checks if a given date is within a specified date range.
     *
     * @param dateToCheck The date to be checked.
     * @param startDate   The start date of the date range (inclusive).
     * @param endDate     The end date of the date range (inclusive).
     * @return {@code true} if the date is within the range, {@code false} otherwise.
     */
    public Boolean isDateInRange(LocalDate dateToCheck, LocalDate startDate, LocalDate endDate) {
        if (Stream.of(dateToCheck, startDate, endDate).anyMatch(Objects::isNull)) {
            return false;
        }

        return !dateToCheck.isBefore(startDate) && !dateToCheck.isAfter(endDate);
    }

    /**
     * Maps a filtered list of ProductContractResigningWithCustomerAndPointOfDeliveryIntersectionMiddleResponse objects
     * to a HashMap of ProductContract and ResigningDataMapModel objects.
     *
     * @param resigningDate                             The resigning date used for mapping the ResigningDataMapModel.
     * @param filteredListOfPodsIntersectionAndCustomer The filtered list of ProductContractResigningWithCustomerAndPointOfDeliveryIntersectionMiddleResponse to be mapped.
     * @return A HashMap of ProductContract and ResigningDataMapModel objects.
     * @throws DomainEntityNotFoundException if any of the required entities are not found in the repositories.
     */
    private HashMap<ProductContract, ResigningDataMapModel> mapFilteredListOfPodsIntersectionAndCustomer(LocalDate resigningDate, List<ProductContractResigningWithCustomerAndPointOfDeliveryIntersectionMiddleResponse> filteredListOfPodsIntersectionAndCustomer) {
        HashMap<ProductContract, ResigningDataMapModel> map = new HashMap<>();

        for (ProductContractResigningWithCustomerAndPointOfDeliveryIntersectionMiddleResponse x : filteredListOfPodsIntersectionAndCustomer) {
            ProductContractDetails productContractDetails = productContractDetailsRepository
                    .findById(x.getContractDetailId())
                    .orElseThrow(() -> new DomainEntityNotFoundException("Product Contract Detail with presented id: [%s] not found".formatted(x.getContractDetailId())));

            ContractPods contractPods = contractPodRepository
                    .findById(x.getContractPointOfDeliveryId())
                    .orElseThrow(() -> new DomainEntityNotFoundException("Contract POD with presented id: [%s] not found".formatted(x.getContractPointOfDeliveryId())));

            PointOfDelivery pointOfDelivery = pointOfDeliveryRepository
                    .findById(x.getPodId())
                    .orElseThrow(() -> new DomainEntityNotFoundException("Point Of Delivery with presented id: [%s] not found".formatted(x.getPodId())));

            ProductContract productContract = productContractRepository
                    .findById(x.getContractId())
                    .orElseThrow(() -> new DomainEntityNotFoundException("Product Contract with id: [%s] not found".formatted(x.getContractId())));

            if (map.containsKey(productContract)) {
                if (map.get(productContract).getPodsMap().containsKey(productContractDetails)) {
                    map.get(productContract).getPodsMap().get(productContractDetails).put(pointOfDelivery, contractPods);
                } else {
                    map.get(productContract).getPodsMap().put(productContractDetails, new HashMap<>(
                            Map.of(
                                    pointOfDelivery, contractPods
                            )));
                }
            } else {
                map.put(productContract, new ResigningDataMapModel(
                        resigningDate,
                        new HashMap<>(
                                Map.of(
                                        productContractDetails, new HashMap<>(
                                                Map.of(
                                                        pointOfDelivery,
                                                        contractPods
                                                )
                                        )
                                )
                        )
                ));
            }
        }

        return map;
    }

    /**
     * Adds a new popup message to the given list of popupMessages.
     *
     * @param message       the message to be added (may not be blank)
     * @param popupMessages the list of popup messages to add the new message to
     */
    private void newPopupMessage(String message, List<String> popupMessages) {
        if (StringUtils.isNotBlank(message)) {
            StringBuilder messageBuilder = new StringBuilder(message);
            popupMessages.add(messageBuilder.charAt(message.length() - 1) != ';' ? messageBuilder.append(";").toString() : message);
        }
    }
}
