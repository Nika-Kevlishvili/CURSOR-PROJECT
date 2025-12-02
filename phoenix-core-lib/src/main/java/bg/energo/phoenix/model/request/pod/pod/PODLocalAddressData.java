package bg.energo.phoenix.model.request.pod.pod;

import bg.energo.phoenix.model.enums.nomenclature.ResidentialAreaType;
import bg.energo.phoenix.model.enums.nomenclature.StreetType;
import lombok.Data;

@Data
public class PODLocalAddressData {
    private Long countryId;

    private Long regionId;
    private Long municipalityId;
    private Long populatedPlaceId;

    private Long zipCodeId;

    private Long districtId;

    private Long residentialAreaId;

    private ResidentialAreaType residentialAreaType;

    private Long streetId;

    private StreetType streetType;
}
