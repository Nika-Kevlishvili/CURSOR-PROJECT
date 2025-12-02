package bg.energo.phoenix.service.notifications;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Configuration
public class NotificationSourceConfig {
    @Bean(value = "notificationSource")
    public ResourceBundleMessageSource notificationSource() {
        ResourceBundleMessageSource source = new ResourceBundleMessageSource();
        source.setDefaultEncoding(StandardCharsets.UTF_8.name());
        source.setDefaultLocale(Locale.ENGLISH);
        source.setBasenames("notifications/notification");
        return source;
    }
}
