package bg.energo.phoenix.model.response.receivable.collectionChannel;

import bg.energo.phoenix.model.entity.customer.AccountManager;
import bg.energo.phoenix.model.entity.customer.PortalTag;
import bg.energo.phoenix.model.enums.task.PerformerType;
import lombok.Data;

@Data
public class CollectionChannelEmployeeResponse {
    private Long id;
    private PerformerType performerType;
    private String name;
    private String nameBg;
    private String portalId;

    public CollectionChannelEmployeeResponse(AccountManager am, PortalTag portalTag) {
        if (am != null) {
            performerType = PerformerType.MANAGER;
        } else if (portalTag != null) {
            performerType = PerformerType.TAG;
        }
        if (PerformerType.MANAGER.equals(performerType) && am != null) {
            this.id = am.getId();
            this.name = "%s ( %s )".formatted(am.getDisplayName(), am.getUserName());
        } else if (PerformerType.TAG.equals(performerType) && portalTag != null) {
            this.id = portalTag.getId();
            this.name = portalTag.getName();
            this.nameBg = portalTag.getNameBg();
            this.portalId = portalTag.getPortalId();
        }
    }
}
