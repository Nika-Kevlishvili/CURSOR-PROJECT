package bg.energo.migration.integration.phoenix.customer.model.request.businesscustomerdetails;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusinessCustomerDetails {
    private Boolean procurementLaw;
    private String name;
    private String nameTranslated;
    private Long legalFormId;
    private Long legalFormTransId;
}
