package bg.energo.phoenix.model.request.contract.express;

import bg.energo.phoenix.model.customAnotations.customer.CustomerNameTransliteratedValidator;
import bg.energo.phoenix.model.customAnotations.customer.CustomerNameValidator;
import bg.energo.phoenix.model.request.customer.BusinessCustomerDetails;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

@Data
public class ExpressContractBusinessCustomer {
    @Length(min = 1, max = 2048, message = "businessCustomerDetails.name-Name length must be between 1 and 2048;")
    @CustomerNameValidator
    @NotBlank(message = "businessCustomerDetails.name-Name must not be blank;")
    private String name;
    @Length(min = 1, max = 2048, message = "businessCustomerDetails.nameTranslated-Name Translated length must be between 1 and 2048;")
    @CustomerNameTransliteratedValidator
    @NotBlank(message = "businessCustomerDetails.nameTranslated-Name Translated must not be blank;")
    private String nameTranslated;
//    @NotNull(message = "businessCustomerDetails.procurementLaw-Procurement Law is required;")
    private Boolean procurementLaw;
    private Long legalFormId;
    private Long legalFormTransId;

    public BusinessCustomerDetails toBusinessCustomer(){
        BusinessCustomerDetails businessCustomerDetails = new BusinessCustomerDetails();
        businessCustomerDetails.setLegalFormTransId(this.legalFormTransId);
        businessCustomerDetails.setLegalFormId(this.legalFormId);
        businessCustomerDetails.setName(this.name);
        businessCustomerDetails.setNameTranslated(this.nameTranslated);
        businessCustomerDetails.setProcurementLaw(this.procurementLaw);
        return businessCustomerDetails;
    }
}
