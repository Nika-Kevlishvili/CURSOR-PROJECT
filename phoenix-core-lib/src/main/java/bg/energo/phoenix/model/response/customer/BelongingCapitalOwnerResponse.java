package bg.energo.phoenix.model.response.customer;

import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
