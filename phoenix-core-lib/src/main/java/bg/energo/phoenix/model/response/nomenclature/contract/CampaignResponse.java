package bg.energo.phoenix.model.response.nomenclature.contract;

import bg.energo.phoenix.model.entity.nomenclature.contract.Campaign;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import lombok.Data;

@Data
public class CampaignResponse {
    private Long id;
    private String name;
    private Long orderingId;
    private boolean defaultSelection;
    private NomenclatureItemStatus status;
    private String systemUserId;

    public CampaignResponse(Campaign campaign) {
        this.id = campaign.getId();
        this.name = campaign.getName();
        this.orderingId = campaign.getOrderingId();
        this.defaultSelection = campaign.isDefaultSelection();
        this.status = campaign.getStatus();
        this.systemUserId = campaign.getSystemUserId();
    }
}
