package bg.energo.phoenix.model.request.customer;


import bg.energo.phoenix.model.enums.nomenclature.ResidentialAreaType;
import bg.energo.phoenix.model.enums.nomenclature.StreetType;
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

    private StreetType streetType;
    private ResidentialAreaType residentialAreaType;

}
