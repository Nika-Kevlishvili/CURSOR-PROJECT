package bg.energo.phoenix.model.response.nomenclature.customer;

import bg.energo.phoenix.model.entity.nomenclature.customer.legalForm.LegalFormTransliterated;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LegalFormTranResponse {
    private Long id;
    private String name;
    private String description;

    public LegalFormTranResponse(LegalFormTransliterated transliterated) {
        this.id = transliterated.getId();
        this.name = transliterated.getName();
        this.description = transliterated.getDescription();
    }

}
