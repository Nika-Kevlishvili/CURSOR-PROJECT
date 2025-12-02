package bg.energo.phoenix.service.portal.heartbeat;

import bg.energo.common.backend.app.HeartbeatChecker;
import bg.energo.common.backend.app.RetryCommand;
import bg.energo.common.portal.api.PortalRegistrationBundle;
import bg.energo.common.portal.api.heartbeat.HeartbeatResponse;
import bg.energo.common.portal.api.register.AppRegisterResponse;
import bg.energo.common.portal.api.register.RemoteResource;
import bg.energo.phoenix.service.portal.ApplicationStateService;
import bg.energo.phoenix.service.portal.PortalResources;
import bg.energo.phoenix.service.portal.PortalService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;


@Slf4j
@Service
@Profile({"dev","test","local"})
@ConditionalOnExpression("${app.cfg.authorization.enabled:true}")
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
public class CheckHeartbeatScheduler {
    @Value("${app.cfg.portal.max-errors}")
    private int max_errors;
    @Value("${app.cfg.portal.max-error-timeout}")
    private long max_error_timeout;
    @Value("${app.cfg.max-retry}")
    private int maxRetry;
    @Value("${app.cfg.seconds-to-wait}")
    private int secondsToWait;
    private final PortalService portalService;
    private final HeartbeatChecker heartbeatChecker;
    private final PortalRegistrationBundle appRegisterBundle;

    public CheckHeartbeatScheduler(PortalService portalService,
                                   HeartbeatChecker heartbeatChecker,
                                   ApplicationStateService applicationStateService) {
        this.portalService = portalService;
        this.heartbeatChecker = heartbeatChecker;
        this.appRegisterBundle = applicationStateService.getPortalRegistrationBundle();
    }

    @Scheduled(fixedDelayString = "${app.cfg.heartbeat-delay}", initialDelayString = "${app.cfg.heartbeat-initial-delay}")
    public void run() {
        checkHeartbeat();
    }


    private void registerApplication() {
        new RetryCommand<AppRegisterResponse>("Try to register in portal", maxRetry, secondsToWait)
                .execute(portalService::doRegisterApp);
    }

    private void checkHeartbeat() {
        if (!appRegisterBundle.isApplicationRegistered()) {
            registerApplication();
        }
        final AppRegisterResponse registration = appRegisterBundle.getRegistrationResponse();
        if (registration == null) {
            appRegisterBundle.setApplicationRegistered(false);
            return;
        }

        final RemoteResource heartbeatResource = registration.getResources().get(PortalResources.HEARTBEAT_RESOURCE);
        if (heartbeatResource==null) {
            return;
        }
        try {
            HeartbeatResponse heartBeatResponse = heartbeatChecker.checkHeartbeat(heartbeatResource.getUrl(), registration.getPortalSessionKey());
            if(heartBeatResponse.getStatusCode()!=0){
                appRegisterBundle.setApplicationRegistered(false);
            }
            log.debug(heartBeatResponse.toString());

        } catch (final Exception e) {
            log.error("", e);
        }
    }
}
