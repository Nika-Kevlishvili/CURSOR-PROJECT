package bg.energo.scheduler.schedulers;

import bg.energo.phoenix.model.customAnotations.aspects.ExecutionTimeLogger;
import bg.energo.phoenix.service.receivable.paymentPackage.PaymentPackageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@Profile({"dev", "test","preProd", "prod"})
@ConditionalOnExpression("${app.cfg.schedulers.enabled:true}")
public class PaymentPackageBlockerScheduler {
    private final PaymentPackageService paymentPackageService;

    @Scheduled(cron = "0 59 23 * * *", zone = "Europe/Sofia")
    @ExecutionTimeLogger(value = "PaymentPackageBlockerScheduler")
    public void start() {
        log.info("Starting online payment package blocker scheduler at end of day");
        paymentPackageService.onlinePackageBlocker();
    }
}
