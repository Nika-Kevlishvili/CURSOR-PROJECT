package bg.energo.migration.integration.phoenix.customer.model.request.communicationdata.commaddress;

import bg.energo.migration.integration.phoenix.customer.model.enums.ResidentialAreaType;
import bg.energo.migration.integration.phoenix.customer.model.enums.StreetType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerCommLocalAddressData {
    private Long countryId;
    private Long regionId;
    private Long municipalityId;
    private Long populatedPlaceId;
    private Long zipCodeId;
    private Long districtId;//empty
    private Long residentialAreaId;//empty
    private Long streetId;
    private StreetType streetType;
    private ResidentialAreaType residentialAreaType;//empty
}
