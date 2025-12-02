package phoenix.core.customer.model.request;

import lombok.Data;
import phoenix.core.customer.model.customAnotations.customer.MainSubjectActivityTranslatedValidator;
import phoenix.core.customer.model.customAnotations.customer.MainSubjectActivityValidator;
import phoenix.core.customer.model.customAnotations.customer.PrivateCustomerNameTranslatedValidator;
import phoenix.core.customer.model.customAnotations.customer.PrivateCustomerNameValidator;

import javax.validation.constraints.NotNull;

@Data
public class PrivateCustomerDetails {

    @NotNull(message = "GDPR Regulation Consent is required; ")
    private Boolean gdprRegulationConsent;

//    @NotNull(message = "Personal Number is required; ")
//    private String personalNumber;

    @NotNull(message = "First Name is required; ")
    @PrivateCustomerNameValidator(value = "First Name")
    private String firstName;

    @NotNull(message = "First Name Transl. is required; ")
    @PrivateCustomerNameTranslatedValidator
    private String firstNameTranslated;

    @PrivateCustomerNameValidator(value = "Middle Name")
    private String middleName;

    @PrivateCustomerNameTranslatedValidator
    private String middleNameTranslated;

    @NotNull(message = "Last Name is required; ")
    @PrivateCustomerNameValidator(value = "Last Name")
    private String lastName;

    @NotNull(message = "Last Name Transl. is required; ")
    @PrivateCustomerNameTranslatedValidator
    private String lastNameTranslated;

    @NotNull(message = "Business Activity Name is required; ")
    @MainSubjectActivityValidator(value = "Business Activity Name")
    private String businessActivityName;

    @NotNull(message = "Business Activity Name Transl. is required; ")
    @MainSubjectActivityTranslatedValidator
    private String businessActivityNameTranslated;

}
