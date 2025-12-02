package bg.energo.phoenix.process.service;

import bg.energo.phoenix.model.entity.customer.AccountManager;
import bg.energo.phoenix.process.model.entity.ProcessNotification;
import bg.energo.phoenix.process.model.enums.ProcessNotificationType;
import bg.energo.phoenix.process.repository.ProcessNotificationRepository;
import bg.energo.phoenix.repository.customer.AccountManagerRepository;
import bg.energo.phoenix.security.PermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProcessEventListener {

    private final AccountManagerRepository accountManagerRepository;
    private final PermissionService permissionService;
    private final ProcessNotificationRepository processNotificationRepository;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void beforeSave(ProcessNotificationCreationEvent process){
        log.debug("ReceivedNotificationEvent; ProcessId={}", process.getProcessId());
        Long processId = process.getProcessId();
        String loggedInUserId = permissionService.getLoggedInUserId();
        log.debug("LoggedInUser={}", loggedInUserId);
        Optional<AccountManager> userOptional = accountManagerRepository.findByUserName(loggedInUserId);
        if(userOptional.isPresent()){
            log.debug("FoundAccountManager");
            ProcessNotification processNotification = new ProcessNotification(null, processId, null, userOptional.get().getId(), ProcessNotificationType.COMPLETION);
            processNotificationRepository.saveAndFlush(processNotification);

        }
    }
}
