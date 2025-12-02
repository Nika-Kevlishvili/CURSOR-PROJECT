package bg.energo.phoenix.model.request.customer.communicationData;

import bg.energo.phoenix.model.customAnotations.customer.communicationData.ValidCustomerCommAddressField;
import bg.energo.phoenix.model.enums.nomenclature.ResidentialAreaType;
import bg.energo.phoenix.model.enums.nomenclature.StreetType;
import bg.energo.phoenix.model.response.customer.communicationData.ForeignAddressInfo;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import java.util.Objects;

@Data
public class CustomerCommForeignAddressData {

    private Long countryId;

    @ValidCustomerCommAddressField(value = "communicationData.address.foreignAddressData.region")
    @Length(min = 1, max = 512, message = "communicationData.address.foreignAddressData.region-Region length must be between 1 and 512;")
    private String region;

    @ValidCustomerCommAddressField(value = "communicationData.address.foreignAddressData.municipality")
    @Length(min = 1, max = 512, message = "communicationData.address.foreignAddressData.municipality-Municipality length must be between 1 and 512;")
    private String municipality;

    @ValidCustomerCommAddressField(value = "communicationData.address.foreignAddressData.populatedPlace")
    @Length(min = 1, max = 512, message = "communicationData.address.foreignAddressData.populatedPlace-Populated Place length must be between 1 and 512;")
    private String populatedPlace;

    @ValidCustomerCommAddressField(value = "communicationData.address.foreignAddressData.zipCode")
    @Length(min = 1, max = 32, message = "communicationData.address.foreignAddressData.zipCode-ZipCode length must be between 1 and 32;")
    private String zipCode;

    @ValidCustomerCommAddressField(value = "communicationData.address.foreignAddressData.district")
    @Length(min = 1, max = 512, message = "communicationData.address.foreignAddressData.district-District length must be between 1 and 512;")
    private String district;

    private ResidentialAreaType residentialAreaType;

    @ValidCustomerCommAddressField(value = "communicationData.address.foreignAddressData.residentialArea")
    @Length(max = 1024, message = "communicationData.address.foreignAddressData.residentialArea-length must be max 1024;")
    private String residentialArea;
    private StreetType streetType;

    @ValidCustomerCommAddressField(value = "communicationData.address.foreignAddressData.street")
    @Length(max = 1024, message = "communicationData.address.foreignAddressData.street-length must be max 1024;")
    private String street;

    public boolean equalsResponse(ForeignAddressInfo foreignAddressData) {
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
