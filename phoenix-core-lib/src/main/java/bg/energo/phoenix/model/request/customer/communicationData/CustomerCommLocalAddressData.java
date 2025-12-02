package bg.energo.phoenix.model.request.customer.communicationData;

import bg.energo.phoenix.model.enums.nomenclature.ResidentialAreaType;
import bg.energo.phoenix.model.enums.nomenclature.StreetType;
import bg.energo.phoenix.model.response.customer.communicationData.LocalAddressInfo;
import lombok.Data;

import java.util.Objects;

@Data
public class CustomerCommLocalAddressData {

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

    public boolean equalsResponse(LocalAddressInfo localAddressData) {
        if(!Objects.equals(this.countryId,localAddressData.getCountryId())) return false;
        if(!Objects.equals(this.regionId,localAddressData.getRegionId())) return false;
        if(!Objects.equals(this.municipalityId,localAddressData.getMunicipalityId())) return false;
        if(!Objects.equals(this.populatedPlaceId,localAddressData.getPopulatedPlaceId())) return false;
        if(!Objects.equals(this.zipCodeId,localAddressData.getZipCodeId())) return false;
        if(!Objects.equals(this.districtId,localAddressData.getDistrictId())) return false;
        if(!Objects.equals(this.residentialAreaId,localAddressData.getResidentialAreaId())) return false;
        if(!Objects.equals(this.streetId,localAddressData.getStreetId())) return false;
        if(!Objects.equals(this.streetType,localAddressData.getStreetType())) return false;
        return Objects.equals(this.residentialAreaType,localAddressData.getResidentialAreaType());
    }
}
