package bg.energo.phoenix.model.request.nomenclature.product.currency;

import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsBaseFilterRequest;
import lombok.*;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString(callSuper = true)
public class CurrencyFilterRequest extends NomenclatureItemsBaseFilterRequest {
    private Long altCurrencyId;
}
