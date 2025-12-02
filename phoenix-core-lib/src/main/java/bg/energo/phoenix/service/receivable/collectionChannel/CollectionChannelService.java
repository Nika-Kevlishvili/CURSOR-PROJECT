package bg.energo.phoenix.service.receivable.collectionChannel;

import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.IllegalArgumentsProvidedException;
import bg.energo.phoenix.exception.OperationNotAllowedException;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.customer.Customer;
import bg.energo.phoenix.model.entity.nomenclature.customer.Bank;
import bg.energo.phoenix.model.entity.nomenclature.product.Currency;
import bg.energo.phoenix.model.entity.nomenclature.product.terms.Calendar;
import bg.energo.phoenix.model.entity.nomenclature.receivable.CollectionPartner;
import bg.energo.phoenix.model.entity.receivable.collectionChannel.CollectionChannel;
import bg.energo.phoenix.model.entity.receivable.collectionChannel.CollectionChannelBanks;
import bg.energo.phoenix.model.entity.receivable.collectionChannel.CollectionChannelExcludePrefix;
import bg.energo.phoenix.model.entity.receivable.collectionChannel.CollectionChannelPriorityPrefix;
import bg.energo.phoenix.model.enums.customer.CustomerStatus;
import bg.energo.phoenix.model.enums.customer.Status;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.receivable.collectionChannel.CollectionChannelType;
import bg.energo.phoenix.model.enums.receivable.collectionChannel.CustomerConditionType;
import bg.energo.phoenix.model.enums.task.PerformerType;
import bg.energo.phoenix.model.request.receivable.collectionChannel.CollectionChannelBaseRequest;
import bg.energo.phoenix.model.request.receivable.collectionChannel.listing.CollectionChannelListColumns;
import bg.energo.phoenix.model.request.receivable.collectionChannel.listing.CollectionChannelListingRequest;
import bg.energo.phoenix.model.request.receivable.collectionChannel.listing.CollectionChannelSearchByEnums;
import bg.energo.phoenix.model.response.receivable.collectionChannel.CollectionChannelEmployeeResponse;
import bg.energo.phoenix.model.response.receivable.collectionChannel.CollectionChannelListingResponse;
import bg.energo.phoenix.model.response.receivable.collectionChannel.CollectionChannelResponse;
import bg.energo.phoenix.model.response.shared.ShortResponse;
import bg.energo.phoenix.repository.customer.AccountManagerRepository;
import bg.energo.phoenix.repository.customer.CustomerRepository;
import bg.energo.phoenix.repository.customer.PortalTagRepository;
import bg.energo.phoenix.repository.nomenclature.billing.PrefixRepository;
import bg.energo.phoenix.repository.nomenclature.customer.BankRepository;
import bg.energo.phoenix.repository.nomenclature.product.CurrencyRepository;
import bg.energo.phoenix.repository.nomenclature.product.terms.CalendarRepository;
import bg.energo.phoenix.repository.nomenclature.receivable.CollectionPartnerRepository;
import bg.energo.phoenix.repository.receivable.collectionChannel.CollectionChannelBanksRepository;
import bg.energo.phoenix.repository.receivable.collectionChannel.CollectionChannelExcludePrefixRepository;
import bg.energo.phoenix.repository.receivable.collectionChannel.CollectionChannelPriorityPrefixRepository;
import bg.energo.phoenix.repository.receivable.collectionChannel.CollectionChannelRepository;
import bg.energo.phoenix.repository.receivable.paymentPackage.PaymentPackageRepository;
import bg.energo.phoenix.security.PermissionService;
import bg.energo.phoenix.service.document.ftpService.FileService;
import bg.energo.phoenix.util.epb.EPBChainedExceptionTriggerUtil;
import bg.energo.phoenix.util.epb.EPBListUtils;
import bg.energo.phoenix.util.epb.EPBStringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static bg.energo.phoenix.permissions.PermissionContextEnum.COLLECTION_CHANNEL;
import static bg.energo.phoenix.permissions.PermissionEnum.VIEW_COLLECTION_CHANNEL;
import static bg.energo.phoenix.permissions.PermissionEnum.VIEW_DELETED_COLLECTION_CHANNEL;

@Slf4j
@Service
@RequiredArgsConstructor
public class CollectionChannelService {
    private final CollectionChannelRepository collectionChannelRepository;
    private final CollectionChannelBanksRepository collectionChannelBanksRepository;
    private final CollectionChannelExcludePrefixRepository excludePrefixRepository;
    private final CollectionChannelPriorityPrefixRepository priorityPrefixRepository;
    private final CollectionPartnerRepository collectionPartnerRepository;
    private final BankRepository bankRepository;
    private final CurrencyRepository currencyRepository;
    private final PrefixRepository prefixRepository;
    private final CalendarRepository calendarRepository;
    private final CustomerRepository customerRepository;
    private final PaymentPackageRepository paymentPackageRepository;
    private final CollectionChannelConditionService collectionChannelConditionService;
    private final PermissionService permissionService;
    private final FileService fileService;
    private final AccountManagerRepository accountManagerRepository;
    private final PortalTagRepository portalTagRepository;
    @Value("${ftp.server.base.path}")
    private String ftpBasePath;

