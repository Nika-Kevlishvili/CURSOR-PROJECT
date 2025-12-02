package bg.energo.phoenix.service.receivable.powerSupplyDisconnectionReminder;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.model.documentModels.remiderForDcn.ReminderForDcnDocumentModel;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.receivable.powerSupplyDisconnectionReminder.PSDReminderTemplate;
import bg.energo.phoenix.model.entity.receivable.powerSupplyDisconnectionReminder.PowerSupplyDcnReminderDocFiles;
import bg.energo.phoenix.model.entity.receivable.powerSupplyDisconnectionReminder.PowerSupplyDisconnectionReminder;
import bg.energo.phoenix.model.entity.receivable.powerSupplyDisconnectionReminder.PowerSupplyDisconnectionReminderCustomers;
import bg.energo.phoenix.model.enums.receivable.powerSupplyDisconnectionReminder.PowerSupplyDisconnectionReminderStatus;
import bg.energo.phoenix.model.enums.receivable.reminder.CommunicationChannel;
import bg.energo.phoenix.model.enums.template.ContractTemplatePurposes;
import bg.energo.phoenix.model.enums.template.ContractTemplateStatus;
import bg.energo.phoenix.model.enums.template.ContractTemplateType;
import bg.energo.phoenix.model.request.receivable.powerSupplyDisconnectionReminder.*;
import bg.energo.phoenix.model.response.proxy.FileContent;
import bg.energo.phoenix.model.response.receivable.powerSupplyDisconnectionReminder.*;
import bg.energo.phoenix.model.response.task.TaskShortResponse;
import bg.energo.phoenix.model.response.template.ContractTemplateShortResponse;
import bg.energo.phoenix.permissions.PermissionEnum;
import bg.energo.phoenix.repository.customer.AccountManagerRepository;
import bg.energo.phoenix.repository.nomenclature.product.CurrencyRepository;
import bg.energo.phoenix.repository.receivable.powerSupplyDisconnectionReminder.PSDReminderTemplatesRepository;
import bg.energo.phoenix.repository.receivable.powerSupplyDisconnectionReminder.PowerSupplyDcnReminderDocFileRepository;
import bg.energo.phoenix.repository.receivable.powerSupplyDisconnectionReminder.PowerSupplyDisconnectionReminderCustomersRepository;
import bg.energo.phoenix.repository.receivable.powerSupplyDisconnectionReminder.PowerSupplyDisconnectionReminderRepository;
import bg.energo.phoenix.repository.template.ContractTemplateRepository;
import bg.energo.phoenix.security.PermissionService;
import bg.energo.phoenix.service.document.ReminderForDcnDocumentCreationService;
import bg.energo.phoenix.service.document.enums.FileFormat;
import bg.energo.phoenix.service.document.ftpService.FileService;
import bg.energo.phoenix.service.documentMergerService.DocumentMergerService;
import bg.energo.phoenix.service.task.TaskService;
import bg.energo.phoenix.util.epb.EPBChainedExceptionTriggerUtil;
import bg.energo.phoenix.util.epb.EPBListUtils;
import bg.energo.phoenix.util.epb.EPBStringUtils;
import bg.energo.phoenix.model.entity.nomenclature.product.Currency;
import bg.energo.phoenix.model.response.receivable.powerSupplyDisconnectionReminder.PowerSupplyDisconnectionReminderExecutionResponse;
import static bg.energo.phoenix.permissions.PermissionContextEnum.REMINDER_FOR_DISCONNECTION_OF_POWER_SUPPLY;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.collections4.ListUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import java.time.LocalDateTime;
import java.util.concurrent.*;


@Service
@RequiredArgsConstructor
@Slf4j
public class PowerSupplyDisconnectionReminderService {

    private final PowerSupplyDisconnectionReminderMapperService mapperService;
    private final ReminderForDcnDocumentCreationService documentCreationService;
    private final PermissionService permissionService;

    private final PowerSupplyDisconnectionReminderRepository powerSupplyDisconnectionReminderRepository;
    private final PowerSupplyDcnReminderDocFileRepository powerSupplyDcnReminderDocFileRepository;
    private final PSDReminderTemplatesRepository psdReminderTemplatesRepository;
    private final ContractTemplateRepository contractTemplateRepository;
    private final AccountManagerRepository accountManagerRepository;
    private final FileService fileService;
    private final TaskService taskService;
    private final PowerSupplyDisconnectionReminderRepository reminderRepository;
    private final PowerSupplyDisconnectionReminderCustomersRepository powerSupplyDisconnectionReminderCustomersRepository;
    private final CurrencyRepository currencyRepository;
    private final ReminderForDcnDocumentCreationService reminderForDcnDocumentCreationService;
    private final DocumentMergerService documentMergerService;
    private final PowerSupplyDisconnectionReminderCommunicationService powerSupplyDisconnectionReminderCommunicationService;

