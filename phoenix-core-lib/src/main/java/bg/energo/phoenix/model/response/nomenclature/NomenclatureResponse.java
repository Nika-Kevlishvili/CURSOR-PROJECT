package bg.energo.phoenix.model.response.nomenclature;

import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class NomenclatureResponse {

    private Long id;

    private String name;

    private Long orderingId;

    private Boolean defaultSelection;

    private NomenclatureItemStatus status;

}