    /**
     * Creates a new CollectionChannel entity based on the provided CollectionChannelBaseRequest.
     *
     * @param request the request object containing the details of the new CollectionChannel
     * @return the ID of the newly created CollectionChannel
     * @throws DomainEntityNotFoundException if a CollectionChannel with the same name already exists
     */
    @Transactional
    public Long create(CollectionChannelBaseRequest request) {
        List<String> errorMessages = new ArrayList<>();
        if (collectionChannelRepository.existsByName(request.getName())) {
            errorMessages.add("name-Collection Channel with the name %s already exists".formatted(request.getName()));
        }
        CollectionChannel collectionChannel = validateRequest(new CollectionChannel(), request, errorMessages, false);
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
        collectionChannelRepository.save(collectionChannel);
        return collectionChannel.getId();
    }

    /**
     * Updates an existing CollectionChannel entity based on the provided CollectionChannelBaseRequest.
     *
     * @param id      the ID of the CollectionChannel to be updated
     * @param request the request object containing the updated details of the CollectionChannel
     * @return the ID of the updated CollectionChannel
     * @throws DomainEntityNotFoundException if the CollectionChannel with the given ID is not found or is not in the ACTIVE status
     * @throws OperationNotAllowedException  if the CollectionChannel type cannot be changed
     */
    @Transactional
    public Long edit(Long id, CollectionChannelBaseRequest request) {
        List<String> errorMessages = new ArrayList<>();
        Optional<CollectionChannel> collectionChannel = collectionChannelRepository.findCollectionChannelByIdAndStatusIn(id, List.of(EntityStatus.ACTIVE));
        if (collectionChannel.isEmpty()) {
            throw new DomainEntityNotFoundException("id-Can not find Active Collection Channel with id %s;".formatted(id));
        }

        if (!request.getName().equals(collectionChannel.get().getName()) && collectionChannelRepository.existsByName(request.getName())) {
            errorMessages.add("name-Collection Channel with the name %s already exists".formatted(request.getName()));
        }

        if (!request.getType().equals(collectionChannel.get().getType())) {
            throw new OperationNotAllowedException("type-Can not change Collection Channel Type;");
        }

        addAndDeleteExcludePrefixes(request, id);
        addAndDeletePriorityPrefixes(request, id);
        addAndDeleteBanks(request, id);

        CollectionChannel updatedCollectionChannel = validateRequest(collectionChannel.get(), request, errorMessages, true);
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
        collectionChannelRepository.save(updatedCollectionChannel);
        return updatedCollectionChannel.getId();
    }

    /**
     * Deletes a CollectionChannel entity by its ID.
     *
     * @param id the ID of the CollectionChannel to be deleted
     * @return the ID of the deleted CollectionChannel
     * @throws DomainEntityNotFoundException if the CollectionChannel with the given ID is not found
     * @throws OperationNotAllowedException  if the CollectionChannel is already deleted or is connected to a PaymentPackage
     */
    @Transactional
    public Long delete(Long id) {
        log.debug("Deleting Collection Channel with ID {};", id);
        CollectionChannel collectionChannel = collectionChannelRepository.findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Collection Channel with ID %s not found;".formatted(id)));

        if (collectionChannel.getStatus().equals(EntityStatus.DELETED)) {
            log.error("id-Collection Channel with ID {} is already deleted;", id);
            throw new OperationNotAllowedException("id-Collection Channel with ID %s is already deleted;".formatted(id));
        }

        if (collectionChannel.getType().equals(CollectionChannelType.ONLINE)) {
            log.error("id-The collection channel with ID {} is type online and cannot be deleted;", id);
            throw new OperationNotAllowedException("id-The collection channel with ID %s is type online and cannot be deleted;".formatted(id));
        }

        if (paymentPackageRepository.existsByCollectionChannelId(id)) {
            log.error("id-Collection Channel with ID {} is connected with a Payment Package and can not be deleted;", id);
            throw new OperationNotAllowedException("id-Collection Channel with ID %s is connected with a Payment Package and can not be deleted;".formatted(id));
        }

        collectionChannel.setStatus(EntityStatus.DELETED);
        collectionChannelRepository.save(collectionChannel);

        return collectionChannel.getId();
    }

