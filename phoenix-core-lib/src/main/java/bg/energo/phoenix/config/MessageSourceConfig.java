package bg.energo.phoenix.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;

@Configuration
public class MessageSourceConfig {

    @Bean
    public ResourceBundleMessageSource messageSource() {

        ResourceBundleMessageSource source = new ResourceBundleMessageSource();
        source.setBasenames("lang/errors/errors");
        source.setDefaultEncoding("UTF-8");

        return source;
    }
}
