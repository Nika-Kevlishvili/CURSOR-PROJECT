package phoenix.core.customer.model.response.customer.communicationData;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LocalAddressInfo {

    private Long countryId;

    private String countryName;

    private Long regionId;

    private String regionName;

    private Long municipalityId;

    private String municipalityName;

    private Long populatedPlaceId;

    private String populatedPlaceName;

    private Long zipCodeId;

    private String zipCodeName;

    private Long districtId;

    private String districtName;

    private Long residentialAreaId;

    private String residentialAreaName;

    private Long streetId;

    private String streetName;

}
