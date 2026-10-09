package bg.energo.phoenix;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(exclude = {RedisRepositoriesAutoConfiguration.class})
@EnableJpaAuditing
@EnableScheduling
@EnableCaching
@EnableAsync
public class PhoenixPaymentApiApplication {

    public static void main(String[] args) {
        try {
            SpringApplication.run(PhoenixPaymentApiApplication.class, args);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
