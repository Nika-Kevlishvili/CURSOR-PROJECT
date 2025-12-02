package phoenix.core.customer.model.response.customer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import phoenix.core.customer.model.entity.nomenclature.customer.Preferences;
import phoenix.core.customer.model.enums.nomenclature.NomenclatureItemStatus;

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
