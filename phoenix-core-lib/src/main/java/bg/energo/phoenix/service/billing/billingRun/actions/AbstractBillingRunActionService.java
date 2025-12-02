package bg.energo.phoenix.service.billing.billingRun.actions;

import bg.energo.phoenix.model.enums.billing.billings.BillingPermissions;
import bg.energo.phoenix.model.enums.billing.billings.BillingType;
import bg.energo.phoenix.repository.billing.billingRun.BillingErrorDataRepository;
import bg.energo.phoenix.repository.billing.billingRun.BillingRunRepository;
import bg.energo.phoenix.security.PermissionService;
import bg.energo.phoenix.service.notifications.enums.NotificationState;
import bg.energo.phoenix.service.notifications.enums.NotificationType;
import bg.energo.phoenix.service.notifications.events.NotificationEvent;
import bg.energo.phoenix.service.notifications.service.NotificationEventPublisher;
import lombok.extern.slf4j.Slf4j;

import static bg.energo.phoenix.permissions.PermissionContextEnum.BILLING_RUN;

@Slf4j
public abstract class AbstractBillingRunActionService {
    private final PermissionService permissionService;
    protected final BillingRunRepository billingRunRepository;
    protected final NotificationEventPublisher notificationEventPublisher;
    protected final BillingErrorDataRepository billingErrorDataRepository;

    public AbstractBillingRunActionService(PermissionService permissionService,
                                           BillingRunRepository billingRunRepository,
                                           NotificationEventPublisher notificationEventPublisher,
                                           BillingErrorDataRepository billingErrorDataRepository) {
        this.permissionService = permissionService;
        this.billingRunRepository = billingRunRepository;
        this.notificationEventPublisher = notificationEventPublisher;
        this.billingErrorDataRepository = billingErrorDataRepository;
    }

    public abstract AbstractBillingRunActionService getNextJobInChain();

    public abstract void execute(Long billingRunId, boolean isResumeProcess, boolean mustCheckPermission);

    protected void publishNotification(Long billingRunId, NotificationType notificationType, NotificationState notificationState) {
        notificationEventPublisher.publishNotification(new NotificationEvent(billingRunId, notificationType, billingRunRepository, notificationState));
    }

    protected boolean missingPermission(BillingPermissions billingPermissions, BillingType billingType) {
        return !permissionService.getPermissionsFromContext(BILLING_RUN).contains(billingPermissions.getRelevantPermission(billingType).getId());
    }
}