    /**
     * Fetches and returns a {@link CollectionChannelResponse} for the CollectionChannel with the given ID.
     *
     * @param id the ID of the CollectionChannel to fetch
     * @return a {@link CollectionChannelResponse} containing the details of the CollectionChannel
     * @throws DomainEntityNotFoundException if the CollectionChannel with the given ID is not found
     * @throws OperationNotAllowedException  if the user does not have permission to view a deleted CollectionChannel
     */
    public CollectionChannelResponse view(Long id) {
        log.debug("Fetching Collection Channel with ID: {};", id);
        CollectionChannel collectionChannel = collectionChannelRepository.findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Collection Channel with ID %s not found;".formatted(id)));

        if (collectionChannel.getStatus().equals(EntityStatus.DELETED) &&
                !permissionService.permissionContextContainsPermissions(COLLECTION_CHANNEL, List.of(VIEW_DELETED_COLLECTION_CHANNEL))) {
            throw new OperationNotAllowedException("You do not have permission to view deleted item!");
        }

        CollectionChannelResponse collectionChannelResponse = new CollectionChannelResponse(collectionChannel);
        if (Objects.nonNull(collectionChannel.getCalendarId())) {
            Optional<Calendar> calendar = calendarRepository.findById(collectionChannel.getCalendarId());
            if (calendar.isPresent()) {
                ShortResponse shortResponse = new ShortResponse(calendar.get().getId(), calendar.get().getName());
                collectionChannelResponse.setCalendarId(shortResponse);
            }
        }

        if (Objects.nonNull(collectionChannel.getCurrencyId())) {
            Optional<Currency> currency = currencyRepository.findById(collectionChannel.getCurrencyId());
            if (currency.isPresent()) {
                ShortResponse shortResponse = new ShortResponse(currency.get().getId(), currency.get().getName());
                collectionChannelResponse.setCurrencyId(shortResponse);
            }
        }

        if (Objects.nonNull(collectionChannel.getCollectionPartnerId())) {
            Optional<CollectionPartner> collectionPartner = collectionPartnerRepository.findById(collectionChannel.getCollectionPartnerId());
            if (collectionPartner.isPresent()) {
                ShortResponse shortResponse = new ShortResponse(collectionPartner.get().getId(), collectionPartner.get().getName());
                collectionChannelResponse.setCollectionPartnerId(shortResponse);
            }
        }

        Optional<List<ShortResponse>> banks = collectionChannelBanksRepository.findBankIdsByCollectionChannelIdAndStatus(id, EntityStatus.ACTIVE);
        banks.ifPresent(collectionChannelResponse::setBankIds);

        Optional<List<ShortResponse>> excludePrefix = excludePrefixRepository.findExcludePrefixIdsByCollectionChannelIdAndStatus(id, EntityStatus.ACTIVE);
        excludePrefix.ifPresent(collectionChannelResponse::setExcludeLiabilitiesByPrefix);

        Optional<List<ShortResponse>> priorityPrefix = priorityPrefixRepository.findPriorityPrefixIdsByCollectionChannelIdAndStatus(id, EntityStatus.ACTIVE);
        priorityPrefix.ifPresent(collectionChannelResponse::setPriorityLiabilitiesByPrefix);

        collectionChannelResponse.setConditionsInfo(collectionChannelConditionService.getConditionInfo(collectionChannel.getCondition()));
        if (collectionChannel.getPerformerType() != null) {
            switch (collectionChannel.getPerformerType()) {
                case MANAGER -> collectionChannelResponse.setEmployee(
                        new CollectionChannelEmployeeResponse(accountManagerRepository
                                .findById(collectionChannel.getEmployeeId()).orElseThrow(() -> new DomainEntityNotFoundException("id-Accoutn manager not found!;")), null));
                case TAG -> collectionChannelResponse.setEmployee(
                        new CollectionChannelEmployeeResponse(null,
                                portalTagRepository
                                        .findById(collectionChannel.getTagId()).orElseThrow(() -> new DomainEntityNotFoundException("id-Portal tag not found!;"))));
            }
        }
        return collectionChannelResponse;
    }

    /**
     * Filters and retrieves a page of collection channel listing responses based on the provided request.
     *
     * @param request the collection channel listing request containing the filter criteria
     * @return a page of collection channel listing responses matching the filter criteria
     */
    public Page<CollectionChannelListingResponse> filter(CollectionChannelListingRequest request) {
        Sort.Order order = new Sort.Order(Objects.requireNonNullElse(request.getDirection(), Sort.Direction.DESC), checkSortField(request));
        List<CollectionChannelType> type = request.getCollectionChannelType();
        return collectionChannelRepository.filter(
                EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                Objects.requireNonNullElse(request.getCollectionPartnerId(), new ArrayList<>()),
                EPBListUtils.convertEnumListIntoStringListIfNotNull(type),
                Objects.requireNonNullElse(request.getCurrencyId(), new ArrayList<>()),
                statusesForListing(),
                getSearchByEnum(request),
                EPBListUtils.convertEnumListIntoStringListIfNotNull(request.getStatuses()),
                PageRequest.of(request.getPage(), request.getSize(), Sort.by(order))).map(CollectionChannelListingResponse::new);
    }

