package phoenix.core.customer.model.response.customer;

import lombok.Data;
import phoenix.core.customer.model.enums.customer.unwantedCustomer.UnwantedCustomerStatus;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Data
public class EditCustomerCreateRequest {
    @NotNull
    Long id;
    @Size(min = 1, max = 2048)
    @NotNull
    @Pattern(regexp = "^[А-Яа-яA-Za-z0-9\\d–@#$&*()+-:.,‘€№=\\s]*$",
            message = "Format is invalid")
    String name;
    @NotNull
    Long unwantedCustomerReasonId;
    @Size(min = 1, max = 2048)
    @Pattern(regexp = "^[А-Яа-яA-Za-z0-9\\d–@#$&*()_+-§?!\\/\\<>:.,‘€№=\\s]*$",
            message = "Format is invalid")
    @NotNull
    String additionalInfo;
    @NotNull
    Boolean contractCreateRestriction;
    @NotNull
    Boolean orderCreateRestriction;
    @NotNull
    UnwantedCustomerStatus unwantedCustomerStatus;
}
