package bg.energo.phoenix.model.request.nomenclature.customer.legalForm;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EditLegalFormTranRequest {
    @NotBlank(message = "description-can not be blank;")
    @Pattern(regexp= "^[A-Za-z\\d@&():+–_\\-.,№\\s]*$", message="description-Pattern does not match the allowed symbols;")
    @Length(min = 1,max = 1024,message = "description-length should be between 1-1024;")
    private String description;

    @NotBlank(message = "description-can not be blank")
    @Pattern(regexp= "^[A-Za-z\\d@&():+–_\\-.,№\\s]*$", message="name-Pattern does not match the allowed symbols;")
    @Length(min = 1,max = 128,message = "description-length should be between 1-128;")
    private String name;

    @Positive(message = "id-should be positive;")
    private Long id;
}
