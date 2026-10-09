package bg.energo.phoenix;

import bg.energo.phoenix.service.signing.qes.QesSigningService;
import bg.energo.phoenix.service.signing.qes.QesSocketSender;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.scheduling.annotation.EnableAsync;

@ComponentScan(
        basePackages = {"bg.energo.phoenix"},
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                classes = {QesSigningService.class, QesSocketSender.class}
        )
)
@SpringBootApplication(exclude = {RedisRepositoriesAutoConfiguration.class})
@EnableAsync
public class BillingApplication {
    public static void main(String[] args) {
        SpringApplication.run(BillingApplication.class, args);
    }
}
