package bg.energo.phoenix.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.Set;
import java.util.UUID;


@Service
@NoArgsConstructor
@AllArgsConstructor
@Data
public class AppConfig implements Serializable {
    @Value("${application.name}")
    private String applicationName;

    @Value("${app.cfg.applicationId}")
    private UUID applicationId;

    @Value("${app.cfg.moduleId}")
    private UUID moduleId;

    @Value("${app.cfg.supported_langs}")
    private Set<String> supportedLangs;

    @Value("${app.cfg.timeout.heartbeat}")
    private Long HeartBeatTimeout;

    @Value("${app.cfg.portal.base_url}")
    private String portalBaseUrl;

    @Value("${app.cfg.portal.register_resource}")
    private String portalRegisterUrl;

//    @Value("${app.cfg.portal.unregister_resource}")
//    private String portalUnRegisterUrl;
//
//    @Value("${app.cfg.portal.query_token}")
//    private String queryTokenUri;
//
//    @Value("${app.cfg.session-header}")
//    private String sessionHeader;

    @Value("${app.cfg.deploy.base_url}")
    private String appBaseUrl;

    @Value("${app.cfg.deploy.port}")
    private int appPort;

    @Value("${app.cfg.deploy.context_path}")
    private String appContext;

    @Value("${app.cfg.request-timeout}")
    private long requestTimeout;
}
