package bg.energo.phoenix.model.response.nomenclature.address.tree;

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
