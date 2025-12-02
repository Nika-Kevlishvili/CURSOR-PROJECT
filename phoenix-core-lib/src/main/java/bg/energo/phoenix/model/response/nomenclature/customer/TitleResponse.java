package bg.energo.phoenix.model.response.nomenclature.customer;

import bg.energo.phoenix.model.entity.nomenclature.customer.Title;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import lombok.Data;

@Data
public class TitleResponse {
    private Long id;
    private String name;
    private Long orderingId;
    private boolean defaultSelection;
    private NomenclatureItemStatus status;
    private String systemUserId;

    public TitleResponse(Title title) {
        this.id = title.getId();
        this.name = title.getName();
        this.orderingId = title.getOrderingId();
        this.defaultSelection = title.isDefaultSelection();
        this.status = title.getStatus();
        this.systemUserId = title.getSystemUserId();
    }
}
