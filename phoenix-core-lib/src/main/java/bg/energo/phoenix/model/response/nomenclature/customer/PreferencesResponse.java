package bg.energo.phoenix.model.response.nomenclature.customer;

import bg.energo.phoenix.model.entity.nomenclature.customer.Preferences;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PreferencesResponse {

    private Long id;
    private String name;
    private NomenclatureItemStatus status;
    private Long orderingId;
    private Boolean defaultSelection;

    public PreferencesResponse(Preferences preferences) {
        this.id = preferences.getId();
        this.name = preferences.getName();
        this.status = preferences.getStatus();
        this.orderingId = preferences.getOrderingId();
        this.defaultSelection = preferences.getIsDefault();
    }

}