    /**
     * Validates the bank IDs provided in the collection channel base request and saves the associated collection channel banks.
     *
     * @param request           the collection channel base request containing the bank IDs
     * @param collectionChannel the collection channel entity
     * @param errorMessages     the list to store any error messages
     */
    private void validateBank(CollectionChannelBaseRequest request, CollectionChannel collectionChannel, List<String> errorMessages) {
        if (CollectionUtils.isNotEmpty(request.getBankIds())) {
            for (Long bankId : request.getBankIds()) {
                Optional<Bank> bank = bankRepository.findByIdAndStatus(bankId, List.of(NomenclatureItemStatus.ACTIVE));
                if (bank.isPresent()) {
                    CollectionChannelBanks collectionChannelBanks = new CollectionChannelBanks();
                    collectionChannelBanks.setCollectionChannelId(collectionChannel.getId());
                    collectionChannelBanks.setBankId(bankId);
                    collectionChannelBanks.setStatus(EntityStatus.ACTIVE);
                    collectionChannelBanksRepository.save(collectionChannelBanks);
                } else {
                    log.error("bankIds-Active Bank with ID %s not found;".formatted(bankId));
                    errorMessages.add("bankIds-Active Bank with ID %s not found;".formatted(bankId));
                }
            }
        }
    }

    /**
     * Validates the exclude prefix IDs provided in the collection channel base request and saves the associated collection channel exclude prefixes.
     *
     * @param request           the collection channel base request containing the exclude prefix IDs
     * @param collectionChannel the collection channel entity
     * @param errorMessages     the list to store any error messages
     */
    private void validateExcludePrefix(CollectionChannelBaseRequest request, CollectionChannel collectionChannel, List<String> errorMessages) {
        if (CollectionUtils.isNotEmpty(request.getExcludeLiabilitiesByPrefix())) {
            for (Long prefixId : request.getExcludeLiabilitiesByPrefix()) {
                if (prefixRepository.existsByIdAndStatusIn(prefixId, List.of(NomenclatureItemStatus.ACTIVE))) {
                    CollectionChannelExcludePrefix excludePrefix = new CollectionChannelExcludePrefix();
                    excludePrefix.setCollectionChannelId(collectionChannel.getId());
                    excludePrefix.setPrefixId(prefixId);
                    excludePrefix.setStatus(EntityStatus.ACTIVE);
                    excludePrefixRepository.save(excludePrefix);
                } else {
                    log.error("excludeLiabilitiesByPrefix-Active Prefix with ID %s not found;".formatted(prefixId));
                    errorMessages.add("excludeLiabilitiesByPrefix-Active Prefix with ID %s not found;".formatted(prefixId));
                }
            }
        }
    }

    /**
     * Validates the priority prefix IDs provided in the collection channel base request and saves the associated collection channel priority prefixes.
     *
     * @param request           the collection channel base request containing the priority prefix IDs
     * @param collectionChannel the collection channel entity
     * @param errorMessages     the list to store any error messages
     */
    private void validatePriorityPrefix(CollectionChannelBaseRequest request, CollectionChannel collectionChannel, List<String> errorMessages) {
        if (CollectionUtils.isNotEmpty(request.getPriorityLiabilitiesByPrefix())) {
            for (Long prefixId : request.getPriorityLiabilitiesByPrefix()) {
                if (prefixRepository.existsByIdAndStatusIn(prefixId, List.of(NomenclatureItemStatus.ACTIVE))) {
                    CollectionChannelPriorityPrefix priorityPrefix = new CollectionChannelPriorityPrefix();
                    priorityPrefix.setCollectionChannelId(collectionChannel.getId());
                    priorityPrefix.setPrefixId(prefixId);
                    priorityPrefix.setStatus(EntityStatus.ACTIVE);
                    priorityPrefixRepository.save(priorityPrefix);
                } else {
                    log.error("priorityLiabilitiesByPrefix-Active Prefix with ID %s not found;".formatted(prefixId));
                    errorMessages.add("priorityLiabilitiesByPrefix-Active Prefix with ID %s not found;".formatted(prefixId));
                }
            }
        }
    }

    /**
     * Validates the list of customers provided in the collection channel base request and adds any not found active customers to the error messages list.
     *
     * @param request       the collection channel base request containing the list of customers
     * @param errorMessages the list to store any error messages
     */
    private void validateListOfCustomers(CollectionChannelBaseRequest request, List<String> errorMessages) {
        List<String> notFoundedCustomers = new ArrayList<>();
        for (String identifier : request.getListOfCustomers().trim().split(",")) {
            Optional<Customer> customer = customerRepository.findByIdentifierAndStatus(identifier.trim(), CustomerStatus.ACTIVE);
            if (customer.isEmpty()) {
                notFoundedCustomers.add(identifier);
            }
        }
        if (!notFoundedCustomers.isEmpty()) {
            log.error("listOfCustomers-Active Customer with identifier %s not found;".formatted(notFoundedCustomers));
            errorMessages.add("listOfCustomers-Active Customer with identifier %s not found;".formatted(notFoundedCustomers));
        }
    }

