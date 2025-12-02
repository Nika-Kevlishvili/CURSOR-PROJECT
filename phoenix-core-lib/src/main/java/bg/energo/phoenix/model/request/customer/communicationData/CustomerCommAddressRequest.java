package bg.energo.phoenix.model.request.customer.communicationData;

import bg.energo.phoenix.model.customAnotations.customer.communicationData.ValidCustomerCommAddress;
import bg.energo.phoenix.model.customAnotations.customer.communicationData.ValidCustomerCommAddressField;
import bg.energo.phoenix.model.response.customer.communicationData.detailed.CustomerAddress;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import java.util.Objects;

@Data
@ValidCustomerCommAddress
public class CustomerCommAddressRequest {

    @NotNull(message = "communicationData.address.foreign-Customer Address Request: Foreign is required;")
    private Boolean foreign;

    @Valid
    private CustomerCommForeignAddressData foreignAddressData;
    @Valid
    private CustomerCommLocalAddressData localAddressData;

    @ValidCustomerCommAddressField(value = "communicationData.address.number")
    @Length(min = 1, max = 32, message = "communicationData.address.number-Street number length must be between 1 and 32;")
    private String number;

    @ValidCustomerCommAddressField(value = "communicationData.address.additionalInformation")
    @Length(min = 1, max = 512, message = "communicationData.address.additionalInformation-Additional Info length must be between 1 and 512;")
    private String additionalInformation;

    @ValidCustomerCommAddressField(value = "communicationData.address.block")
    @Length(min = 1, max = 128, message = "communicationData.address.block-Block length must be between 1 and 128;")
    private String block;

    @ValidCustomerCommAddressField(value = "communicationData.address.entrance")
    @Length(min = 1, max = 32, message = "communicationData.address.entrance-Entrance length must be between 1 and 32;")
    private String entrance;

    @ValidCustomerCommAddressField(value = "communicationData.address.floor")
    @Length(min = 1, max = 16, message = "communicationData.address.floor-Floor length must be between 1 and 16;")
    private String floor;

    @ValidCustomerCommAddressField(value = "communicationData.address.apartment")
    @Length(min = 1, max = 32, message = "communicationData.address.apartment-Apartment length must be between 1 and 32;")
    private String apartment;

    @ValidCustomerCommAddressField(value = "communicationData.address.mailbox")
    @Length(min = 1, max = 32, message = "communicationData.address.mailbox-Mailbox length must be between 1 and 32;")
    private String mailbox;

    public boolean equalsResponse(CustomerAddress address) {
        if (!Objects.equals(this.foreign, address.getForeign())) return false;
        if (foreign.equals(Boolean.TRUE)) {
            if (!foreignAddressData.equalsResponse(address.getForeignAddressData())) return false;


        } else {
            if (!localAddressData.equalsResponse(address.getLocalAddressData())) return false;

        }
        if (!Objects.equals(number, address.getNumber())) return false;
        if (!Objects.equals(additionalInformation, address.getAdditionalInformation())) return false;
        if (!Objects.equals(block, address.getBlock())) return false;
        if (!Objects.equals(entrance, address.getEntrance())) return false;
        if (!Objects.equals(floor, address.getFloor())) return false;
        if (!Objects.equals(apartment, address.getApartment())) return false;
        return Objects.equals(mailbox, address.getMailbox());
    }
}
