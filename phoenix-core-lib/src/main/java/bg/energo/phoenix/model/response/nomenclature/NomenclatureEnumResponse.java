package bg.energo.phoenix.model.response.nomenclature;

import bg.energo.phoenix.model.enums.nomenclature.Nomenclature;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NomenclatureEnumResponse {
    Nomenclature name;
    String value;
}
