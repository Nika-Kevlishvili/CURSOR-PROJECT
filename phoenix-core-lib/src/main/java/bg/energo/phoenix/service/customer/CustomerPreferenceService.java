package bg.energo.phoenix.service.customer;

import bg.energo.phoenix.model.entity.customer.CustomerDetails;
import bg.energo.phoenix.model.entity.customer.CustomerPreference;
import bg.energo.phoenix.model.entity.nomenclature.customer.Preferences;
import bg.energo.phoenix.model.enums.customer.Status;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.request.customer.CreateCustomerRequest;
import bg.energo.phoenix.model.request.customer.EditCustomerRequest;
import bg.energo.phoenix.repository.customer.CustomerPreferenceRepository;
import bg.energo.phoenix.repository.nomenclature.customer.PreferencesRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class CustomerPreferenceService {

    private final PreferencesRepository preferencesRepository;
    private final CustomerPreferenceRepository customerPreferenceRepository;

    /**
     * <h1>Create Customer Preference</h1>
     * Function validates if preference ids is not null or empty , then creates CustomerPreference object and saves it in db
     *
     * @param request           {@link CreateCustomerRequest}
     * @param customerDetails   {@link CustomerDetails}
     * @param statuses          {@link NomenclatureItemStatus}
     * @param exceptionMessages list of exception messages that will be populated if there is any exception
     */
    public void createCustomerPreference(CreateCustomerRequest request,
                                         CustomerDetails customerDetails,
                                         List<NomenclatureItemStatus> statuses,
                                         List<String> exceptionMessages) {
        if (request.getBankingDetails() != null) {
            if (request.getBankingDetails().getPreferenceIds() != null) {
                List<CustomerPreference> customerPreferences = new ArrayList<>();
                for (Long preferencesId : request.getBankingDetails().getPreferenceIds()) {
                    Preferences preferences = getPreferences(preferencesId, statuses, exceptionMessages);

                    if (preferences != null) {
                        CustomerPreference customerPreference = new CustomerPreference();
                        customerPreference.setStatus(Status.ACTIVE);
                        customerPreference.setPreferences(preferences);
                        customerPreference.setCustomerDetail(customerDetails);
                        customerPreferences.add(customerPreference);
                    }
                }

                if (exceptionMessages.isEmpty()) {
                    customerPreferenceRepository.saveAll(customerPreferences);
                }
            }
        }
    }

    /**
     * <h1>createAndGetCustomerPreference</h1>
     * if preferenceIds from request is not null function will create new CustomerPreference object
     * and save it in database
     *
     * @param preferenceIds     list of preference ids
     * @param customerDetails   {@link CustomerDetails}
     * @param statuses          {@link NomenclatureItemStatus}
     * @param exceptionMessages list of exception messages that will be populated if there is any exception
     * @return list of save CustomerPreference objects
     */
    public List<CustomerPreference> createAndGetCustomerPreference(List<Long> preferenceIds,
                                                                   CustomerDetails customerDetails,
                                                                   CustomerDetails oldCustomerDetails,
                                                                   List<String> exceptionMessages
    ) {
        if (CollectionUtils.isNotEmpty(preferenceIds)) {
            List<CustomerPreference> customerPreferences = new ArrayList<>();
            List<Long> oldPreferences = oldCustomerDetails.getCustomerPreferences().stream().map(x -> x.getPreferences().getId()).toList();
            for (Long preferencesId : preferenceIds) {
                CustomerPreference customerPreference = new CustomerPreference();
                customerPreference.setStatus(Status.ACTIVE); //TODO: may be refactored in the future
                customerPreference.setPreferences(getPreferences(preferencesId, getPreferenceStatus(oldPreferences, preferencesId), exceptionMessages));
                customerPreference.setCustomerDetail(customerDetails);
                customerPreferences.add(customerPreference);
            }
            if (exceptionMessages.isEmpty()) {
                return customerPreferenceRepository.saveAll(customerPreferences);
            }
        }
        return null;
    }

    private List<NomenclatureItemStatus> getPreferenceStatus(List<Long> oldPreferences, Long id) {
        if (oldPreferences.contains(id)) {
            return List.of(NomenclatureItemStatus.ACTIVE, NomenclatureItemStatus.INACTIVE);
        }
        return List.of(NomenclatureItemStatus.ACTIVE);
    }

    /**
     * <h1>Get Preferences</h1>
     * function gets preference from table based on preference db id and status and returns it
     *
     * @param preferencesId     preference db id
     * @param statuses          {@link NomenclatureItemStatus}
     * @param exceptionMessages list of exception messages that will be populated if there is any exception
     * @return Preferences object
     */
    private Preferences getPreferences(Long preferencesId,
                                       List<NomenclatureItemStatus> statuses,
                                       List<String> exceptionMessages) {
        Optional<Preferences> optionalPreferences = preferencesRepository
                .findByIdAndStatus(preferencesId, statuses);
        if (optionalPreferences.isEmpty()) {
            if (preferencesId != null) {
                log.error("bankingDetails.preferenceIds-Preferences not found with id: " + preferencesId + ";");
                exceptionMessages.add("bankingDetails.preferenceIds-Preferences not found with id: " + preferencesId + ";");
            }
            return null;
        } else {
            return optionalPreferences.get();
        }
    }

    /**
     * <h1>Edit Customer Preference</h1>
     * function will create new one or update customer preferences list attached to the customer
     *
     * @param request           {@link EditCustomerRequest}
     * @param customerDetails   {@link CustomerDetails}
     * @param statuses          {@link NomenclatureItemStatus}
     * @param exceptionMessages list of exception messages that will be populated if there is any exception
     * @return list of customer preference object
     */
    @Transactional
    public List<CustomerPreference> editCustomerPreference(EditCustomerRequest request, CustomerDetails customerDetails, List<NomenclatureItemStatus> statuses, List<String> exceptionMessages) {
        if (request.getBankingDetails() == null || request.getBankingDetails().getPreferenceIds() == null) {
            customerPreferenceRepository.deleteAll(customerDetails.getCustomerPreferences());
        } else {
            List<Long> editedCustomerPreference = new ArrayList<>();
            List<CustomerPreference> newCustomerPreference = new ArrayList<>();
            for (long item : request.getBankingDetails().getPreferenceIds()) {
                Optional<Preferences> preferencesOptional = preferencesRepository.findById(item);
                if (preferencesOptional.isEmpty()) {
                    log.error("bankingDetails.preferenceIds-Preference not found;");
                    exceptionMessages.add("bankingDetails.preferenceIds-Preference not found;");
                    return null;
                }

                Preferences preferences = preferencesOptional.get();

                Optional<CustomerPreference> dbPreferenceOptional = customerPreferenceRepository.findByPreferencesIdAndCustomerDetailId(item, customerDetails.getId());
                if (dbPreferenceOptional.isPresent()) {
                    editedCustomerPreference.add(dbPreferenceOptional.get().getId());
                } else {
                    if (!preferences.getStatus().equals(NomenclatureItemStatus.ACTIVE)) {
                        log.error("bankingDetails.preferenceIds-Preference %s is not ACTIVE;".formatted(item));
                        exceptionMessages.add("bankingDetails.preferenceIds-Preference %s is not ACTIVE;".formatted(item));
                        return null;
                    }
                    CustomerPreference customerPreference = new CustomerPreference();
                    customerPreference.setStatus(Status.ACTIVE);
                    customerPreference.setPreferences(preferences);
                    customerPreference.setCustomerDetail(customerDetails);
                    newCustomerPreference.add(customerPreference);
                }
            }
            for (CustomerPreference customerPreference : customerDetails.getCustomerPreferences()) {
                if (!editedCustomerPreference.contains(customerPreference.getId())) {
                    customerPreferenceRepository.delete(customerPreference);
                }
            }
            customerPreferenceRepository.saveAll(newCustomerPreference);
        }
        return customerPreferenceRepository.findAllByCustomerDetailId(customerDetails.getId());
    }

    public List<CustomerPreference> findCustomerPreferencesForCustomer(Long customerDetailsId) {
        return customerPreferenceRepository.findAllByCustomerDetailIdAndStatusIn(customerDetailsId, List.of(Status.ACTIVE));
    }
}
