package bg.energo.phoenix.model.request.nomenclature.address;

import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsBaseFilterRequest;
import lombok.*;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString(callSuper = true)
public class ZipCodeFilterRequest extends NomenclatureItemsBaseFilterRequest {

    private Long populatedPlaceId;

}