    private final static int numberOfThreads = 10;

    public List<TaskShortResponse> getTasks(Long id) {
        return taskService.getTasksByPowerSupplyDisconnectionReminderId(id);
    }

    /**
     * Creates a new power supply disconnection reminder.
     *
     * @param request the request object containing the details for the new reminder
     * @return the ID of the newly created reminder
     */
    @Transactional
    public Long create(PowerSupplyDisconnectionReminderBaseRequest request) {
        log.info("Creating reminder for disconnection of power supply with request: {}", request);

        List<String> errorMessages = new ArrayList<>();
        PowerSupplyDisconnectionReminder reminder = mapperService.mapParametersForCreate(request, errorMessages);
        validateAndSetTemplate(request.getDocumentTemplateId(), reminder.getDocumentTemplateId(), errorMessages, ContractTemplateType.DOCUMENT, reminder);
        validateAndSetTemplate(request.getEmailTemplateId(), reminder.getEmailTemplateId(), errorMessages, ContractTemplateType.EMAIL, reminder);
        validateAndSetTemplate(request.getSmsTemplateId(), reminder.getSmsTemplateId(), errorMessages, ContractTemplateType.SMS, reminder);
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);

        return reminder.getId();
    }

    /**
     * Retrieves the details of a power supply disconnection reminder by its ID.
     *
     * @param id the ID of the power supply disconnection reminder to retrieve
     * @return the details of the power supply disconnection reminder
     * @throws DomainEntityNotFoundException if the reminder with the given ID is not found
     * @throws ClientException               if the user does not have the necessary permissions to view the reminder
     */
    @Transactional(readOnly = true)
    public PowerSupplyDisconnectionReminderResponse view(Long id) {
        log.info("Previewing reminder for disconnection of power supply with id: %s".formatted(id));

        PowerSupplyDisconnectionReminder reminder = powerSupplyDisconnectionReminderRepository.findById(id).orElseThrow(() -> new DomainEntityNotFoundException("Can't find reminder for disconnection of power supply with id: %s;".formatted(id)));

        if (reminder.getStatus().equals(EntityStatus.DELETED)) {
            if (!hasDeletedPermission()) {
                throw new ClientException("You don't have View deleted reminder for disconnection of power supply Permission;", ErrorCode.ACCESS_DENIED);
            }
        } else if (reminder.getReminderStatus().equals(PowerSupplyDisconnectionReminderStatus.DRAFT)) {
            if (!hasViewDraftPermission()) {
                throw new ClientException("You don't have View draft reminder for disconnection of power supply Permission;", ErrorCode.ACCESS_DENIED);
            }
        } else if (reminder.getReminderStatus().equals(PowerSupplyDisconnectionReminderStatus.EXECUTED)) {
            if (!hasViewExecutedPermission()) {
                throw new ClientException("You don't have View executed reminder for disconnection of power supply Permission;", ErrorCode.ACCESS_DENIED);
            }
        }

        PowerSupplyDisconnectionReminderResponse response = mapperService.mapToReminderResponse(reminder);
        applyTemplateResponse(reminder.getDocumentTemplateId(), response::setDocumentTemplateResponse);
        applyTemplateResponse(reminder.getEmailTemplateId(), response::setEmailTemplateResponse);
        applyTemplateResponse(reminder.getSmsTemplateId(), response::setSmsTemplateResponse);
        setPowerSupplyDisconnectionReminderDocFiles(reminder, response);

        return response;
    }

    private void setPowerSupplyDisconnectionReminderDocFiles(PowerSupplyDisconnectionReminder reminder, PowerSupplyDisconnectionReminderResponse response) {
        validateReminder(reminder);
        List<PowerSupplyDcnReminderDocFileResponse> files = fetchReminderFiles(reminder);
        response.setSubFiles(files);
    }

    private void validateReminder(PowerSupplyDisconnectionReminder reminder) {
        if (reminder == null || reminder.getId() == null) {
            throw new ClientException("Reminder or reminder ID cannot be null", ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED);
        }
    }

    private List<PowerSupplyDcnReminderDocFileResponse> fetchReminderFiles(PowerSupplyDisconnectionReminder reminder) {
        return powerSupplyDcnReminderDocFileRepository.findPowerSupplyDcnReminderDocFilesByReminderForDcnIdAndStatus(reminder.getId(), EntityStatus.ACTIVE).stream().map(file -> new PowerSupplyDcnReminderDocFileResponse(file, getManagerDisplayName(file.getSystemUserId()))).toList();
    }

    private String getManagerDisplayName(String userId) {
        return accountManagerRepository.findByUserName(userId).map(manager -> " (".concat(manager.getDisplayName()).concat(")")).orElse("");
    }

    /**
     * Updates an existing power supply disconnection reminder.
     *
     * @param request the request object containing the updated details for the reminder
     * @param id      the ID of the power supply disconnection reminder to update
     * @return the ID of the updated reminder
     * @throws DomainEntityNotFoundException if the reminder with the given ID is not found
     * @throws ClientException               if the user does not have the necessary permissions to update the reminder or if certain fields cannot be edited
     */
    @Transactional
    public Long edit(PowerSupplyDisconnectionReminderBaseRequest request, Long id) {
        List<String> errorMessages = new ArrayList<>();
        PowerSupplyDisconnectionReminder powerSupplyDisconnectionReminder = powerSupplyDisconnectionReminderRepository.findByIdAndGeneralStatuses(id, List.of(EntityStatus.ACTIVE)).orElseThrow(() -> new DomainEntityNotFoundException("Reminder for disconnection of power supply not found with id : " + id));

        if (powerSupplyDisconnectionReminder.getReminderStatus().equals(PowerSupplyDisconnectionReminderStatus.EXECUTED)) {
            checkForNulls(request, powerSupplyDisconnectionReminder, errorMessages);
            EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
        }

        mapperService.mapParametersForUpdate(request, errorMessages, powerSupplyDisconnectionReminder);
        Map<ContractTemplateType, Long> templateMap = new HashMap<>();

        if (request.getEmailTemplateId() != null) {
            templateMap.put(ContractTemplateType.EMAIL, request.getEmailTemplateId());
        }
        if (request.getSmsTemplateId() != null) {
            templateMap.put(ContractTemplateType.SMS, request.getSmsTemplateId());
        }
        if (request.getDocumentTemplateId() != null) {
            templateMap.put(ContractTemplateType.DOCUMENT, request.getDocumentTemplateId());
        }

        templateMap.forEach((type, templateId) -> validateAndSetTemplate(templateId, getTemplateIdByType(powerSupplyDisconnectionReminder, type), errorMessages, type, powerSupplyDisconnectionReminder));
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);

        return powerSupplyDisconnectionReminder.getId();
    }

    public ReminderForDcnDocumentModel createDocument(Long reminderId, Long templateId, Long customerId, FileFormat fileFormat) {
        return documentCreationService.generateDocument(reminderId, templateId, customerId, fileFormat, false).getLeft();
    }

    public FileContent downloadDocument(Long id) {
        PowerSupplyDcnReminderDocFiles document = powerSupplyDcnReminderDocFileRepository.findById(id).orElseThrow(() -> new DomainEntityNotFoundException("document with id %s not found".formatted(id)));
        var content = fileService.downloadFile(document.getFileUrl());
        return new FileContent(document.getFileName(), content.getByteArray());
    }


    /**
     * Checks for null or changed values in the provided PowerSupplyDisconnectionReminderBaseRequest and adds error messages to the provided list.
     * <p>
     * This method is used to validate that certain fields in the edit request cannot be modified for reminders that have been executed.
     *
     * @param request                          the edit request containing the updated details for the reminder
     * @param powerSupplyDisconnectionReminder the existing reminder being updated
     * @param errorMessages                    the list to add any error messages to
     */
    private void checkForNulls(PowerSupplyDisconnectionReminderBaseRequest request, PowerSupplyDisconnectionReminder powerSupplyDisconnectionReminder, List<String> errorMessages) {
        verifyField("customerSendToDateAndTime", request.getCustomerSendToDateAndTime(), powerSupplyDisconnectionReminder.getCustomerSendDate(), errorMessages);
        verifyField("liabilityAmountFrom", request.getLiabilityAmountFrom(), powerSupplyDisconnectionReminder.getLiabilityAmountFrom(), errorMessages);
        verifyField("liabilityAmountTo", request.getLiabilityAmountTo(), powerSupplyDisconnectionReminder.getLiabilityAmountTo(), errorMessages);
        verifyField("currencyId", request.getCurrencyId(), powerSupplyDisconnectionReminder.getCurrencyId(), errorMessages);
        verifyField("liabilitiesMaxDueDate", request.getLiabilitiesMaxDueDate(), powerSupplyDisconnectionReminder.getLiabilitiesMaxDueDate(), errorMessages);
        verifyField("excludeCustomers", request.getExcludeCustomers(), powerSupplyDisconnectionReminder.getExcludedCustomerList(), errorMessages);
    }

    private <T> void verifyField(String fieldName, T requestValue, T existingValue, List<String> errorMessages) {
        if (!Objects.equals(requestValue, existingValue)) {
            errorMessages.add(String.format("%s-[%s] it is not possible to edit %s;", fieldName, fieldName, fieldName));
        }
    }

    /**
     * Lists the Power Supply Disconnection Reminders based on the provided request parameters.
     *
     * @param request the request containing the parameters to filter the reminders
     * @return a page of Power Supply Disconnection Reminder listing responses
     */
    public Page<PowerSupplyDisconnectionReminderListingResponse> list(PowerSupplyDisconnectionReminderListingRequest request) {
        log.info("Calling reminder for disconnection of power supply listing with request: %s".formatted(request));

        List<EntityStatus> statuses = getStatuses();
        List<PowerSupplyDisconnectionReminderStatus> reminderStatuses = getReminderStatuses(request);
        checkPermissionsForRequestedStatuses(request.getStatuses());

        return powerSupplyDisconnectionReminderRepository.filter(getSearchByEnum(request.getSearchFields()), EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()), request.getNumberOfCustomersFrom(), request.getNumberOfCustomersTo(), request.getCreationDateFrom(), request.getCreationDateTo(), request.getSendingDateFrom(), request.getSendingDateTo(), EPBListUtils.convertEnumListIntoStringListIfNotNull(reminderStatuses), EPBListUtils.convertEnumListIntoStringListIfNotNull(statuses), PageRequest.of(request.getPage(), request.getSize(), Sort.by(new Sort.Order(request.getDirection(), getSorByEnum(request.getColumns())))));
    }

    /**
     * Lists the Power Supply Disconnection Reminders based on the provided request parameters.
     *
     * @param request the request containing the parameters to filter the reminders
     * @return a page of RemindersForDPSRequestResponse objects representing the filtered reminders
     */
    public Page<RemindersForDPSRequestResponse> listForDisconnectionRequest(ListForDisconnectionRequestRequest request) {
        return powerSupplyDisconnectionReminderRepository.getRemindersForDPSRequest(EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()), PageRequest.of(request.getPage(), request.getSize())).map(RemindersForDPSRequestResponse::new);
    }

    /**
     * Deletes a Power Supply Disconnection Reminder with the given ID, if it exists and has a status of DRAFT.
     *
     * @param id the ID of the Power Supply Disconnection Reminder to delete
     * @throws DomainEntityNotFoundException if the Power Supply Disconnection Reminder with the given ID is not found
     * @throws ClientException               if the Power Supply Disconnection Reminder has a status other than DRAFT
     */
    @Transactional
    public void delete(Long id) {
        PowerSupplyDisconnectionReminder powerSupplyDisconnectionReminder = powerSupplyDisconnectionReminderRepository.findByIdAndGeneralStatuses(id, List.of(EntityStatus.ACTIVE)).orElseThrow(() -> new DomainEntityNotFoundException("Power Supply Disconnection reminder not found with id : " + id));
        if (!powerSupplyDisconnectionReminder.getReminderStatus().equals(PowerSupplyDisconnectionReminderStatus.DRAFT)) {
            throw new ClientException("It is not possible to delete Power Supply Disconnection reminder with status executed;", ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED);
        }
        powerSupplyDisconnectionReminder.setStatus(EntityStatus.DELETED);
    }

    /**
     * Lists the Power Supply Disconnection Reminders based on the provided request parameters.
     *
     * @param request the request containing the parameters to filter the reminders
     * @return a page of Power Supply Disconnection Reminder listing responses
     */
    public Page<PowerSupplyDisconnectionReminderSecondTabResponse> secondTab(PowerSupplyDisconnectionReminderSecondTabRequest request) {
        return powerSupplyDisconnectionReminderRepository.secondTabFilter(getSearchBySecondTabEnum(request.getSearchFields()), EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()), request.getPowerSupplyDisconnectionReminderId(), PageRequest.of(request.getPage(), request.getSize()));
    }

    /**
     * Retrieves the search field value from the provided ReminderForDisconnectionSecondTabSearchFields enum.
     * If the searchFields parameter is not null, it returns the value of the provided enum.
     * Otherwise, it returns the value of the ReminderForDisconnectionSecondTabSearchFields.ALL enum.
     *
     * @param searchFields the ReminderForDisconnectionSecondTabSearchFields enum to retrieve the value from
     * @return the value of the provided enum or the value of ReminderForDisconnectionSecondTabSearchFields.ALL
     */
    private String getSearchBySecondTabEnum(ReminderForDisconnectionSecondTabSearchFields searchFields) {
        return searchFields != null ? searchFields.getValue() : ReminderForDisconnectionSecondTabSearchFields.ALL.getValue();
    }

    /**
     * Retrieves the search field value from the provided ReminderForDisconnectionSearchFields enum.
     * If the searchFields parameter is not null, it returns the value of the provided enum.
     * Otherwise, it returns the value of the ReminderForDisconnectionSearchFields.ALL enum.
     *
     * @param searchFields the ReminderForDisconnectionSearchFields enum to retrieve the value from
     * @return the value of the provided enum or the value of ReminderForDisconnectionSearchFields.ALL
     */
    private String getSearchByEnum(ReminderForDisconnectionSearchFields searchFields) {
        return searchFields != null ? searchFields.getValue() : ReminderForDisconnectionSearchFields.ALL.getValue();
    }

    /**
     * Retrieves the sort field value from the provided ReminderForDisconnectionListColumns enum.
     * If the sortByColumn parameter is not null, it returns the value of the provided enum.
     * Otherwise, it returns the value of the ReminderForDisconnectionListColumns.CREATION_DATE enum.
     *
     * @param sortByColumn the ReminderForDisconnectionListColumns enum to retrieve the value from
     * @return the value of the provided enum or the value of ReminderForDisconnectionListColumns.CREATION_DATE
     */
    private String getSorByEnum(ReminderForDisconnectionListColumns sortByColumn) {
        return sortByColumn != null ? sortByColumn.getValue() : ReminderForDisconnectionListColumns.CREATION_DATE.getValue();
    }

    /**
     * Checks if the current user has the permission to view deleted reminders.
     *
     * @return true if the user has the 'REMINDER_FOR_DISCONNECTION_OF_POWER_SUPPLY_VIEW_DELETE' permission, false otherwise.
     */
    private boolean hasDeletedPermission() {
        return permissionService.permissionContextContainsPermissions(REMINDER_FOR_DISCONNECTION_OF_POWER_SUPPLY, List.of(PermissionEnum.REMINDER_FOR_DISCONNECTION_OF_POWER_SUPPLY_VIEW_DELETE));
    }

    /**
     * Checks if the current user has the permission to view draft reminders.
     *
     * @return true if the user has the 'REMINDER_FOR_DISCONNECTION_OF_POWER_SUPPLY_VIEW_DRAFT' permission, false otherwise.
     */
    private boolean hasViewDraftPermission() {
        return permissionService.permissionContextContainsPermissions(REMINDER_FOR_DISCONNECTION_OF_POWER_SUPPLY, List.of(PermissionEnum.REMINDER_FOR_DISCONNECTION_OF_POWER_SUPPLY_VIEW_DRAFT));
    }

    /**
     * Checks if the current user has the permission to view executed reminders.
     *
     * @return true if the user has the 'REMINDER_FOR_DISCONNECTION_OF_POWER_SUPPLY_VIEW_EXECUTED' permission, false otherwise.
     */
    private boolean hasViewExecutedPermission() {
        return permissionService.permissionContextContainsPermissions(REMINDER_FOR_DISCONNECTION_OF_POWER_SUPPLY, List.of(PermissionEnum.REMINDER_FOR_DISCONNECTION_OF_POWER_SUPPLY_VIEW_EXECUTED));
    }

    /**
     * Retrieves the list of statuses that the current user has permission to view.
     * If the user has the 'REMINDER_FOR_DISCONNECTION_OF_POWER_SUPPLY_VIEW_DELETE' permission, it returns all available statuses.
     * If the user has the 'REMINDER_FOR_DISCONNECTION_OF_POWER_SUPPLY_VIEW_DRAFT' and/or 'REMINDER_FOR_DISCONNECTION_OF_POWER_SUPPLY_VIEW_EXECUTED' permissions, it returns the corresponding statuses.
     * If the user only has the 'REMINDER_FOR_DISCONNECTION_OF_POWER_SUPPLY_VIEW_DELETE' permission, it returns the 'DELETED' status.
     * If the user has none of the required permissions, it returns an empty list.
     *
     * @return the list of statuses the user has permission to view
     */
    private List<EntityStatus> getStatuses() {
        if (hasViewDraftPermission() || hasViewExecutedPermission()) {
            if (hasDeletedPermission()) {
                return Arrays.asList(EntityStatus.values());
            } else {
                return List.of(EntityStatus.ACTIVE);
            }
        } else if (hasDeletedPermission()) {
            return List.of(EntityStatus.DELETED);
        } else {
            return Collections.emptyList();
        }
    }

    /**
     * Retrieves the list of reminder statuses that the current user has permission to view.
     * If the user has the 'REMINDER_FOR_DISCONNECTION_OF_POWER_SUPPLY_VIEW_DRAFT' and/or 'REMINDER_FOR_DISCONNECTION_OF_POWER_SUPPLY_VIEW_EXECUTED' permissions, it returns the corresponding statuses.
     * If the user has no permissions, it returns an empty list.
     *
     * @param request the request object containing the requested reminder statuses
     * @return the list of reminder statuses the user has permission to view
     */
    private List<PowerSupplyDisconnectionReminderStatus> getReminderStatuses(PowerSupplyDisconnectionReminderListingRequest request) {
        List<PowerSupplyDisconnectionReminderStatus> reminderStatuses = request.getStatuses();

        if (reminderStatuses == null || reminderStatuses.isEmpty()) {
            reminderStatuses = new ArrayList<>();
            if (hasViewDraftPermission()) {
                reminderStatuses.add(PowerSupplyDisconnectionReminderStatus.DRAFT);
            }
            if (hasViewExecutedPermission()) {
                reminderStatuses.add(PowerSupplyDisconnectionReminderStatus.EXECUTED);
            }
        }

        return reminderStatuses;
    }

    /**
     * Checks the permissions for the requested reminder statuses.
     * If the user does not have the necessary permissions to view a requested status, a {@link ClientException} is thrown.
     *
     * @param requestedStatuses the list of requested reminder statuses to check permissions for
     * @throws ClientException if the user does not have permission to view a requested status
     */
    private void checkPermissionsForRequestedStatuses(List<PowerSupplyDisconnectionReminderStatus> requestedStatuses) {
        if (requestedStatuses != null) {
            for (PowerSupplyDisconnectionReminderStatus status : requestedStatuses) {
                if (status == PowerSupplyDisconnectionReminderStatus.DRAFT && !hasViewDraftPermission()) {
                    throw new ClientException("You do not have permission to view draft reminders;", ErrorCode.ACCESS_DENIED);
                }
                if (status == PowerSupplyDisconnectionReminderStatus.EXECUTED && !hasViewExecutedPermission()) {
                    throw new ClientException("You do not have permission to view executed reminders;", ErrorCode.ACCESS_DENIED);
                }
            }
        }
    }

    private Long getTemplateIdByType(PowerSupplyDisconnectionReminder reminder, ContractTemplateType type) {
        return switch (type) {
            case DOCUMENT -> reminder.getDocumentTemplateId();
            case EMAIL -> reminder.getEmailTemplateId();
            case SMS -> reminder.getSmsTemplateId();
        };
    }

    private void applyTemplateResponse(Long templateId, Consumer<ContractTemplateShortResponse> setter) {
        if (templateId != null) {
            contractTemplateRepository.findTemplateResponseById(templateId, LocalDate.now()).ifPresent(setter);
        }
    }

    /**
     * Validates and sets the template ID for a reminder based on the provided contract template type.
     *
     * @param templateId           the ID of the template to validate and set
     * @param reminderTemplateId   the existing template ID of the reminder
     * @param errorMessages        a list to store any error messages encountered during the validation
     * @param contractTemplateType the type of contract template (document, email, or SMS)
     * @param reminder             the reminder to update with the validated template ID
     */
    private void validateAndSetTemplate(Long templateId, Long reminderTemplateId, List<String> errorMessages, ContractTemplateType contractTemplateType, PowerSupplyDisconnectionReminder reminder) {
        if (Objects.equals(templateId, reminderTemplateId)) return;
        if (templateId == null) {
            setTemplateIdToNull(reminder, contractTemplateType);
            return;
        }
        checkTemplateExists(templateId, contractTemplateType, errorMessages);
        setTemplateId(reminder, templateId, contractTemplateType);
    }

    private void setTemplateIdToNull(PowerSupplyDisconnectionReminder reminder, ContractTemplateType contractTemplateType) {
        switch (contractTemplateType) {
            case DOCUMENT -> reminder.setDocumentTemplateId(null);
            case EMAIL -> reminder.setEmailTemplateId(null);
            case SMS -> reminder.setSmsTemplateId(null);
        }
    }

    private void checkTemplateExists(Long templateId, ContractTemplateType contractTemplateType, List<String> errorMessages) {
        if (!contractTemplateRepository.existsByIdAndTemplatePurposeAndTemplateType(templateId, ContractTemplatePurposes.REMINDER_DISCONNECT_POWER, contractTemplateType, LocalDate.now())) {
            errorMessages.add("templateId-Template with id %s do not exist!;".formatted(templateId));
        }
    }

    private void setTemplateId(PowerSupplyDisconnectionReminder reminder, Long templateId, ContractTemplateType contractTemplateType) {
        switch (contractTemplateType) {
            case DOCUMENT -> reminder.setDocumentTemplateId(templateId);
            case EMAIL -> reminder.setEmailTemplateId(templateId);
            case SMS -> reminder.setSmsTemplateId(templateId);
        }
    }

    /**
     * Updates the templates associated with a power supply disconnection reminder.
     *
     * @param templateIds                   the IDs of the templates to update
     * @param objectionToChangeWithdrawalId the ID of the object to change withdrawal
     * @param errorMessages                 a list to store any error messages encountered during the update
     */
    public void updateTemplates(Set<Long> templateIds, Long objectionToChangeWithdrawalId, List<String> errorMessages) {
        if (CollectionUtils.isEmpty(templateIds)) {
            return;
        }
        Map<Long, PSDReminderTemplate> templateMap = psdReminderTemplatesRepository.findByProductDetailId(objectionToChangeWithdrawalId).stream().collect(Collectors.toMap(PSDReminderTemplate::getTemplateId, j -> j));
        List<PSDReminderTemplate> templatesToSave = new ArrayList<>();
        Map<Long, Integer> templatesToCheck = new HashMap<>();
        int i = 0;
        for (Long templateId : templateIds) {
            PSDReminderTemplate remove = templateMap.remove(templateId);
            if (remove == null) {
                templatesToSave.add(new PSDReminderTemplate(templateId, objectionToChangeWithdrawalId));
                templatesToCheck.put(templateId, i);
            }
            i++;
        }
        Set<Long> allIdByIdAndStatus = contractTemplateRepository.findAllIdByIdAndStatusAndPurpose(templatesToCheck.keySet(), ContractTemplatePurposes.REMINDER_DISCONNECT_POWER, ContractTemplateStatus.ACTIVE);
        templatesToCheck.forEach((key, value) -> {
            if (!allIdByIdAndStatus.contains(key)) {
                errorMessages.add("templateIds[%s]-Template with id %s was not found or has wrong purpose;".formatted(value, key));
            }
        });
        if (!errorMessages.isEmpty()) {
            return;
        }
        Collection<PSDReminderTemplate> values = templateMap.values();
        for (PSDReminderTemplate value : values) {
            value.setStatus(EntityStatus.DELETED);
            templatesToSave.add(value);
        }
        psdReminderTemplatesRepository.saveAll(templatesToSave);

    }

    @Transactional
    @SneakyThrows
    public void documentGenerationJob() {
        log.debug("starting process");
        Map<Long, PowerSupplyDisconnectionReminder> disconnectionReminders = reminderRepository.findByStatusAndSendTimePessimisticLock(PowerSupplyDisconnectionReminderStatus.DRAFT, LocalDateTime.now(), LocalDateTime.now().minusHours(1)).stream().collect(Collectors.toMap(PowerSupplyDisconnectionReminder::getId, x -> x));
        Set<Long> toExecute = new HashSet<>();
        if (!disconnectionReminders.isEmpty()) {
            List<Long> ids = disconnectionReminders.keySet().stream().toList();
            Currency defaultCurrency = currencyRepository.findByDefaultSelectionIsTrue().orElseThrow(() -> new DomainEntityNotFoundException("Default currency not found!"));
            List<PowerSupplyDisconnectionReminderCustomers> executed = reminderRepository.execute(ids).stream().filter(resp -> {
                if (resp.getPsdrCurrencyId() != null && !resp.getCurrencyId().equals(resp.getPsdrCurrencyId()) && !Objects.equals(resp.getAlternativeCurrencyId(), resp.getPsdrCurrencyId())) {
                    log.debug("Can not convert from liability currency to reminder currency");
                    log.debug("Liability currency id : {},PSDR currency id : {}", resp.getCurrencyId(), resp.getPsdrCurrencyId());
                    return false;
                }
                if (resp.getPsdrCurrencyId() == null && !resp.getCurrencyId().equals(defaultCurrency.getId()) && !Objects.equals(resp.getAlternativeCurrencyId(), defaultCurrency.getId())) {
                    log.debug("Can not convert from liability currency to default currency");
                    log.debug("Liability currency id {}, default currency id {}", resp.getCurrencyId(), defaultCurrency.getId());
                    return false;
                }
                return true;
            }).map(this::create).peek(ex -> toExecute.add(ex.getPowerSupplyDisconnectionReminderId())).toList();
            int batchSize = executed.size() / numberOfThreads + 1;
            ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
            List<List<PowerSupplyDisconnectionReminderCustomers>> partitions = ListUtils.partition(executed.stream().toList(), batchSize);
            for (List<PowerSupplyDisconnectionReminderCustomers> partition : partitions) {
                executorService.submit(() -> insertChunk(partition));
            }
            Map<PowerSupplyDisconnectionReminder, List<PowerSupplyDcnReminderDocFiles>> remindersAndGeneratedDocuments = generateReminderDocuments(disconnectionReminders, executed, executorService);

            try {
                for (PowerSupplyDisconnectionReminder reminder : disconnectionReminders.values()) {
                    reminderRepository.saveAndFlush(reminder);
                    executorService.submit(() -> {
                        try {
                            if (reminder.getCommunicationChannels().contains(CommunicationChannel.ON_PAPER)) {
                                documentMergerService.mergePSDRDocuments(reminder, remindersAndGeneratedDocuments.get(reminder));
                            }

                            List<PowerSupplyDisconnectionReminderCustomers> reminderCustomers = executed.stream().filter(c -> c.getPowerSupplyDisconnectionReminderId().equals(reminder.getId())).collect(Collectors.toList());

                            if (!reminder.getCommunicationChannels().isEmpty()) {
                                powerSupplyDisconnectionReminderCommunicationService.generateCommunications(reminder, reminderCustomers);
                            }

                            reminder.setReminderStatus(PowerSupplyDisconnectionReminderStatus.EXECUTED);
                        } catch (Exception e) {
                            log.error("Error processing reminder ID: {}", reminder.getId(), e);
                        }
                    });
                }
            } finally {
                executorService.shutdown();
                boolean ignored = executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            }
            log.debug("process end");
        }
    }

    private Map<PowerSupplyDisconnectionReminder, List<PowerSupplyDcnReminderDocFiles>> generateReminderDocuments(Map<Long, PowerSupplyDisconnectionReminder> disconnectionReminders, List<PowerSupplyDisconnectionReminderCustomers> executed, ExecutorService executorService) {

        log.debug("Generating reminder documents for executed reminders: {}", executed.size());
        Set<Long> processedCustomerIds = new HashSet<>();
        ConcurrentHashMap<PowerSupplyDisconnectionReminder, List<PowerSupplyDcnReminderDocFiles>> remindersAndGeneratedDocuments = new ConcurrentHashMap<>();
        List<Future<?>> futures = new ArrayList<>();

        for (PowerSupplyDisconnectionReminder reminder : disconnectionReminders.values()) {
            remindersAndGeneratedDocuments.putIfAbsent(reminder, new CopyOnWriteArrayList<>());
        }

        for (PowerSupplyDisconnectionReminderCustomers customer : executed) {
            if (!processedCustomerIds.add(customer.getCustomerId())) {
                continue;
            }

            Long reminderId = customer.getPowerSupplyDisconnectionReminderId();
            PowerSupplyDisconnectionReminder reminder = disconnectionReminders.get(reminderId);

            if (reminder != null && reminder.getCommunicationChannels().contains(CommunicationChannel.ON_PAPER)) {
                log.debug("Generating paper document for reminder ID: {} and customer ID: {}", reminderId, customer.getCustomerId());

                futures.add(executorService.submit(() -> {
                    try {
                        log.info("Starting document generation for reminder ID: {} and customer ID: {}", reminderId, customer.getCustomerId());
                        Pair<ReminderForDcnDocumentModel, PowerSupplyDcnReminderDocFiles> pair = reminderForDcnDocumentCreationService.generateDocument(reminderId, reminder.getDocumentTemplateId(), customer.getCustomerId(), FileFormat.PDF, true);
                        remindersAndGeneratedDocuments.get(reminder).add(pair.getRight());
                    } catch (Exception e) {
                        log.error("Error generating document for reminder ID: {}", reminderId, e);
                    }
                }));
            }
        }

        // Ensure all tasks finish before returning
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                log.error("Error while generating documents", e);
            }
        }

        log.debug("Finished generating reminder documents for executed reminders: {}", executed.size());
        return remindersAndGeneratedDocuments;
    }

    private void insertChunk(List<PowerSupplyDisconnectionReminderCustomers> chunk) {
        powerSupplyDisconnectionReminderCustomersRepository.saveAll(chunk);
    }

    private PowerSupplyDisconnectionReminderCustomers create(PowerSupplyDisconnectionReminderExecutionResponse response) {
        PowerSupplyDisconnectionReminderCustomers powerSupplyDisconnectionReminderCustomers = new PowerSupplyDisconnectionReminderCustomers();
        powerSupplyDisconnectionReminderCustomers.setCustomerId(response.getCustomerId());
        powerSupplyDisconnectionReminderCustomers.setPowerSupplyDisconnectionReminderId(response.getPsdrId());
        powerSupplyDisconnectionReminderCustomers.setLiabilityAmount(response.getCurrentAmount());
        powerSupplyDisconnectionReminderCustomers.setCustomerLiabilityId(response.getLiabilityId());
        return powerSupplyDisconnectionReminderCustomers;
    }
}
