package bg.energo.phoenix.model.request.nomenclature.receivable;

import bg.energo.phoenix.model.customAnotations.nomenclature.NameDefaultValidator;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.receivable.massOperationForBlocking.ReceivableBlockingReasonType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BlockingReasonRequest {

    @NotBlank(message = "name-[Name] must not be blank;")
    @Size(min = 1, max = 1024, message = "name-[Name] does not match the allowed length, range: [1-1024];")
    @NameDefaultValidator
    private String name;

    @NotNull(message = "reasonType-[reasonType] must not be null")
    @NotEmpty(message = "reasonType-[reasonType] must not be empty")
    private List<ReceivableBlockingReasonType> reasonTypes;

    @NotNull(message = "status-[Status] must not be null;")
    private NomenclatureItemStatus status;

    @NotNull(message = "defaultSelection-[Default] Selection must not be null;")
    private Boolean defaultSelection;

}
