package bg.energo.phoenix.model.request.nomenclature.customer;

import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class MissingCustomerRequest {

    @NotNull(message = "uic-should not be null;")
    @NotBlank(message = "uic-should not be blank;")
    @Pattern(regexp = "^[A-Z0-9]{9}|[A-Z0-9]{10}|[A-Z0-9]{13}$", message = "uic-should be 9, 10, or 13 alphanumeric characters;")
    private String uic;

    @NotNull(message = "name-should not be null;")
    @NotBlank(message = "name-should not be Blank;")
    @Length(min = 1, max = 2048, message = "name-length should be between 1 and 2048;")
    @Pattern(regexp = "^[А-ЯA-Z0-9\\-–@#$&*()+:.,'‘€№=\\s]+$", message = "name-contains invalid characters;")
    private String name;

    @NotNull(message = "nameTransliterated-should not be null;")
    @NotBlank(message = "nameTransliterated-should not be blank;")
    @Length(min = 1, max = 2048, message = "nameTransliterated-length should be between 1 and 2048;")
    @Pattern(regexp = "^[A-Z0-9\\-–@#$&*()+:.,'‘€№=\\s]+$", message = "nameTransliterated-contains invalid characters;")
    private String nameTransliterated;

    @NotNull(message = "legalForm-should not be null;")
    @NotBlank(message = "legalForm-should not be blank;")
    @Length(min = 1, max = 128, message = "legalForm-length should be between 1 and 128;")
    @Pattern(regexp = "^[А-ЯA-Z0-9\\-–@&()+:.,№\\s]+$", message = "legalForm-contains invalid characters;")
    private String legalForm;

    @NotNull(message = "legalFormTransliterated-should not be null;")
    @NotBlank(message = "legalFormTransliterated-should not be blank;")
    @Length(min = 1, max = 128, message = "legalFormTransliterated-length should be between 1 and 128;")
    @Pattern(regexp = "^[A-Z0-9\\-–@&()+:.,№\\s]+$", message = "legalFormTransliterated-contains invalid characters;")
    private String legalFormTransliterated;

    @NotNull(message = "status-should not be null;")
    private NomenclatureItemStatus status;

    @NotNull(message = "defaultSelection-should not be null;")
    private Boolean defaultSelection;
}
