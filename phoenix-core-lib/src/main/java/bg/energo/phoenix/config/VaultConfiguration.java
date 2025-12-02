package bg.energo.phoenix.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
public class VaultConfiguration {

    @Value("${apis.api.token}")
    private String apisApiToken;

}