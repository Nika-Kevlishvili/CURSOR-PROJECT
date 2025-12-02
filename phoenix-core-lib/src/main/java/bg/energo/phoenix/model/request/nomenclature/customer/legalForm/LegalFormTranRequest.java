package bg.energo.phoenix.model.request.nomenclature.customer.legalForm;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LegalFormTranRequest {
    @NotBlank(message = "description-Description can not be blank;")
    @Pattern(regexp= "^[A-Za-z\\d@&()+–_:\\-.,№\\s]*$", message="description-Description pattern does not match the allowed symbols;")
    @Length(min = 1,max = 1024,message = "description-Description length should be between {min}:{max} characters;")
    private String description;

    @Pattern(regexp = "^[A-Za-z\\d@&():+–_\\-.,№\\s]*$", message = "name-Pattern does not match the allowed symbols")
    @NotBlank(message = "name-Name can not be blank;")
    @Length(min = 1,max = 128,message = "name-Name length should be between {min}:{max} characters;")
    private String name;
}
