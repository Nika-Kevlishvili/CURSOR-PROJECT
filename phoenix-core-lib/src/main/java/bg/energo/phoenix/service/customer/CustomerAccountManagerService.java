package bg.energo.phoenix.service.customer;

import bg.energo.common.portal.api.appTag.AppTag;
import bg.energo.common.portal.api.appUser.AppUserInfoDto;
import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.model.customAnotations.aspects.ExecutionTimeLogger;
import bg.energo.phoenix.model.entity.customer.*;
import bg.energo.phoenix.model.entity.nomenclature.customer.AccountManagerType;
import bg.energo.phoenix.model.enums.customer.Status;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.request.customer.accountManager.GetAccountManagerRequest;
import bg.energo.phoenix.model.request.customer.customerAccountManager.CreateCustomerAccountManagerRequest;
import bg.energo.phoenix.model.request.customer.customerAccountManager.EditCustomerAccountManagerRequest;
import bg.energo.phoenix.model.response.communication.portal.customerAccountManager.PortalCustomerAccountManager;
import bg.energo.phoenix.model.response.customer.customerAccountManager.AccountManagerResponse;
import bg.energo.phoenix.model.response.customer.customerAccountManager.CustomerAccountManagerResponse;
import bg.energo.phoenix.repository.customer.AccountManagerRepository;
import bg.energo.phoenix.repository.customer.AccountManagerTagRepository;
import bg.energo.phoenix.repository.customer.CustomerAccountManagerRepository;
import bg.energo.phoenix.repository.customer.PortalTagRepository;
import bg.energo.phoenix.repository.nomenclature.customer.AccountManagerTypeRepository;
import bg.energo.phoenix.util.epb.EPBStringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * <h2>CustomerAccountManagerService</h2>
 * Customer Account Manager Service
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerAccountManagerService {
    private final AccountManagerRepository accountManagerRepository;
    private final CustomerAccountManagerRepository customerAccountManagerRepository;
    private final AccountManagerTypeRepository accountManagerTypeRepository;
    private final PortalTagRepository portalTagRepository;
    private final AccountManagerTagRepository accountManagerTagRepository;

    /**
     * <h2>Create Customer Account Managers</h2>
     * Create {@link CustomerAccountManager} logic for presented {@link CustomerDetails}.
     * <ul>
     *     <li>If presented {@link CustomerDetails} is null or {@link CustomerDetails#id} is null, method will immediately return, exception will be added in {@link List<String> List&lt;String&gt; exceptionMessages}</li>
     *     <li>If {@link AccountManagerType} entity with presented {@link CreateCustomerAccountManagerRequest#accountManagerTypeId} does not exists in database, exception will be added in {@link List<String> List&lt;String&gt; exceptionMessages}</li>
     *     <li>If {@link AccountManager} entity with presented {@link CreateCustomerAccountManagerRequest#accountManagerId} does not exists in database, exception will be added in {@link List<String> List&lt;String&gt; exceptionMessages}</li>
     * </ul>
     * <p>
     * At the end of method execution, if {@link List<String> List&lt;String&gt; exceptionMessages}{@link List#isEmpty() .isEmpty()}, {@link CustomerAccountManager} will be saved in database, else
     * transaction will be rolled back,
     *
     * @param customerAccountManagers - {@link List<CreateCustomerAccountManagerRequest> List&lt;CreateCustomerAccountManagerRequest&gt;} - list of Customer Account Managers that must be created for presented {@link CustomerDetails}
     * @param customerDetails         - {@link CustomerDetails} - presented Customer Details
     * @param exceptionMessages       - {@link List<String> List&lt;String&gt;} - live list of exceptions messages that handled while method runs
     * @return {@link List<String> List&lt;String&gt; exceptionsMessages}
     */
    @Transactional
    public void createCustomerAccountManagers(List<CreateCustomerAccountManagerRequest> customerAccountManagers, CustomerDetails customerDetails, List<String> exceptionMessages) {
        if (!CollectionUtils.isEmpty(customerAccountManagers)) {
            List<CustomerAccountManager> validCustomerAccountManagers = new ArrayList<>();

            for (int i = 0; i < customerAccountManagers.size(); i++) {
                CreateCustomerAccountManagerRequest customerAccountManagerRequest = customerAccountManagers.get(i);
                Optional<AccountManager> accountManagerOptional = accountManagerRepository
                        .findByIdAndStatus(
                                customerAccountManagerRequest.getAccountManagerId(),
                                List.of(Status.ACTIVE));
                if (accountManagerOptional.isPresent()) {
                    Optional<AccountManagerType> accountManagerTypeOptional = accountManagerTypeRepository
                            .findByIdAndStatus(
                                    customerAccountManagerRequest.getAccountManagerTypeId(),
                                    List.of(NomenclatureItemStatus.ACTIVE));
                    validateAndCreateManagers(customerDetails, exceptionMessages, validCustomerAccountManagers, i, customerAccountManagerRequest, accountManagerOptional, accountManagerTypeOptional);
                } else {
                    log.error(String.format("Active Account Manager with id [%s], not found", customerAccountManagerRequest.getAccountManagerId()));
                    exceptionMessages.add(String.format("accountManagers[%s].accountManagerId-Active Account Manager with id [%s], not found;", i, customerAccountManagerRequest.getAccountManagerId()));
                }
            }

            if (exceptionMessages.isEmpty()) {
                customerAccountManagerRepository.saveAll(validCustomerAccountManagers);
            }
        }
    }

    public void createCustomerAccountManagersForNewVersion(List<CreateCustomerAccountManagerRequest> customerAccountManagers, CustomerDetails customerDetails, CustomerDetails oldDetails, List<String> exceptionMessages) {
        if (!CollectionUtils.isEmpty(customerAccountManagers)) {
            List<CustomerAccountManager> validCustomerAccountManagers = new ArrayList<>();
            List<Long> managerTypeIds = customerAccountManagerRepository.getByCustomerDetailsIdAndStatus(oldDetails.getId(), Status.ACTIVE).stream().map(x -> x.getAccountManagerType().getId()).toList();
            for (int i = 0; i < customerAccountManagers.size(); i++) {
                CreateCustomerAccountManagerRequest customerAccountManagerRequest = customerAccountManagers.get(i);
                Optional<AccountManager> accountManagerOptional = accountManagerRepository
                        .findByIdAndStatus(
                                customerAccountManagerRequest.getAccountManagerId(),
                                List.of(Status.ACTIVE));
                if (accountManagerOptional.isPresent()) {
                    Optional<AccountManagerType> accountManagerTypeOptional = accountManagerTypeRepository
                            .findByIdAndStatus(
                                    customerAccountManagerRequest.getAccountManagerTypeId(),
                                    getManagerStatuses(customerAccountManagerRequest.getAccountManagerTypeId(), managerTypeIds));
                    validateAndCreateManagers(customerDetails, exceptionMessages, validCustomerAccountManagers, i, customerAccountManagerRequest, accountManagerOptional, accountManagerTypeOptional);
                } else {
                    log.error(String.format("Active Account Manager with id [%s], not found", customerAccountManagerRequest.getAccountManagerId()));
                    exceptionMessages.add(String.format("accountManagers[%s].accountManagerId-Active Account Manager with id [%s], not found;", i, customerAccountManagerRequest.getAccountManagerId()));
                }
            }

            if (exceptionMessages.isEmpty()) {
                customerAccountManagerRepository.saveAll(validCustomerAccountManagers);
            }
        }
    }

    private void validateAndCreateManagers(CustomerDetails customerDetails, List<String> exceptionMessages, List<CustomerAccountManager> validCustomerAccountManagers, int i, CreateCustomerAccountManagerRequest customerAccountManagerRequest, Optional<AccountManager> accountManagerOptional, Optional<AccountManagerType> accountManagerTypeOptional) {
        if (accountManagerTypeOptional.isPresent()) {
            CustomerAccountManager customerAccountManager = createActiveCustomerAccountManager(customerDetails, accountManagerOptional.get(), accountManagerTypeOptional.get());

            validCustomerAccountManagers.add(customerAccountManager);
        } else {
            log.error(String.format("Active account manager type not found for given id [%s], account manager creation for user [%s] failed", customerAccountManagerRequest.getAccountManagerTypeId(), accountManagerOptional.get().getUserName()));
            exceptionMessages.add(String.format("accountManagers[%s].accountManagerTypeId-Active account manager type not found for given id [%s], account manager creation for user [%s] failed;", i, customerAccountManagerRequest.getAccountManagerTypeId(), accountManagerOptional.get().getUserName()));
        }
    }

    private List<NomenclatureItemStatus> getManagerStatuses(Long accountManagerTypeId, List<Long> managerTypeIds) {
        if (managerTypeIds.contains(accountManagerTypeId)) {
            return List.of(NomenclatureItemStatus.ACTIVE, NomenclatureItemStatus.INACTIVE);
        }
        return List.of(NomenclatureItemStatus.ACTIVE);
    }

    /**
     * <h2>Edit Customer Account Managers</h2>
     * Edit {@link CustomerAccountManager} logic for presented {@link CustomerDetails}.
     * <ul>
     *     <li>If presented {@link CustomerDetails} is null or {@link CustomerDetails#id} is null, method will immediately return, exception will be added in {@link List<String> List&lt;String&gt; exceptionMessages}</li>
     *     <li>If {@link AccountManagerType} entity with presented {@link EditCustomerAccountManagerRequest#accountManagerTypeId} does not exists in database, exception will be added in {@link List<String> List&lt;String&gt; exceptionMessages}</li>
     *     <li>If {@link AccountManager} entity with presented {@link EditCustomerAccountManagerRequest#accountManagerId} does not exists in database, exception will be added in {@link List<String> List&lt;String&gt; exceptionMessages}</li>
     *     <li>If {@link CustomerAccountManager} entity with presented {@link EditCustomerAccountManagerRequest#id} does not assigned for presented {@link CustomerDetails}, exception will be added in {@link List<String> List&lt;String&gt; exceptionMessages}</li>
     *     <li>If {@link AccountManager} entity with presented {@link EditCustomerAccountManagerRequest#accountManagerId} has {@link NomenclatureItemStatus}.{@link AccountManagerType#status INACTIVE} and requested operation for current {@link AccountManager} is "EDIT" and persisted {@link NomenclatureItemStatus} is different from the requested one, exception will be added in {@link List<String> List&lt;String&gt; exceptionMessages}</li>
     *     <li>If {@link AccountManager} entity with presented {@link EditCustomerAccountManagerRequest#accountManagerId} has {@link NomenclatureItemStatus}.{@link AccountManagerType#status INACTIVE} and requested operation for current {@link AccountManager} is "CREATE", exception will be added in {@link List<String> List&lt;String&gt; exceptionMessages}</li>
     *     <li>If {@link AccountManager} entity with presented {@link EditCustomerAccountManagerRequest#accountManagerId} is already assigned for presented {@link CustomerDetails} and has {@link Status#ACTIVE}, exception will be added in {@link List<String> List&lt;String&gt; exceptionMessages}</li>
     * </ul>
     * At the end of method execution, if {@link List<String> List&lt;String&gt; exceptionMessages}{@link List#isEmpty() .isEmpty()}, {@link CustomerAccountManager} will be saved in database, else
     * transaction will be rolled back,
     *
     * @param customerAccountManagers - {@link List<CreateCustomerAccountManagerRequest> List&lt;CreateCustomerAccountManagerRequest&gt;} - list of Customer Account Managers that must be created for presented {@link CustomerDetails}
     * @param customerDetails         - {@link CustomerDetails} - presented Customer Details
     * @param exceptionMessages       - {@link List<String> List&lt;String&gt;} - live list of exceptions messages that handled while method runs
     * @return {@link List<String> List&lt;String&gt; exceptionsMessages}
     */
    @Transactional
    public void editCustomerAccountManagers(List<EditCustomerAccountManagerRequest> customerAccountManagers, CustomerDetails customerDetails, List<String> exceptionMessages) {
        if (CollectionUtils.isEmpty(customerAccountManagers)) {
            customerAccountManagers = Collections.emptyList();
        }

        if (customerDetails == null || customerDetails.getId() == null) {
            log.error("Customer details object is null, cannot edit account managers");
            exceptionMessages.add("id-Customer details object is null, cannot edit account managers;");
            return;
        }

        customerAccountManagers.stream().collect(Collectors.groupingBy(editCustomerAccountManagerRequest -> editCustomerAccountManagerRequest.getAccountManagerId())).forEach((k, v) -> {
            if (v.size() > 1) {
                exceptionMessages.add(String.format("accountManagers-The same Account Manager with id [%s] cannot be assigned to customer details multiple times;", k));
            }
        });

        List<CustomerAccountManager> validCustomerAccountManagers = new ArrayList<>();
        List<CustomerAccountManager> allCustomerAccountManagersByCustomerDetails = customerAccountManagerRepository.getByCustomerDetailsIdAndStatus(customerDetails.getId(), Status.ACTIVE);

        for (int i = 0; i < customerAccountManagers.size(); i++) {
            EditCustomerAccountManagerRequest request = customerAccountManagers.get(i);
            Optional<AccountManager> accountManagerOptional = accountManagerRepository.findByIdAndStatus(request.getAccountManagerId(), List.of(Status.ACTIVE));
            if (request.getId() != null) {
                if (accountManagerOptional.isPresent()) {
                    Optional<CustomerAccountManager> customerAccountManagerOptional = allCustomerAccountManagersByCustomerDetails.stream().filter(cm -> Objects.equals(cm.getId(), request.getId())).findAny();
                    if (customerAccountManagerOptional.isPresent()) {
                        CustomerAccountManager customerAccountManager = customerAccountManagerOptional.get();

                        Optional<AccountManagerType> accountManagerTypeOptional = accountManagerTypeRepository
                                .findByIdAndStatus(
                                        request.getAccountManagerTypeId(),
                                        List.of(NomenclatureItemStatus.ACTIVE,
                                                NomenclatureItemStatus.INACTIVE));
                        if (accountManagerTypeOptional.isPresent()) {
                            AccountManagerType accountManagerType = accountManagerTypeOptional.get();
                            if (accountManagerType.getStatus() == NomenclatureItemStatus.INACTIVE
                                && !Objects.equals(customerAccountManager.getAccountManagerType().getId(), request.getAccountManagerTypeId())) {
                                log.error(String.format("Cannot change Account Manager Type with INACTIVE id [%s]", request.getAccountManagerTypeId()));
                                exceptionMessages.add(String.format("accountManagers[%s].accountManagerTypeId-Cannot change Account Manager Type with INACTIVE id [%s];", i, request.getAccountManagerTypeId()));
                            } else {
                                Optional<CustomerAccountManager> localCustomerAccountManagerOptional = allCustomerAccountManagersByCustomerDetails.stream()
                                        .filter(
                                                localAccountManager ->
                                                        localAccountManager.getManagerId().equals(request.getId()))
                                        .findAny();
                                if (localCustomerAccountManagerOptional.isPresent()) {
                                    log.error(String.format("Active Account Manager with presented ID [%s] is already assigned to presented Customer Detail", request.getId()));
                                    exceptionMessages.add(String.format("accountManagers[%s].accountManagerId-Active Account Manager with presented ID [%s] is already assigned to presented Customer Detail;", i, request.getId()));
                                } else {
                                    customerAccountManager.setAccountManagerType(accountManagerType);
                                    customerAccountManager.setManagerId(request.getAccountManagerId());

                                    validCustomerAccountManagers.add(customerAccountManager);
                                }
                            }
                        } else {
                            log.error(String.format("Account Manager Type with presented id [%s] not found", request.getAccountManagerTypeId()));
                            exceptionMessages.add(String.format("accountManagers[%s].accountManagerTypeId-Account Manager Type with presented id [%s] not found;", i, request.getAccountManagerTypeId()));
                        }
                    } else {
                        log.error(String.format("Customer Account Manager with presented id [%s] not found", request.getId()));
                        exceptionMessages.add(String.format("accountManagers[%s].accountManagerId-Customer Account Manager with presented id [%s] not found;", i, request.getId()));
                    }
                } else {
                    log.error(String.format("Active Account Manager with presented id [%s] not found", request.getAccountManagerId()));
                    exceptionMessages.add(String.format("accountManagers[%s].accountManagerId-Active Account Manager with presented id [%s] not found;", i, request.getAccountManagerId()));
                }
            } else {
                if (accountManagerOptional.isPresent()) {
                    AccountManager accountManager = accountManagerOptional.get();
                    Optional<CustomerAccountManager> localCustomerAccountManagerOptional = allCustomerAccountManagersByCustomerDetails.stream()
                            .filter(
                                    localAccountManager ->
                                            localAccountManager.getManagerId().equals(accountManager.getId()))
                            .findAny();
                    if (localCustomerAccountManagerOptional.isPresent()) {
                        log.error(String.format("Active Account Manager with presented ID [%s] is already assigned to presented Customer Detail", accountManager.getId()));
                        exceptionMessages.add(String.format("accountManagers[%s].accountManagerId-Active Account Manager with presented ID [%s] is already assigned to presented Customer Detail;", i, accountManager.getId()));
                    } else {
                        Optional<AccountManagerType> accountManagerTypeOptional = accountManagerTypeRepository
                                .findByIdAndStatus(
                                        request.getAccountManagerTypeId(),
                                        List.of(NomenclatureItemStatus.ACTIVE));
                        if (accountManagerTypeOptional.isPresent()) {
                            CustomerAccountManager customerAccountManager = createActiveCustomerAccountManager(customerDetails, accountManager, accountManagerTypeOptional.get());

                            validCustomerAccountManagers.add(customerAccountManager);
                        } else {
                            log.error(String.format("Active Account Manager Type with presented id [%s] not found", request.getAccountManagerTypeId()));
                            exceptionMessages.add(String.format("accountManagers[%s].accountManagerTypeId-Active Account Manager Type with presented id [%s] not found;", i, request.getAccountManagerTypeId()));
                        }
                    }
                } else {
                    log.error(String.format("Active Account Manager with presented id [%s] not found", request.getAccountManagerId()));
                    exceptionMessages.add(String.format("accountManagers[%s].accountManagerId-Active Account Manager with presented id [%s] not found;", i, request.getAccountManagerId()));
                }
            }
        }

        if (exceptionMessages.isEmpty()) {
            allCustomerAccountManagersByCustomerDetails.forEach(customerAccountManager -> {
                if (!validCustomerAccountManagers.contains(customerAccountManager)) {
                    customerAccountManager.setStatus(Status.DELETED);
                    validCustomerAccountManagers.add(customerAccountManager);
                }
            });

            customerAccountManagerRepository.saveAll(validCustomerAccountManagers);
        }
    }

    /**
     * <h2>Update Account Managers From Portal</h2>
     * {@link AccountManager} update main logic in {@link AccountManagerRepository}, for each received active manager from param {@link #updateAccountManagersFromPortal(List) List&lt;PortalCustomerAccountManager&gt;}, method will validate presented entity.
     * <ul>
     *     <li>If entity with presented {@link PortalCustomerAccountManager#userName username} already exists in database, it will be updated with new options.</li>
     *     <li>If entity with presented {@link PortalCustomerAccountManager#userName username} does not exists in database, creating new {@link AccountManager} entity and saving to database.</li>
     *     <li>If entity with presented {@link PortalCustomerAccountManager#userName username} exists in database, but does not received from {@link PortalCustomerAccountManager} response,
     *     entities will be marked as {@link Status#DELETED} and saved in database</li>
     * </ul>
     * If exception handled while saving any {@link AccountManager} entity,
     * method will skip invalid entity, and continue to save remaining entities.
     *
     * @param portalCustomerAccountManagers - list of active account managers received from portal and must be updated
     */
    @ExecutionTimeLogger
    @Transactional
    public void updateAccountManagersFromPortal(List<AppUserInfoDto> portalCustomerAccountManagers) {
        List<AccountManager> listOfAccountManagers = new ArrayList<>();
        List<AccountManager> allLocalAccountManagersInRepository = accountManagerRepository.findAll();
        for (AppUserInfoDto portalCustomerAccountManager : portalCustomerAccountManagers) {
            AccountManager accountManager;
            Optional<AccountManager> accountManagerOptional;
            List<AccountManager> accountManagers = allLocalAccountManagersInRepository
                    .stream()
                    .filter(localAccountManager ->
                            localAccountManager.getUserName().equals(portalCustomerAccountManager.getUserName())
                    ).toList();
            if (accountManagers.size() == 1) {
                accountManagerOptional = Optional.of(accountManagers.get(0));
            } else if (accountManagers.isEmpty()) {
                accountManagerOptional = Optional.empty();
            } else {
                log.error("Found more then one Account Manager with same username, skipping");
                continue;
            }

            if (accountManagerOptional.isPresent()) {
                accountManager = accountManagerOptional.get();

                accountManager.setStatus(Status.valueOf(portalCustomerAccountManager.getStatus()));
                accountManager.setOrganizationalUnit(portalCustomerAccountManager.getUserDepartment());
                accountManager.setFirstName(portalCustomerAccountManager.getUserFirstName());
                accountManager.setLastName(portalCustomerAccountManager.getUserLastName());
                accountManager.setDisplayName(portalCustomerAccountManager.getUserDisplayName());
                accountManager.setEmail(portalCustomerAccountManager.getEmailAddress());
                accountManager.setBusinessUnit(""); // todo -> will be added in API later

                listOfAccountManagers.add(accountManager);
            } else {
                accountManager = mapPortalCustomerAccountManagerToAccountManagerEntity(portalCustomerAccountManager);

                listOfAccountManagers.add(accountManager);
            }
        }

        List<AccountManager> updatedAccountManagersList = new ArrayList<>();

        log.info("Deleting outdated account managers in database");
        for (AccountManager localAccountManager : allLocalAccountManagersInRepository) {
            Optional<AccountManager> portalAccountManagerMatchWithLocal = listOfAccountManagers
                    .stream()
                    .filter(portalAccountManager -> portalAccountManager.getUserName().equals(localAccountManager.getUserName()))
                    .findAny();
            if (portalAccountManagerMatchWithLocal.isEmpty()) {
                if (!localAccountManager.getStatus().equals(Status.DELETED)) {
                    localAccountManager.setStatus(Status.DELETED);
                    updatedAccountManagersList.add(localAccountManager);
                }
            }
        }

        try {
            log.info("Saving account managers to database");
            accountManagerRepository.saveAll(listOfAccountManagers);
            accountManagerRepository.saveAll(updatedAccountManagersList);
        } catch (Exception e) {
            log.error("Exception handled when saving account managers to database", e);
        }
        saveTags(portalCustomerAccountManagers);
    }

    private void saveTags(List<AppUserInfoDto> portalCustomerAccountManagers) {
        Map<String, AccountManager> userMap = accountManagerRepository.findByStatus(List.of(Status.ACTIVE)).stream().collect(Collectors.toMap(AccountManager::getUserName, j -> j));
        Map<String, PortalTag> tags = portalTagRepository.findAll().stream().collect(Collectors.toMap(PortalTag::getPortalId, j -> j));
        Map<Pair<Long, Long>, Long> accountManagerTagMap = accountManagerTagRepository.findAll().stream().collect(Collectors.toMap(x -> Pair.of(x.getAccountManagerId(), x.getPortalTagId()), AccountManagerTag::getId));
        List<AccountManagerTag> tagsToSave=new ArrayList<>();
        for (AppUserInfoDto appUserInfoDto : portalCustomerAccountManagers) {
            AccountManager accountManager = userMap.get(appUserInfoDto.getUserName());
            if(accountManager == null) continue;
            List<AppTag> userTags = appUserInfoDto.getUserTags();
            for (AppTag tag : userTags) {
                PortalTag portalTag = tags.get(tag.getId().toString());
                if(portalTag == null) continue;
                if(accountManagerTagMap.remove(Pair.of(accountManager.getId(),portalTag.getId()))!=null) continue;;
                tagsToSave.add(new AccountManagerTag(portalTag.getId(),accountManager.getId()));
            }
        }
        accountManagerTagRepository.saveAll(tagsToSave);
        accountManagerTagRepository.deleteAllById(accountManagerTagMap.values());
    }

    /**
     * <h2>Get Customer Account Managers By Customer Details ID</h2>
     * Finding in {@link CustomerAccountManagerRepository} all {@link CustomerAccountManager} by requested customer
     * details id, then merging with {@link AccountManager} by id and creates a new {@link CustomerAccountManagerResponse}, then returns
     * merged object
     *
     * @param customerDetailsId - {@link Long} - requested customer details id
     * @return {@link List<CustomerAccountManagerResponse> List&lt;CustomerAccountManagerResponse&gt;}
     */
    public List<CustomerAccountManagerResponse> getCustomerAccountManagersByCustomerDetailsId(Long customerDetailsId) {
        List<CustomerAccountManagerResponse> customerAccountManagerResponses = new ArrayList<>();

        List<CustomerAccountManager> customerAccountManagersByCustomerDetails = customerAccountManagerRepository
                .getByCustomerDetailsIdAndStatus(customerDetailsId, Status.ACTIVE);
        for (CustomerAccountManager customerAccountManager : customerAccountManagersByCustomerDetails) {
            Optional<AccountManager> accountManagerOptional = accountManagerRepository.findById(customerAccountManager.getManagerId());
            if (accountManagerOptional.isPresent()) {
                customerAccountManagerResponses.add(new CustomerAccountManagerResponse(accountManagerOptional.get(), customerAccountManager));
            }
        }

        return customerAccountManagerResponses;
    }


    /**
     * @param request containing search criteria
     * @return all account managers optionally filtered by status and prompt
     */
    public Page<AccountManagerResponse> getAccountManagersByStatus(GetAccountManagerRequest request) {
        log.debug("Getting account managers by status with request: {}", request);
        return accountManagerRepository.filter(
                EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                request.getStatuses(),
                PageRequest.of(request.getPage(), request.getSize())
        );
    }


    /**
     * <h2>Create Active Customer Account Manager</h2>
     * Creates new Customer active Account Manager entity
     *
     * @param customerDetails    - {@link CustomerDetails}
     * @param accountManager     - {@link AccountManager}
     * @param accountManagerType - {@link AccountManagerType}
     * @return {@link CustomerAccountManager}
     */
    private CustomerAccountManager createActiveCustomerAccountManager(CustomerDetails customerDetails, AccountManager accountManager, AccountManagerType accountManagerType) {
        CustomerAccountManager customerAccountManager = new CustomerAccountManager();
        customerAccountManager.setCustomerDetail(customerDetails);
        customerAccountManager.setAccountManagerType(accountManagerType);
        customerAccountManager.setStatus(Status.ACTIVE);
        customerAccountManager.setManagerId(accountManager.getId());

        return customerAccountManager;
    }

    public boolean checkCustomerDetailsAccess(CustomerDetails customerDetails, String loggedInUserId) {
        List<CustomerAccountManager> customerAccountManagers = customerAccountManagerRepository.getByCustomerDetailsIdAndStatus(customerDetails.getId(), Status.ACTIVE);
        for (CustomerAccountManager customerAccountManager : customerAccountManagers) {
            AccountManager accountManager = accountManagerRepository.findByIdAndStatus(customerAccountManager.getManagerId(), List.of(Status.ACTIVE))
                    .orElseThrow(() -> new ClientException(String.format("Customer account manager mapping has wrong manager id. CustomerAccountManager id:  %s", customerAccountManager.getId()), ErrorCode.APPLICATION_ERROR));
            if (accountManager.getUserName().equals(loggedInUserId)) {
                return true;
            }
        }
        return false;
    }

    public AccountManager mapPortalCustomerAccountManagerToAccountManagerEntity(AppUserInfoDto user) {
        AccountManager accountManager = new AccountManager();
        accountManager.setUserName(user.getUserName());
        accountManager.setFirstName(user.getUserFirstName());
        accountManager.setLastName(user.getUserLastName());
        accountManager.setDisplayName(user.getUserDisplayName());
        accountManager.setEmail(user.getEmailAddress());
        accountManager.setStatus(Status.valueOf(user.getStatus()));
        accountManager.setOrganizationalUnit(user.getUserDepartment());
        accountManager.setBusinessUnit(""); // todo check this later

        return accountManager;
    }
}
