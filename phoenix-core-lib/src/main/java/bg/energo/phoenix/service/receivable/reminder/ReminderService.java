package bg.energo.phoenix.service.receivable.reminder;

import bg.energo.phoenix.exception.AccessDeniedException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.OperationNotAllowedException;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.billing.processPeriodicity.ProcessPeriodicity;
import bg.energo.phoenix.model.entity.nomenclature.billing.Prefix;
import bg.energo.phoenix.model.entity.nomenclature.customer.ContactPurpose;
import bg.energo.phoenix.model.entity.nomenclature.product.Currency;
import bg.energo.phoenix.model.entity.receivable.reminder.ExcludeLiabilitiesByPrefix;
import bg.energo.phoenix.model.entity.receivable.reminder.OnlyLiabilitiesWithPrefix;
import bg.energo.phoenix.model.entity.receivable.reminder.Reminder;
import bg.energo.phoenix.model.entity.receivable.reminder.ReminderPeriodicity;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.template.ContractTemplatePurposes;
import bg.energo.phoenix.model.enums.template.ContractTemplateType;
import bg.energo.phoenix.model.request.receivable.reminder.*;
import bg.energo.phoenix.model.response.receivable.ValidateConditionResponse;
import bg.energo.phoenix.model.response.receivable.reminder.ReminderListingResponse;
import bg.energo.phoenix.model.response.receivable.reminder.ReminderResponse;
import bg.energo.phoenix.permissions.PermissionEnum;
import bg.energo.phoenix.repository.billing.processPeriodicity.ProcessPeriodicityRepository;
import bg.energo.phoenix.repository.customer.CustomerRepository;
import bg.energo.phoenix.repository.nomenclature.billing.PrefixRepository;
import bg.energo.phoenix.repository.nomenclature.customer.ContactPurposeRepository;
import bg.energo.phoenix.repository.nomenclature.product.CurrencyRepository;
import bg.energo.phoenix.repository.receivable.reminder.ExcludeLiabilitiesByPrefixRepository;
import bg.energo.phoenix.repository.receivable.reminder.OnlyLiabilitiesWithPrefixRepository;
import bg.energo.phoenix.repository.receivable.reminder.ReminderPeriodicityRepository;
import bg.energo.phoenix.repository.receivable.reminder.ReminderRepository;
import bg.energo.phoenix.repository.template.ContractTemplateRepository;
import bg.energo.phoenix.security.PermissionService;
import bg.energo.phoenix.util.epb.EPBChainedExceptionTriggerUtil;
import bg.energo.phoenix.util.epb.EPBListUtils;
import bg.energo.phoenix.util.epb.EPBStringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static bg.energo.phoenix.model.entity.EntityStatus.ACTIVE;
import static bg.energo.phoenix.permissions.PermissionContextEnum.REMINDER;
import static bg.energo.phoenix.util.epb.EPBFinalFields.REMINDER_NUMBER_PREFIX;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReminderService {

    private final ExcludeLiabilitiesByPrefixRepository excludeLiabilitiesByPrefixRepository;
    private final OnlyLiabilitiesWithPrefixRepository onlyLiabilitiesWithPrefixRepository;
    private final ReminderPeriodicityRepository reminderPeriodicityRepository;
    private final ProcessPeriodicityRepository processPeriodicityRepository;
    private final ContactPurposeRepository contactPurposeRepository;
    private final ReminderRepository reminderRepository;
    private final CurrencyRepository currencyRepository;
    private final CustomerRepository customerRepository;
    private final PrefixRepository prefixRepository;
    private final ReminderMapper reminderMapper;
    private final ReminderConditionService reminderConditionService;
    private final PermissionService permissionService;
    private final ContractTemplateRepository templateRepository;

    @Transactional
    public Long create(ReminderCreateRequest request) {
        log.info("Create reminder: %s;".formatted(request));
        List<String> errorMessages = new ArrayList<>();
        Reminder reminder = reminderMapper.fromCreateRequestToEntity(request);
        validateAndSetCurrency(reminder, request.getCurrencyId(), errorMessages);
        validateAndSetConditions(request.getConditions(), reminder, errorMessages);
        validateAndSetListOfCustomers(request.getListOfCustomers(), reminder, errorMessages);
        validateAndSetPurposeOfTheContact(request.getPurposeOfTheContactId(), reminder, errorMessages);
        generateAndSetNumberAndId(reminder);
        validateAndSetTemplate(request.getDocumentTemplateId(), reminder.getDocumentTemplateId(), errorMessages,ContractTemplateType.DOCUMENT,reminder);
        validateAndSetTemplate(request.getEmailTemplateId(),reminder.getEmailTemplateId(),errorMessages,ContractTemplateType.EMAIL,reminder);
        validateAndSetTemplate(request.getSmsTemplateId(),reminder.getSmsTemplateId(),errorMessages,ContractTemplateType.SMS,reminder);

        reminderRepository.save(reminder);
        validateAndSetExcludeLiabilitiesByPrefixes(request.getExcludeLiabilitiesByPrefixes(), reminder.getId(), errorMessages);
        validateAndSetOnlyLiabilitiesWithPrefixes(request.getOnlyLiabilitiesWithPrefixes(), reminder.getId(), errorMessages);
        validateAndSetPeriodicity(request.getPeriodicityIds(), reminder.getId(), errorMessages);

        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
        return reminder.getId();
    }


    private void validateAndSetTemplate(Long templateId, Long reminderTemplateId, List<String> errorMessages, ContractTemplateType contractTemplateType,Reminder reminder) {
        if (Objects.equals(templateId, reminderTemplateId))
            return;
        if(templateId==null) {
            switch (contractTemplateType) {
                case DOCUMENT -> reminder.setDocumentTemplateId(null);
                case EMAIL -> reminder.setEmailTemplateId(null);
                case SMS -> reminder.setSmsTemplateId(null);
            }
            return;
        }
        if (!templateRepository.existsByIdAndTemplatePurposeAndTemplateType(templateId, ContractTemplatePurposes.REMINDER,contractTemplateType,LocalDate.now())) {
            errorMessages.add("templateId-Template with id %s do not exist!;".formatted(templateId));
        }
        switch (contractTemplateType) {
            case DOCUMENT -> reminder.setDocumentTemplateId(templateId);
            case EMAIL -> reminder.setEmailTemplateId(templateId);
            case SMS -> reminder.setSmsTemplateId(templateId);
        }
    }

    private void generateAndSetNumberAndId(Reminder reminder) {
        Long nextSequenceValue = reminderRepository.getNextSequenceValue();
        String number = "%s%s".formatted(REMINDER_NUMBER_PREFIX, nextSequenceValue);
        reminder.setNumber(number);
        reminder.setId(nextSequenceValue);
    }

    private void validateAndSetCurrency(Reminder reminder, Long currencyId, List<String> errorMessages) {
        if (currencyId != null) {
            Optional<Currency> currencyOptional = currencyRepository.findByIdAndStatus(currencyId, List.of(NomenclatureItemStatus.ACTIVE));
            if (currencyOptional.isPresent()) {
                reminder.setCurrencyId(currencyOptional.get().getId());
            } else {
                errorMessages.add("currencyId-[currencyId] active currency with: %s can't be found;".formatted(currencyId));
            }
        }
    }

    private void validateAndSetConditions(String conditions, Reminder reminder, List<String> errorMessages) {
        if (StringUtils.isNotBlank(conditions)) {
            reminderConditionService.validateCondition(conditions, errorMessages);
            EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
            reminder.setConditions(conditions);
        }
    }

    private void validateAndSetPurposeOfTheContact(Long purposeOfTheContactId, Reminder reminder, List<String> errorMessages) {
        if (purposeOfTheContactId != null) {
            Optional<ContactPurpose> contactPurposeOptional = contactPurposeRepository.findByIdAndStatuses(purposeOfTheContactId, List.of(NomenclatureItemStatus.ACTIVE));
            if (contactPurposeOptional.isPresent()) {
                reminder.setContactPurposeId(contactPurposeOptional.get().getId());
            } else {
                errorMessages.add("purposeOfTheContactId-[purposeOfTheContactId] active contact purpose with: %s can't be found;".formatted(purposeOfTheContactId));
            }
        }
    }

    private void validateAndSetPeriodicity(List<Long> periodicityIds, Long reminderId, List<String> errorMessages) {
        if (CollectionUtils.isNotEmpty(periodicityIds)) {
            for (int i = 0; i < periodicityIds.size(); i++) {
                Long currentPeriodicityId = periodicityIds.get(i);
                Optional<ProcessPeriodicity> processPeriodicityOptional = processPeriodicityRepository.findByIdAndStatus(currentPeriodicityId, ACTIVE);
                if (processPeriodicityOptional.isPresent()) {
                    ReminderPeriodicity reminderPeriodicity = reminderMapper.relateToReminderPeriodicity(currentPeriodicityId, reminderId);
                    reminderPeriodicityRepository.save(reminderPeriodicity);
                } else {
                    errorMessages.add("periodicityIds[%s]-Can't find active periodicity with id:%s;".formatted(i, currentPeriodicityId));
                }
            }
        }
    }

    private void validateAndSetExcludeLiabilitiesByPrefixes(List<Long> prefixIds, Long reminderId, List<String> errorMessages) {
        if (CollectionUtils.isNotEmpty(prefixIds)) {
            for (int i = 0; i < prefixIds.size(); i++) {
                Long currentPrefixId = prefixIds.get(i);
                if (checkAndGetPrefixById(currentPrefixId) != null) {
                    ExcludeLiabilitiesByPrefix excludeLiabilitiesByPrefix = reminderMapper.relateToExcludeLiabilitiesByPrefix(currentPrefixId, reminderId);
                    excludeLiabilitiesByPrefixRepository.save(excludeLiabilitiesByPrefix);
                } else {
                    errorMessages.add("excludeLiabilitiesByPrefixes[%s]-Can't find active prefix with id:%s;".formatted(i, currentPrefixId));
                }
            }
        }
    }

    private void validateAndSetOnlyLiabilitiesWithPrefixes(List<Long> prefixIds, Long reminderId, List<String> errorMessages) {
        if (CollectionUtils.isNotEmpty(prefixIds)) {
            for (int i = 0; i < prefixIds.size(); i++) {
                Long currentPrefixId = prefixIds.get(i);
                if (checkAndGetPrefixById(currentPrefixId) != null) {
                    OnlyLiabilitiesWithPrefix onlyLiabilitiesWithPrefix = reminderMapper.relateToOnlyLiabilitiesWithPrefix(currentPrefixId, reminderId);
                    onlyLiabilitiesWithPrefixRepository.save(onlyLiabilitiesWithPrefix);
                } else {
                    errorMessages.add("onlyLiabilitiesWithPrefixes[%s]-Can't find active predix with id:%s;".formatted(i, currentPrefixId));
                }
            }
        }
    }

    private void validateAndSetListOfCustomers(String customers, Reminder reminder, List<String> errorMessages) {
        if (StringUtils.isNotBlank(customers)) {
            List<String> invalidCustomerIdentifiers = customerRepository.findByStringIdentifierInAndStatus(customers);
            if (CollectionUtils.isEmpty(invalidCustomerIdentifiers)) {
                Set<String> customerSet = Arrays
                        .stream(customers.split(","))
                        .collect(Collectors.toSet());

                reminder.setListOfCustomers(String.join(",", customerSet));
            } else {
                errorMessages.add("listOfCustomers-[listOfCustomers] customers with: %s can't be found;".formatted(StringUtils.join(invalidCustomerIdentifiers)));
            }
        }
    }

    private Prefix checkAndGetPrefixById(Long prefixId) {
        Optional<Prefix> prefixOptional = prefixRepository.findByIdAndStatusIn(prefixId, List.of(NomenclatureItemStatus.ACTIVE));
        return prefixOptional.orElse(null);
    }

    public ValidateConditionResponse validateCondition(ReminderConditionValidationRequest request) {
        List<String> errorMessages = new ArrayList<>();
        if (StringUtils.isNotBlank(request.getCondition())) {
            reminderConditionService.validateCondition(request.getCondition(), errorMessages);
        }
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
        return new ValidateConditionResponse(true);
    }

    public ReminderResponse view(Long id) {
        log.info("view for reminder with id: %s ".formatted(id));

        Reminder reminder = reminderRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("Can't find reminder with id: %s;".formatted(id)));

        checkOnViewPermissionsByStatus(reminder.getStatus());

        ReminderResponse response = reminderMapper.fromEntityToPreviewResponse(reminder);
        if (reminder.getDocumentTemplateId() != null) {
            templateRepository.findTemplateResponseById(reminder.getDocumentTemplateId(), LocalDate.now()).ifPresent(
                    response::setDocumentTemplateResponse
            );
        }
        if (reminder.getEmailTemplateId() != null) {
            templateRepository.findTemplateResponseById(reminder.getEmailTemplateId(), LocalDate.now()).ifPresent(
                    response::setEmailTemplateResponse
            );
        }

        if (reminder.getSmsTemplateId() != null) {
            templateRepository.findTemplateResponseById(reminder.getSmsTemplateId(), LocalDate.now()).ifPresent(
                    response::setSmsTemplateResponse
            );
        }
        fetchAndMapConditionsAndConditionsInfo(reminder.getConditions(), response);
        return response;
    }

    private void fetchAndMapConditionsAndConditionsInfo(String condition, ReminderResponse response) {
        response.setConditionsInfo(reminderConditionService.getConditionsInfo(condition));
        response.setConditions(condition);
    }

    private void checkOnViewPermissionsByStatus(EntityStatus entityStatus) {
        switch (entityStatus) {
            case ACTIVE -> {
                if (!hasViewPermission()) {
                    log.error("You do not have permission to view active reminder.");
                    throw new AccessDeniedException("You do not have permission to view active reminder.");
                }
            }
            case DELETED -> {
                if (!hasViewDeletedPermission()) {
                    log.error("You do not have permission to view deleted reminder.");
                    throw new AccessDeniedException("You do not have permission to view deleted reminder.");
                }
            }
        }
    }

    private boolean hasViewPermission() {
        return checkOnPermission(List.of(PermissionEnum.REMINDER_VIEW));
    }

    private boolean hasViewDeletedPermission() {
        return checkOnPermission(List.of(PermissionEnum.REMINDER_VIEW_DELETED));
    }

    private boolean checkOnPermission(List<PermissionEnum> requiredPermissions) {
        return permissionService.permissionContextContainsPermissions(REMINDER, requiredPermissions);
    }

    @Transactional
    public Long delete(Long id) {
        log.info("Deleting reminder with id: {}", id);

        Reminder reminder = reminderRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("Reminder not found by ID %s;".formatted(id)));

        if (reminder.getStatus().equals(EntityStatus.DELETED)) {
            log.error("Reminder with id: {} is already deleted", id);
            throw new OperationNotAllowedException("Reminder is already deleted;");
        }

        if (!hasDeletePermission()) {
            log.error("You do not have permission to delete reminder.");
            throw new AccessDeniedException("You do not have permission to delete reminder.");
        }

        reminder.setStatus(EntityStatus.DELETED);
        reminderRepository.save(reminder);
        return id;
    }

    private boolean hasDeletePermission() {
        return checkOnPermission(List.of(PermissionEnum.REMINDER_DELETE));
    }

    public Page<ReminderListingResponse> list(ReminderListingRequest request) {
        return reminderRepository
                .filter(
                        getSearchByEnum(request.getSearchField()),
                        EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        EPBListUtils.convertEnumListIntoStringListIfNotNull(request.getTriggerForLiabilities()),
                        EPBListUtils.convertEnumListToDBEnumArray(request.getCommunicationChannels()),
                        EPBListUtils.convertEnumListIntoStringListIfNotNull(request.getReminderConditionTypes()),
                        getAllowedViewStatusesByPermission(),
                        PageRequest.of(
                                request.getPage(),
                                request.getSize(),
                                Sort.by(
                                        new Sort.Order(request.getDirection(), getSorByEnum(request.getColumn()))
                                )
                        )
                )
                .map(ReminderListingResponse::new);
    }

    private String getSearchByEnum(ReminderListingSearchField searchField) {
        return searchField != null ? searchField.getValue() : ReminderListingSearchField.ALL.getValue();
    }

    private String getSorByEnum(ReminderListingColumn sortByColumn) {
        return sortByColumn != null ? sortByColumn.getValue() : ReminderListingColumn.NUMBER.getValue();
    }

    private List<String> getAllowedViewStatusesByPermission() {
        List<String> statuses = new ArrayList<>();
        if (hasViewPermission()) {
            statuses.add(EntityStatus.ACTIVE.name());
        }

        if (hasViewDeletedPermission()) {
            statuses.add(EntityStatus.DELETED.name());
        }
        return statuses;
    }

    @Transactional
    public Long update(Long id, ReminderEditRequest request) {
        log.info("Updating reminder with id: %s;".formatted(request));
        List<String> errorMessages = new ArrayList<>();

        Optional<Reminder> reminderOptional = reminderRepository.findByIdAndStatus(id, ACTIVE);
        if (reminderOptional.isEmpty()) {
            throw new DomainEntityNotFoundException("Can't find active reminder with id: %s;".formatted(id));
        }

        Reminder reminderEntity = reminderOptional.get();
        validateAndSetConditions(request.getConditions(), reminderEntity, errorMessages);
        reminderMapper.fromEditRequestToEntity(request, reminderEntity);
        checkGetAndSetCurrencyOnEdit(request.getCurrencyId(), reminderEntity, errorMessages);
        validateAndSetListOfCustomersEdit(request.getListOfCustomers(), reminderEntity, errorMessages);
        validateAndSetPurposeOfTheContactEdit(request.getPurposeOfTheContactId(), reminderEntity, errorMessages);
        validateAndSetExcludeLiabilitiesByPrefixesEdit(request.getExcludeLiabilitiesByPrefixes(), reminderEntity.getId(), errorMessages);
        validateAndSetOnlyLiabilitiesWithPrefixesEdit(request.getOnlyLiabilitiesWithPrefixes(), reminderEntity.getId(), errorMessages);
        validateAndSetPeriodicityEdit(request.getPeriodicityIds(), reminderEntity.getId(), errorMessages);
        validateAndSetTemplate(request.getEmailTemplateId(),reminderEntity.getEmailTemplateId(),errorMessages,ContractTemplateType.EMAIL,reminderEntity);
        validateAndSetTemplate(request.getSmsTemplateId(),reminderEntity.getSmsTemplateId(),errorMessages,ContractTemplateType.SMS,reminderEntity);
        validateAndSetTemplate(request.getDocumentTemplateId(),reminderEntity.getDocumentTemplateId(),errorMessages,ContractTemplateType.DOCUMENT,reminderEntity);
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
        return reminderEntity.getId();
    }

    private void checkGetAndSetCurrencyOnEdit(Long currencyId, Reminder reminder, List<String> errorMessages) {
        if (currencyId != null) {
            if (!Objects.equals(reminder.getCurrencyId(), currencyId)) {
                Optional<Currency> currencyOptional = currencyRepository.findByIdAndStatus(currencyId, List.of(NomenclatureItemStatus.ACTIVE));
                if (currencyOptional.isPresent()) {
                    reminder.setCurrencyId(currencyOptional.get().getId());
                } else {
                    errorMessages.add("currencyId-[currencyId] active currency with: %s can't be found;".formatted(currencyId));
                }
            }
        } else {
            reminder.setCurrencyId(null);
        }
    }

    private void validateAndSetPurposeOfTheContactEdit(Long purposeOfTheContactId, Reminder reminder, List<String> errorMessages) {
        if (purposeOfTheContactId != null) {
            if (!Objects.equals(reminder.getContactPurposeId(), purposeOfTheContactId)) {
                Optional<ContactPurpose> contactPurposeOptional = contactPurposeRepository.findByIdAndStatuses(purposeOfTheContactId, List.of(NomenclatureItemStatus.ACTIVE));
                if (contactPurposeOptional.isPresent()) {
                    reminder.setContactPurposeId(contactPurposeOptional.get().getId());
                } else {
                    errorMessages.add("purposeOfTheContactId-[purposeOfTheContactId] active contact purpose with: %s can't be found;".formatted(purposeOfTheContactId));
                }
            }
        }
    }

    private void validateAndSetExcludeLiabilitiesByPrefixesEdit(List<Long> requestedPrefixIds, Long reminderId, List<String> errorMessages) {
        List<ExcludeLiabilitiesByPrefix> excludeLiabilitiesByPrefixes = getRelatedExcludePrefixes(reminderId);
        List<Long> oldPrefixes = EPBListUtils.transform(excludeLiabilitiesByPrefixes, ExcludeLiabilitiesByPrefix::getPrefixId);
        List<Long> prefixIds = Objects.requireNonNullElse(requestedPrefixIds, new ArrayList<>());

        validateAndSetExcludeLiabilitiesByPrefixes(
                EPBListUtils.getAddedElementsFromList(oldPrefixes, prefixIds),
                reminderId,
                errorMessages
        );

        deleteExcludeLiabilitiesByPrefixes(
                EPBListUtils.getDeletedElementsFromList(oldPrefixes, prefixIds),
                excludeLiabilitiesByPrefixes
        );
    }

    private void deleteExcludeLiabilitiesByPrefixes(List<Long> deletedIds, List<ExcludeLiabilitiesByPrefix> excludeLiabilitiesByPrefixes) {
        Map<Long, ExcludeLiabilitiesByPrefix> excludeLiabilitiesByPrefixMap = EPBListUtils.transformToMap(excludeLiabilitiesByPrefixes, ExcludeLiabilitiesByPrefix::getPrefixId);

        List<ExcludeLiabilitiesByPrefix> deletedExcludeLiabilitiesByPrefixList = deletedIds
                .stream()
                .filter(Objects::nonNull)
                .map(excludeLiabilitiesByPrefixMap::get)
                .filter(Objects::nonNull)
                .peek(excludeLiabilitiesByPrefix -> excludeLiabilitiesByPrefix.setStatus(EntityStatus.DELETED))
                .toList();

        excludeLiabilitiesByPrefixRepository.saveAll(deletedExcludeLiabilitiesByPrefixList);
    }

    private List<ExcludeLiabilitiesByPrefix> getRelatedExcludePrefixes(Long reminderId) {
        Optional<List<ExcludeLiabilitiesByPrefix>> excludeLiabilitiesByPrefixesOptional = excludeLiabilitiesByPrefixRepository.findByReminderIdAndStatus(reminderId, EntityStatus.ACTIVE);
        return excludeLiabilitiesByPrefixesOptional.orElseGet(ArrayList::new);
    }

    private List<OnlyLiabilitiesWithPrefix> getRelatedOnlyLiabilitiesPrefixes(Long reminderId) {
        Optional<List<OnlyLiabilitiesWithPrefix>> onlyLiabilitiesWithPrefixesOptional = onlyLiabilitiesWithPrefixRepository.findByReminderIdAndStatus(reminderId, EntityStatus.ACTIVE);
        return onlyLiabilitiesWithPrefixesOptional.orElseGet(ArrayList::new);
    }

    private void validateAndSetOnlyLiabilitiesWithPrefixesEdit(List<Long> requestedPrefixIds, Long reminderId, List<String> errorMessages) {
        List<OnlyLiabilitiesWithPrefix> onlyLiabilitiesWithPrefixes = getRelatedOnlyLiabilitiesPrefixes(reminderId);
        List<Long> oldPrefixes = EPBListUtils.transform(onlyLiabilitiesWithPrefixes, OnlyLiabilitiesWithPrefix::getPrefixId);
        List<Long> prefixIds = Objects.requireNonNullElse(requestedPrefixIds, new ArrayList<>());

        validateAndSetOnlyLiabilitiesWithPrefixes(
                EPBListUtils.getAddedElementsFromList(oldPrefixes, prefixIds),
                reminderId,
                errorMessages
        );

        deleteOnlyLiabilitiesWithPrefixes(
                EPBListUtils.getDeletedElementsFromList(oldPrefixes, prefixIds),
                onlyLiabilitiesWithPrefixes
        );
    }

    private void deleteOnlyLiabilitiesWithPrefixes(List<Long> deletedIds, List<OnlyLiabilitiesWithPrefix> onlyLiabilitiesWithPrefixes) {
        Map<Long, OnlyLiabilitiesWithPrefix> onlyLiabilitiesWithPrefixMap = EPBListUtils.transformToMap(onlyLiabilitiesWithPrefixes, OnlyLiabilitiesWithPrefix::getPrefixId);

        List<OnlyLiabilitiesWithPrefix> deletedOnlyLiabilitiesWithPrefixList = deletedIds
                .stream()
                .filter(Objects::nonNull)
                .map(onlyLiabilitiesWithPrefixMap::get)
                .filter(Objects::nonNull)
                .peek(onlyLiabilitiesWithPrefix -> onlyLiabilitiesWithPrefix.setStatus(EntityStatus.DELETED))
                .toList();

        onlyLiabilitiesWithPrefixRepository.saveAll(deletedOnlyLiabilitiesWithPrefixList);
    }

    private void validateAndSetPeriodicityEdit(List<Long> periodicityIds, Long reminderId, List<String> errorMessages) {
        List<ReminderPeriodicity> reminderPeriodicityList = getRelatedPeriodicity(reminderId);
        List<Long> oldPrefixes = EPBListUtils.transform(reminderPeriodicityList, ReminderPeriodicity::getPeriodicityId);

        validateAndSetPeriodicity(
                EPBListUtils.getAddedElementsFromList(oldPrefixes, periodicityIds),
                reminderId,
                errorMessages
        );

        deletePeriodicity(
                EPBListUtils.getDeletedElementsFromList(oldPrefixes, periodicityIds),
                reminderPeriodicityList
        );
    }

    private void deletePeriodicity(List<Long> deletedIds, List<ReminderPeriodicity> reminderPeriodicityList) {
        Map<Long, ReminderPeriodicity> periodicityMap = EPBListUtils.transformToMap(reminderPeriodicityList, ReminderPeriodicity::getPeriodicityId);

        List<ReminderPeriodicity> deletetReminderPeriodicityList = deletedIds
                .stream()
                .filter(Objects::nonNull)
                .map(periodicityMap::get)
                .filter(Objects::nonNull)
                .peek(reminderPeriodicity -> reminderPeriodicity.setStatus(EntityStatus.DELETED))
                .toList();

        reminderPeriodicityRepository.saveAll(deletetReminderPeriodicityList);
    }

    private List<ReminderPeriodicity> getRelatedPeriodicity(Long reminderId) {
        Optional<List<ReminderPeriodicity>> reminderPeriodicitiesOptional = reminderPeriodicityRepository.findByReminderIdAndStatus(reminderId, EntityStatus.ACTIVE);
        return reminderPeriodicitiesOptional.orElseGet(ArrayList::new);
    }

    private void validateAndSetListOfCustomersEdit(String customers, Reminder reminder, List<String> errorMessages) {
        if (StringUtils.isNotBlank(customers)) {
            List<String> invalidCustomerIdentifiers = customerRepository.findByStringIdentifierInAndStatus(customers);
            if (CollectionUtils.isEmpty(invalidCustomerIdentifiers)) {
                Set<String> customerSet = Arrays
                        .stream(customers.split(","))
                        .collect(Collectors.toSet());

                reminder.setListOfCustomers(String.join(",", customerSet));
            } else {
                errorMessages.add("listOfCustomers-[listOfCustomers] customers with: %s can't be found;".formatted(StringUtils.join(invalidCustomerIdentifiers)));
            }
        } else {
            reminder.setListOfCustomers(null);
        }
    }

}
