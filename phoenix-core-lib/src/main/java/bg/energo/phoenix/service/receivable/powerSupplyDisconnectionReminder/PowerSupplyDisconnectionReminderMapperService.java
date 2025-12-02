package bg.energo.phoenix.service.receivable.powerSupplyDisconnectionReminder;

import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.nomenclature.product.Currency;
import bg.energo.phoenix.model.entity.receivable.powerSupplyDisconnectionReminder.PowerSupplyDisconnectionReminder;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.receivable.powerSupplyDisconnectionReminder.PowerSupplyDisconnectionReminderStatus;
import bg.energo.phoenix.model.enums.receivable.reminder.CommunicationChannel;
import bg.energo.phoenix.model.request.receivable.powerSupplyDisconnectionReminder.PowerSupplyDisconnectionReminderBaseRequest;
import bg.energo.phoenix.model.response.receivable.powerSupplyDisconnectionReminder.PowerSupplyDisconnectionReminderResponse;
import bg.energo.phoenix.model.response.shared.ShortResponse;
import bg.energo.phoenix.model.response.task.TaskShortResponse;
import bg.energo.phoenix.repository.customer.CustomerRepository;
import bg.energo.phoenix.repository.nomenclature.product.CurrencyRepository;
import bg.energo.phoenix.repository.receivable.powerSupplyDisconnectionReminder.PowerSupplyDisconnectionReminderRepository;
import bg.energo.phoenix.service.task.TaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PowerSupplyDisconnectionReminderMapperService {

    private static final String RESCHEDULING_PREFIX = "Reminder-";
    private final CurrencyRepository currencyRepository;
    private final PowerSupplyDisconnectionReminderRepository powerSupplyDisconnectionReminderRepository;
    private final CustomerRepository customerRepository;
    private final TaskService taskService;

    /**
     * Maps the parameters from the given request to create a new PowerSupplyDisconnectionReminder entity,
     * and saves it with an initial temporary reminder number. The method also performs additional validations
     * and sets relevant fields such as reminder status, excluded customers, and communication channels.
     *
     * @param request       The input request containing details required to create the power supply disconnection reminder.
     * @param errorMessages A list to capture error messages during the validation and mapping process.
     * @return A newly created and saved PowerSupplyDisconnectionReminder object with all properties set.
     */
    public PowerSupplyDisconnectionReminder mapParametersForCreate(PowerSupplyDisconnectionReminderBaseRequest request, List<String> errorMessages) {
        log.info("Mapping parameters for reminder for disconnection of power supply create with request %s;".formatted(request));
        PowerSupplyDisconnectionReminder reminder = new PowerSupplyDisconnectionReminder();
        //set temporary number for saving
        reminder.setReminderNumber("TEMPORARY_NUMBER");
        reminder.setReminderStatus(PowerSupplyDisconnectionReminderStatus.DRAFT);
        reminder.setStatus(EntityStatus.ACTIVE);
        reminder.setCurrencyId(request.getCurrencyId());
        reminder.setCustomerSendDate(request.getCustomerSendToDateAndTime());
        reminder.setLiabilitiesMaxDueDate(request.getLiabilitiesMaxDueDate());
        reminder.setLiabilityAmountFrom(request.getLiabilityAmountFrom());
        reminder.setLiabilityAmountTo(request.getLiabilityAmountTo());
        reminder.setExcludedCustomerList(request.getExcludeCustomers());
        reminder.setDisconnectionDate(request.getDisconnectionDate());
        reminder.setCommunicationChannels(CollectionUtils.isEmpty(request.getCommunicationChannels()) ? List.of(CommunicationChannel.EMAIL) : request.getCommunicationChannels());
        checkAndSetCurrency(request.getCurrencyId(), reminder);
        powerSupplyDisconnectionReminderRepository.saveAndFlush(reminder);
        reminder.setReminderNumber(RESCHEDULING_PREFIX + reminder.getId());
        Set<String> excludedCustomerIds = parseExcludeCustomers(request.getExcludeCustomers());
        checkAndSetReminderCustomers(excludedCustomerIds, errorMessages);

        return reminder;
    }

    /**
     * Maps a PowerSupplyDisconnectionReminder object to a PowerSupplyDisconnectionReminderResponse object.
     *
     * @param reminder the PowerSupplyDisconnectionReminder object containing the source data
     * @return a PowerSupplyDisconnectionReminderResponse object with the mapped data
     */
    public PowerSupplyDisconnectionReminderResponse mapToReminderResponse(PowerSupplyDisconnectionReminder reminder) {
        log.info("Mapping parameters for reminder for disconnection of power supply view with id %s;".formatted(reminder.getId()));

        PowerSupplyDisconnectionReminderResponse response = new PowerSupplyDisconnectionReminderResponse();
        response.setId(reminder.getId());
        response.setReminderNumber(reminder.getReminderNumber());
        response.setReminderStatus(reminder.getReminderStatus());
        response.setCustomerSendDate(reminder.getCustomerSendDate());
        response.setCreationDate(reminder.getCreateDate());
        response.setStatus(reminder.getStatus());
        response.setCommunicationChannels(reminder.getCommunicationChannels());
        response.setExcludedCustomerList(reminder.getExcludedCustomerList());
        response.setLiabilitiesMaxDueDate(reminder.getLiabilitiesMaxDueDate());
        response.setDisconnectionDate(reminder.getDisconnectionDate());
        response.setLiabilityAmountFrom(reminder.getLiabilityAmountFrom());
        response.setLiabilityAmountTo(reminder.getLiabilityAmountTo());
        response.setCurrencyResponse(reminder.getCurrencyId() != null ? getCurrencyResponse(reminder.getCurrencyId()) : null);

        response.setTasks(getTasks(reminder.getId()));

        return response;
    }

    /**
     * Maps the parameters from the given request object to update the specified reminder.
     * This method validates and sets various properties of the reminder based on the provided request.
     * If inconsistencies are detected between the request and the reminder, necessary validations
     * and updates are performed, and relevant errors are added to the errorMessages list.
     *
     * @param request       The {@code PowerSupplyDisconnectionReminderBaseRequest} object containing
     *                      the new parameters to update the reminder.
     * @param errorMessages A list to which error messages will be added if any validation issues occur
     *                      during the parameter mapping process.
     * @param reminder      The {@code PowerSupplyDisconnectionReminder} object to be updated
     *                      with the parameters from the request.
     */
    public void mapParametersForUpdate(PowerSupplyDisconnectionReminderBaseRequest request, List<String> errorMessages, PowerSupplyDisconnectionReminder reminder) {
        log.info("Mapping parameters for reminder for update with request %s;".formatted(request));
        reminder.setCustomerSendDate(request.getCustomerSendToDateAndTime());
        reminder.setLiabilitiesMaxDueDate(request.getLiabilitiesMaxDueDate());
        reminder.setLiabilityAmountFrom(request.getLiabilityAmountFrom());
        reminder.setLiabilityAmountTo(request.getLiabilityAmountTo());
        if (!Objects.equals(reminder.getCurrencyId(), request.getCurrencyId())) {
            checkAndSetCurrency(request.getCurrencyId(), reminder);
        }
        if (!Objects.equals(request.getExcludeCustomers(), reminder.getExcludedCustomerList())) {
            Set<String> excludedCustomerIds = parseExcludeCustomers(request.getExcludeCustomers());
            checkAndSetReminderCustomers(excludedCustomerIds, errorMessages);
        }
        reminder.setCurrencyId(request.getCurrencyId());
        reminder.setDisconnectionDate(request.getDisconnectionDate());
        reminder.setCommunicationChannels(CollectionUtils.isEmpty(request.getCommunicationChannels()) ? List.of(CommunicationChannel.EMAIL) : request.getCommunicationChannels());
        reminder.setExcludedCustomerList(request.getExcludeCustomers());
    }

    /**
     * Checks if the provided currency ID exists and is active. If valid, sets the currency ID to the provided reminder object.
     *
     * @param currencyId the ID of the currency to validate and set
     * @param reminder   the reminder object where the currency ID will be set if valid
     */
    private void checkAndSetCurrency(Long currencyId, PowerSupplyDisconnectionReminder reminder) {
        if (currencyId != null) {
            currencyRepository.findByIdAndStatus(currencyId, List.of(NomenclatureItemStatus.ACTIVE))
                    .orElseThrow(() -> new DomainEntityNotFoundException("Currency not found by ID %s;".formatted(currencyId)));
            reminder.setCurrencyId(currencyId);
        }
    }

    /**
     * Parses a comma-separated string of customer identifiers, trims each identifier,
     * and returns a set of unique, non-empty customer identifiers.
     *
     * @param excludeCustomers a comma-separated string containing customer identifiers to be excluded;
     *                         may be null or empty
     * @return a set of unique, non-empty customer identifiers; returns an empty set if the input
     * string is null or empty
     */
    private Set<String> parseExcludeCustomers(String excludeCustomers) {
        if (excludeCustomers == null || excludeCustomers.isEmpty()) {
            return Set.of();
        }

        return Arrays.stream(excludeCustomers.split(","))
                .map(String::trim)
                .filter(customerIdentifier -> !customerIdentifier.isEmpty())
                .collect(Collectors.toSet());
    }


    /**
     * Checks the provided customer identifiers against valid and active identifiers in the repository.
     * If any invalid or inactive customer identifiers are found, an error message is added to the provided error messages list.
     *
     * @param customerIdentifiers Set of customer identifiers to check for validity and activity status.
     * @param errorMessages       List to which error messages are appended if invalid or inactive customer identifiers are found.
     */
    private void checkAndSetReminderCustomers(Set<String> customerIdentifiers, List<String> errorMessages) {
        Set<String> validCustomerIdentifiers = customerRepository.findIdentifiersByIdentifierInAndStatusIn(customerIdentifiers);

        List<String> invalidCustomerIdentifiers = customerIdentifiers.stream()
                .filter(customerIdentifier -> !validCustomerIdentifiers.contains(customerIdentifier))
                .toList();

        if (!invalidCustomerIdentifiers.isEmpty()) {
            String errorMessage = "The following customer UICs are invalid or inactive: " + invalidCustomerIdentifiers;
            errorMessages.add(errorMessage);
        }
    }

    /**
     * Retrieves a ShortResponse containing currency details for the specified currency ID.
     *
     * @param currencyId the unique identifier of the currency to retrieve
     * @return a ShortResponse object containing the currency's ID and name
     * @throws DomainEntityNotFoundException if no currency is found with the specified ID
     */
    private ShortResponse getCurrencyResponse(Long currencyId) {
        Currency currency = currencyRepository.findById(currencyId)
                .orElseThrow(() -> new DomainEntityNotFoundException("Currency not found with given id: %s".formatted(currencyId)));
        return new ShortResponse(currency.getId(), currency.getName());
    }

    /**
     * Retrieves a list of tasks associated with a specific power supply disconnection reminder.
     *
     * @param id the unique identifier of the power supply disconnection reminder
     * @return a list of TaskShortResponse objects related to the specified reminder
     */
    public List<TaskShortResponse> getTasks(Long id) {
        return taskService.getTasksByPowerSupplyDisconnectionReminderId(id);
    }

}
