package bg.energo.migration.integration.phoenix.customer.model.request.communicationdata.commaddress;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerCommAddressRequest {
    private Boolean foreign;
    private CustomerCommForeignAddressData foreignAddressData;//empty
    private CustomerCommLocalAddressData localAddressData;
    private String number;
    private String additionalInformation;//empty
    private String block;
    private String entrance;//empty
    private String floor;//empty
    private String apartment;
    private String mailbox;//empty
}
