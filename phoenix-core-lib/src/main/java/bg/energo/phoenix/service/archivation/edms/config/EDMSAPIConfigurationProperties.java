package bg.energo.phoenix.service.archivation.edms.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "edms.endpoints")
public class EDMSAPIConfigurationProperties {
    private String url;
    private String login;
    private String createDocument;
    private String deleteDocument;
    private String setAttributes;
    private String uploadFiles;
    private String publishDocument;
    private String downloadFiles;
    private String logOut;
}
