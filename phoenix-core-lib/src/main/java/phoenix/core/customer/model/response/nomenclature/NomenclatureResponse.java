package phoenix.core.customer.model.response.nomenclature;

import lombok.AllArgsConstructor;
import lombok.Data;
import phoenix.core.customer.model.enums.nomenclature.NomenclatureItemStatus;

@Data
@AllArgsConstructor
public class NomenclatureResponse {

    private Long id;

    private String name;

    private Long orderingId;

    private Boolean defaultSelection;

    private NomenclatureItemStatus status;

}
