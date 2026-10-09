package bg.energo.migration.integration.phoenix.customer.model.request.address.foreignaddressdata;

import bg.energo.migration.integration.phoenix.customer.model.enums.ResidentialAreaType;
import bg.energo.migration.integration.phoenix.customer.model.enums.StreetType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ForeignAddressData {
    private Long countryId;
    private String region;
    private String municipality;
    private String populatedPlace;
    private String zipCode;
    private String district;
    private ResidentialAreaType residentialAreaType;
    private String residentialArea;
    private StreetType streetType;
    private String street;
}
