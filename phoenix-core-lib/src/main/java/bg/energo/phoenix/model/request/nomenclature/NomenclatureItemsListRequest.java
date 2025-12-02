package bg.energo.phoenix.model.request.nomenclature;

import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NomenclatureItemsListRequest {

    @NotNull
    @NotEmpty
    private List<NomenclatureItemStatus> statuses;

}
