package phoenix.core.customer.model.request;

import lombok.Data;
import org.hibernate.validator.constraints.Length;
import phoenix.core.customer.model.customAnotations.customer.CustomerNameTransliteratedValidator;
import phoenix.core.customer.model.customAnotations.customer.CustomerNameValidator;

import javax.validation.constraints.NotNull;

@Data
public class BusinessCustomerDetails {
    @NotNull(message = "Procurement Law is required; ")
    private Boolean procurementLaw;
    @Length(min = 1, max = 2048)
    @CustomerNameValidator
    private String name;
    @Length(min = 1, max = 2048)
    @CustomerNameTransliteratedValidator
    private String nameTranslated;

//    @UICDefaultValidator
//    private String uic;//TODO: Anotation will be added after merge
    @NotNull(message = "Legal Form ID is required; ")
    private Long legalFormId;
    @NotNull(message = "Legal Form Transl. ID is required; ")
    private Long legalFormTransId;

}
