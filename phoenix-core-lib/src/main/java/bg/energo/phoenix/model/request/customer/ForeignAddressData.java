package bg.energo.phoenix.model.request.customer;

import bg.energo.phoenix.model.customAnotations.customer.manager.AddressFieldValidator;
import bg.energo.phoenix.model.enums.nomenclature.ResidentialAreaType;
import bg.energo.phoenix.model.enums.nomenclature.StreetType;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

@Data
public class ForeignAddressData {

    private Long countryId;

    @AddressFieldValidator(value = "address.foreignAddressData.region")
    @Length(min = 1, max = 512, message = "address.foreignAddressData.region-Region length must be between 1 and 512;")
    private String region;

    @AddressFieldValidator(value = "address.foreignAddressData.municipality")
    @Length(min = 1, max = 512, message = "address.foreignAddressData.municipality-Municipality length must be between 1 and 512;")
    private String municipality;

    @AddressFieldValidator(value = "address.foreignAddressData.populatedPlace")
    @Length(min = 1, max = 512, message = "address.foreignAddressData.populatedPlace-Populated Place length must be between 1 and 512;")
    private String populatedPlace;

    @AddressFieldValidator(value = "address.foreignAddressData.zipCode")
    @Length(min = 1, max = 32, message = "address.foreignAddressData.zipCode-ZipCode length must be between 1 and 32;")
    private String zipCode;

    @AddressFieldValidator(value = "address.foreignAddressData.district")
    @Length(min = 1, max = 512, message = "address.foreignAddressData.district-District length must be between 1 and 512;")
    private String district;

    private ResidentialAreaType residentialAreaType;

    @AddressFieldValidator(value = "address.foreignAddressData.residentialArea")
    @Length(max = 1024, message = "address.foreignAddressData.residentialArea-length must be max 1024;")
    private String residentialArea;

    private StreetType streetType;

    @AddressFieldValidator(value = "address.foreignAddressData.street")
    @Length(max = 1024, message = "address.foreignAddressData.street-length must be max 1024;")
    private String street;

}
