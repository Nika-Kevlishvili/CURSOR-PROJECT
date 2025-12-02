package phoenix.core.customer.model.response.nomenclature;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PopulatedPlaceTreeResponse {
    private Long municipalityId;
    private String municipalityName;
    private Long regionId;
    private String regionName;
    private Long countryId;
    private String countryName;
}
