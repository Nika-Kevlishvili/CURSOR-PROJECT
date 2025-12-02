package bg.energo.phoenix.model.entity.activity;

import bg.energo.phoenix.model.entity.nomenclature.contract.subActivity.SubActivityJsonField;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SystemActivityJsonField extends SubActivityJsonField {

    private String selectedValue = "";

}
