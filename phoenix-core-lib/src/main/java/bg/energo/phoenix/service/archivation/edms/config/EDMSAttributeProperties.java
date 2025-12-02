package bg.energo.phoenix.service.archivation.edms.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "edms.attributes")
public class EDMSAttributeProperties {
    private String documentTypeGuid;
    private String documentNumberGuid;
    private String documentDateGuid;
    private String customerNumberGuid;
    private String customerIdentifierGuid;
    private String signedGuid;
}
