package bg.energo.scheduler.schedulers;

import bg.energo.phoenix.model.customAnotations.aspects.ExecutionTimeLogger;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.CallableStatement;

@Slf4j
@Component
@RequiredArgsConstructor
@Profile({"dev", "test","preProd", "prod"})
@ConditionalOnExpression("${app.cfg.schedulers.enabled:true}")
public class InvoiceCleanerService {
    @PersistenceContext
    private final EntityManager entityManager;

    @Transactional
    @Scheduled(cron = "${invoices.cleaner.cron}")
    @ExecutionTimeLogger(value = "InvoiceCleanerService")
    public void cleanOutdatedInvoices() {
        try (Session session = entityManager.unwrap(Session.class)) {
            session.doWork((work) -> {
                CallableStatement statement = work.prepareCall("call invoice.clean_outdated_invoices()");
                statement.execute();
            });
        } catch (Exception e) {
            log.error("Error occurred while cleaning outdated invoices", e);
        }
    }
}
