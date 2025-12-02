package bg.energo.phoenix.service.archivation.edms.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "edms.credential")
public class EDMSCredentialConfigurationProperties {
    private String profileGuid;
    private String productName;
    private String userName;
    private String password;
}
