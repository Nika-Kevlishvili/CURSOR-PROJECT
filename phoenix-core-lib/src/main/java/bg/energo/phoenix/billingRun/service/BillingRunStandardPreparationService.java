package bg.energo.phoenix.billingRun.service;

import bg.energo.phoenix.model.entity.billing.billingRun.BillingRun;
import bg.energo.phoenix.model.enums.billing.billings.BillingRunDataPreparationStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.CallableStatement;

@Slf4j
@Service
@RequiredArgsConstructor
public class BillingRunStandardPreparationService {
    @PersistenceContext
    private final EntityManager entityManager;

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void startDataPreparation(BillingRun billingRun) {
        try (Session session = entityManager.unwrap(Session.class)) {
            session.doWork((work) -> {
                Long runId = billingRun.getId();
                MDC.put("billingId", String.valueOf(runId));
                log.debug("Starting data preparation for billing run with id: [%s]".formatted(runId));
                CallableStatement statement = work.prepareCall("CALL billing_run.run_standard_billing_main_data_preparation(?)");
                statement.setLong(1, runId);
                log.debug("Procedure call was successful;");
                statement.execute();

                billingRun.setMainDataPreparationStatus(BillingRunDataPreparationStatus.RUNNING);
            });
        } catch (Exception e) {
            log.error("Exception handled while preparing billing run data", e);
        }
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void startDataPreparationCorrection(BillingRun billingRun) {
        try (Session session = entityManager.unwrap(Session.class)) {
            session.doWork((work) -> {
                Long runId = billingRun.getId();
                MDC.put("billingId", String.valueOf(runId));
                log.debug("Starting correction data preparation for billing run with id: [%s]".formatted(runId));
                CallableStatement statement = work.prepareCall("CALL billing_run.run_standard_billing_main_data_preparation_correction(?)");
                statement.setLong(1, runId);
                log.debug("Procedure call was successful;");
                statement.execute();

                billingRun.setMainDataPreparationStatus(BillingRunDataPreparationStatus.RUNNING);
            });
        } catch (Exception e) {
            log.error("Exception handled while preparing billing run data", e);
        }
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void startInterimAdvancePaymentPreparation(BillingRun billingRun) {
        MDC.put("billingId", String.valueOf(billingRun.getId()));

        try (Session session = entityManager.unwrap(Session.class)) {
            session.doWork((work) -> {
                Long runId = billingRun.getId();
                MDC.put("billingId", String.valueOf(runId));
                log.debug("Starting data preparation for billing run with id: [%s]".formatted(runId));
                CallableStatement statement = work.prepareCall("CALL billing_run.run_standard_billing_interim_data_preparation(?)");
                log.debug("Procedure call was successful for interim;");
                statement.setLong(1, runId);

                statement.execute();
            });
        } catch (Exception e) {
            log.error("Exception handled while preparing billing run data for interim", e);
        }
    }
}
