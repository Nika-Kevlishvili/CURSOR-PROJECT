package phoenix.core.customer.model.response.customer.communicationData;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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

    private String residentialArea;

    private String street;
}
