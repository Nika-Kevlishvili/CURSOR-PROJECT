package bg.energo.phoenix.model.response.nomenclature.address;

import bg.energo.phoenix.model.entity.nomenclature.address.Country;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import lombok.Data;

@Data
public class CountryResponse {
    private Long id;
    private String name;
    private Long orderingId;
    private boolean defaultSelection;
    private NomenclatureItemStatus status;
    private String systemUserId;

    public CountryResponse(Country country) {
        this.id = country.getId();
        this.name = country.getName();
        this.orderingId = country.getOrderingId();
        this.defaultSelection = country.isDefaultSelection();
        this.status = country.getStatus();
        this.systemUserId = country.getSystemUserId();
    }
}