    /**
     * Validates the collection partner ID provided in the collection channel base request and sets the associated collection channel's collection partner ID.
     *
     * @param request           the collection channel base request containing the collection partner ID
     * @param errorMessages     the list to store any error messages
     * @param collectionChannel the collection channel entity
     */
    private void validateCollectionPartner(CollectionChannelBaseRequest request, List<String> errorMessages, CollectionChannel collectionChannel) {
        Optional<CollectionPartner> collectionPartner = collectionPartnerRepository.findByIdAndStatusIn(request.getCollectionPartnerId(), List.of(NomenclatureItemStatus.ACTIVE));
        if (collectionPartner.isPresent()) {
            collectionChannel.setCollectionPartnerId(request.getCollectionPartnerId());
        } else {
            errorMessages.add("collectionPartnerId-Active Collection Partner with ID %s not found;".formatted(request.getCollectionPartnerId()));
        }
    }

    /**
     * Validates the currency ID provided in the collection channel base request and sets the associated collection channel's currency ID.
     *
     * @param request           the collection channel base request containing the currency ID
     * @param errorMessages     the list to store any error messages
     * @param collectionChannel the collection channel entity
     */
    private void validateCurrency(CollectionChannelBaseRequest request, List<String> errorMessages, CollectionChannel collectionChannel) {
        Optional<Currency> currency = currencyRepository.findByIdAndStatus(request.getCurrencyId(), List.of(NomenclatureItemStatus.ACTIVE));
        if (currency.isPresent()) {
            collectionChannel.setCurrencyId(request.getCurrencyId());
        } else {
            errorMessages.add("currencyId-Active Currency with ID %s not found;".formatted(request.getCurrencyId()));
        }
    }

    /**
     * Validates the calendar ID provided in the collection channel base request and sets the associated collection channel's calendar ID.
     *
     * @param calendarId        the calendar ID to validate
     * @param errorMessages     the list to store any error messages
     * @param collectionChannel the collection channel entity
     */
    private void validateCalendar(Long calendarId, List<String> errorMessages, CollectionChannel collectionChannel) {
        Optional<Calendar> calendar = calendarRepository.findByIdAndStatusIsIn(calendarId, List.of(NomenclatureItemStatus.ACTIVE));
        if (calendar.isPresent()) {
            collectionChannel.setCalendarId(calendarId);
        } else {
            errorMessages.add("calendarId-Active Calendar with ID %s not found;".formatted(calendarId));
        }
    }

    /**
     * Checks the sort field for a collection channel listing request.
     *
     * @param request the collection channel listing request
     * @return the sort field value, or the default value of {@link CollectionChannelListColumns#ID} if the sort field is not provided in the request
     */
    private String checkSortField(CollectionChannelListingRequest request) {
        if (request.getSortBy() == null) {
            return CollectionChannelListColumns.ID.getValue();
        } else
            return request.getSortBy().getValue();
    }

    /**
     * Retrieves the search field value from the provided {@link CollectionChannelListingRequest}.
     * If the search field is not provided in the request, the default value of {@link CollectionChannelSearchByEnums#ALL} is returned.
     *
     * @param request the {@link CollectionChannelListingRequest} containing the search field
     * @return the search field value, or the default value of {@link CollectionChannelSearchByEnums#ALL} if not provided
     */
    private String getSearchByEnum(CollectionChannelListingRequest request) {
        String searchByField;
        if (request.getSearchBy() != null) {
            searchByField = request.getSearchBy().getValue();
        } else
            searchByField = CollectionChannelSearchByEnums.ALL.getValue();
        return searchByField;
    }

    /**
     * Retrieves a list of statuses to be used for filtering collection channels based on the user's permissions.
     * If the user has the 'VIEW_COLLECTION_CHANNEL' permission, the 'ACTIVE' status is included in the list.
     * If the user has the 'VIEW_DELETED_COLLECTION_CHANNEL' permission, the 'DELETED' status is included in the list.
     *
     * @return a list of statuses to be used for filtering collection channels
     */
    private List<String> statusesForListing() {
        List<String> statuses = new ArrayList<>();
        if (permissionService.permissionContextContainsPermissions(COLLECTION_CHANNEL, List.of(VIEW_COLLECTION_CHANNEL))) {
            statuses.add(EntityStatus.ACTIVE.name());
        }

        if (permissionService.permissionContextContainsPermissions(COLLECTION_CHANNEL, List.of(VIEW_DELETED_COLLECTION_CHANNEL))) {
            statuses.add(EntityStatus.DELETED.name());
        }
        return statuses;
    }

