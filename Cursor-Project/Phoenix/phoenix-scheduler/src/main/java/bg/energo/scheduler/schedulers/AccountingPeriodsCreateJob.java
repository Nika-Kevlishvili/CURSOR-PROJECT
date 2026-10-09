package bg.energo.scheduler.schedulers;

import bg.energo.phoenix.model.customAnotations.aspects.ExecutionTimeLogger;
import bg.energo.phoenix.model.entity.billing.accountingPeriod.AccountingPeriods;
import bg.energo.phoenix.model.enums.billing.accountingsPeriods.AccountPeriodFileGenerationStatus;
import bg.energo.phoenix.service.billing.accountingPeriods.AccountingPeriodService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.YearMonth;

import static bg.energo.phoenix.model.enums.billing.accountingsPeriods.AccountingPeriodStatus.OPEN;

@Slf4j
@Service
@RequiredArgsConstructor
@Profile({"dev", "test","preProd", "prod"})
@ConditionalOnExpression("${app.cfg.schedulers.enabled:true}")
public class AccountingPeriodsCreateJob {
    private final AccountingPeriodService accountingPeriodService;

    @Scheduled(cron = "${accounting.period.creation.cron}")
    @ExecutionTimeLogger(value = "AccountingPeriodsCreateJob")
    public void work() {
        try {
            log.debug("Accounting Period Adding");
            YearMonth currentYearMonth = YearMonth.now();
            LocalDateTime startOfMonth = currentYearMonth.atDay(1).atStartOfDay();
            LocalDateTime endOfMonth = currentYearMonth.atEndOfMonth().atTime(23, 59, 59);
            String name = String.format("ACCOUNTING%s%s", startOfMonth.getYear(), String.format("%02d", startOfMonth.getMonthValue()));

            AccountingPeriods accountingPeriods = new AccountingPeriods();
            accountingPeriods.setName(name);
            accountingPeriods.setStartDate(startOfMonth);
            accountingPeriods.setEndDate(endOfMonth);
            accountingPeriods.setStatus(OPEN);
            accountingPeriods.setModifyDate(startOfMonth);
            accountingPeriods.setFileGenerationStatus(AccountPeriodFileGenerationStatus.INITIAL);
            accountingPeriodService.create(accountingPeriods);
        } catch (Exception e) {
            log.error("Error while adding accounting period.", e);
        }
    }
}
