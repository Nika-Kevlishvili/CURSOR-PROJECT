package phoenix.core.customer.model.response.customer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import phoenix.core.customer.model.enums.nomenclature.NomenclatureItemStatus;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BelongingCapitalOwnerResponse {
    private Long id;
    private String name;
    private NomenclatureItemStatus status;
    private Long orderingId;
    private boolean defaultSelection;
    private String systemUserId;
}
