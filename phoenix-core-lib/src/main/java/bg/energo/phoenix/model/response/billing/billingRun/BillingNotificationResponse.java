package bg.energo.phoenix.model.response.billing.billingRun;

import bg.energo.phoenix.model.entity.customer.AccountManager;
import bg.energo.phoenix.model.entity.customer.PortalTag;
import bg.energo.phoenix.model.enums.task.PerformerType;
import bg.energo.phoenix.model.request.billing.billingRun.BillingNotificationType;
import lombok.Data;

@Data
public class BillingNotificationResponse {
    private Long performerId;
    private PerformerType performerType;
    private BillingNotificationType type;
    private String performerName;
    private String performerNameBg;

    public BillingNotificationResponse(AccountManager am, PortalTag portalTag, BillingNotificationType type) {
        if(am!=null){
            performerType=PerformerType.MANAGER;
        }else if(portalTag!=null){
            performerType=PerformerType.TAG;
        }
        this.type = type;
        if(PerformerType.MANAGER.equals(performerType)){
            this.performerId = am.getId();
            this.performerName = "%s ( %s )".formatted(am.getDisplayName(),am.getUserName());
        }else if (PerformerType.TAG.equals(performerType))
        {
            this.performerId = portalTag.getId();
            this.performerName=portalTag.getName();
            this.performerNameBg=portalTag.getNameBg();
        }
    }
}
