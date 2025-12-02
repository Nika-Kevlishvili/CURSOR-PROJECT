package bg.energo.phoenix.model.response.customer.communicationData;

import bg.energo.phoenix.model.enums.nomenclature.ResidentialAreaType;
import bg.energo.phoenix.model.enums.nomenclature.StreetType;
import bg.energo.phoenix.model.request.customer.ForeignAddressData;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ForeignAddressInfo {

    private Long countryId;

    private String countryName;

    private String region;

    private String municipality;

    private String populatedPlace;

    private String zipCode;

    private String district;

    private ResidentialAreaType residentialAreaType;

    private String residentialArea;

    private StreetType streetType;

    private String street;

    public boolean equalsRequest(ForeignAddressData foreignAddressData) {
        if(!Objects.equals(this.countryId,foreignAddressData.getCountryId())) return false;
        if(!Objects.equals(this.region,foreignAddressData.getRegion())) return false;
        if(!Objects.equals(this.municipality,foreignAddressData.getMunicipality())) return false;
        if(!Objects.equals(this.populatedPlace,foreignAddressData.getPopulatedPlace())) return false;
        if(!Objects.equals(this.zipCode,foreignAddressData.getZipCode())) return false;
        if(!Objects.equals(this.district,foreignAddressData.getDistrict())) return false;
        if(!Objects.equals(this.residentialAreaType,foreignAddressData.getResidentialAreaType())) return false;
        if(!Objects.equals(this.residentialArea,foreignAddressData.getResidentialArea())) return false;
        if(!Objects.equals(this.streetType,foreignAddressData.getStreetType())) return false;
        return Objects.equals(this.street,foreignAddressData.getStreet());
    }
}
