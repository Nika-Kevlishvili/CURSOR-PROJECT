package phoenix.core.customer.model.request;

import lombok.Data;

@Data
public class LocalAddressData {

    private Long countryId;
    private Long regionId;
    private Long municipalityId;
    private Long populatedPlaceId;

    private Long zipCodeId;

    private Long districtId;

    private Long residentialAreaId;

    private Long streetId;

}
