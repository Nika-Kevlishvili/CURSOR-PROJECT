package bg.energo.phoenix.service.portal;

import bg.energo.common.portal.api.PortalRegistrationBundle;
import bg.energo.common.portal.api.register.AppRegisterRequest;
import bg.energo.common.portal.api.register.RemoteResource;
import bg.energo.phoenix.config.AppConfig;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class ApplicationStateService {

    @Getter
    private ZonedDateTime started;
    @Getter
    private int statusCode;
    @Getter
    private String statusMessage;

    @Getter
    @Setter
    private PortalRegistrationBundle portalRegistrationBundle;

    public ApplicationStateService(AppConfig appConfig) {
        log.debug("app config {}", appConfig);
        started = ZonedDateTime.now();
        statusCode = 0;
        statusMessage = "up and running";

        portalRegistrationBundle = new PortalRegistrationBundle();

        AppRegisterRequest appRegisterRequest = new AppRegisterRequest();
        appRegisterRequest.setApplicationId(appConfig.getApplicationId());
        appRegisterRequest.setModuleId(appConfig.getModuleId());
        appRegisterRequest.setVersion("1.0");
        appRegisterRequest.setBaseUrl(appConfig.getAppBaseUrl());
        appRegisterRequest.setResources(getStringRemoteResourceMap());

        appRegisterRequest.setAppSessionKey(UUID.randomUUID().toString());
        appRegisterRequest.setSupportedLangs(appConfig.getSupportedLangs());
        appRegisterRequest.setAppSessionHeader("X-Phoenix-Session-ID");
        portalRegistrationBundle.setRegisterUrl(appConfig.getPortalRegisterUrl());
        portalRegistrationBundle.setApplicationRegistered(false);
        portalRegistrationBundle.setRegisterRequest(appRegisterRequest);
    }

    public long getUptime() {
        return System.currentTimeMillis() - started.toInstant().toEpochMilli();
    }
    private Map<String, RemoteResource> getStringRemoteResourceMap() {
        Map<String, RemoteResource> resourceMap = new HashMap<>();
        RemoteResource permissionResource = new RemoteResource("/portal/permissions", true, "application permissions resource.");
        RemoteResource heartbeatResource = new RemoteResource("/portal/heartbeat", true, "application heartbeat resource.");
        RemoteResource messageResource = new RemoteResource("/portal/message", true, "application message resource.");
        resourceMap.put(PortalResources.APP_PERMISSIONS_RESOURCE, permissionResource);
        resourceMap.put(PortalResources.APP_HEARTBEAT_RESOURCE, heartbeatResource);
        resourceMap.put(PortalResources.APP_MESSAGE_RESOURCE, messageResource);
        return resourceMap;
    }
}
