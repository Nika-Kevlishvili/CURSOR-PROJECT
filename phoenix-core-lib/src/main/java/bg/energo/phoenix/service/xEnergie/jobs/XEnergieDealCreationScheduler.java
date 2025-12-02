package bg.energo.phoenix.service.xEnergie.jobs;//package bg.energo.phoenix.service.xEnergie.jobs;
//
//import bg.energo.phoenix.service.xEnergie.jobs.service.XEnergieDealCreationService;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
//import org.springframework.context.annotation.DependsOn;
//import org.springframework.context.annotation.Profile;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Service;
//
//import jakarta.annotation.PostConstruct;
//
//@Slf4j
//@Service
//@Profile({"dev","test"})
//@RequiredArgsConstructor
//@DependsOn({"XEnergieSchedulerErrorHandler"})
//@ConditionalOnExpression("${app.cfg.schedulers.enabled:true}")
//public class XEnergieDealCreationScheduler {
//    private final XEnergieDealCreationService xEnergieDealCreationService;
//
//    @PostConstruct
//    private void init() {
//        new Thread(this::work).start();
//    }
//
//    @Scheduled(cron = "${xEnergie.jobs.cron}")
//    public void work() {
//        xEnergieDealCreationService.execute();
//    }
//}
