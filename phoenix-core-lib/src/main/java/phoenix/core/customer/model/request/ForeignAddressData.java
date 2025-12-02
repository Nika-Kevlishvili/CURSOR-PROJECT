package phoenix.core.customer.model.request;

import lombok.Data;
import org.hibernate.validator.constraints.Length;
import phoenix.core.customer.model.customAnotations.customer.manager.AddressFieldValidator;

@Data
public class ForeignAddressData {

    private Long countryId;

    @AddressFieldValidator
    @Length(min = 1, max = 512)
    private String region;

    @AddressFieldValidator
    @Length(min = 1, max = 512)
    private String municipality;

    @AddressFieldValidator
    @Length(min = 1, max = 512)
    private String populatedPlace;

    @AddressFieldValidator
    @Length(min = 1, max = 32)
    private String zipCode;

    @AddressFieldValidator
    @Length(min = 1, max = 512)
    private String district;

    private String residentialArea;
    private String street;

}
