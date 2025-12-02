package bg.energo.phoenix.model.response.nomenclature.billing;

import bg.energo.phoenix.model.entity.nomenclature.billing.Prefix;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import lombok.Data;

@Data
public class PrefixResponse {

    private Long id;
    private String name;
    private String prefixType;
    private Long orderingId;
    private Boolean defaultSelection;
    private Boolean isHardCoded;
    private NomenclatureItemStatus status;
    private String systemUserId;

    public PrefixResponse(Prefix prefix) {
        this.id = prefix.getId();
        this.name = prefix.getName();
        this.prefixType = prefix.getPrefixType();
        this.orderingId = prefix.getOrderingId();
        this.defaultSelection = prefix.isDefault();
        this.isHardCoded = prefix.getIsHardCoded();
        this.status = prefix.getStatus();
        this.systemUserId = prefix.getSystemUserId();
    }
}
