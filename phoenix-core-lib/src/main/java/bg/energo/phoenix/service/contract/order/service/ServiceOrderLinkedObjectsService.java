package bg.energo.phoenix.service.contract.order.service;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.contract.order.service.*;
import bg.energo.phoenix.model.enums.contract.ContractType;
import bg.energo.phoenix.model.enums.contract.products.ProductContractStatus;
import bg.energo.phoenix.model.request.contract.order.service.ServiceOrderLinkedContractRequest;
import bg.energo.phoenix.model.request.contract.order.service.ServiceOrderServiceParametersRequest;
import bg.energo.phoenix.repository.contract.order.service.ServiceOrderLinkedProductContractRepository;
import bg.energo.phoenix.repository.contract.order.service.ServiceOrderLinkedServiceContractRepository;
import bg.energo.phoenix.repository.contract.order.service.ServiceOrderPodRepository;
import bg.energo.phoenix.repository.contract.order.service.ServiceOrderUnrecognizedPodRepository;
import bg.energo.phoenix.repository.contract.product.ProductContractRepository;
import bg.energo.phoenix.repository.contract.service.ServiceContractsRepository;
import bg.energo.phoenix.repository.pod.pod.PointOfDeliveryRepository;
import bg.energo.phoenix.util.epb.EPBListUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceOrderLinkedObjectsService {

    private final ProductContractRepository productContractRepository;
    private final ServiceContractsRepository serviceContractsRepository;
    private final PointOfDeliveryRepository podRepository;
    private final ServiceOrderLinkedProductContractRepository serviceOrderLinkedProductContractRepository;
    private final ServiceOrderLinkedServiceContractRepository serviceOrderLinkedServiceContractRepository;
    private final ServiceOrderPodRepository serviceOrderPodRepository;
    private final ServiceOrderUnrecognizedPodRepository serviceOrderUnrecognizedPodRepository;


    /**
     * Creates linked contracts for the given service order. Before creation the requested contracts are validated.
     *
     * @param request       The request containing the service parameters.
     * @param order         The service order entity.
     * @param errorMessages The list of error messages to be populated if any validation fails.
     */
    public void createLinkedContracts(ServiceOrderServiceParametersRequest request, ServiceOrder order, List<String> errorMessages) {
        if (CollectionUtils.isNotEmpty(request.getLinkedContracts())) {
            Map<ContractType, List<ServiceOrderLinkedContractRequest>> linkedContractsMap = request
                    .getLinkedContracts()
                    .stream()
                    .collect(Collectors.groupingBy(ServiceOrderLinkedContractRequest::getType));

            List<Long> linkedProductContracts = linkedContractsMap.containsKey(ContractType.PRODUCT_CONTRACT)
                    ? linkedContractsMap.get(ContractType.PRODUCT_CONTRACT)
                    .stream()
                    .map(ServiceOrderLinkedContractRequest::getId)
                    .toList()
                    : new ArrayList<>();

            List<Long> linkedServiceContracts = linkedContractsMap.containsKey(ContractType.SERVICE_CONTRACT)
                    ? linkedContractsMap.get(ContractType.SERVICE_CONTRACT)
                    .stream()
                    .map(ServiceOrderLinkedContractRequest::getId)
                    .toList()
                    : new ArrayList<>();

            if (EPBListUtils.notAllUnique(linkedProductContracts)) {
                log.error("Linked product contracts contain duplicate values;");
                errorMessages.add("Linked product contracts contain duplicate values;");
            }

            if (EPBListUtils.notAllUnique(linkedServiceContracts)) {
                log.error("Linked service contracts contain duplicate values;");
                errorMessages.add("Linked service contracts contain duplicate values;");
            }

            List<Long> availableProductContracts = productContractRepository.findAllByIdInAndStatusIn(linkedProductContracts, List.of(ProductContractStatus.ACTIVE));
            List<Long> availableServiceContracts = serviceContractsRepository.findByIdInAndStatusIn(linkedServiceContracts, List.of(EntityStatus.ACTIVE));

            List<ServiceOrderLinkedContractRequest> linkedContracts = request.getLinkedContracts();
            for (int i = 0; i < linkedContracts.size(); i++) {
                ServiceOrderLinkedContractRequest linkedContractRequest = linkedContracts.get(i);
                if (linkedContractRequest.getType().equals(ContractType.PRODUCT_CONTRACT)) {
                    if (!availableProductContracts.contains(linkedContractRequest.getId())) {
                        log.error("serviceParameters.linkedContracts[%s].id-Product contract with ID %s is not valid;".formatted(i, linkedContractRequest.getId()));
                        errorMessages.add("serviceParameters.linkedContracts[%s].id-Product contract with ID %s is not valid;".formatted(i, linkedContractRequest.getId()));
                        continue;
                    }

                    ServiceOrderLinkedProductContract linkedProductContract = new ServiceOrderLinkedProductContract();
                    linkedProductContract.setOrderId(order.getId());
                    linkedProductContract.setContractId(linkedContractRequest.getId());
                    linkedProductContract.setStatus(EntityStatus.ACTIVE);
                    serviceOrderLinkedProductContractRepository.save(linkedProductContract);
                } else {
                    if (!availableServiceContracts.contains(linkedContractRequest.getId())) {
                        log.error("serviceParameters.linkedContracts[%s].id-Service contract with ID %s is not valid;".formatted(i, linkedContractRequest.getId()));
                        errorMessages.add("serviceParameters.linkedContracts[%s].id-Service contract with ID %s is not valid;".formatted(i, linkedContractRequest.getId()));
                        continue;
                    }

                    ServiceOrderLinkedServiceContract linkedServiceContract = new ServiceOrderLinkedServiceContract();
                    linkedServiceContract.setOrderId(order.getId());
                    linkedServiceContract.setContractId(linkedContractRequest.getId());
                    linkedServiceContract.setStatus(EntityStatus.ACTIVE);
                    serviceOrderLinkedServiceContractRepository.save(linkedServiceContract);
                }
            }
        }
    }


    /**
     * Creates the PODs for the given service order.
     *
     * @param request       The request containing the PODs.
     * @param serviceOrder  The service order entity.
     * @param errorMessages The list of error messages to be populated if any validation fails.
     */
    public void createPods(ServiceOrderServiceParametersRequest request, ServiceOrder serviceOrder, List<String> errorMessages) {
        if (CollectionUtils.isNotEmpty(request.getPods())) {
            List<Long> pods = request.getPods();
            List<ServiceOrderPod> tempList = new ArrayList<>();
            List<Long> availablePods = podRepository.findByStatusActiveAndIdIn(pods);
            for (int i = 0; i < pods.size(); i++) {
                Long podId = pods.get(i);
                if (!availablePods.contains(podId)) {
                    errorMessages.add("serviceParameters.pods[%s]-POD with ID %s is not valid;".formatted(i, podId));
                    continue;
                }

                ServiceOrderPod pod = new ServiceOrderPod();
                pod.setOrderId(serviceOrder.getId());
                pod.setPodId(podId);
                pod.setStatus(EntityStatus.ACTIVE);
                tempList.add(pod);
            }

            if (CollectionUtils.isEmpty(errorMessages)) {
                serviceOrderPodRepository.saveAll(tempList);
            }
        }
    }


    /**
     * Creates unrecognized PODs for the given service order.
     *
     * @param request      The request containing the unrecognized PODs.
     * @param serviceOrder The service order entity.
     */
    public void createUnrecognizedPods(ServiceOrderServiceParametersRequest request, ServiceOrder serviceOrder, List<String> errorMessages) {
        if (CollectionUtils.isNotEmpty(request.getUnrecognizedPods())) {
            List<ServiceOrderUnrecognizedPod> tempList = new ArrayList<>();
            List<String> unrecognizedPods = request.getUnrecognizedPods();
            for (int i = 0; i < unrecognizedPods.size(); i++) {
                String identifier = unrecognizedPods.get(i);
                if (!identifier.matches("^[0-9A-Za-z]{1,33}$")) {
                    errorMessages.add("serviceParameters.unrecognizedPods[%s]-Unrecognized POD with identifier %s is not valid;".formatted(i, identifier));
                    continue;
                }

                ServiceOrderUnrecognizedPod unrecognizedPod = new ServiceOrderUnrecognizedPod();
                unrecognizedPod.setOrderId(serviceOrder.getId());
                unrecognizedPod.setPodIdentifier(identifier);
                unrecognizedPod.setStatus(EntityStatus.ACTIVE);
                tempList.add(unrecognizedPod);
            }

            if (CollectionUtils.isEmpty(errorMessages)) {
                serviceOrderUnrecognizedPodRepository.saveAll(tempList);
            }
        }
    }


    /**
     * Updates linked contracts for the given service order. Before update the requested contracts are validated.
     *
     * @param serviceOrder  The service order entity.
     * @param errorMessages The list of error messages to be populated if any validation fails.
     * @param request       The request containing the service parameters.
     */
    public void updateLinkedContracts(ServiceOrder serviceOrder, List<String> errorMessages, ServiceOrderServiceParametersRequest request) {
        List<ServiceOrderLinkedProductContract> persistedLinkedProductContracts = serviceOrderLinkedProductContractRepository
                .findByOrderIdAndStatusIn(serviceOrder.getId(), List.of(EntityStatus.ACTIVE));

        List<ServiceOrderLinkedServiceContract> persistedLinkedServiceContracts = serviceOrderLinkedServiceContractRepository
                .findByOrderIdAndStatusIn(serviceOrder.getId(), List.of(EntityStatus.ACTIVE));

        if (CollectionUtils.isEmpty(request.getLinkedContracts())) {
            if (CollectionUtils.isNotEmpty(persistedLinkedProductContracts)) {
                persistedLinkedProductContracts.forEach(contract -> contract.setStatus(EntityStatus.DELETED));
                serviceOrderLinkedProductContractRepository.saveAll(persistedLinkedProductContracts);
            }

            if (CollectionUtils.isNotEmpty(persistedLinkedServiceContracts)) {
                persistedLinkedServiceContracts.forEach(contract -> contract.setStatus(EntityStatus.DELETED));
                serviceOrderLinkedServiceContractRepository.saveAll(persistedLinkedServiceContracts);
            }

            return;
        }

        Map<ContractType, List<ServiceOrderLinkedContractRequest>> linkedContractsMap = request
                .getLinkedContracts()
                .stream()
                .collect(Collectors.groupingBy(ServiceOrderLinkedContractRequest::getType));

        List<Long> linkedProductContracts = linkedContractsMap.containsKey(ContractType.PRODUCT_CONTRACT)
                ? linkedContractsMap.get(ContractType.PRODUCT_CONTRACT)
                .stream()
                .map(ServiceOrderLinkedContractRequest::getId)
                .toList()
                : new ArrayList<>();

        List<Long> linkedServiceContracts = linkedContractsMap.containsKey(ContractType.SERVICE_CONTRACT)
                ? linkedContractsMap.get(ContractType.SERVICE_CONTRACT)
                .stream()
                .map(ServiceOrderLinkedContractRequest::getId)
                .toList()
                : new ArrayList<>();

        if (EPBListUtils.notAllUnique(linkedProductContracts)) {
            log.error("Linked product contracts contain duplicate values;");
            errorMessages.add("Linked product contracts contain duplicate values;");
        }

        if (EPBListUtils.notAllUnique(linkedServiceContracts)) {
            log.error("Linked service contracts contain duplicate values;");
            errorMessages.add("Linked service contracts contain duplicate values;");
        }

        List<Long> availableProductContracts = productContractRepository.findAllByIdInAndStatusIn(linkedProductContracts, List.of(ProductContractStatus.ACTIVE));
        List<Long> availableServiceContracts = serviceContractsRepository.findByIdInAndStatusIn(linkedServiceContracts, List.of(EntityStatus.ACTIVE));

        List<Long> persistedLinkedProductContractIds = persistedLinkedProductContracts
                .stream()
                .map(ServiceOrderLinkedProductContract::getContractId)
                .toList();

        List<Long> persistedLinkedServiceContractIds = persistedLinkedServiceContracts
                .stream()
                .map(ServiceOrderLinkedServiceContract::getContractId)
                .toList();

        List<ServiceOrderLinkedContractRequest> linkedContracts = request.getLinkedContracts();
        for (int i = 0; i < linkedContracts.size(); i++) {
            ServiceOrderLinkedContractRequest linkedContractRequest = linkedContracts.get(i);
            Long contractId = linkedContractRequest.getId();

            if (linkedContractRequest.getType().equals(ContractType.PRODUCT_CONTRACT)) {
                if (!availableProductContracts.contains(contractId)) {
                    log.error("serviceParameters.linkedContracts[%s].id-Product contract with ID %s is not valid;".formatted(i, contractId));
                    errorMessages.add("serviceParameters.linkedContracts[%s].id-Product contract with ID %s is not valid;".formatted(i, contractId));
                    continue;
                }

                if (!persistedLinkedProductContractIds.contains(contractId)) {
                    ServiceOrderLinkedProductContract linkedProductContract = new ServiceOrderLinkedProductContract();
                    linkedProductContract.setOrderId(serviceOrder.getId());
                    linkedProductContract.setContractId(contractId);
                    linkedProductContract.setStatus(EntityStatus.ACTIVE);
                    serviceOrderLinkedProductContractRepository.save(linkedProductContract);
                }
            } else {
                if (!availableServiceContracts.contains(contractId)) {
                    log.error("serviceParameters.linkedContracts[%s].id-Service contract with ID %s is not valid;".formatted(i, contractId));
                    errorMessages.add("serviceParameters.linkedContracts[%s].id-Service contract with ID %s is not valid;".formatted(i, contractId));
                    continue;
                }

                if (!persistedLinkedServiceContractIds.contains(contractId)) {
                    ServiceOrderLinkedServiceContract linkedServiceContract = new ServiceOrderLinkedServiceContract();
                    linkedServiceContract.setOrderId(serviceOrder.getId());
                    linkedServiceContract.setContractId(contractId);
                    linkedServiceContract.setStatus(EntityStatus.ACTIVE);
                    serviceOrderLinkedServiceContractRepository.save(linkedServiceContract);
                }
            }
        }

        for (ServiceOrderLinkedProductContract linkedProductContract : persistedLinkedProductContracts) {
            if (!linkedProductContracts.contains(linkedProductContract.getContractId())) {
                linkedProductContract.setStatus(EntityStatus.DELETED);
                serviceOrderLinkedProductContractRepository.save(linkedProductContract);
            }
        }

        for (ServiceOrderLinkedServiceContract linkedServiceContract : persistedLinkedServiceContracts) {
            if (!linkedServiceContracts.contains(linkedServiceContract.getContractId())) {
                linkedServiceContract.setStatus(EntityStatus.DELETED);
                serviceOrderLinkedServiceContractRepository.save(linkedServiceContract);
            }
        }
    }


    /**
     * Updates pods sub objects.
     *
     * @param serviceOrder      The service order entity.
     * @param errorMessages     The list of error messages to be populated if any validation fails.
     * @param serviceParameters The service parameters request.
     */
    public void updatePods(ServiceOrder serviceOrder, List<String> errorMessages, ServiceOrderServiceParametersRequest serviceParameters) {
        List<Long> linkedPods = serviceParameters.getPods();

        List<ServiceOrderPod> persistedLinkedPods = serviceOrderPodRepository.findByOrderIdAndStatusIn(serviceOrder.getId(), List.of(EntityStatus.ACTIVE));

        if (CollectionUtils.isEmpty(linkedPods) && CollectionUtils.isNotEmpty(persistedLinkedPods)) {
            persistedLinkedPods.forEach(pod -> pod.setStatus(EntityStatus.DELETED));
            serviceOrderPodRepository.saveAll(persistedLinkedPods);
            return;
        }

        List<ServiceOrderPod> tempList = new ArrayList<>();

        if (CollectionUtils.isEmpty(persistedLinkedPods)) {
            createPods(serviceParameters, serviceOrder, errorMessages);
            return;
        } else {
            List<Long> persistedLinkedPodIds = persistedLinkedPods
                    .stream()
                    .map(ServiceOrderPod::getPodId)
                    .toList();

            List<Long> availablePods = podRepository.findByStatusActiveAndIdIn(linkedPods);

            for (int i = 0; i < linkedPods.size(); i++) {
                Long pod = linkedPods.get(i);

                if (!availablePods.contains(pod)) {
                    log.error("serviceParameters.pods[%s]-Pod with ID %s is not valid;".formatted(i, pod));
                    errorMessages.add("serviceParameters.pods[%s]-Pod with ID %s is not valid;".formatted(i, pod));
                    continue;
                }

                if (!persistedLinkedPodIds.contains(pod)) {
                    ServiceOrderPod linkedPod = new ServiceOrderPod();
                    linkedPod.setOrderId(serviceOrder.getId());
                    linkedPod.setPodId(pod);
                    linkedPod.setStatus(EntityStatus.ACTIVE);
                    tempList.add(linkedPod);
                } else {
                    Optional<ServiceOrderPod> persistedLinkedPodOptional = persistedLinkedPods
                            .stream()
                            .filter(linkedPod -> linkedPod.getPodId().equals(pod))
                            .findFirst();

                    if (persistedLinkedPodOptional.isEmpty()) {
                        log.error("serviceParameters.pods[%s]-Unable to find persisted pod relation;".formatted(i));
                        errorMessages.add("serviceParameters.pods[%s]-Unable to find persisted pod relation;".formatted(i));
                    } else {
                        ServiceOrderPod linkedPod = persistedLinkedPodOptional.get();
                        linkedPod.setPodId(pod);
                        tempList.add(linkedPod);
                    }
                }

            }

            for (ServiceOrderPod linkedPod : persistedLinkedPods) {
                if (!linkedPods.contains(linkedPod.getPodId())) {
                    linkedPod.setStatus(EntityStatus.DELETED);
                    tempList.add(linkedPod);
                }
            }
        }

        if (CollectionUtils.isEmpty(errorMessages)) {
            serviceOrderPodRepository.saveAll(tempList);
        }
    }


    /**
     * Updates the unrecognized pods.
     *
     * @param serviceOrder      The service order entity.
     * @param errorMessages     The list of error messages to be populated if any validation fails.
     * @param serviceParameters The service parameters request.
     */
    public void updateUnrecognizedPods(ServiceOrder serviceOrder, List<String> errorMessages, ServiceOrderServiceParametersRequest serviceParameters) {
        List<String> linkedUnrecognizedPods = serviceParameters.getUnrecognizedPods();

        List<ServiceOrderUnrecognizedPod> persistedUnrecognizedPods = serviceOrderUnrecognizedPodRepository.findByOrderIdAndStatusIn(serviceOrder.getId(), List.of(EntityStatus.ACTIVE));

        if (CollectionUtils.isEmpty(linkedUnrecognizedPods) && CollectionUtils.isNotEmpty(persistedUnrecognizedPods)) {
            persistedUnrecognizedPods.forEach(pod -> pod.setStatus(EntityStatus.DELETED));
            serviceOrderUnrecognizedPodRepository.saveAll(persistedUnrecognizedPods);
            return;
        }

        List<ServiceOrderUnrecognizedPod> tempList = new ArrayList<>();

        if (CollectionUtils.isEmpty(persistedUnrecognizedPods)) {
            createUnrecognizedPods(serviceParameters, serviceOrder, errorMessages);
            return;
        } else {
            List<String> persistedUnrecognizedPodIds = persistedUnrecognizedPods
                    .stream()
                    .map(ServiceOrderUnrecognizedPod::getPodIdentifier)
                    .toList();

            for (int i = 0; i < linkedUnrecognizedPods.size(); i++) {
                String identifier = linkedUnrecognizedPods.get(i);
                if (!identifier.matches("^[0-9A-Za-z]{1,33}$")) {
                    log.error("serviceParameters.unrecognizedPods[%s]-Unrecognized POD with identifier %s is not valid;".formatted(i, identifier));
                    errorMessages.add("serviceParameters.unrecognizedPods[%s]-Unrecognized POD with identifier %s is not valid;".formatted(i, identifier));
                    continue;
                }

                if (!persistedUnrecognizedPodIds.contains(identifier)) {
                    ServiceOrderUnrecognizedPod unrecognizedPod = new ServiceOrderUnrecognizedPod();
                    unrecognizedPod.setOrderId(serviceOrder.getId());
                    unrecognizedPod.setPodIdentifier(identifier);
                    unrecognizedPod.setStatus(EntityStatus.ACTIVE);
                    tempList.add(unrecognizedPod);
                } else {
                    Optional<ServiceOrderUnrecognizedPod> persistedUnrecognizedPodOptional = persistedUnrecognizedPods
                            .stream()
                            .filter(unrecognizedPod -> unrecognizedPod.getPodIdentifier().equals(identifier))
                            .findFirst();

                    if (persistedUnrecognizedPodOptional.isEmpty()) {
                        log.error("serviceParameters.unrecognizedPods[%s]-Unable to find persisted unrecognized pod relation;".formatted(i));
                        errorMessages.add("serviceParameters.unrecognizedPods[%s]-Unable to find persisted unrecognized pod relation;".formatted(i));
                    } else {
                        ServiceOrderUnrecognizedPod unrecognizedPod = persistedUnrecognizedPodOptional.get();
                        unrecognizedPod.setPodIdentifier(identifier);
                        tempList.add(unrecognizedPod);
                    }
                }
            }

            for (ServiceOrderUnrecognizedPod unrecognizedPod : persistedUnrecognizedPods) {
                if (!linkedUnrecognizedPods.contains(unrecognizedPod.getPodIdentifier())) {
                    unrecognizedPod.setStatus(EntityStatus.DELETED);
                    tempList.add(unrecognizedPod);
                }
            }
        }

        if (CollectionUtils.isEmpty(errorMessages)) {
            serviceOrderUnrecognizedPodRepository.saveAll(tempList);
        }
    }


    /**
     * Clears sub objects depending on the service execution level.
     * Should be used in case of changing the service detail.
     *
     * @param serviceOrder The service order entity.
     */
    public void clearRemovedServiceParametersAndObjects(ServiceOrder serviceOrder) {
        List<ServiceOrderLinkedProductContract> persistedLinkedProductContracts = serviceOrderLinkedProductContractRepository.findByOrderIdAndStatusIn(serviceOrder.getId(), List.of(EntityStatus.ACTIVE));
        for (ServiceOrderLinkedProductContract linkedProduct : persistedLinkedProductContracts) {
            linkedProduct.setStatus(EntityStatus.DELETED);
            serviceOrderLinkedProductContractRepository.save(linkedProduct);
        }

        List<ServiceOrderLinkedServiceContract> persistedLinkedServiceContracts = serviceOrderLinkedServiceContractRepository.findByOrderIdAndStatusIn(serviceOrder.getId(), List.of(EntityStatus.ACTIVE));
        for (ServiceOrderLinkedServiceContract linkedService : persistedLinkedServiceContracts) {
            linkedService.setStatus(EntityStatus.DELETED);
            serviceOrderLinkedServiceContractRepository.save(linkedService);
        }

        List<ServiceOrderPod> persistedPods = serviceOrderPodRepository.findByOrderIdAndStatusIn(serviceOrder.getId(), List.of(EntityStatus.ACTIVE));
        for (ServiceOrderPod pod : persistedPods) {
            pod.setStatus(EntityStatus.DELETED);
            serviceOrderPodRepository.save(pod);
        }

        List<ServiceOrderUnrecognizedPod> persistedUnrecognizedPods = serviceOrderUnrecognizedPodRepository.findByOrderIdAndStatusIn(serviceOrder.getId(), List.of(EntityStatus.ACTIVE));
        for (ServiceOrderUnrecognizedPod unrecognizedPod : persistedUnrecognizedPods) {
            unrecognizedPod.setStatus(EntityStatus.DELETED);
            serviceOrderUnrecognizedPodRepository.save(unrecognizedPod);
        }
    }

}
