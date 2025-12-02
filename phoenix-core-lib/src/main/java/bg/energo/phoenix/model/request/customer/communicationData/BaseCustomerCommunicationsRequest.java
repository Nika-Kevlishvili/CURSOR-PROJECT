package bg.energo.phoenix.model.request.customer.communicationData;

import bg.energo.phoenix.model.enums.customer.Status;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BaseCustomerCommunicationsRequest {

    @Pattern(regexp = "^[А-Яа-яA-Za-z\\d-@#$&*()+\\-:.,'‘€№= ѝЍ]*$", message = "communicationData.contactTypeName-Contact Type Name invalid pattern;")
    @NotEmpty(message = "communicationData.contactTypeName-communication data request: contact name must not be empty;")
    private String contactTypeName;

    @Valid
    @NotNull(message = "communicationData.address-Communication Request Customer address is required;")
    private CustomerCommAddressRequest address;

    @NotNull(message = "communicationData.status-communication data request: status must not be null;")
    private Status status;

}
