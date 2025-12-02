package bg.energo.phoenix.service.receivable.latePaymentFine;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.model.process.latePaymentFIne.InterestCalculationResponseDTO;
import bg.energo.phoenix.model.process.latePaymentFIne.ResultItemDTO;
import bg.energo.phoenix.repository.receivable.customerLiability.CustomerLiabilityRepository;
import bg.energo.phoenix.repository.receivable.latePaymentFine.LatePaymentFineRepository;
import bg.energo.phoenix.service.document.LatePaymentFineDocumentCreationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
//@Profile({"dev", "test"})
//@ConditionalOnExpression("${app.cfg.schedulers.enabled:true}")
public class LatePaymentFineJob {
    private final LatePaymentFineService latePaymentFineService;
    private final CustomerLiabilityRepository customerLiabilityRepository;
    private final ObjectMapper objectMapper;
    private final LatePaymentProcessTransactionalService latePaymentProcessTransactionalService;
    private final LatePaymentFineRepository latePaymentFineRepository;
    private final LatePaymentFineDocumentCreationService latePaymentFineDocumentCreationService;

    //    @Scheduled(cron = "${late-payment-fine.job.cron}")
    @Transactional
    public Integer latePaymentFineJob() {
        try {
            log.debug("Starting Late Payment Job");
            CompletableFuture<Void> latePaymentFineCreationJob = CompletableFuture.runAsync(() ->
                    latePaymentFineProcessJob(LocalDate.now())
            );

            latePaymentFineCreationJob.thenRun(() -> {
                log.debug("Starting Late Payment Email creation Job");
                latePaymentFIneEmailSenderJob(LocalDate.now());
            }).join();

        } catch (Exception e) {
            log.error("Exception handled while trying to start late payment fine job");
        }
        return 1;
    }

    /**
     * Processes late payment fines for all eligible liabilities.
     *
     * @param date The date to process fines for
     * @throws ClientException if processing fails
     */
    @Transactional
    public void latePaymentFineProcessJob(LocalDate date) {
        List<Object[]> liabilitiesToFine = customerLiabilityRepository.getLiabilitiesToFine(null);

        for (Object[] item : liabilitiesToFine) {
            try {
                Long id = (Long) item[0];
                String lfpJson = (String) item[1];

                if (lfpJson != null && !lfpJson.isEmpty()) {
                    InterestCalculationResponseDTO interestCalc = objectMapper.readValue(lfpJson, InterestCalculationResponseDTO.class);
                    BigDecimal amount = interestCalc.getCalculatedInterest();
                    List<ResultItemDTO> results = interestCalc.getResults();

                    if (id != null && amount != null && amount.compareTo(BigDecimal.ZERO) > 0) {
                        latePaymentProcessTransactionalService.processLiability(id, amount, results);
                    }
                }
            } catch (Exception e) {
                log.error("Error processing liability ID : {}", e.getMessage(), e);
                throw new ClientException(e.getMessage(), ErrorCode.APPLICATION_ERROR);
            }
        }
    }

    /**
     * Sends emails for late payment fines created on the specified date.
     * <p>
     * This method retrieves a list of late payment fine IDs created on the provided date
     * and attempts to generate a document and send an email for each. If an error occurs
     * during the process, a {@code ClientException} is thrown.
     * <p>
     * The method is annotated with {@code @Transactional} to ensure that the operations
     * are executed within a transactional context.
     *
     * @param date the date for which late payment fine emails need to be sent
     * @throws ClientException if an error occurs during the document generation or email sending process
     */
    @Transactional
    public void latePaymentFIneEmailSenderJob(LocalDate date) {
        List<Long> latePaymentFinesByCreateDate = latePaymentFineRepository.getLatePaymentFinesByCreateDate(date);

        for (Long latePaymentFineId : latePaymentFinesByCreateDate) {
            try {
                latePaymentFineDocumentCreationService.generateDocumentAndSendEmail(latePaymentFineId);
            } catch (Exception e) {
                log.error("Error while generating email : {}", e.getMessage(), e);
                throw new ClientException(e.getMessage(), ErrorCode.APPLICATION_ERROR);
            }
        }

    }

}
