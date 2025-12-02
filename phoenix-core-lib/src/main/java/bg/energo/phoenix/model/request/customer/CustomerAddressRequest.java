package bg.energo.phoenix.model.request.customer;

import bg.energo.phoenix.model.customAnotations.customer.manager.AddressFieldValidator;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

@Data
public class CustomerAddressRequest {

    @NotNull(message = "address.foreign-Customer Address Request: Foreign is required;")
    private Boolean foreign;

    @Valid
    private ForeignAddressData foreignAddressData;

    @Valid
    private LocalAddressData localAddressData;

    @AddressFieldValidator(value = "address.number")
    @Length(min = 1, max = 32, message = "address.number-Street number length must be between 1 and 32;")
    private String number;

    @AddressFieldValidator(value = "address.additionalInformation")
    @Length(min = 1, max = 512, message = "address.additionalInformation-Additional Info length must be between 1 and 512;")
    private String additionalInformation;

    @AddressFieldValidator(value = "address.block")
    @Length(min = 1, max = 128, message = "address.block-Block length must be between 1 and 128;")
    private String block;

    @AddressFieldValidator(value = "address.entrance")
    @Length(min = 1, max = 32, message = "address.entrance-Entrance length must be between 1 and 32;")
    private String entrance;

    @AddressFieldValidator(value = "address.floor")
    @Length(min = 1, max = 16, message = "address.floor-Floor length must be between 1 and 16;")
    private String floor;

    @AddressFieldValidator(value = "address.apartment")
    @Length(min = 1, max = 32, message = "address.apartment-Apartment length must be between 1 and 32;")
    private String apartment;

    @AddressFieldValidator(value = "address.mailbox")
    @Length(min = 1, max = 32, message = "address.mailbox-Mailbox length must be between 1 and 32;")
    private String mailbox;
}
