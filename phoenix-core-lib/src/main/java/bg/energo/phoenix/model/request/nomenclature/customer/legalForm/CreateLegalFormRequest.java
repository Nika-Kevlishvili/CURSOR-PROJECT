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
public class CreateLegalFormRequest {
    @Pattern(regexp = "^[А-Яа-яA-Za-z\\d@&()+_\\-.,№:–\\s]*$", message = "name-Pattern does not match the allowed symbols")
    @NotBlank(message = "name-Name must not be blank;")
    @Length(min = 1,max = 128,message = "name-Name length should be between {min}:{max} characters;")
    private String name;

    @NotBlank(message = "description-Description can not be blank;")
    @Pattern(regexp= "^[А-Яа-яA-Za-z\\d@&():+–_\\-.,№\\s]*$", message="description-Description pattern does not match the allowed symbols;")
    @Length(min = 1,max = 1024,message = "description-Description length should be between {min}-{max} characters;")
    private String description;

    @Valid
    @NotEmpty(message = "legalFormsTransliterated-[LegalFormsTransliterated] field can not be empty;")
    private List<LegalFormTranRequest> legalFormsTransliterated;

    @NotNull(message = "defaultSelection-Default selection can not be null;")
    private Boolean defaultSelection;

    @NotNull(message = "status-Status can not be null;")
    private NomenclatureItemStatus status;
}
