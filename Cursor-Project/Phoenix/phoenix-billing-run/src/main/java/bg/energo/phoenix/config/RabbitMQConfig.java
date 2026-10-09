package bg.energo.phoenix.config;

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * NOTE: this class deliberately carries the same fully-qualified name as phoenix-core-lib's
 * {@code bg.energo.phoenix.config.RabbitMQConfig}. Application classes take precedence over
 * {@code BOOT-INF/lib} jars, so core-lib's copy never loads here and every bean it declares must be
 * repeated below, or core-lib components that inject them fail context startup.
 */
@EnableRabbit
@Configuration
public class RabbitMQConfig {
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
