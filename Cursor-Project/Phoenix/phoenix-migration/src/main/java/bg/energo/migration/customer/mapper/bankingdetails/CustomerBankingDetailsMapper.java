package bg.energo.migration.customer.mapper.bankingdetails;

import bg.energo.migration.customer.entity.CustomerGeneralData;
import bg.energo.migration.integration.phoenix.customer.model.request.bankingdetails.CustomerBankingDetails;
import org.springframework.stereotype.Service;

@Service
public class CustomerBankingDetailsMapper {
    public CustomerBankingDetails map(CustomerGeneralData customerGeneralData) {

        return CustomerBankingDetails
                .builder()
                .directDebit(customerGeneralData.getDirectDebit())
                .bankId(customerGeneralData.getBankId())
                .bic(customerGeneralData.getBicCode())
                .iban(customerGeneralData.getIban())
                .build();
    }
}