    /**
     * Validates the provided {@link CollectionChannelBaseRequest} and updates the {@link CollectionChannel} entity accordingly.
     * This method performs the following validations:
     * - Validates the condition expression provided in the request, if any.
     * - Maps the request data into the {@link CollectionChannel} entity.
     * - Validates the collection partner, currency, and calendar associated with the request, if applicable.
     * - Validates the bank, exclude prefix, priority prefix, and list of customers (if applicable).
     * - Skips the validation of performer type and ID if {@code isOnEdit} is {@code true} and both performer fields are empty.
     *
     * @param collectionChannel the {@link CollectionChannel} entity to be updated
     * @param request           the {@link CollectionChannelBaseRequest} containing the updated data
     * @param errorMessages     a list to store any error messages encountered during validation
     * @param isOnEdit            a flag indicating if the validation is being performed in an edit context. If {@code true},
     *                           certain validations (e.g., performer validation when both fields are empty) are skipped.
     * @return the updated {@link CollectionChannel} entity
     */
    private CollectionChannel validateRequest(CollectionChannel collectionChannel, CollectionChannelBaseRequest request, List<String> errorMessages, boolean isOnEdit) {
        boolean skipPerformerValidation = isOnEdit && request.getPerformerId() == null;

        if (StringUtils.isNotBlank(request.getCondition())) {
            collectionChannelConditionService.validateCondition(request.getCondition(), errorMessages);
            collectionChannelConditionService.validateConditionKeys(request.getCondition(), errorMessages);
            EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
        }

        mapIntoEntity(collectionChannel, request);
        collectionChannel.setStatus(EntityStatus.ACTIVE);

        if (!request.getCollectionPartnerId().equals(collectionChannel.getCollectionPartnerId())) {
            validateCollectionPartner(request, errorMessages, collectionChannel);
        }

        if (!request.getCurrencyId().equals(collectionChannel.getCurrencyId())) {
            validateCurrency(request, errorMessages, collectionChannel);
        }

        Long calendarId = request.getCalendarId();
        if (Objects.nonNull(calendarId) && !calendarId.equals(collectionChannel.getCalendarId())) {
            validateCalendar(calendarId, errorMessages, collectionChannel);
        }
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
        collectionChannelRepository.saveAndFlush(collectionChannel);
        if (!request.isGlobalBank()) {
            validateBank(request, collectionChannel, errorMessages);
        }
        validateExcludePrefix(request, collectionChannel, errorMessages);
        validatePriorityPrefix(request, collectionChannel, errorMessages);
        if (request.getCustomerConditionType().equals(CustomerConditionType.LIST_OF_CUSTOMERS)) {
            validateListOfCustomers(request, errorMessages);
        }
        if (!skipPerformerValidation) {
            validatePerformer(request.getPerformerType(), request.getPerformerId(), errorMessages);
        }
        return collectionChannel;
    }

    /**
     * Validates the performer type and ID provided in the request.
     * If the performer type is MANAGER, it checks if the performer ID corresponds to an active account manager.
     * If the performer type is TAG, it checks if the performer ID corresponds to an active portal tag.
     * If the performer type is null, no validation is performed.
     *
     * @param performerType the type of performer (MANAGER or TAG)
     * @param performerId   the ID of the performer
     * @param errorMessages a list to store any error messages encountered during validation
     */
    private void validatePerformer(PerformerType performerType, Long performerId, List<String> errorMessages) {
        if (performerType == null) {
            return;
        }
        switch (performerType) {
            case MANAGER -> {
                if (!accountManagerRepository.existsByIdAndStatusIn(performerId, List.of(Status.ACTIVE))) {
                    errorMessages.add("performerId-Performer does not exist!;");
                }
            }
            case TAG -> {
                if (!portalTagRepository.existsPortalTagForGroup(performerId, EntityStatus.ACTIVE)) {
                    errorMessages.add("performerId-Performer does not exist!;");
                }
            }
        }

    }

    /**
     * Maps the properties of a CollectionChannelBaseRequest object to a CollectionChannel entity.
     *
     * @param collectionChannel the CollectionChannel entity to be updated
     * @param request           the CollectionChannelBaseRequest containing the new property values
     */
    private void mapIntoEntity(CollectionChannel collectionChannel, CollectionChannelBaseRequest request) {
        collectionChannel.setName(request.getName());
        collectionChannel.setType(request.getType());
        collectionChannel.setPerformerType(request.getPerformerType());
        if (request.getPerformerType() != null && request.getPerformerType().equals(PerformerType.TAG)) {
            collectionChannel.setTagId(request.getPerformerId());
        }
        if (request.getPerformerType() != null && request.getPerformerType().equals(PerformerType.MANAGER)) {
            collectionChannel.setEmployeeId(request.getPerformerId());
        }
        collectionChannel.setNumberOfIncomeAccount(request.getNumberOfIncomeAccount());
        collectionChannel.setConditionType(request.getCustomerConditionType());
        collectionChannel.setCondition(request.getCondition());
        collectionChannel.setListOfCustomers(request.getListOfCustomers());
        if (Objects.nonNull(request.getExcludeLiabilitiesByAmount())) {
            collectionChannel.setLessThan(request.getExcludeLiabilitiesByAmount().getLessThan());
            collectionChannel.setGreaterThan(request.getExcludeLiabilitiesByAmount().getGreaterThan());
        }
        collectionChannel.setTypeOfFile(request.getTypeOfFile());
        collectionChannel.setIsGlobalBank(request.isGlobalBank());
        collectionChannel.setDataSendingSchedule(request.getDataSendingSchedule());
        collectionChannel.setDataReceivingSchedule(request.getDataReceivingSchedule());
        collectionChannel.setNumberOfWorkingDays(request.getNumberOfWorkingDays());
        collectionChannel.setWaitingPeriodToleranceInHours(request.getWaitingPeriodToleranceInHours());
        collectionChannel.setFolderForFileReceiving(request.getFolderForFileReceiving());
        collectionChannel.setFolderForFileSending(request.getFolderForFileSending());
        collectionChannel.setEmailForFileSending(request.getEmailForFileSending());
        collectionChannel.setCombineLiabilities(!Objects.isNull(request.getCombineLiabilities()) && request.getCombineLiabilities());
    }

