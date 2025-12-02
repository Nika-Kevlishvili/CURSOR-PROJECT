package bg.energo.phoenix.model.request.nomenclature;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NomenclatureItemsSortOrderRequest {

    @NotNull(message = "id-id should not be null")
    private Long id; // id of the item which needs to be moved

    @NotNull(message = "orderingId-orderingId should not be null")
    @Positive(message = "orderingId-orderingId should be positive number")
    private Long orderingId; // new ordering id which need to be given to the item after moving

    private Integer page;

    private Integer size;
}
