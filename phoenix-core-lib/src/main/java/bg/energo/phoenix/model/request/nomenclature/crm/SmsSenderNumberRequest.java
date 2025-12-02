package bg.energo.phoenix.model.request.nomenclature.crm;

import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SmsSenderNumberRequest {

    @NotBlank(message = "name-Name must not be blank;")
    @Size(min = 1, max = 1024, message = "name-Name does not match the allowed length;")
    private String name;

    @NotBlank(message = "smsNumber-smsNumber must not be blank;")
    @Size(min = 1, max = 32, message = "smsNumber-smsNumber does not match the allowed length;")
    @Pattern(regexp = "^[0-9+\\-*]{1,32}$", message = "smsNumber-smsNumber format is: 0-9 - + *;")
    private String smsNumber;

    @NotNull(message = "status-status must not be null;")
    private NomenclatureItemStatus status;

    @NotNull(message = "defaultSelection-Default selection must not be null;")
    private Boolean defaultSelection;

}
