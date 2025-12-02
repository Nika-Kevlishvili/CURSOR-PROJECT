package bg.energo.phoenix.service.archivation.edms.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "edms.properties")
public class EDMSProcessConfigurationProperties {
    private Long globalTimeout;
    private Long documentTypeId;
    private Long folderId;
}
