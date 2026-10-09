package bg.energo.phoenix.config;

import bg.energo.phoenix.service.signing.qes.QesSigningService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WebSocketConfig {
    @Bean
    public QesSigningService qesSigningService() {
        // Avoid creating this bean by returning null or not registering it
        return null;  // This will exclude the bean from the Spring context
    }
}