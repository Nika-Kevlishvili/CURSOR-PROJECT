package phoenix.core.customer.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import phoenix.core.customer.model.entity.customer.CustomerDetails;
import phoenix.core.customer.model.entity.customer.CustomerPreference;
import phoenix.core.customer.model.entity.nomenclature.customer.Preferences;
import phoenix.core.customer.model.enums.customer.Status;
import phoenix.core.customer.model.enums.nomenclature.NomenclatureItemStatus;
import phoenix.core.customer.model.request.CreateCustomerRequest;
import phoenix.core.customer.model.request.EditCustomerRequest;
import phoenix.core.customer.repository.customer.CustomerPreferenceRepository;
import phoenix.core.customer.repository.nomenclature.customer.PreferencesRepository;
import phoenix.core.exception.ClientException;
import phoenix.core.exception.DomainEntityNotFoundException;
import phoenix.core.exception.ErrorCode;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service("coreCustomerPreferenceService")
@RequiredArgsConstructor
@Validated
public class CustomerPreferenceFacade {

    private final PreferencesRepository preferencesRepository;
    private final CustomerPreferenceRepository customerPreferenceRepository;

    public void createCustomerPreference(CreateCustomerRequest request,
                                          CustomerDetails customerDetails,
                                          List<NomenclatureItemStatus> statuses,
                                          List<String> exceptionMessages){
        if(request.getBankingDetails().getPreferenceIds() != null) {
            List<CustomerPreference> customerPreferences = new ArrayList<>();
            for (Long preferencesId : request.getBankingDetails().getPreferenceIds()) {
                CustomerPreference customerPreference = new CustomerPreference();
                customerPreference.setCreateDate(LocalDateTime.now());
                customerPreference.setSystemUserId("bla");
                customerPreference.setStatus(Status.ACTIVE);//TODO: may be refactored in the future
                customerPreference.setPreferences(getPreferences(preferencesId, statuses, exceptionMessages));
                customerPreference.setCustomerDetail(customerDetails);
                customerPreferences.add(customerPreference);
            }
            if (exceptionMessages.isEmpty()) customerPreferenceRepository.saveAll(customerPreferences);
        }
    }

    private Preferences getPreferences(Long preferencesId,
                                       List<NomenclatureItemStatus> statuses,
                                       List<String> exceptionMessages){
        Optional<Preferences> optionalPreferences = preferencesRepository
                .findByIdAndStatus(preferencesId, statuses);
        if (optionalPreferences.isEmpty()) {
            if (preferencesId != null) {
                exceptionMessages.add("Preferences not found with id: " + preferencesId + "; ");
            }
            return null;
        } else {
            return  optionalPreferences.get();
        }
    }
    @Transactional
    public void editCustomerPreference(EditCustomerRequest request, CustomerDetails customerDetails, List<NomenclatureItemStatus> statuses, List<String> exceptionMessages) {
        List<CustomerPreference> customerPreferencesList = new ArrayList<>();
        List<Long> changedPreferencesList = getPreferencesList(request.getBankingDetails().getPreferenceIds(), customerDetails.getCustomerPreferences());
        if (customerDetails.getCustomerPreferences().size() == 0) {
            createCustomerPreference(new CreateCustomerRequest(request), customerDetails, List.of(NomenclatureItemStatus.ACTIVE), exceptionMessages);
        } else {

            for (int i = 0; i < changedPreferencesList.size(); i++) {
                Optional<CustomerPreference> dbPreferenceOptional =
                        customerPreferenceRepository
                                .findByIdAndCustomerDetailId(getPreferenceDetailsIdByPreferenceId(changedPreferencesList.get(i),
                                                customerDetails.getCustomerPreferences()),
                                        customerDetails.getId());
                if (dbPreferenceOptional.isPresent()) {
                    CustomerPreference customerPreference = dbPreferenceOptional.get();

                    Preferences preference = preferencesRepository.findById(customerPreference.getPreferences().getId()).
                            orElseThrow(() -> new DomainEntityNotFoundException("Preference not found"));
                    if (!preference.getStatus().equals(NomenclatureItemStatus.ACTIVE) ||
                            !customerPreference.getPreferences().getId().equals(preference.getId())) {
                        preference = preferencesRepository.findByIdAndStatus(customerPreference.getPreferences().getId(), statuses).
                                orElseThrow(() -> new DomainEntityNotFoundException("Preference not found"));
                    }
                    customerPreference.setPreferences(preference);
                    customerPreference.setCustomerDetail(customerDetails);
                    customerPreference.setModifyDate(LocalDateTime.now());
                    customerPreference.setModifySystemUserId("user");//TODO add SysUser
                    customerPreferencesList.add(customerPreference);
                } else {
                    throw new ClientException("Customer Preference with this id not found", ErrorCode.DOMAIN_ENTITY_NOT_FOUND);
                }
            }


            customerPreferenceRepository.saveAll(customerPreferencesList);
            List<Long> savePreferences = getNewPreferences(changedPreferencesList, request.getBankingDetails().getPreferenceIds());
            if (savePreferences.size() != 0) {
                request.getBankingDetails().setPreferenceIds(savePreferences);
                createCustomerPreference(new CreateCustomerRequest(request), customerDetails, List.of(NomenclatureItemStatus.ACTIVE), exceptionMessages);
            }
            if (changedPreferencesList.size() != 0) {
                customerPreferenceRepository.deleteAllByIdNotInAndCustomerDetailId(changedPreferencesList, customerDetails.getId());
            }
        }
    }

    private List<Long> getNewPreferences(List<Long> changedPreferencesList, List<Long> preferenceIds) {
        List<Long> saveList = new ArrayList<>();
        for (int i = 0; i < preferenceIds.size(); i++) {
            if (!changedPreferencesList.contains(preferenceIds.get(i))) {
                saveList.add(preferenceIds.get(i));
            }
        }
        return saveList;
    }

    private List<Long> getPreferencesList(List<Long> preferencesList, List<CustomerPreference> customerPreferences) {
        List<Long> returnList = new ArrayList<>();
        if (preferencesList == null) {
            return new ArrayList<>();
        }
        if (preferencesList.size() != 0) {
            for (int i = 0; i < preferencesList.size(); i++) {
                if (customerPreferences.size() != 0) {
                    for (int j = 0; j < customerPreferences.size(); j++) {
                        if (preferencesList.get(i).equals(customerPreferences.get(j).getPreferences().getId())) {
                            if (!returnList.contains(preferencesList.get(i))) {
                                returnList.add(preferencesList.get(i));
                            }
                        }
                    }
                } else return preferencesList;

            }
        }
        return returnList;
    }
    private Long getPreferenceDetailsIdByPreferenceId(Long preferenceId, List<CustomerPreference> customerPreferences) {
        Long id = null;
        for (int i = 0; i < customerPreferences.size(); i++) {
            Preferences preferences = customerPreferences.get(i).getPreferences();
            if (preferences.getId().equals(preferenceId))
                id = customerPreferences.get(i).getId();
        }
        if (id == null) {
            throw new ClientException("Preference Id is null", ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED);
        }
        return id;
    }
}
