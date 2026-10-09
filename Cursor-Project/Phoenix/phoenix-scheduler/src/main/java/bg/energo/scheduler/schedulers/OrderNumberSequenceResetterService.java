package bg.energo.scheduler.schedulers;

import bg.energo.phoenix.model.customAnotations.aspects.ExecutionTimeLogger;
import bg.energo.phoenix.util.order.OrderUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Profile({"dev", "test","preProd", "prod"})
@RequiredArgsConstructor
@ConditionalOnExpression("${app.cfg.schedulers.enabled:true}")
public class OrderNumberSequenceResetterService {
    private final OrderUtils orderUtils;

    @Scheduled(cron = "${order.number.sequence.resetter.cron}")
    @ExecutionTimeLogger(value = "OrderNumberSequenceResetterService")
    public void resetOrderNumberSequence() {
        try {
            log.debug("Starting resetting order number sequence");
            orderUtils.resetOrderNumberSequence();
        } catch (Exception e) {
            log.error("Error while resetting order number sequence.", e);
        }
    }

}
