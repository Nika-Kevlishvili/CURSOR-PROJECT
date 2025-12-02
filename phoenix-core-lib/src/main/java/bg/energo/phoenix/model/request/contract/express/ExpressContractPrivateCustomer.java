package bg.energo.phoenix.model.request.contract.express;

import bg.energo.phoenix.model.request.customer.PrivateCustomerDetails;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.hibernate.validator.constraints.Length;
@Data
public class ExpressContractPrivateCustomer {
    @Length(max = 512, message = "privateCustomerDetails.firstName-Length must not be greater than 512;")
    @Pattern(regexp = "^[\\dA-ZА-Я–& \\-'‘\\s]*$", message = "privateCustomerDetails.firstName-Invalid Format or symbols;")
    @NotBlank(message = "privateCustomerDetails.firstName-Firstname must not be blank;")
    private String firstName;

    @Length(max = 512, message = "privateCustomerDetails.firstNameTranslated-Length must not be greater than 512;")
    @Pattern(regexp = "^[\\dA-Z–& \\-'‘\\s]*$", message = "privateCustomerDetails.firstNameTranslated-Invalid Format or symbols;")
    @NotBlank(message = "privateCustomerDetails.firstNameTranslated-First Name Translated must not be blank;")
    private String firstNameTranslated;

    @Length(max = 512, message = "privateCustomerDetails.middleName-Length must not be greater than 512;")
    @Pattern(regexp = "^[\\dA-ZА-Я–& \\-'‘\\s]*$", message = "privateCustomerDetails.middleName-Invalid Format or symbols;")
    private String middleName;

    @Length(max = 512, message = "privateCustomerDetails.middleNameTranslated-Length must not be greater than 512;")
    @Pattern(regexp = "^[\\dA-Z–& \\-'‘\\s]*$", message = "privateCustomerDetails.middleNameTranslated-Invalid Format or symbols;")
    private String middleNameTranslated;

    @Length(max = 512, message = "privateCustomerDetails.lastName-Length must not be greater than 512;")
    @Pattern(regexp = "^[\\dA-ZА-Я–& \\-'‘\\s]*$", message = "privateCustomerDetails.lastName-Invalid Format or symbols;")
    @NotBlank(message = "privateCustomerDetails.lastName-Lastname must not be blank;")
    private String lastName;

    @Length(max = 512, message = "privateCustomerDetails.lastNameTranslated-Length must not be greater than 512;")
    @Pattern(regexp = "^[\\dA-Z–& \\-'‘\\s]*$", message = "privateCustomerDetails.lastNameTranslated-Invalid Format or symbols;")
    @NotBlank(message = "privateCustomerDetails.lastNameTranslated-Lastname Translated must not be blank;")
    private String lastNameTranslated;

    public PrivateCustomerDetails toPrivateCustomerDetails(){
        PrivateCustomerDetails privateCustomerDetails = new PrivateCustomerDetails();
        privateCustomerDetails.setFirstName(this.firstName);
        privateCustomerDetails.setLastName(this.lastName);
        privateCustomerDetails.setMiddleName(this.middleName);
        privateCustomerDetails.setMiddleNameTranslated(this.middleNameTranslated);
        privateCustomerDetails.setLastNameTranslated(this.lastNameTranslated);
        privateCustomerDetails.setFirstNameTranslated(this.firstNameTranslated);
        return privateCustomerDetails;
    }
}
