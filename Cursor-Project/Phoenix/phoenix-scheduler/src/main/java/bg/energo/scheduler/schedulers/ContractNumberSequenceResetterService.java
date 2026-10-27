package bg.energo.scheduler.schedulers;

import bg.energo.phoenix.model.customAnotations.aspects.ExecutionTimeLogger;
import bg.energo.phoenix.util.contract.ContractUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Profile({"dev", "test","preProd", "prod"})
@ConditionalOnExpression("${app.cfg.schedulers.enabled:true}")
@RequiredArgsConstructor
public class ContractNumberSequenceResetterService {
    private final ContractUtils contractUtils;

    @Scheduled(cron = "${contract.number.sequence.resetter.cron}")
    @ExecutionTimeLogger(value = "ContractNumberSequenceResetterService")
    public void resetContractNumberSequence() {
        try {
            log.debug("Starting resetting contract number sequence");
            contractUtils.resetContractNumberSequence();
        } catch (Exception e) {
            log.error("Error while resetting contract number sequence.", e);
        }
    }

}