    /**
     * Adds and deletes exclude prefixes for a collection channel.
     * <p>
     * This method updates the list of exclude prefixes for a collection channel based on the changes in the provided
     * CollectionChannelBaseRequest. It retrieves the existing exclude prefixes from the database, compares them with the
     * new list, and saves any deleted prefixes with a status of DELETED. It also updates the
     * CollectionChannelBaseRequest with the list of added prefixes.
     *
     * @param request the CollectionChannelBaseRequest containing the new exclude prefixes
     * @param id      the ID of the collection channel
     */
    private void addAndDeleteExcludePrefixes(CollectionChannelBaseRequest request, Long id) {
        List<Long> excludePrefixesList = Objects.requireNonNullElse(request.getExcludeLiabilitiesByPrefix(), new ArrayList<>());

        List<CollectionChannelExcludePrefix> exclusionPrefixesFromDb = getRelatedExcludePrefixes(id);
        List<Long> oldExcludePrefixesList = exclusionPrefixesFromDb.stream().map(CollectionChannelExcludePrefix::getPrefixId).toList();

        List<Long> deletedFromList = EPBListUtils.getDeletedElementsFromList(oldExcludePrefixesList, excludePrefixesList);
        deletedFromList
                .stream()
                .filter(Objects::nonNull)
                .map(aLong -> exclusionPrefixesFromDb
                        .stream()
                        .filter(exclusionPrefix -> Objects.equals(exclusionPrefix.getPrefixId(), aLong))
                        .findFirst()
                        .orElse(null)
                )
                .filter(Objects::nonNull)
                .forEach(exclusionPrefix -> {
                            exclusionPrefix.setStatus(EntityStatus.DELETED);
                            excludePrefixRepository.save(exclusionPrefix);
                        }
                );

        request.setExcludeLiabilitiesByPrefix(EPBListUtils.getAddedElementsFromList(oldExcludePrefixesList, excludePrefixesList));
    }

    /**
     * Adds and deletes priority prefixes for a collection channel.
     * <p>
     * This method updates the list of priority prefixes for a collection channel based on the changes in the provided
     * CollectionChannelBaseRequest. It retrieves the existing priority prefixes from the database, compares them with the
     * new list, and saves any deleted prefixes with a status of DELETED. It also updates the
     * CollectionChannelBaseRequest with the list of added prefixes.
     *
     * @param request the CollectionChannelBaseRequest containing the new priority prefixes
     * @param id      the ID of the collection channel
     */
    private void addAndDeletePriorityPrefixes(CollectionChannelBaseRequest request, Long id) {
        List<Long> priorityPrefixesList = Objects.requireNonNullElse(request.getPriorityLiabilitiesByPrefix(), new ArrayList<>());

        List<CollectionChannelPriorityPrefix> priorityPrefixesFromDb = getRelatedPriorityPrefixes(id);
        List<Long> oldPriorityPrefixesList = priorityPrefixesFromDb.stream().map(CollectionChannelPriorityPrefix::getPrefixId).toList();

        List<Long> deletedFromList = EPBListUtils.getDeletedElementsFromList(oldPriorityPrefixesList, priorityPrefixesList);
        deletedFromList
                .stream()
                .filter(Objects::nonNull)
                .map(aLong -> priorityPrefixesFromDb
                        .stream()
                        .filter(exclusionPrefix -> Objects.equals(exclusionPrefix.getPrefixId(), aLong))
                        .findFirst()
                        .orElse(null)
                )
                .filter(Objects::nonNull)
                .forEach(exclusionPrefix -> {
                            exclusionPrefix.setStatus(EntityStatus.DELETED);
                            priorityPrefixRepository.save(exclusionPrefix);
                        }
                );

        request.setPriorityLiabilitiesByPrefix(EPBListUtils.getAddedElementsFromList(oldPriorityPrefixesList, priorityPrefixesList));
    }

