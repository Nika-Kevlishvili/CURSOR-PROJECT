package bg.energo.migration.customer.mapper.businesscustomerdetails;

import bg.energo.migration.customer.entity.CustomerGeneralData;
import bg.energo.migration.integration.phoenix.customer.model.request.businesscustomerdetails.BusinessCustomerDetails;
import org.springframework.stereotype.Service;

import static bg.energo.migration.utils.CyrillicTransliteration.transliterateToLatin;

@Service
public class CustomerBusinessDetailsMapper {
    public BusinessCustomerDetails map(CustomerGeneralData customerGeneralData) {

        return BusinessCustomerDetails
                .builder()
                .name(customerGeneralData.getName())
                .nameTranslated(transliterateToLatin(customerGeneralData.getName()))
                .procurementLaw(customerGeneralData.getPublicProcurementLaw())
                .legalFormId(customerGeneralData.getLegalFormId())
                .legalFormTransId(customerGeneralData.getLegalFormTranslId())
                .build();
    }

}
