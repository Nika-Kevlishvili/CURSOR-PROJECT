package bg.energo.migration.integration.phoenix.customer.model.request.privatecustomerdetails;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrivateCustomerDetails {
    private Boolean gdprRegulationConsent;
    private String firstName;
    private String firstNameTranslated;
    private String middleName;
    private String middleNameTranslated;
    private String lastName;
    private String lastNameTranslated;
}
