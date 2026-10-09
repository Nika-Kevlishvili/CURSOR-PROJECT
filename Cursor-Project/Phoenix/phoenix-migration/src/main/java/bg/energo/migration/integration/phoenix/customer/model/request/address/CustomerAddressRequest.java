package bg.energo.migration.integration.phoenix.customer.model.request.address;

import bg.energo.migration.integration.phoenix.customer.model.request.address.foreignaddressdata.ForeignAddressData;
import bg.energo.migration.integration.phoenix.customer.model.request.address.localaddressdata.LocalAddressData;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerAddressRequest {
    private Boolean foreign;
    private ForeignAddressData foreignAddressData;//EMPTY
    private LocalAddressData localAddressData;
    private String number;
    private String additionalInformation;//empty
    private String block;
    private String entrance;//empty
    private String floor;//empty
    private String apartment;
    private String mailbox;//empty
}
