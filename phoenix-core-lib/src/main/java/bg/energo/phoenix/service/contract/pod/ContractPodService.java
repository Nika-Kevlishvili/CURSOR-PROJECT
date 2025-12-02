package bg.energo.phoenix.service.contract.pod;


import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.IllegalArgumentsProvidedException;
import bg.energo.phoenix.exception.OperationNotAllowedException;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.contract.action.Action;
import bg.energo.phoenix.model.entity.contract.billing.ContractPods;
import bg.energo.phoenix.model.entity.contract.product.ProductContract;
import bg.energo.phoenix.model.entity.contract.product.ProductContractDetails;
import bg.energo.phoenix.model.entity.pod.pod.PointOfDelivery;
import bg.energo.phoenix.model.entity.product.product.ProductContractTerms;
import bg.energo.phoenix.model.enums.contract.action.ActionPenaltyPayer;
import bg.energo.phoenix.model.enums.contract.express.ProductContractVersionStatus;
import bg.energo.phoenix.model.enums.contract.products.ContractDetailsStatus;
import bg.energo.phoenix.model.enums.contract.products.ContractDetailsSubStatus;
import bg.energo.phoenix.model.enums.contract.products.ProductContractStatus;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.pod.pod.PodStatus;
import bg.energo.phoenix.model.enums.product.product.ProductTermPeriodType;
import bg.energo.phoenix.model.enums.product.term.terms.ContractEntryIntoForce;
import bg.energo.phoenix.model.enums.product.term.terms.StartOfContractInitialTerm;
import bg.energo.phoenix.model.request.contract.pod.*;
import bg.energo.phoenix.model.response.contract.productContract.ProductContractVersionResponse;
import bg.energo.phoenix.repository.contract.action.ActionRepository;
import bg.energo.phoenix.repository.contract.billing.ContractPodRepository;
import bg.energo.phoenix.repository.contract.product.ProductContractDetailsRepository;
import bg.energo.phoenix.repository.contract.product.ProductContractRepository;
import bg.energo.phoenix.repository.nomenclature.contract.DeactivationPurposeRepository;
import bg.energo.phoenix.repository.pod.pod.PointOfDeliveryDetailsRepository;
import bg.energo.phoenix.repository.pod.pod.PointOfDeliveryRepository;
import bg.energo.phoenix.repository.product.product.ProductContractTermRepository;
import bg.energo.phoenix.security.PermissionService;
import bg.energo.phoenix.service.contract.product.ProductContractBillingLockValidationService;
import bg.energo.phoenix.service.contract.product.dealCreationEvent.ProductContractDealCreationEvent;
import bg.energo.phoenix.service.contract.product.dealCreationEvent.ProductContractDealCreationEventPublisher;
import bg.energo.phoenix.service.customer.statusChangeEvent.CustomerStatusChangeEventPublisher;
import bg.energo.phoenix.util.epb.EPBChainedExceptionTriggerUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static bg.energo.phoenix.permissions.PermissionContextEnum.PRODUCT_CONTRACTS;
import static bg.energo.phoenix.permissions.PermissionEnum.PRODUCT_CONTRACT_EDIT_LOCKED;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContractPodService {
    private final PointOfDeliveryRepository pointOfDeliveryRepository;
    private final PointOfDeliveryDetailsRepository pointOfDeliveryDetailsRepository;
    private final ContractPodRepository contractPodRepository;
    private final ProductContractDetailsRepository contractDetailsRepository;
    private final ProductContractRepository productContractRepository;
    private final DeactivationPurposeRepository deactivationPurposeRepository;
    private final ProductContractTermRepository productContractTermRepository;
    private final ProductContractDetailsRepository productContractDetailsRepository;
    private final CustomerStatusChangeEventPublisher customerStatusChangeEventPublisher;
    private final ProductContractDealCreationEventPublisher productContractDealCreationEventPublisher;
    private final ProductContractBillingLockValidationService productContractBillingLockValidationService;
    private final PermissionService permissionService;
    private final ActionRepository actionRepository;
    @Value("${nomenclature.deactivation.purpose.EPES.id}")
    private Long energoPurposeId;
    @Value("${nomenclature.deactivation.purpose.customer.id}")
    private Long customerPurposeId;
    @Value("${nomenclature.deactivation.purpose.other.id}")
    private Long otherPurposeId;


    @Transactional
    public void startManualActivation(PodManualActivationRequest request) {
        List<String> errorMessagesContext = new ArrayList<>();
        Optional<ProductContractDetails> productContractDetailsOptional = contractDetailsRepository.findById(request.getContractDetailId());
        if (productContractDetailsOptional.isEmpty()) {
            throw new DomainEntityNotFoundException("contractDetailId-Contract detail found;");
        }

        ProductContractDetails productContractDetails = productContractDetailsOptional.get();

        if (productContractDetails.getVersionStatus() != ProductContractVersionStatus.SIGNED) {
            throw new OperationNotAllowedException("Operation dont allowed");
        }
        Boolean hasEditLockedPermission = permissionService.permissionContextContainsPermissions(
                PRODUCT_CONTRACTS,
                List.of(PRODUCT_CONTRACT_EDIT_LOCKED));


        if (!productContractRepository.existsForActivation(productContractDetails.getContractId(), request.getActivationDate())) {
            throw new IllegalArgumentException("contractDetailId-You can not activate Point Of Delivery for this contract, contract status must be [%s(%s, %s)/%s/%s/%s];"
                    .formatted(
                            ContractDetailsStatus.SIGNED,
                            ContractDetailsSubStatus.SIGNED_BY_BOTH_SIDES,
                            ContractDetailsSubStatus.SPECIAL_PROCESSES,
                            ContractDetailsStatus.ENTERED_INTO_FORCE,
                            ContractDetailsStatus.ACTIVE_IN_TERM,
                            ContractDetailsStatus.ACTIVE_IN_PERPETUITY
                    ));
        }

        if (!pointOfDeliveryDetailsRepository.existsByIdAndPodStatusIn(request.getPodDetailId(), List.of(PodStatus.ACTIVE))) {
            throw new DomainEntityNotFoundException("Point Of Delivery does not exists or is not ACTIVE anymore;");
        }

        List<ActivationFilteredModel> pointOfDeliveryInDifferentProductContractVersions = contractPodRepository.findAllContractPodsForPodWithPodDetailId(request.getPodDetailId());
        processPointOfDeliveryActivation(pointOfDeliveryInDifferentProductContractVersions, request, productContractDetailsOptional.get().getContractId(), errorMessagesContext, hasEditLockedPermission);
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessagesContext, log);

        productContractDealCreationEventPublisher
                .publishProductContractDealCreationEvent(
                        new ProductContractDealCreationEvent(productContractDetails)
                );
    }

    public void setDeactivationPurposeId(ContractPods contractPods, Long deactivationPurposeId) {
        if (deactivationPurposeId == null) {
            throw new IllegalArgumentsProvidedException("deactivationPurposeId-Deactivation purpose id!;");
        }
        if (!deactivationPurposeRepository.existsByIdAndStatusIn(deactivationPurposeId, List.of(NomenclatureItemStatus.ACTIVE))) {
            throw new DomainEntityNotFoundException("deactivationPurposeId-Deactivation purpose do not exist!;");
        }

        contractPods.setDeactivationPurposeId(deactivationPurposeId);
    }

    private void processPointOfDeliveryActivation(List<ActivationFilteredModel> pointOfDeliveryInDifferentProductContractVersions,
                                                  PodManualActivationRequest request,
                                                  Long contractId,
                                                  List<String> messages,
                                                  Boolean hasEditLockedPermission) {
        boolean existsInOtherContracts = false;
        ActivationFilteredModel currentModel = null;
        for (ActivationFilteredModel pointOfDeliveryModel : pointOfDeliveryInDifferentProductContractVersions) {
            ContractPods contractPods = pointOfDeliveryModel.getContractPods();
            if (Objects.equals(request.getPodDetailId(), contractPods.getPodDetailId()) && Objects.equals(request.getContractDetailId(), contractPods.getContractDetailId())) {
                currentModel = pointOfDeliveryModel;
            }

            if (contractPods.getActivationDate() != null &&
                    request.getActivationDate() != null &&
                    contractPods.getDeactivationDate() == null &&
                    contractPods.getActivationDate().isBefore(request.getActivationDate()) &&
                    !contractPods.getPodDetailId().equals(request.getPodDetailId()) &&
                    !contractPods.getContractDetailId().equals(request.getContractDetailId())) {
                throw new IllegalArgumentException("activationDate-pod is already activated from %s;".formatted(contractPods.getActivationDate()));
            }

            if (!Objects.equals(contractId, pointOfDeliveryModel.getContractId())) {
                if (contractPods.getActivationDate() != null) {
                    existsInOtherContracts = true;
                }
            }
        }

        if (currentModel == null) {
            throw new IllegalArgumentException("pod is not for this contract detail!;");
        }

        ContractPods currentContractPod = currentModel.getContractPods();

        Boolean isRestricted = productContractBillingLockValidationService.isRestrictedPod(contractId,
                currentContractPod.getActivationDate(),
                currentContractPod.getDeactivationDate(),
                request.getActivationDate(),
                request.getDeactivationDate(),
                hasEditLockedPermission);
        if (Boolean.TRUE.equals(isRestricted)) {
            throw new OperationNotAllowedException("Operation dont allowed");
        }


        Optional<LocalDate> nextStartDate = contractPodRepository.findNextStartDate(currentModel.getStartDate(), currentModel.getContractId());
        if (request.getActivationDate() != null && currentModel.getStartDate().isAfter(request.getActivationDate())) {
            throw new IllegalArgumentException("activationDate-Activation date should be equal or greater than %s".formatted(currentModel.getStartDate()));
        } else if (request.getActivationDate() != null && nextStartDate.isPresent() && !nextStartDate.get().isAfter(request.getActivationDate())) {
            throw new IllegalArgumentException("activationDate-Activation date should be less or equal than %s".formatted(nextStartDate.get().minusDays(1)));
        }

        if (nextStartDate.isPresent() && request.getDeactivationDate() != null && !request.getDeactivationDate().isBefore(nextStartDate.get())) {
            throw new IllegalArgumentException("deactivationDate-Deactivation date should be provided and be less or equal to %s".formatted(nextStartDate.get().minusDays(1)));
        }

        if (request.getActivationDate() == null && request.getDeactivationDate() == null) {
            ContractPods contractPods = currentContractPod;
            contractPods.setActivationDate(null);
            contractPods.setDeactivationDate(null);
            contractPods.setDeactivationPurposeId(null);
            contractPods.setCustomModifyDate(LocalDateTime.now());
            contractPodRepository.save(contractPods);
            startEndState(currentModel, request.getActivationDate(), messages, false); //TODO: this is manual flow
            return;
        }

        if (existsInOtherContracts) {
            podExistsInMultipleContract(pointOfDeliveryInDifferentProductContractVersions, currentModel, request, messages);
        } else {
            podExistsInOneContract(pointOfDeliveryInDifferentProductContractVersions, currentModel, request, messages);
        }

        startEndState(currentModel, request.getActivationDate(), messages, false); //TODO: this is manual flow
    }

    private void podExistsInOneContract(List<ActivationFilteredModel> allPods, ActivationFilteredModel currentModel, PodManualActivationRequest request, List<String> messages) {
        List<ActivationFilteredModel> podsToSend = allPods.stream().filter(x -> x.getContractId().equals(currentModel.getContractId()) && !x.getStartDate().isBefore(currentModel.getStartDate())).toList();
        List<ProductContractVersionResponse> productContractVersionsOrdered = contractDetailsRepository.findProductContractVersionsOrderedByStartDate(currentModel.getContractId(), currentModel.getStartDate());
        for (int i = 0; i < productContractVersionsOrdered.size(); i++) {
            ProductContractVersionResponse tempModel = productContractVersionsOrdered.get(i);
            if (tempModel.getId().equals(currentModel.getDetailId())) {
                if (request.getActivationDate() != null && tempModel.getStartDate().isAfter(request.getActivationDate())) {
                    throw new IllegalArgumentsProvidedException("activationDate-Activation date should not be less than %s".formatted(tempModel.getStartDate()));
                }
                if (i != productContractVersionsOrdered.size() - 1) {
                    ProductContractVersionResponse newVersion = productContractVersionsOrdered.get(i + 1);
                    if (request.getActivationDate() != null && request.getActivationDate().isAfter(newVersion.getStartDate())) {
                        throw new IllegalArgumentsProvidedException("activationDate-activationDate should not be greater than %s".formatted(newVersion.getStartDate().minusDays(1)));
                    }

                    if (request.getDeactivationDate() != null && !newVersion.getStartDate().isAfter(request.getDeactivationDate())) {
                        throw new IllegalArgumentsProvidedException("deactivationDate-Deactivation date should not be greater than %s".formatted(newVersion.getStartDate().minusDays(1)));
                    }
                }

                break;
            }
        }
        if (request.getActivationDate() != null && request.getDeactivationDate() == null) {
            setActivationDateForContractPod(request, currentModel, podsToSend, productContractVersionsOrdered, messages);
        } else {

            ContractPods contractPods = currentModel.getContractPods();
            setActivationDeactivationForContractPod(contractPods, request.getActivationDate(), request.getDeactivationDate());
            if (request.getDeactivationDate() != null) {
                setDeactivationPurposeId(contractPods, request.getDeactivationPurposeId());
            }
            contractPods.setCustomModifyDate(LocalDateTime.now());
            contractPodRepository.save(contractPods);
        }
    }

    private void setActivationDateForContractPod(PodManualActivationRequest request,
                                                 ActivationFilteredModel activationFilteredModel,
                                                 List<ActivationFilteredModel> podsToSend,
                                                 List<ProductContractVersionResponse> productContractVersionsOrdered,
                                                 List<String> messages) {

        ContractPods contractPods = activationFilteredModel.getContractPods();
        contractPods.setDeactivationDate(null);
        contractPods.setDeactivationPurposeId(null);
        contractPodWithoutActivation(podsToSend, productContractVersionsOrdered, activationFilteredModel, request.getActivationDate());
    }

    private void setActivationDeactivationForContractPod(ContractPods contractPods, LocalDate activationDate, LocalDate deactivationDate) {
        contractPods.setActivationDate(activationDate);
        contractPods.setDeactivationDate(deactivationDate);
    }

    private void podExistsInMultipleContract(List<ActivationFilteredModel> collect,
                                             ActivationFilteredModel currentModel,
                                             PodManualActivationRequest request,
                                             List<String> messages) {
        List<ActivationFilteredModel> filteredModels = collect.stream().filter(x -> x.getContractPods().getActivationDate() != null && !x.getContractPods().getId().equals(currentModel.getContractPods().getId())).toList();
        LocalDate activationDate = request.getActivationDate();
        int start = 0;
        int end = filteredModels.size() - 1;
        while (start <= end) {
            int mid = (start + end) / 2;
            if (mid >= filteredModels.size() || mid < 0) {
                break;
            }
            ContractPods contractPods = filteredModels.get(mid).getContractPods();
            LocalDate podsActivationDate = contractPods.getActivationDate();
            if (activationDate != null && podsActivationDate.isEqual(activationDate)) {
                throw new IllegalArgumentException("Pod is already active from this date;");
            } else if (activationDate != null && podsActivationDate.isAfter(activationDate)) {
                end = mid - 1;
            } else if (activationDate != null && podsActivationDate.isBefore(activationDate)) {
                start = mid + 1;
            }
        }
        ContractPods minPod = null;
        ContractPods maxPod = null;
        if (!(end <= 0 && start == 0)) {
            minPod = filteredModels.get(end).getContractPods();
        }
        if (start != filteredModels.size() && end != filteredModels.size() - 1) {
            maxPod = filteredModels.get(start).getContractPods();
        }
        checkPodPlace(collect, request, currentModel, messages, minPod, maxPod);
    }

    private void checkPodPlace(List<ActivationFilteredModel> collect,
                               PodManualActivationRequest request,
                               ActivationFilteredModel currentModel,
                               List<String> messages,
                               ContractPods minPod,
                               ContractPods maxPod) {
        if (minPod == null) {
            podIsLessThanEveryDate(collect, request, currentModel, messages, maxPod.getActivationDate());
        } else if (maxPod == null) {
            podIsGreaterThanEveryDate(collect, request, currentModel, messages, minPod.getActivationDate());
        } else {
            podIsBetweenTwoDates(collect, request, currentModel, messages, minPod, maxPod);
        }
    }

    private void podIsBetweenTwoDates(List<ActivationFilteredModel> collect,
                                      PodManualActivationRequest request,
                                      ActivationFilteredModel currentModel,
                                      List<String> messages,
                                      ContractPods minPod,
                                      ContractPods maxPod) {
        if (request.getActivationDate().isBefore(minPod.getDeactivationDate())) {
            messages.add("Pod is already active from selected activation date: %s to %s!;".formatted(minPod.getActivationDate(), minPod.getDeactivationDate()));
            return;
        }

        if (request.getDeactivationDate() == null || !request.getDeactivationDate().isBefore(maxPod.getActivationDate())) {
            messages.add("Selected deactivation should be provided and  less then or equal to %s;".formatted(maxPod.getActivationDate().minusDays(1)));
            return;
        }
        ContractPods contractPods = currentModel.getContractPods();
        setActivationDeactivationForContractPod(contractPods, request.getActivationDate(), request.getDeactivationDate());
        setDeactivationPurposeId(contractPods, request.getDeactivationPurposeId());
        contractPods.setCustomModifyDate(LocalDateTime.now());
        contractPodRepository.save(contractPods);
    }

    private void podIsGreaterThanEveryDate(List<ActivationFilteredModel> collect, PodManualActivationRequest request, ActivationFilteredModel currentModel, List<String> messages, LocalDate startDate) {

        ContractPods greatestPod = null;
        for (ActivationFilteredModel activationFilteredModel : collect) {
            ContractPods contractPods = activationFilteredModel.getContractPods();
            if (contractPods.getDeactivationDate() == null && contractPods.getActivationDate() != null && !Objects.equals(contractPods.getContractDetailId(), currentModel.getDetailId())) {
                messages.add("Pod version is already active from selected activation date: %s!;".formatted(contractPods.getActivationDate()));
                return;
            } else if (contractPods.getDeactivationDate() != null && (greatestPod == null || greatestPod.getDeactivationDate().isBefore(contractPods.getDeactivationDate()))) {
                greatestPod = contractPods;
            }
        }
        if (greatestPod != null && !request.getActivationDate().isAfter(greatestPod.getDeactivationDate()) && !greatestPod.getId().equals(currentModel.getContractPods().getId())) {
            messages.add("pod version is already active from selected activation date: %s to %s!;".formatted(greatestPod.getActivationDate(), greatestPod.getDeactivationDate()));
            return;
        }
        ContractPods contractPods = currentModel.getContractPods();
        if (contractPods.getDeactivationDate() != null && contractPods.getDeactivationDate().isBefore(request.getActivationDate())) {
            contractPods.setDeactivationDate(null);
            contractPods.setDeactivationPurposeId(null);
        }


        podExistsInOneContract(collect, currentModel, request, messages);
    }

    private void podIsLessThanEveryDate(List<ActivationFilteredModel> collect,
                                        PodManualActivationRequest request,
                                        ActivationFilteredModel currentModel,
                                        List<String> messages,
                                        LocalDate minDate) {
        if (request.getDeactivationDate() == null) {
            messages.add("deactivation is not set");
            return;
        }
        if (!request.getDeactivationDate().isBefore(minDate)) {
            messages.add("deactivation date should be less than minimum activation date!;");
            return;
        }
        ContractPods contractPods = currentModel.getContractPods();
        contractPods.setActivationDate(request.getActivationDate());
        contractPods.setDeactivationDate(request.getDeactivationDate());
        setDeactivationPurposeId(contractPods, request.getDeactivationPurposeId());
        contractPods.setCustomModifyDate(LocalDateTime.now());
        contractPodRepository.save(contractPods);
    }

    @Transactional
    public String startMIActivation(MassImportActivationRequest request, Boolean hasEditLockedPermission) {
        String podIdentifier = request.getPodIdentifier();
        List<String> errorMessages = new ArrayList<>();
        PointOfDelivery pointOfDelivery = pointOfDeliveryRepository.findByIdentifierAndStatus(podIdentifier, PodStatus.ACTIVE)
                .orElseThrow(() -> new DomainEntityNotFoundException("pod with identifier do not exists;"));
        filterStatuses(
                pointOfDelivery,
                request, errorMessages,
                hasEditLockedPermission
        );
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
        return podIdentifier;
    }

    private void filterStatuses(PointOfDelivery pointOfDelivery,
                                MassImportActivationRequest request,
                                List<String> errorMessages,
                                Boolean hasEditLockedPermission) {
        List<ContractModelMassImport> contractsForPodForActivation = productContractRepository.
                getContractsForPodForActivation(pointOfDelivery.getId());

        removeRestrictedModels(
                contractsForPodForActivation,
                hasEditLockedPermission
        );

        if (CollectionUtils.isEmpty(contractsForPodForActivation)) {
            errorMessages.add("No contract found with valid statuses for activation;");
            return;
        }
        List<ContractModelMassImport> list = contractsForPodForActivation.stream().filter(x -> x.getSigningDate() != null && request.getActivationDate().isAfter(x.getSigningDate())).toList();
        if (CollectionUtils.isEmpty(list)) {
            errorMessages.add("No contract left where activation date is greater than signing date;");
            return;
        }
        filterBy4(pointOfDelivery,
                list,
                request.getActivationDate(),
                errorMessages,
                hasEditLockedPermission
        );
    }

    private void removeRestrictedModels(List<ContractModelMassImport> contractsForPodForActivation,
                                        Boolean hasEditLockedPermission) {
        List<ContractModelMassImport> restrictedContracts = new ArrayList<>();
        for (ContractModelMassImport contractModelMassImport : contractsForPodForActivation) {
            Boolean isRestricted = productContractBillingLockValidationService.isRestrictedForAutomations(
                    contractModelMassImport.getContractId(),
                    contractModelMassImport.getVersionStartDate(),
                    hasEditLockedPermission
            );
            if (isRestricted) {
                restrictedContracts.add(contractModelMassImport);
            }
        }
        contractsForPodForActivation.removeAll(restrictedContracts);
    }

    private void filterBy4(PointOfDelivery pointOfDelivery,
                           List<ContractModelMassImport> list,
                           LocalDate activationDate,
                           List<String> errorMessages,
                           Boolean hasEditLockedPermission
    ) {
        List<Long> contractIds = list.stream().map(ContractModelMassImport::getContractId).distinct().toList();
        filterMultipleContracts(pointOfDelivery,
                list,
                contractIds,
                activationDate,
                errorMessages,
                hasEditLockedPermission
        );
    }

    private void removeRestrictedModelsAnother(List<ActivationFilteredModel> contractModelMassImports,
                                               Boolean hasEditLockedPermission) {
        List<ActivationFilteredModel> restrictedContracts = new ArrayList<>();
        for (ActivationFilteredModel contractModelMassImport : contractModelMassImports) {
            Boolean isRestricted = productContractBillingLockValidationService.isRestrictedForAutomations(
                    contractModelMassImport.getContractId(),
                    contractModelMassImport.getStartDate(),
                    hasEditLockedPermission
            );
            if (isRestricted) {
                restrictedContracts.add(contractModelMassImport);
            }
        }
        contractModelMassImports.removeAll(restrictedContracts);
    }

    private void filterMultipleContracts(PointOfDelivery pointOfDelivery,
                                         List<ContractModelMassImport> list,
                                         List<Long> contractIds,
                                         LocalDate activationDate,
                                         List<String> errorMessages,
                                         Boolean hasEditLockedPermission) {

        List<ActivationFilteredModel> contractModelMassImports = productContractRepository.filterOtherVersionsForPod(
                pointOfDelivery.getId(),
                contractIds,
                activationDate
        );
        removeRestrictedModelsAnother(
                contractModelMassImports,
                hasEditLockedPermission
        );
        Long contractId = null;
        for (ActivationFilteredModel contractModelMassImport : contractModelMassImports) {
            if (contractId == null) {
                contractId = contractModelMassImport.getContractId();
            }
            if (!Objects.equals(contractId, contractModelMassImport.getContractId())) {
                errorMessages.add("More than 1 contract found;");
                return;
            }
        }
        if (contractId == null) {
            errorMessages.add("Valid contract not found!;");
            return;
        }
        Optional<ProductContract> contract = productContractRepository.findById(contractId);
        if (contract.isPresent()) {
            if (contract.get().getLocked()) {
                errorMessages.add("POD can’t be activated because Contract is locked by automatic process in xEnergie;");
                return;
            }
        }
        List<ProductContractVersionResponse> productContractVersionsOrdered = contractDetailsRepository
                .findProductContractVersionsOrdered(contractId);
        boolean exists = contractPodRepository.checkIfPodIsActiveInAnyContract(activationDate, pointOfDelivery.getId(), contractId);
        if (exists) {
            errorMessages.add("pod with activation date exists!");
            return;
        }

        findCorrectVersionForOneContract(contractModelMassImports, productContractVersionsOrdered, activationDate, errorMessages);
    }


    private void findCorrectVersionForOneContract(
            List<ActivationFilteredModel> activationModels,
            List<ProductContractVersionResponse> contractVersions,
            LocalDate activationDate,
            List<String> errorMessages) {
        //It will be always first element of list.
        ActivationFilteredModel currentModel = activationModels.get(0);
        if (currentModel.getContractPods().getActivationDate() == null) {
            contractPodWithoutActivation(activationModels, contractVersions, currentModel, activationDate);
        } else {
            contractPodWithActivation(currentModel, activationDate, errorMessages);
        }

        startEndState(currentModel, activationDate, errorMessages, true); //TODO: this is mass import flow
    }

    private void startEndState(ActivationFilteredModel currentModel, LocalDate activationDate, List<String> errorMessages, boolean massImport) {

        Optional<ProductContract> productContractOptional = productContractRepository.findByIdAndStatusIn(currentModel.getContractId(), List.of(ProductContractStatus.ACTIVE));
        if (productContractOptional.isEmpty()) {
            errorMessages.add("ProductContract-do not exist!;");
            return;
        }
        Optional<ProductContractDetails> byId = contractDetailsRepository.findById(currentModel.getDetailId());
        if (byId.isEmpty()) {
            errorMessages.add("ProductContractDetails-do not exist!;");
            return;
        }

        ProductContractDetails productContractDetails = byId.get();
        ProductContract productContract = productContractOptional.get();

        if (activationDate == null) {
            if (!massImport) {
                List<Long> productContractDetailIds = contractDetailsRepository.findProductContractVersionsOrdered(currentModel.getContractId())
                        .stream().map(ProductContractVersionResponse::getId).toList();

                Long podsWithActivationsCount = contractPodRepository.countByContractDetailIdInAndStatusInAndActivationDateNotNull(productContractDetailIds, List.of(EntityStatus.ACTIVE));
                if (podsWithActivationsCount == 0) {
                    productContract.setActivationDate(null);
                    if (productContractDetails.getStartInitialTerm().equals(StartOfContractInitialTerm.DATE_OF_CHANGE_OF_CBG) || productContractDetails.getStartInitialTerm().equals(StartOfContractInitialTerm.FIRST_DELIVERY)) {
                        productContract.setInitialTermDate(null);
                        productContract.setContractTermEndDate(null);
                    }
                    if (productContractDetails.getEntryIntoForce().equals(ContractEntryIntoForce.DATE_CHANGE_OF_CBG) || productContractDetails.getEntryIntoForce().equals(ContractEntryIntoForce.FIRST_DELIVERY)) {
                        productContract.setEntryIntoForceDate(null);
                        productContract.setContractStatus(ContractDetailsStatus.SIGNED);
                        productContract.setSubStatus(ContractDetailsSubStatus.SIGNED_BY_BOTH_SIDES);
                    } else {
                        productContract.setContractStatus(ContractDetailsStatus.ENTERED_INTO_FORCE);
                        productContract.setSubStatus(ContractDetailsSubStatus.AWAITING_ACTIVATION);
                    }
                    productContractRepository.saveAndFlush(productContract);
                    List<Long> customerIdsToChangeStatusWithContractId = productContractDetailsRepository.getCustomerIdsToChangeStatusWithContractId(productContract.getId());
                    customerStatusChangeEventPublisher.publishCustomerStatusChangeEvent(customerIdsToChangeStatusWithContractId);
                }
            }
            return;
        }
        LocalDate minActivationDate = productContractDetailsRepository.minPodActivationDateForContractId(productContract.getId());
        if (minActivationDate != null) {
            activationDate = minActivationDate;
        }
        if (productContract.getActivationDate() != null && activationDate.isEqual(productContract.getActivationDate())) {
            return;
        }

        ProductContractTerms productContractTerms = productContractTermRepository.findById(productContractDetails.getProductContractTermId())
                .orElseThrow(() -> new DomainEntityNotFoundException("Term not found!;"));

        if (productContract.getActivationDate() == null || !activationDate.isEqual(productContract.getActivationDate())) {
            productContract.setActivationDate(activationDate);
            if (!activationDate.isAfter(LocalDate.now())) {
                productContract.setContractStatus(ContractDetailsStatus.ACTIVE_IN_TERM);
                productContract.setSubStatus(ContractDetailsSubStatus.DELIVERY);
            } else {
                productContract.setContractStatus(ContractDetailsStatus.ENTERED_INTO_FORCE);
                productContract.setSubStatus(ContractDetailsSubStatus.AWAITING_ACTIVATION);
            }

        }
        if (productContractDetails.getEntryIntoForce() != null && (productContractDetails.getEntryIntoForce().equals(ContractEntryIntoForce.FIRST_DELIVERY) || productContractDetails.getEntryIntoForce().equals(ContractEntryIntoForce.DATE_CHANGE_OF_CBG))) {
            productContract.setEntryIntoForceDate(activationDate);
        }
        if (productContractDetails.getStartInitialTerm() != null && (productContractDetails.getStartInitialTerm().equals(StartOfContractInitialTerm.FIRST_DELIVERY) || productContractDetails.getStartInitialTerm().equals(StartOfContractInitialTerm.DATE_OF_CHANGE_OF_CBG))) {
            productContract.setInitialTermDate(activationDate);
            //TODO: should be checked
//            if (productContractTerms.getPeriodType().equals(ProductTermPeriodType.PERIOD) && productContract.getContractTermEndDate() == null) {
            if (productContractTerms.getPeriodType().equals(ProductTermPeriodType.PERIOD)) {
                productContract.setContractTermEndDate(productContract.getInitialTermDate().plus(productContractTerms.getValue(), productContractTerms.getType().getUnit()).minusDays(1));
            }
        }
        productContractRepository.saveAndFlush(productContract);
        List<Long> customerIdsToChangeStatusWithContractId = productContractDetailsRepository.getCustomerIdsToChangeStatusWithContractId(productContract.getId());
        customerStatusChangeEventPublisher.publishCustomerStatusChangeEvent(customerIdsToChangeStatusWithContractId);
    }

    private void contractPodWithActivation(ActivationFilteredModel currentModel, LocalDate activationDate, List<String> errorMessages) {
        if (!currentModel.getContractPods().getActivationDate().isAfter(activationDate)) {
            errorMessages.add("Imported activation date is more/equal than POD activation date;");
            return;
        }
        currentModel.getContractPods().setActivationDate(activationDate);
    }

    private void contractPodWithoutActivation(List<ActivationFilteredModel> activationModels,
                                              List<ProductContractVersionResponse> contractVersions,
                                              ActivationFilteredModel currentModel,
                                              LocalDate activationDate) {
        ContractPods contractPods = currentModel.getContractPods();

        if (activationDate.isBefore(currentModel.getStartDate())) {
            throw new IllegalArgumentException("startDate-Start date should be greater or equal to %s".formatted(currentModel.getStartDate()));
        }
        contractPods.setActivationDate(activationDate);
        contractPods.setCustomModifyDate(LocalDateTime.now());
        contractPodRepository.save(contractPods);

        checkPodsFutureVersion(activationModels, contractVersions, activationDate);
    }

    private void checkPodsFutureVersion(List<ActivationFilteredModel> activationModels,
                                        List<ProductContractVersionResponse> contractVersions,
                                        LocalDate activationDate) {

        if (activationModels.size() > 1) {
            checkContractPodsForActivation(activationModels, contractVersions, activationDate);
        }
    }

    private void checkContractPodsForActivation(List<ActivationFilteredModel> activationModels,
                                                List<ProductContractVersionResponse> contractVersions,
                                                LocalDate activationDate) {
        Map<Long, ActivationFilteredModel> collect = activationModels.stream().collect(Collectors.toMap(ActivationFilteredModel::getDetailId, j -> j));

        LocalDate startDate = activationDate;
        boolean firstActivated = false;
        for (int i = 0; i < contractVersions.size(); i++) {
            ProductContractVersionResponse contractVersion = contractVersions.get(i);
            ActivationFilteredModel activationFilteredModel = collect.remove(contractVersion.getId());
            if (i + 1 < contractVersions.size() && activationFilteredModel == null && firstActivated) {
                startDate = contractVersions.get(i + 1).getStartDate();
            }
            if (activationFilteredModel == null) {
                continue;
            }
            ContractPods contractPods = activationFilteredModel.getContractPods();
            contractPods.setActivationDate(startDate);
            if (!firstActivated) {
                firstActivated = true;
            }
            if (contractPods.getDeactivationDate() != null) {
                break;
            }
            if (i + 1 < contractVersions.size()) {
                ProductContractVersionResponse newVersion = contractVersions.get(i + 1);
                startDate = newVersion.getStartDate();
                if (collect.size() != 0) {
                    contractPods.setDeactivationDate(newVersion.getStartDate().minusDays(1));
                }

            }
            contractPods.setCustomModifyDate(LocalDateTime.now());
        }
        contractPodRepository.saveAll(activationModels.stream().map(ActivationFilteredModel::getContractPods).toList());
    }

    @Transactional
    public String deactivate(MassImportDeactivationRequest request, Boolean hasEditLockedPermission) {

        PointOfDelivery pointOfDelivery = pointOfDeliveryRepository.findByIdentifierAndStatus(request.getPodIdentifier(), PodStatus.ACTIVE)
                .orElseThrow(() -> new DomainEntityNotFoundException("pod not found!;"));
        List<DeactivationFilteredModel> allPods = new ArrayList<>(contractPodRepository.
                findAllContractPodsForPodWithPodId(pointOfDelivery.getId()));
        removeRestrictedDeactivationModels(
                allPods,
                hasEditLockedPermission,
                request.getDeactivationDate()
        );
        filterPodsWithActivationDate(allPods, pointOfDelivery, request.getDeactivationDate());
        removeActivationDeactivation(allPods, request.getDeactivationDate());
        return request.getPodIdentifier();
    }

    private void removeRestrictedDeactivationModels(List<DeactivationFilteredModel> contractsForPodForDeactivation,
                                                    Boolean hasEditLockedPermission, LocalDate deactivationDate) {
        List<DeactivationFilteredModel> restrictedContracts = new ArrayList<>();
        for (DeactivationFilteredModel contractModelMassImport : contractsForPodForDeactivation) {
            Boolean isRestricted = productContractBillingLockValidationService.isRestrictedForAutomationsPod(
                    contractModelMassImport.getContractId(),
                    deactivationDate,
                    contractModelMassImport.getContractPods().getDeactivationDate(),
                    hasEditLockedPermission
            );
            if (isRestricted) {
                restrictedContracts.add(contractModelMassImport);
            }
        }
        contractsForPodForDeactivation.removeAll(restrictedContracts);
    }

    private void removeActivationDeactivation(List<DeactivationFilteredModel> allPods, LocalDate currentDeactivationDate) {
        List<ContractPods> contractPodsToSave = new ArrayList<>();
        for (ActivationFilteredModel allPod : allPods) {
            ContractPods contractPods = allPod.getContractPods();
            //TODO: check if needed
            Long contractId = allPod.getContractId();
            Optional<ProductContract> contract = productContractRepository.findById(contractId);
            if (contract.get().getLocked()) {
                throw new IllegalArgumentsProvidedException("POD can’t be deactivated because Contract is locked by automatic process in xEnergie;");
            }
            LocalDate activationDate = contractPods.getActivationDate();
            LocalDate deactivationDate = contractPods.getDeactivationDate();
            if (activationDate != null && (activationDate.isAfter(currentDeactivationDate))) {
                contractPods.setDeactivationDate(null);
                contractPods.setActivationDate(null);
                contractPods.setCustomModifyDate(LocalDateTime.now());
                contractPodsToSave.add(contractPods);
            }
        }
        contractPodRepository.saveAll(contractPodsToSave);
    }

    private void filterPodsWithActivationDate(List<DeactivationFilteredModel> allPods, PointOfDelivery pointOfDelivery, LocalDate deactivationDate) {
        List<DeactivationFilteredModel> filteredModels = allPods.stream().filter(x -> x.getContractPods().getActivationDate() != null).toList();
        if (CollectionUtils.isEmpty(filteredModels)) {
            throw new IllegalArgumentException("Active pod does not exist!;");
        }
        checkActivationDates(filteredModels, pointOfDelivery, deactivationDate);
    }

    //step 3
    private void checkActivationDates(List<DeactivationFilteredModel> models, PointOfDelivery pointOfDelivery, LocalDate deactivationDate) {
        List<DeactivationFilteredModel> filteredModels = models.stream().filter(mdl -> {
            ContractPods contractPods = mdl.getContractPods();
            return !deactivationDate.isBefore(contractPods.getActivationDate()) && (contractPods.getDeactivationDate() == null || deactivationDate.isBefore(contractPods.getDeactivationDate()));
        }).toList();
        if (CollectionUtils.isEmpty(filteredModels)) {
            exceptionMessageChecks(models, deactivationDate);
        }
        checkContractPodsForDeactivation(filteredModels, pointOfDelivery, deactivationDate);
    }

    private void exceptionMessageChecks(List<DeactivationFilteredModel> models, LocalDate deactivationDate) {
        List<DeactivationFilteredModel> futureActivations = models.stream().filter(x -> !x.getContractPods().getActivationDate().isBefore(deactivationDate)).toList();
        if (futureActivations.isEmpty()) {
            throw new IllegalArgumentException("Respective contracts version for this POD is not found!;");
        }
        StringBuilder sb = new StringBuilder();
        for (DeactivationFilteredModel futureActivation : futureActivations) {
            sb.append("POD is activated in future contract with number: %s and version: %s ;".formatted(futureActivation.getContractNumber(), futureActivation.getVersionId()));
        }
        throw new IllegalArgumentException(sb.toString());
    }

    private void checkContractPodsForDeactivation(List<DeactivationFilteredModel> models, PointOfDelivery pointOfDelivery, LocalDate deactivationDate) {

        List<ContractPods> contractPodsToSave = new ArrayList<>();
        for (ActivationFilteredModel model : models) {
            ContractPods contractPods = model.getContractPods();
            //TODO: check if needed
            Long contractId = model.getContractId();
            Optional<ProductContract> contract = productContractRepository.findById(contractId);
            if (contract.get().getLocked()) {
                throw new IllegalArgumentsProvidedException("POD can’t be deactivated because Contract is locked by automatic process in xEnergie;");
            }
            contractPods.setDeactivationDate(deactivationDate);
            contractPods.setCustomModifyDate(LocalDateTime.now());
            List<Action> actions = actionRepository.findByPodAndContract(pointOfDelivery.getId(), contract.get().getId());

            Optional<Action> action = actions.stream().filter(x -> x.getExecutionDate().equals(deactivationDate)).max(Comparator.comparing(Action::getExecutionDate));
            if (action.isPresent()) {
                if (action.get().getPenaltyPayer().equals(ActionPenaltyPayer.EPRES)) {
                    contractPods.setDeactivationPurposeId(energoPurposeId);
                } else {
                    contractPods.setDeactivationPurposeId(customerPurposeId);
                }
            } else {
                contractPods.setDeactivationPurposeId(otherPurposeId);
            }


            contractPodsToSave.add(contractPods);
        }
        contractPodRepository.saveAll(contractPodsToSave);
    }

}
