package bg.energo.phoenix.service.notifications.service;

import bg.energo.common.utils.StringUtils;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.IllegalArgumentsProvidedException;
import bg.energo.phoenix.model.entity.customer.AccountManager;
import bg.energo.phoenix.model.entity.notification.UserNotification;
import bg.energo.phoenix.model.response.notification.UserNotificationResponse;
import bg.energo.phoenix.repository.customer.AccountManagerRepository;
import bg.energo.phoenix.repository.notification.UserNotificationRepository;
import bg.energo.phoenix.security.PermissionService;
import bg.energo.phoenix.service.crm.emailClient.EmailSenderService;
import bg.energo.phoenix.service.notifications.models.UserNotificationPage;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Slf4j
@Service
@Lazy(value = false)
public class NotificationService {
    public NotificationService(UserNotificationRepository userNotificationRepository,
                               PermissionService permissionService,
                               AccountManagerRepository accountManagerRepository,
                               @Qualifier(value = "notificationSource")
                               MessageSource notificationSource,
                               EmailSenderService emailSenderService) {
        this.userNotificationRepository = userNotificationRepository;
        this.permissionService = permissionService;
        this.accountManagerRepository = accountManagerRepository;
        this.notificationSource = notificationSource;
        this.emailSenderService = emailSenderService;
    }

    private final UserNotificationRepository userNotificationRepository;
    private final PermissionService permissionService;
    private final AccountManagerRepository accountManagerRepository;
    private final MessageSource notificationSource;
    private final EmailSenderService emailSenderService;

    private final Locale englishLocale = new Locale("en");
    private final Locale bulgarianLocale = new Locale("bg");

    @Transactional
    public void sendNotifications(List<NotificationModel> notificationModels) {
        if (CollectionUtils.isNotEmpty(notificationModels)) {
            for (NotificationModel notificationModel : notificationModels) {
                log.debug("notification received for account manager with id: %s, entityId:%s ,notificationType: %s;"
                        .formatted(notificationModel.accountManagerId(), notificationModel.entityId(), notificationModel.notificationType()));
                try {
                    Long accountManagerId = notificationModel.accountManagerId();
                    AccountManager accountManager = accountManagerRepository
                            .findById(accountManagerId)
                            .orElseThrow(() -> new DomainEntityNotFoundException("Account manager with id: [%s] not found;".formatted(accountManagerId)));

                    UserNotification userNotification = UserNotification
                            .builder()
                            .accountManagerId(accountManagerId)
                            .notificationType(notificationModel.notificationType())
                            .isReaden(false)
                            .entityId(notificationModel.entityId())
                            .build();

                    userNotificationRepository.save(userNotification);

                    String email = accountManager.getEmail(); //TODO EMAIL CHECK PART SHOULD BE REMOVED IN THE PRODUCTION
                    if (!StringUtils.isNullOrEmpty(email)) {
                        log.debug("email is: %s;".formatted(email));
                        if (email.contains("@oppa.ge") || email.contains("@asterbit.io")) {
                            String engMessage = "";
                            String bgMessage = "";

                            try {
                                engMessage = notificationSource.getMessage(notificationModel.notificationType().getNotificationResourceLocation(), null, englishLocale);
                            } catch (Exception e) {
                                log.error("Cannot found english locale message");
                            }

                            try {
                                bgMessage = notificationSource.getMessage(notificationModel.notificationType().getNotificationResourceLocation(), null, bulgarianLocale);
                            } catch (Exception e) {
                                log.error("Cannot found bulgarian locale message");
                            }

                            String emailBody = "%s\n%s".formatted(engMessage.formatted(notificationModel.entityId()), bgMessage.formatted(notificationModel.entityId()));

                            emailSenderService.sendEmail(
                                    accountManager.getEmail(),
                                    "Notification",
                                    emailBody,
                                    List.of()
                            );
                        }
                    }
                } catch (Exception e) {
                    log.error("Exception handled while trying to send notification");
                }
            }
        }
    }

    public UserNotificationPage<UserNotificationResponse> getUserNotifications(Integer page, Integer size) {
        String user = permissionService.getLoggedInUserId();

        AccountManager accountManager = accountManagerRepository
                .findByUserName(user)
                .orElseThrow(() -> new IllegalArgumentsProvidedException("User not found"));

        Page<UserNotificationResponse> userNotificationPage = userNotificationRepository
                .findUserNotificationByAccountManagerId(
                        accountManager.getId(),
                        LocalDateTime.now().minusDays(5),
                        PageRequest.of(
                                Objects.requireNonNullElse(page, 0),
                                Objects.requireNonNullElse(size, 10)
                        )
                );

        long unreadenUserNotificationCount = userNotificationRepository
                .countUnreadenUserNotificationByAccountManagerId(accountManager.getId());

        return new UserNotificationPage<>(userNotificationPage.getContent(), userNotificationPage.getPageable(), userNotificationPage.getTotalElements(), unreadenUserNotificationCount);
    }

    @Transactional
    public void readNotification(Long id) {
        String user = permissionService.getLoggedInUserId();

        AccountManager accountManager = accountManagerRepository
                .findByUserName(user)
                .orElseThrow(() -> new IllegalArgumentsProvidedException("User not found"));

        UserNotification userNotification = userNotificationRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("Notification with id: [%s] not found;".formatted(id)));

        if (Objects.equals(userNotification.getAccountManagerId(), accountManager.getId())) {
            userNotification.setIsReaden(true);
            userNotification.setReadDate(LocalDateTime.now());
        } else {
            log.error("Provided notification is not assigned to current user");
            throw new IllegalArgumentsProvidedException("Provided notification is not assigned to current user");
        }
    }
}
