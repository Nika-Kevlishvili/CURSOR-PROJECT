package bg.energo.scheduler.schedulers;

import bg.energo.phoenix.model.customAnotations.aspects.ExecutionTimeLogger;
import bg.energo.phoenix.process.model.entity.Process;
import bg.energo.phoenix.process.model.enums.ProcessStatus;
import bg.energo.phoenix.process.model.enums.ProcessType;
import bg.energo.phoenix.process.repository.ProcessRepository;
import bg.energo.phoenix.service.xEnergie.jobs.service.XEnergieDealDatesUpdaterService;
import bg.energo.phoenix.service.xEnergie.jobs.service.XEnergieSplitUpdaterService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Slf4j
@Service
@Profile({"dev", "test","preProd", "prod"})
@RequiredArgsConstructor
@DependsOn({"XEnergieSchedulerErrorHandler"})
@ConditionalOnExpression("${app.cfg.schedulers.enabled:true}")
public class XEnergieSynchronizationScheduler {
    private final XEnergieDealDatesUpdaterService xEnergieDealDatesUpdaterService;
    private final ProcessRepository processRepository;

    @Scheduled(cron = "${xEnergie.jobs.cron}")
    @ExecutionTimeLogger(value = "XEnergieSynchronizationScheduler")
    public void work() {
        Process process = createProcess();

        xEnergieDealDatesUpdaterService.execute(process);
    }

    private Process createProcess() {
        Process process = Process
                .builder()
                .name("XEnergie Report - %s".formatted(LocalDate.now()))
                .status(ProcessStatus.IN_PROGRESS)
                .type(ProcessType.X_ENERGIE_EXCEPTION_REPORT)
                .processStartDate(LocalDateTime.now())
                .build();

        return processRepository.save(process);
    }
}
