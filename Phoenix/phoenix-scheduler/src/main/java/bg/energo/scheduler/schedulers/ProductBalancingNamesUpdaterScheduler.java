package bg.energo.scheduler.schedulers;

import bg.energo.phoenix.model.customAnotations.aspects.ExecutionTimeLogger;
import bg.energo.phoenix.service.product.product.ProductBalancingNamesUpdaterService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.Executors;

@Service
@Profile({"dev","test","preProd", "prod"})
@RequiredArgsConstructor
@ConditionalOnExpression("${app.cfg.schedulers.enabled:true}")
public class ProductBalancingNamesUpdaterScheduler {
    private final ProductBalancingNamesUpdaterService productBalancingNamesUpdaterService;



    @Scheduled(cron = "${product.balancing.name.updater.cron}")
    @ExecutionTimeLogger(value = "ProductBalancingNamesUpdaterScheduler")
    public void updateProductBalancingNames() {
        productBalancingNamesUpdaterService.updateBalancingProducts();
        productBalancingNamesUpdaterService.updateBalancingProfiles();
    }
}
