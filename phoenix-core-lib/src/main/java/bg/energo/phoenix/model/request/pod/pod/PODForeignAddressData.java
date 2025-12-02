package bg.energo.phoenix.model.request.pod.pod;

import bg.energo.phoenix.model.customAnotations.customer.manager.AddressFieldValidator;
import bg.energo.phoenix.model.enums.nomenclature.ResidentialAreaType;
import bg.energo.phoenix.model.enums.nomenclature.StreetType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

@Data
public class PODForeignAddressData {

    private Long countryId;
    @AddressFieldValidator(value = "addressRequest.foreignAddressData.region")
    @Length(min = 1, max = 512, message = "addressRequest.foreignAddressData.region-region length must be between {min} and {max};")
    @NotNull(message = "addressRequest.foreignAddressData.region-region can not be null;")
    private String region;
    @AddressFieldValidator(value = "addressRequest.foreignAddressData.municipality")
    @Length(min = 1, max = 512, message = "addressRequest.foreignAddressData.municipality-municipality length must be between {min} and {max};")
    @NotNull(message = "addressRequest.foreignAddressData.municipality- municipality can not be null;")
    private String municipality;
    @AddressFieldValidator(value = "addressRequest.foreignAddressData.populatedPlace")
    @Length(min = 1, max = 512, message = "addressRequest.foreignAddressData.populatedPlace-populatedPlace length must be between {min} and {max};")
    @NotNull(message = "addressRequest.foreignAddressData.populatedPlace-populatedPlace can not be null;")
    private String populatedPlace;
    @AddressFieldValidator(value = "addressRequest.foreignAddressData.zipCode")
    @Length(min = 1, max = 32, message = "addressRequest.foreignAddressData.zipCode-zipCode length must be between {min} and {max};")
    @NotNull(message = "addressRequest.foreignAddressData.zipCode-zipCode can not be null;")
    private String zipCode;
    @AddressFieldValidator(value = "addressRequest.foreignAddressData.district")
    @Length(min = 1, max = 512, message = "addressRequest.foreignAddressData.district-district length must be between {min} and {max};")
    private String district;

    private ResidentialAreaType residentialAreaType;
    @AddressFieldValidator(value = "addressRequest.foreignAddressData.residentialArea")
    @Length(min = 1, max = 1024, message = "addressRequest.foreignAddressData.residentialArea-residentialArea length must be between {min} and {max};")
    private String residentialArea;

    private StreetType streetType;
    @AddressFieldValidator(value = "addressRequest.foreignAddressData.street")
    @Length(min = 1, max = 1024, message = "addressRequest.foreignAddressData.street-street length must be between {min} and {max};")
    private String street;

}
