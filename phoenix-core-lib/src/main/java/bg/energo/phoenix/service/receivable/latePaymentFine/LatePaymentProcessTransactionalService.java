package bg.energo.phoenix.service.receivable.latePaymentFine;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.model.entity.receivable.customerLiability.CustomerLiability;
import bg.energo.phoenix.model.enums.receivable.latePaymentFine.LatePaymentFineOutDocType;
import bg.energo.phoenix.model.process.latePaymentFIne.ResultItemDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LatePaymentProcessTransactionalService {
    private final LatePaymentFineService latePaymentFineService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processLiability(Long id, BigDecimal amount, List<ResultItemDTO> results) {
        try {
            CustomerLiability customerLiability = latePaymentFineService.getCustomerLiability(id);
            latePaymentFineService.createLatePaymentFineAndLiability(
                    customerLiability,
                    amount,
                    new ArrayList<>(),
                    results,
                    LatePaymentFineOutDocType.LATE_PAYMENT_FINE_JOB,
                    LocalDate.now(),
                    null
            );
        } catch (Exception e) {
            log.error("Error processing liability ID : {}", e.getMessage(), e);
            throw new ClientException(e.getMessage(), ErrorCode.APPLICATION_ERROR);
        }
    }

}
