package bg.energo.phoenix.model.request.nomenclature.receivable;

import bg.energo.phoenix.model.enums.nomenclature.BlockingReasonType;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsBaseFilterRequest;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BlockingReasonFilterRequest extends NomenclatureItemsBaseFilterRequest {

    private BlockingReasonType type;

}
