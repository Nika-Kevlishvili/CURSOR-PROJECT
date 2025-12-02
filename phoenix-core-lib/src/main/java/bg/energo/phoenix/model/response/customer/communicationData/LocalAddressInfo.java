package bg.energo.phoenix.model.response.customer.communicationData;

import bg.energo.phoenix.model.enums.nomenclature.ResidentialAreaType;
import bg.energo.phoenix.model.enums.nomenclature.StreetType;
import bg.energo.phoenix.model.request.customer.LocalAddressData;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Objects;

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

    private ResidentialAreaType residentialAreaType;

    private Long streetId;

    private String streetName;

    private StreetType streetType;

    public boolean equalsRequest(LocalAddressData localAddressData) {
        if(!Objects.equals(this.countryId,localAddressData.getCountryId())) return false;
        if(!Objects.equals(this.regionId,localAddressData.getRegionId())) return false;
        if(!Objects.equals(this.municipalityId,localAddressData.getMunicipalityId())) return false;
        if(!Objects.equals(this.populatedPlaceId,localAddressData.getPopulatedPlaceId())) return false;
        if(!Objects.equals(this.zipCodeId,localAddressData.getZipCodeId())) return false;
        if(!Objects.equals(this.districtId,localAddressData.getDistrictId())) return false;
        if(!Objects.equals(this.residentialAreaId,localAddressData.getResidentialAreaId())) return false;
        if(!Objects.equals(this.residentialAreaType,localAddressData.getResidentialAreaType())) return false;
        if(!Objects.equals(this.streetId,localAddressData.getStreetId())) return false;
        return Objects.equals(this.streetType,localAddressData.getStreetType());
    }
}