    /**
     * Adds and deletes banks associated with a collection channel.
     * <p>
     * This method updates the list of banks associated with a collection channel based on the changes in the provided
     * CollectionChannelBaseRequest. It retrieves the existing banks from the database, compares them with the
     * new list, and saves any deleted banks with a status of DELETED. It also updates the
     * CollectionChannelBaseRequest with the list of added banks.
     *
     * @param request the CollectionChannelBaseRequest containing the new bank IDs
     * @param id      the ID of the collection channel
     */
    private void addAndDeleteBanks(CollectionChannelBaseRequest request, Long id) {
        List<Long> BankIdList = Objects.requireNonNullElse(request.getBankIds(), new ArrayList<>());
        List<CollectionChannelBanks> bankIdsFromDb = getRelatedBanks(id);
        List<Long> oldBankIdList = bankIdsFromDb.stream().map(CollectionChannelBanks::getBankId).toList();

        List<Long> deletedFromList = EPBListUtils.getDeletedElementsFromList(oldBankIdList, BankIdList);
        deletedFromList
                .stream()
                .filter(Objects::nonNull)
                .map(aLong -> bankIdsFromDb
                        .stream()
                        .filter(exclusionPrefix -> Objects.equals(exclusionPrefix.getBankId(), aLong))
                        .findFirst()
                        .orElse(null)
                )
                .filter(Objects::nonNull)
                .forEach(exclusionPrefix -> {
                            exclusionPrefix.setStatus(EntityStatus.DELETED);
                            collectionChannelBanksRepository.save(exclusionPrefix);
                        }
                );

        request.setBankIds(EPBListUtils.getAddedElementsFromList(oldBankIdList, BankIdList));
    }

    /**
     * Retrieves a list of active collection channel exclude prefixes associated with the given collection channel ID.
     *
     * @param collectionChannelId the ID of the collection channel
     * @return a list of active {@link CollectionChannelExcludePrefix} objects, or an empty list if none are found
     */
    private List<CollectionChannelExcludePrefix> getRelatedExcludePrefixes(Long collectionChannelId) {
        Optional<List<CollectionChannelExcludePrefix>> exclusionPrefixesOptional = excludePrefixRepository.findByCollectionChannelIdAndStatus(collectionChannelId, EntityStatus.ACTIVE);
        return exclusionPrefixesOptional.orElseGet(ArrayList::new);
    }

    /**
     * Retrieves a list of active collection channel priority prefixes associated with the given collection channel ID.
     *
     * @param collectionChannelId the ID of the collection channel
     * @return a list of active {@link CollectionChannelPriorityPrefix} objects, or an empty list if none are found
     */
    private List<CollectionChannelPriorityPrefix> getRelatedPriorityPrefixes(Long collectionChannelId) {
        Optional<List<CollectionChannelPriorityPrefix>> priorityPrefixesOptional = priorityPrefixRepository.findByCollectionChannelIdAndStatus(collectionChannelId, EntityStatus.ACTIVE);
        return priorityPrefixesOptional.orElseGet(ArrayList::new);
    }

    /**
     * Retrieves a list of active collection channel banks associated with the given collection channel ID.
     *
     * @param collectionChannelId the ID of the collection channel
     * @return a list of active {@link CollectionChannelBanks} objects, or an empty list if none are found
     */
    private List<CollectionChannelBanks> getRelatedBanks(Long collectionChannelId) {
        Optional<List<CollectionChannelBanks>> banksOptional = collectionChannelBanksRepository.findByCollectionChannelIdAndStatus(collectionChannelId, EntityStatus.ACTIVE);
        return banksOptional.orElseGet(ArrayList::new);
    }

    /**
     * Filters the collection channels based on the provided request parameters and returns a page of {@link CollectionChannelListingResponse} objects.
     *
     * @param request the {@link CollectionChannelListingRequest} containing the filter criteria
     * @return a page of {@link CollectionChannelListingResponse} objects matching the filter criteria
     */
    public Page<CollectionChannelListingResponse> filterForPaymentMassImport(CollectionChannelListingRequest request) {
        Sort.Order order = new Sort.Order(Objects.requireNonNullElse(request.getDirection(), Sort.Direction.DESC), checkSortField(request));
        List<CollectionChannelType> type = request.getCollectionChannelType();
        return collectionChannelRepository.filterForPaymentMass(
                EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                Objects.requireNonNullElse(request.getCollectionPartnerId(), new ArrayList<>()),
                EPBListUtils.convertEnumListIntoStringListIfNotNull(type),
                Objects.requireNonNullElse(request.getCurrencyId(), new ArrayList<>()),
                getSearchByEnum(request),
                PageRequest.of(request.getPage(), request.getSize(), Sort.by(order))).map(CollectionChannelListingResponse::new);
    }

    /**
     * Uploads a file to the specified folder on the FTP server.
     *
     * @param folder the folder on the FTP server to upload the file to
     * @param file   the file to be uploaded
     * @return the path of the uploaded file on the FTP server
     * @throws IllegalArgumentsProvidedException if the file name is empty or null
     */
    public String uploadFile(String folder, MultipartFile file) {
        log.debug("Sms communication file {}.", file.getName());


        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            log.error("Sms communication file name is empty or null");
            throw new IllegalArgumentsProvidedException("Email communication file name is empty or null");
        }

        return fileService.uploadFile(file, String.format("%s/%s/%s", ftpBasePath, folder, LocalDate.now()), originalFilename);
    }

}
