package bg.energo.phoenix.model.request.nomenclature.customer.legalForm;

import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import java.util.List;

@Data
public class EditLegalFormRequest {
    @Pattern(regexp = "^[А-Яа-яA-Za-z\\d@&()+_\\-.,№:–\\s]*$", message = "name-Pattern does not match the allowed symbols")
    @NotBlank(message = "name-can not be blank")
    @Length(min = 1,max = 128,message = "description-length should be between 1-128")
    private String name;

    @NotBlank(message = "description-can not be blank")
    @Pattern(regexp= "^[А-Яа-яA-Za-z\\d@&()+–_\\-:.,№\\s]*$", message="description-Pattern does not match the allowed symbols")
    @Length(min = 1,max = 1024,message = "description-length should be between 1-1024")
    private String description;

    @Valid
    @NotEmpty(message = "legalFormsTransliterated-can not be empty")
    private List<EditLegalFormTranRequest> legalFormsTransliterated;

    @NotNull(message = "defaultSelection-can not be null")
    private Boolean defaultSelection;

    @NotNull(message = "status-can not be null")
    private NomenclatureItemStatus status;
}
