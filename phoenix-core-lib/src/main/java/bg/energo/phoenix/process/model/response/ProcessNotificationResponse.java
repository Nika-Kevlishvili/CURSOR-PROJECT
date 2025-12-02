package bg.energo.phoenix.process.model.response;

import bg.energo.phoenix.model.entity.customer.AccountManager;
import bg.energo.phoenix.model.entity.customer.PortalTag;
import bg.energo.phoenix.model.enums.task.PerformerType;
import bg.energo.phoenix.process.model.enums.ProcessNotificationType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProcessNotificationResponse {
    private Long id;
    private PerformerType performerType;
    private ProcessNotificationType type;
    private String name;
    private String nameBg;

    public ProcessNotificationResponse(AccountManager am, PortalTag portalTag,ProcessNotificationType type) {
        if(am!=null){
            performerType=PerformerType.MANAGER;
        }else if(portalTag!=null){
            performerType=PerformerType.TAG;
        }
        this.type = type;
        if(PerformerType.MANAGER.equals(performerType)){
            this.id = am.getId();
            this.name = "%s ( %s )".formatted(am.getDisplayName(),am.getUserName());
        }else if (PerformerType.TAG.equals(performerType))
        {
            this.id = portalTag.getId();
            this.name =portalTag.getName();
            this.nameBg =portalTag.getNameBg();
        }
    }
}
