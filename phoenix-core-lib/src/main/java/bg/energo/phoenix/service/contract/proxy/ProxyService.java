package bg.energo.phoenix.service.contract.proxy;

import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.model.entity.contract.proxy.ProductContractProxy;
import bg.energo.phoenix.model.entity.contract.proxy.ProductContractProxyFile;
import bg.energo.phoenix.model.entity.contract.proxy.ProxyManagers;
import bg.energo.phoenix.model.entity.customer.Customer;
import bg.energo.phoenix.model.entity.customer.CustomerDetails;
import bg.energo.phoenix.model.entity.customer.Manager;
import bg.energo.phoenix.model.enums.contract.products.ContractSubObjectStatus;
import bg.energo.phoenix.model.enums.customer.CustomerStatus;
import bg.energo.phoenix.model.enums.customer.CustomerType;
import bg.energo.phoenix.model.enums.customer.Status;
import bg.energo.phoenix.model.request.contract.ProxyAddRequest;
import bg.energo.phoenix.model.request.contract.ProxyEditRequest;
import bg.energo.phoenix.model.response.proxy.ProxyFilesResponse;
import bg.energo.phoenix.model.response.proxy.ProxyManagersResponse;
import bg.energo.phoenix.model.response.proxy.ProxyResponse;
import bg.energo.phoenix.repository.contract.proxy.ProductContractProxyRepository;
import bg.energo.phoenix.repository.contract.proxy.ProxyFilesRepository;
import bg.energo.phoenix.repository.contract.proxy.ProxyManagersRepository;
import bg.energo.phoenix.repository.customer.CustomerDetailsRepository;
import bg.energo.phoenix.repository.customer.CustomerRepository;
import bg.energo.phoenix.repository.customer.ManagerRepository;
import bg.energo.phoenix.util.epb.EPBChainedExceptionTriggerUtil;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProxyService {
    private final CustomerDetailsRepository customerDetailsRepository;
    private final CustomerRepository customerRepository;
    private final ManagerRepository managerRepository;
    private final ProxyManagersRepository proxyManagersRepository;

    private final ProductContractProxyRepository proxyRepository;
    private final ProxyFilesRepository proxyFilesRepository;
    private final Validator validator;

    public static List<ProxyEditRequest> getProxies(List<ProxyEditRequest> proxyEditList,
                                                    List<ProductContractProxy> dbProxies,
                                                    List<Long> shouldBeDeleted,
                                                    List<ProxyEditRequest> shouldBeCreated) {
        List<ProxyEditRequest> shouldBeEdited = new ArrayList<>();
        List<ProxyEditRequest> copyList = new ArrayList<>(proxyEditList);
        findMissingProxyIds(dbProxies, proxyEditList, shouldBeDeleted);

        for (ProxyEditRequest proxyEditRequest : copyList) {
            Long proxyEditRequestId = proxyEditRequest.getId();
            if (proxyEditRequest.getId() != null) {
                boolean hasSameId = dbProxies.stream()
                        .anyMatch(proxy -> proxy.getId().equals(proxyEditRequestId));
                if (hasSameId) {
                    shouldBeEdited.add(proxyEditRequest);
                }
            } else {
                shouldBeCreated.add(proxyEditRequest);
            }
        }
        return shouldBeEdited;
    }

    public static void findMissingProxyIds(List<ProductContractProxy> dbProxies, List<ProxyEditRequest> proxyEditList, List<Long> shouldBeDeleted) {
        for (ProductContractProxy proxy : dbProxies) {
            long proxyId = proxy.getId();
            boolean found = false;

            for (ProxyEditRequest editRequest : proxyEditList) {
                if (editRequest.getId() != null && editRequest.getId().equals(proxyId)) {
                    found = true;
                    break; // Found a match, no need to continue checking
                }
            }

            if (!found) {
                shouldBeDeleted.add(proxyId);
            }
        }
    }

    @Transactional
    public Long create(Customer customer, Long customerDetailId, @Valid ProxyAddRequest request, Long contractDetailId, List<String> exceptionMessages) {
        if (request != null) {
            log.debug("Creating proxy object: {}", request);
            Set<ConstraintViolation<ProxyAddRequest>> violations = validator.validate(request);
            if (!violations.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (ConstraintViolation<ProxyAddRequest> constraintViolation : violations) {
                    sb.append(constraintViolation.getMessage());
                }
                exceptionMessages.add(sb.toString());
            }
            EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(exceptionMessages, log);
            List<Manager> managers = getManagers(customer, customerDetailId, request, exceptionMessages);
            List<ProductContractProxyFile> proxyFile = getProxyFiles(request.getFileIds(), exceptionMessages);
            EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(exceptionMessages, log);
            ProductContractProxy proxy = mapProxyRequestToProxyEntity(request, contractDetailId);
            ProductContractProxy dbProxy = proxyRepository.saveAndFlush(proxy);
            updateProxyFiles(dbProxy, proxyFile, exceptionMessages);
            updateProxyManagers(dbProxy, managers, exceptionMessages);
            EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(exceptionMessages, log);
            return dbProxy.getId();
        } else return null;
    }

    @Transactional
    public void createProxies(Long id, Long customerDetailVersionId, List<ProxyEditRequest> proxyList, Long contractDetailId, List<String> exceptionMessages) {
        if (!CollectionUtils.isEmpty(proxyList)) {
            log.debug("Creating proxy object: {}", proxyList);
            Optional<Customer> customerOptional = customerRepository.findByIdAndStatuses(id, List.of(CustomerStatus.ACTIVE));
            if (customerOptional.isPresent()) {
                Customer customer = customerOptional.get();
                if (!customer.getCustomerType().equals(CustomerType.LEGAL_ENTITY) && proxyList.size() > 1) {
                    exceptionMessages.add("proxy-[proxy] you cant add multiple proxy when customer type is not legal entity;");
                }
                Optional<CustomerDetails> customerDetailsOptional = customerDetailsRepository.findByCustomerIdAndVersionId(customer.getId(), customerDetailVersionId);
                if (customerDetailsOptional.isPresent()) {
                    CustomerDetails customerDetails = customerDetailsOptional.get();
                    for (ProxyAddRequest request : proxyList) {
                        Set<ConstraintViolation<ProxyAddRequest>> violations = validator.validate(request);
                        if (!violations.isEmpty()) {
                            StringBuilder sb = new StringBuilder();
                            for (ConstraintViolation<ProxyAddRequest> constraintViolation : violations) {
                                sb.append(constraintViolation.getMessage());
                            }
                            exceptionMessages.add(sb.toString());
                        }
                        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(exceptionMessages, log);
                        List<Manager> managers = getManagers(customer, customerDetails.getId(), request, exceptionMessages);
                        List<ProductContractProxyFile> proxyFile = getProxyFiles(request.getFileIds(), exceptionMessages);
                        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(exceptionMessages, log);
                        ProductContractProxy proxy = mapProxyRequestToProxyEntity(request, contractDetailId);
                        ProductContractProxy dbProxy = proxyRepository.saveAndFlush(proxy);
                        updateProxyFiles(dbProxy, proxyFile, exceptionMessages);
                        updateProxyManagers(dbProxy, managers, exceptionMessages);
                        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(exceptionMessages, log);
                    }
                } else {
                    exceptionMessages.add("proxy-[proxy] customerDetails can't be found;");
                }

            }
        }
    }

    @Transactional
    public void updateProxies(Long id, Long customerDetailVersionId, List<ProxyEditRequest> proxyEditList, Long contractDetailId, List<String> exceptionMessages) {
        if (CollectionUtils.isNotEmpty(proxyEditList)) {
            log.debug("Update proxy object: {}", proxyEditList);
            List<Long> updatedProxyIds = new ArrayList<>();
            Optional<Customer> customerOptional = customerRepository.findByIdAndStatuses(id, List.of(CustomerStatus.ACTIVE));
            if (customerOptional.isPresent()) {
                Customer customer = customerOptional.get();
                if (!customer.getCustomerType().equals(CustomerType.LEGAL_ENTITY) && proxyEditList.size() > 1) {
                    exceptionMessages.add("proxy-[proxy] you cant add multiple proxy when customer type is not legal entity;");
                }
                Optional<CustomerDetails> customerDetailsOptional = customerDetailsRepository.findByCustomerIdAndVersionId(customer.getId(), customerDetailVersionId);
                if (customerDetailsOptional.isPresent()) {
                    CustomerDetails customerDetails = customerDetailsOptional.get();
                    Long customerDetailId = customerDetails.getId();
                    List<ProductContractProxy> dbProxies = proxyRepository.findByContractDetailIdAndStatusIn(contractDetailId, List.of(ContractSubObjectStatus.ACTIVE));
                    if (dbProxies.isEmpty()) {
                        List<ProxyEditRequest> proxiesToCreate = proxyEditList;
                        for (ProxyEditRequest request : proxiesToCreate) {
                            updatedProxyIds.add(create(customer, customerDetailId, request, contractDetailId, exceptionMessages));
                        }
                    } else {
                        //List<ProxyEditRequest> shouldBeDeleted = new ArrayList<>();
                        List<Long> shouldBeDeleted = new ArrayList<>();
                        List<ProxyEditRequest> shouldBeCreated = new ArrayList<>();
                        List<ProxyEditRequest> shouldBeEdited = getProxies(proxyEditList, dbProxies, shouldBeDeleted, shouldBeCreated);
                        if (CollectionUtils.isNotEmpty(shouldBeEdited)) {
                            for (ProxyEditRequest request : shouldBeEdited) {
                                updatedProxyIds.add(edit(customer, customerDetailId, request, request.getId(), exceptionMessages));
                            }
                        }
                        if (CollectionUtils.isNotEmpty(shouldBeDeleted)) {
                            for (Long proxyId : shouldBeDeleted) {
                                Optional<ProductContractProxy> proxyOptional = proxyRepository.findByIdAndStatus(proxyId, ContractSubObjectStatus.ACTIVE);
                                if (proxyOptional.isPresent()) {
                                    ProductContractProxy proxy = proxyOptional.get();
                                    proxy.setStatus(ContractSubObjectStatus.DELETED);
                                    proxyRepository.save(proxy);
                                } else {
                                    exceptionMessages.add("Can't find active proxy with id: %s;".formatted(proxyId));
                                }
                            }
                        }
                        if (CollectionUtils.isNotEmpty(shouldBeCreated)) {
                            for (ProxyEditRequest request : shouldBeCreated) {
                                updatedProxyIds.add(create(customer, customerDetailId, request, contractDetailId, exceptionMessages));
                            }
                        }
                    }
                } else {
                    exceptionMessages.add("proxy-[proxy] can't find customerDetails;");
                }
            } else {
                exceptionMessages.add("proxy-[proxy] can't find customer;");
            }
        } else {
            List<ProductContractProxy> proxies = proxyRepository.getProxiesByContractDetailIdAndStatus(contractDetailId, ContractSubObjectStatus.ACTIVE);
            if (CollectionUtils.isNotEmpty(proxies)) {
                for (ProductContractProxy item : proxies) {
                    item.setStatus(ContractSubObjectStatus.DELETED);
                    proxyRepository.save(item);
                }
            }
            // proxyRepository.deleteAllByContractDetailsId(contractDetailId, ContractSubObjectStatus.ACTIVE);
        }
    }

    @Transactional
    public Long edit(Customer customer, Long customerDetailId, @Valid ProxyAddRequest request, Long proxyId, List<String> exceptionMessages) {
        log.debug("Update proxy object: {}", request.toString());
        Set<ConstraintViolation<ProxyAddRequest>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (ConstraintViolation<ProxyAddRequest> constraintViolation : violations) {
                sb.append(constraintViolation.getMessage());
            }
            exceptionMessages.add(sb.toString());
        }
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(exceptionMessages, log);
        Optional<ProductContractProxy> proxyOptional = proxyRepository.findByIdAndStatus(proxyId, ContractSubObjectStatus.ACTIVE);
        if (proxyOptional.isPresent()) {
            ProductContractProxy proxy = proxyOptional.get();
            ProductContractProxy updatedProxy = mapProxyRequestToProxyEntity(request, proxy.getContractDetailId());
            List<ProductContractProxyFile> proxyFiles = proxyFilesRepository.findByContractProxyIdAndStatus(proxy.getId(), ContractSubObjectStatus.ACTIVE);
            List<ProxyManagers> proxyManagers = proxyManagersRepository.findByContractProxyIdAndStatus(proxy.getId(), ContractSubObjectStatus.ACTIVE);
            editProxyFiles(proxy, updatedProxy, proxyFiles, request.getFileIds());
            List<Manager> managers = getManagers(customer, customerDetailId, request, exceptionMessages);
            editManagers(proxy, proxyManagers, request.getManagerIds(), managers);
            EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(exceptionMessages, log);
        } else {
            exceptionMessages.add("id-can't find proxy with id: %s;".formatted(proxyId));
        }
        return proxyId;
    }

    private void editManagers(ProductContractProxy proxy, List<ProxyManagers> proxyManagers, Set<Long> managerIds, List<Manager> managers) {
        Set<Long> notInManagerIds = new HashSet<>(managerIds);
        Set<Long> shouldBeUpdated = new HashSet<>();
        if (CollectionUtils.isEmpty(managerIds)) {
            if (CollectionUtils.isNotEmpty(proxyManagers)) {
                List<ProxyManagers> proxyManagersToDelete = proxyManagersRepository.findByContractProxyIdAndStatus(proxy.getId(), ContractSubObjectStatus.ACTIVE);
                if (CollectionUtils.isNotEmpty(proxyManagersToDelete)) {
                    for (ProxyManagers item : proxyManagersToDelete) {
                        item.setStatus(ContractSubObjectStatus.DELETED);
                        proxyManagersRepository.save(item);
                    }
                }
            }
        }
        for (ProxyManagers proxyManager : proxyManagers) {
            Long fileId = proxyManager.getCustomerManagerId();
            Long actualId = proxyManager.getId();
            if (managerIds.contains(fileId)) {
                shouldBeUpdated.add(actualId);
                notInManagerIds.remove(fileId);
            }
        }
        if (CollectionUtils.isNotEmpty(shouldBeUpdated)) {
            List<ProxyManagers> proxyManagersShouldBeDeleted = proxyManagersRepository.findByContractProxyIdAndIdNotInAndStatus(proxy.getId(), shouldBeUpdated, ContractSubObjectStatus.ACTIVE);
            if (CollectionUtils.isNotEmpty(proxyManagersShouldBeDeleted)) {
                for (ProxyManagers item : proxyManagersShouldBeDeleted) {
                    item.setStatus(ContractSubObjectStatus.DELETED);
                    proxyManagersRepository.save(item);
                }
            }
        }
        if (CollectionUtils.isNotEmpty(notInManagerIds)) {
            for (Long id : notInManagerIds) {
                ProxyManagers proxyManagers1 = new ProxyManagers();
                proxyManagers1.setContractProxyId(proxy.getId());
                proxyManagers1.setCustomerManagerId(id);
                proxyManagers1.setStatus(ContractSubObjectStatus.ACTIVE);
                proxyManagersRepository.save(proxyManagers1);
            }
        }

    }

    private void editProxyFiles(ProductContractProxy proxy, ProductContractProxy updatedProxy, List<ProductContractProxyFile> proxyFiles, Set<Long> fileIds) {
        Set<Long> notInProxyFiles = new HashSet<>(fileIds);
        Set<Long> shouldBeUpdated = new HashSet<>();
        if (CollectionUtils.isEmpty(fileIds)) {
            if (CollectionUtils.isNotEmpty(proxyFiles)) {
                List<ProductContractProxyFile> proxyFilesToDelete = proxyFilesRepository.findByContractProxyIdAndStatus(proxy.getId(), ContractSubObjectStatus.ACTIVE);
                if (CollectionUtils.isNotEmpty(proxyFilesToDelete)) {
                    for (ProductContractProxyFile item : proxyFilesToDelete) {
                        item.setStatus(ContractSubObjectStatus.DELETED);
                        proxyFilesRepository.save(item);
                    }
                }
            }
        }
        for (ProductContractProxyFile proxyFile : proxyFiles) {
            Long fileId = proxyFile.getId();
            if (fileIds.contains(fileId)) {
                shouldBeUpdated.add(fileId);
                notInProxyFiles.remove(fileId);
            }
        }

        if (CollectionUtils.isNotEmpty(shouldBeUpdated)) {
            List<ProductContractProxyFile> proxiesShouldBeDeleted = proxyFilesRepository.findByContractProxyIdAndIdNotInAndStatus(proxy.getId(), shouldBeUpdated, ContractSubObjectStatus.ACTIVE);
            if (CollectionUtils.isNotEmpty(proxiesShouldBeDeleted)) {
                for (ProductContractProxyFile item : proxiesShouldBeDeleted) {
                    item.setStatus(ContractSubObjectStatus.DELETED);
                    proxyFilesRepository.save(item);
                }
            }
        }
        if (CollectionUtils.isNotEmpty(notInProxyFiles)) {
            for (Long id : notInProxyFiles) {
                ProductContractProxyFile proxyFile = proxyFilesRepository.findByIdAndStatus(id, ContractSubObjectStatus.ACTIVE)
                        .orElseThrow(() -> new DomainEntityNotFoundException("id-can't find Active proxyFiles with id: %s;".formatted(id)));
                proxyFile.setContractProxyId(proxy.getId());
                proxyFilesRepository.save(proxyFile);
            }
        }
    }

    private ProductContractProxy mapProxyRequestToProxyEntity(ProxyAddRequest request, Long contractDetailId) {
        return ProductContractProxy.builder()
                .proxyName(request.getProxyName())
                .proxyForeignEntityPerson(request.getProxyForeignEntityPerson())
                .proxyPersonalIdentifier(request.getProxyCustomerIdentifier())
                .proxyEmail(request.getProxyEmail())
                .proxyMobilePhone(request.getProxyPhone())
                .proxyAttorneyPowerNumber(request.getProxyPowerOfAttorneyNumber())
                .proxyDate(request.getProxyData())
                .proxyValidTill(request.getProxyValidTill())
                .proxyNotaryPublic(request.getNotaryPublic())
                .proxyRegistrationNumber(request.getRegistrationNumber())
                .proxyOperationArea(request.getAreaOfOperation())
                .proxyByProxyForeignEntityPerson(request.getAuthorizedProxyForeignEntityPerson() != null ? request.getAuthorizedProxyForeignEntityPerson() : false)
                .proxyByProxyName(request.getProxyAuthorizedByProxy())
                .proxyByProxyPersonalIdentifier(request.getAuthorizedProxyCustomerIdentifier())
                .proxyByProxyEmail(request.getAuthorizedProxyEmail())
                .proxyByProxyMobilePhone(request.getAuthorizedProxyPhone())
                .proxyByProxyAttorneyPowerNumber(request.getAuthorizedProxyPowerOfAttorneyNumber())
                .proxyByProxyDate(request.getAuthorizedProxyData())
                .proxyByProxyValidTill(request.getAuthorizedProxyValidTill())
                .proxyByProxyNotaryPublic(request.getAuthorizedProxyNotaryPublic())
                .proxyByProxyRegistrationNumber(request.getAuthorizedProxyRegistrationNumber())
                .proxyByProxyOperationArea(request.getAuthorizedProxyAreaOfOperation())
                .status(ContractSubObjectStatus.ACTIVE)
                .contractDetailId(contractDetailId)
                .build();
    }

    public List<ProxyResponse> preview(Long contractDetailId) {
        List<ProxyResponse> proxyResponses = new ArrayList<>();
        List<ProductContractProxy> proxyList = proxyRepository.findByContractDetailIdAndStatusIn(contractDetailId, List.of(ContractSubObjectStatus.ACTIVE));
        if (CollectionUtils.isEmpty(proxyList)) {
            return null;
        }
        for (ProductContractProxy proxy : proxyList) {
            List<ProductContractProxyFile> proxyFiles = proxyFilesRepository.findByContractProxyIdAndStatus(proxy.getId(), ContractSubObjectStatus.ACTIVE);
            List<ProxyManagers> proxyManagers = proxyManagersRepository.findByContractProxyIdAndStatus(proxy.getId(), ContractSubObjectStatus.ACTIVE);
            proxyResponses.add(mapProxyResponse(proxy, proxyFiles, proxyManagers));
        }

        return proxyResponses;

    }

    public Long delete(Long proxyId, List<String> exceptionMessages) {
        Optional<ProductContractProxy> proxyOptional = proxyRepository.findById(proxyId);

        if (proxyOptional.isPresent()) {
            ProductContractProxy proxy = proxyOptional.get();
            if (proxy.getStatus().equals(ContractSubObjectStatus.DELETED)) {
                exceptionMessages.add("id-Proxy with id: %s is already deleted;".formatted(proxyId));
                return null;
            }
            proxy.setStatus(ContractSubObjectStatus.DELETED);
            ProductContractProxy dbProxy = proxyRepository.save(proxy);
            return dbProxy.getId();
        } else {
            exceptionMessages.add("id-Can't find proxy with id: %s".formatted(proxyId));
            return null;
        }

    }

    private ProxyResponse mapProxyResponse(ProductContractProxy proxy, List<ProductContractProxyFile> proxyFiles, List<ProxyManagers> proxyManagers) {
        return ProxyResponse.builder()
                .id(proxy.getId())
                .proxyForeignEntityPerson(proxy.getProxyForeignEntityPerson())
                .proxyName(proxy.getProxyName())
                .proxyCustomerIdentifier(proxy.getProxyPersonalIdentifier())
                .proxyEmail(proxy.getProxyEmail())
                .proxyPhone(proxy.getProxyMobilePhone())
                .proxyPowerOfAttorneyNumber(proxy.getProxyAttorneyPowerNumber())
                .proxyData(proxy.getProxyDate())
                .proxyValidTill(proxy.getProxyValidTill())
                .notaryPublic(proxy.getProxyNotaryPublic())
                .registrationNumber(proxy.getProxyRegistrationNumber())
                .areaOfOperation(proxy.getProxyOperationArea())
                .authorizedProxyForeignEntityPerson(proxy.getProxyByProxyForeignEntityPerson())
                .proxyAuthorizedByProxy(proxy.getProxyByProxyName())
                .authorizedProxyCustomerIdentifier(proxy.getProxyByProxyPersonalIdentifier())
                .authorizedProxyEmail(proxy.getProxyByProxyEmail())
                .authorizedProxyPhone(proxy.getProxyByProxyMobilePhone())
                .authorizedProxyPowerOfAttorneyNumber(proxy.getProxyByProxyAttorneyPowerNumber())
                .authorizedProxyData(proxy.getProxyByProxyDate())
                .authorizedProxyValidTill(proxy.getProxyByProxyValidTill())
                .authorizedProxyNotaryPublic(proxy.getProxyByProxyNotaryPublic())
                .authorizedProxyRegistrationNumber(proxy.getProxyByProxyRegistrationNumber())
                .authorizedProxyAreaOfOperation(proxy.getProxyByProxyOperationArea())
                .status(proxy.getStatus())
                .proxyFiles(mapProxyFiles(proxyFiles))
                .proxyManagers(mapProxyManagers(proxyManagers))
                .build();

    }

    private List<ProxyManagersResponse> mapProxyManagers(List<ProxyManagers> proxyManagers) {
        List<ProxyManagersResponse> responses = new ArrayList<>();
        for (ProxyManagers item : proxyManagers) {
            ProxyManagersResponse proxyManagersResponse = new ProxyManagersResponse();
            proxyManagersResponse.setId(item.getId());
            Manager manager = getManager(item.getCustomerManagerId());
            if (manager != null) {
                proxyManagersResponse.setManagerName(manager.getName());
                proxyManagersResponse.setManagerMiddleName(manager.getMiddleName());
                proxyManagersResponse.setManagerSurName(manager.getSurname());
            }

            proxyManagersResponse.setContractProxyId(item.getContractProxyId());
            proxyManagersResponse.setCustomerManagerId(item.getCustomerManagerId());
            proxyManagersResponse.setStatus(item.getStatus());
            responses.add(proxyManagersResponse);
        }
        return responses;
    }

    private Manager getManager(Long customerManagerId) {
        Optional<Manager> accountManagerOptional = managerRepository.findById(customerManagerId);
        if (accountManagerOptional.isPresent()) {
            Manager manager = accountManagerOptional.get();
            return manager;
        }
        return null;
    }

    private List<ProxyFilesResponse> mapProxyFiles(List<ProductContractProxyFile> proxyFiles) {
        List<ProxyFilesResponse> responses = new ArrayList<>();
        for (ProductContractProxyFile item : proxyFiles) {
            ProxyFilesResponse proxyFileResponse = new ProxyFilesResponse();
            proxyFileResponse.setId(item.getId());
            proxyFileResponse.setName(item.getName());
            proxyFileResponse.setFileUrl(item.getFileUrl());
            proxyFileResponse.setContractProxyId(item.getContractProxyId());
            proxyFileResponse.setStatus(item.getStatus());
            responses.add(proxyFileResponse);
        }
        return responses;
    }


    private void updateProxyManagers(ProductContractProxy proxy, List<Manager> managers, List<String> exceptionMessages) {
        Long proxyId = proxy.getId();
        List<ProxyManagers> proxyManagers = new ArrayList<>();
        for (Manager item : managers) {
            ProxyManagers proxyManager = new ProxyManagers();
            proxyManager.setContractProxyId(proxyId);
            proxyManager.setCustomerManagerId(item.getId());
            proxyManager.setStatus(ContractSubObjectStatus.ACTIVE);
            proxyManagers.add(proxyManager);
        }
        if (CollectionUtils.isNotEmpty(proxyManagers)) {
            proxyManagersRepository.saveAll(proxyManagers);
        }
    }

    private void updateProxyFiles(ProductContractProxy proxy, List<ProductContractProxyFile> proxyFiles, List<String> exceptionMessages) {
        if (CollectionUtils.isNotEmpty(proxyFiles)) {
            Long proxyId = proxy.getId();
            for (ProductContractProxyFile item : proxyFiles) {
                item.setContractProxyId(proxyId);
                proxyFilesRepository.save(item);
            }
        }
    }

    private List<ProductContractProxyFile> getProxyFiles(Set<Long> fileIds, List<String> exceptionMessages) {
        if (CollectionUtils.isNotEmpty(fileIds)) {
            List<ProductContractProxyFile> proxyFiles = new ArrayList<>();
            for (Long id : fileIds) {
                Optional<ProductContractProxyFile> proxyFile =
                        proxyFilesRepository.findByIdAndStatus(id, ContractSubObjectStatus.ACTIVE);
                if (proxyFile.isPresent()) {
                    ProductContractProxyFile dbProxyFile = proxyFile.get();
                    if (proxyFile.get().getContractProxyId() != null) {
                        ProductContractProxyFile productContractProxyFiles = ProductContractProxyFile.builder().
                                name(dbProxyFile.getName())
                                .fileUrl(dbProxyFile.getFileUrl())
                                .contractProxyId(null)
                                .status(ContractSubObjectStatus.ACTIVE)
                                .build();
                        ProductContractProxyFile savedProxyFile = proxyFilesRepository.save(productContractProxyFiles);
                        proxyFiles.add(savedProxyFile);
                    } else proxyFiles.add(proxyFile.get());
                } else {
                    exceptionMessages.add("fileIds-[FileIds] can't find active file with id: %s;".formatted(id));
                }
            }
            return proxyFiles;
        } else
            return null;
    }

    private List<Manager> getManagers(Customer customer, Long customerDetailId, ProxyAddRequest request, List<String> exceptionMessages) {
        List<Manager> managers = new ArrayList<>();
        Set<Long> managerIds = request.getManagerIds();
        if (!customer.getCustomerType().equals(CustomerType.PRIVATE_CUSTOMER)) {
            if (!CollectionUtils.isEmpty(managerIds)) {
                for (Long id : managerIds) {
                    Optional<Manager> manager = managerRepository.findByIdAndStatus(id, Status.ACTIVE);
                    if (manager.isEmpty()) {
                        exceptionMessages.add("managerIds-[managerIds] can't find account manager with id: %s;".formatted(id));
                    } else {
                        if (manager.get().getCustomerDetailId().equals(customerDetailId)) {
                            managers.add(manager.get());
                        } else {
                            exceptionMessages.add("managerIds-[managerIds] this manager doesn't belong to the customer: %s;".formatted(id));
                        }
                    }
                }
            } else {
                exceptionMessages.add("managerIds-[managerIds] should be present when customer is LEGAL_ENTITY or PRIVATE_CUSTOMER_WITH_BUSINESS_ACTIVITY;");
            }
        } else {
            if (!CollectionUtils.isEmpty(managers)) {
                exceptionMessages.add("managerIds-[managerIds] should not be present when customer is PRIVATE_CUSTOMER;");
            }
        }
        return managers;
    }


}
