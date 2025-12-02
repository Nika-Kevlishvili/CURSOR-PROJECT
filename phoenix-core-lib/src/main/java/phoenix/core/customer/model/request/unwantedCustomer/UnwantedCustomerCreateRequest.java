package phoenix.core.customer.model.request.unwantedCustomer;

import lombok.Data;
import phoenix.core.customer.model.customAnotations.UICDefaultValidator;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Data
public class UnwantedCustomerCreateRequest {
    @NotEmpty
    @Pattern(regexp = "^([0-9]{9}|[0-9]{10}|[0-9]{12}|[0-9]{13})$",
            message = "length must be 9,10,12 or 13 characters long")
    @UICDefaultValidator
    String identificationNumber;
    @Size(min = 1, max = 2048)
    @Pattern(regexp = "^[А-Яа-яA-Za-z0-9\\d–@#$&*()+-:.,‘€№=\\s]*$",
            message = "Format is invalid")
    String name;
    @NotNull
    Long unwantedCustomerReasonId;
    //@Size(min = 1, max = 2048)
    @Pattern(regexp = "^[А-Яа-яA-Za-z0-9\\d–@#$&*()_+-§?!\\/\\<>:.,‘€№=\\s]*$",
            message = "Format is invalid")
    String additionalInfo;
    @NotNull
    Boolean contractCreateRestriction;
    @NotNull
    Boolean orderCreateRestriction;
}
