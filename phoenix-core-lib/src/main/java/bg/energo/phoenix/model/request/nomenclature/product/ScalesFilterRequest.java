package bg.energo.phoenix.model.request.nomenclature.product;

import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsBaseFilterRequest;
import lombok.*;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString(callSuper = true)
public class ScalesFilterRequest extends NomenclatureItemsBaseFilterRequest {

    private Long gridOperatorId;
}
