package bg.energo.migration.customer.mapper.privatecustomerdetails;

import bg.energo.migration.customer.entity.CustomerGeneralData;
import bg.energo.migration.integration.phoenix.customer.model.request.privatecustomerdetails.PrivateCustomerDetails;
import org.springframework.stereotype.Service;

import static bg.energo.migration.utils.CyrillicTransliteration.transliterateToLatin;

@Service
public class PrivateCustomerDetailsMapper {
    public PrivateCustomerDetails map(CustomerGeneralData customerGeneralData) {
        return PrivateCustomerDetails
                .builder()
                .gdprRegulationConsent(customerGeneralData.getGdprRegulationConsent())
                .firstName(customerGeneralData.getName())
                .middleName(customerGeneralData.getCustomerMiddleName())
                .lastName(customerGeneralData.getCustomerLastName())
                .firstNameTranslated(transliterateToLatin(customerGeneralData.getName()))
                .middleNameTranslated(transliterateToLatin(customerGeneralData.getCustomerMiddleName()))
                .lastNameTranslated(transliterateToLatin(customerGeneralData.getCustomerLastName()))
                .build();
    }
}
