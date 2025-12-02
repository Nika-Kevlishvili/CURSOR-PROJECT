package bg.energo.phoenix.model.request.billing.billingRun;

import bg.energo.phoenix.model.enums.task.PerformerType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class BillingNotificationRequest {

    private PerformerType performerType;;
    private Long performerId;
    private BillingNotificationType type;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BillingNotificationRequest that = (BillingNotificationRequest) o;
        return performerType == that.performerType && performerId.equals(that.performerId) && type == that.type;
    }

    @Override
    public int hashCode() {
        int result = performerType.hashCode();
        result = 31 * result + performerId.hashCode();
        result = 31 * result + type.hashCode();
        return result;
    }
}
