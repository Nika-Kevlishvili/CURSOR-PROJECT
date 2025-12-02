package phoenix.core.customer.model.request;

import lombok.Data;
import org.hibernate.validator.constraints.Length;
import phoenix.core.customer.model.customAnotations.customer.manager.AddressFieldValidator;
import phoenix.core.customer.model.customAnotations.customer.withValidators.CustomerAddressValidator;

import javax.validation.constraints.NotNull;

@Data
@CustomerAddressValidator
public class CustomerAddressRequest {

    @NotNull(message = "Customer Address Request - Foreign is required; ")
    private Boolean foreign;

    private ForeignAddressData foreignAddressData;

    private LocalAddressData localAddressData;

    //TODO: check in annotation if field is null
    @AddressFieldValidator
    @Length(min = 1, max = 32)
    private String number;

    @AddressFieldValidator
    @Length(min = 1, max = 512)
    private String additionalInformation;

    @AddressFieldValidator
    @Length(min = 1, max = 128)
    private String block;

    @AddressFieldValidator
    @Length(min = 1, max = 32)
    private String entrance;

    @AddressFieldValidator
    @Length(min = 1, max = 16)
    private String floor;

    @AddressFieldValidator
    @Length(min = 1, max = 32)
    private String apartment;

    @AddressFieldValidator
    @Length(min = 1, max = 32)
    private String mailbox;
}
