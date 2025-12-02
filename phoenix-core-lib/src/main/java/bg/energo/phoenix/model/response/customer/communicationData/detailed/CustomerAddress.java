package bg.energo.phoenix.model.response.customer.communicationData.detailed;

import bg.energo.phoenix.model.request.customer.CustomerAddressRequest;
import bg.energo.phoenix.model.response.customer.communicationData.ForeignAddressInfo;
import bg.energo.phoenix.model.response.customer.communicationData.LocalAddressInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerAddress {
    private Boolean foreign;
    private ForeignAddressInfo foreignAddressData;
    private LocalAddressInfo localAddressData;
    private String number;
    private String additionalInformation;
    private String block;
    private String entrance;
    private String floor;
    private String apartment;
    private String mailbox;

    public boolean equalsRequest(CustomerAddressRequest addressRequest){
        if(!Objects.equals(this.number,addressRequest.getNumber())) return false;
        if(!Objects.equals(this.additionalInformation,addressRequest.getAdditionalInformation())) return false;
        if(!Objects.equals(this.block,addressRequest.getBlock())) return false;
        if(!Objects.equals(this.entrance,addressRequest.getEntrance())) return false;
        if(!Objects.equals(this.floor,addressRequest.getFloor())) return false;
        if(!Objects.equals(this.apartment,addressRequest.getApartment())) return false;
        if(!Objects.equals(this.mailbox,addressRequest.getMailbox())) return false;
        if(!Objects.equals(this.foreign,addressRequest.getForeign())) return false;

        if (Boolean.TRUE.equals(addressRequest.getForeign())){
           return foreignAddressData.equalsRequest(addressRequest.getForeignAddressData());
        }else {
           return localAddressData.equalsRequest(addressRequest.getLocalAddressData());
        }

    }
}
