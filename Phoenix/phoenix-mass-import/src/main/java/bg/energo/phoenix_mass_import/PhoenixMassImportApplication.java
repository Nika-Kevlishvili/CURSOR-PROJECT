package bg.energo.phoenix_mass_import;

import bg.energo.phoenix.service.signing.qes.QesSigningService;
import bg.energo.phoenix.service.signing.qes.QesSocketSender;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(exclude = {RedisRepositoriesAutoConfiguration.class})
@ComponentScan(basePackages = {"bg.energo.phoenix", "bg.energo.phoenix_mass_import"}, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {QesSigningService.class, QesSocketSender.class}))
@EntityScan(basePackages = {"bg.energo.phoenix"})
@EnableJpaRepositories(basePackages = "bg.energo.phoenix")
@EnableRabbit
@EnableJpaAuditing
public class PhoenixMassImportApplication {


    public static void main(String[] args) {
        SpringApplication.run(PhoenixMassImportApplication.class, args);
    }

}
