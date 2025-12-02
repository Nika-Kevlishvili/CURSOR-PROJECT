package bg.energo.phoenix.service.receivable.customerReceivables;

import bg.energo.phoenix.model.enums.receivable.DirectOffsettingSourceType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.StoredProcedureQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * <p><b>Tate's</b></p>
 * Service class responsible for handling the direct offsetting of receivables.
 * Executes a stored procedure to perform the offsetting process with provided parameters.
 * <p>
 * This service uses an {@link EntityManager} to create and execute a stored procedure
 * for direct offsetting. If the procedure execution fails or returns an error,
 * appropriate logging and exception handling are triggered.
 * <p>
 * Methods in this class are transactional and ensure that database operations
 * are handled within the required transaction propagation scope.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ReceivableDirectOffsettingService {

    private final EntityManager entityManager;

    @Transactional(propagation = Propagation.REQUIRED)
    public void directOffsetting(
            DirectOffsettingSourceType sourceType,
            Long sourceId,
            Long receivableId,
            String userId,
            String modifyUserId,
            String operationContext
    ) {
        try {
            StoredProcedureQuery query = entityManager.createStoredProcedureQuery("receivable.direct_receivable_offsetting");

            query.registerStoredProcedureParameter(1, String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter(2, Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter(3, Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter(4, String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter(5, String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter(6, Long.class, ParameterMode.OUT);
            query.registerStoredProcedureParameter(7, String.class, ParameterMode.OUT);
            query.registerStoredProcedureParameter(8, BigDecimal.class, ParameterMode.IN);
            query.registerStoredProcedureParameter(9, Integer.class, ParameterMode.IN);
            query.registerStoredProcedureParameter(10, LocalDateTime.class, ParameterMode.IN);
            query.registerStoredProcedureParameter(11, String.class, ParameterMode.IN);

            query.setParameter(1, sourceType.name());
            query.setParameter(2, sourceId);
            query.setParameter(3, receivableId);
            query.setParameter(4, userId);
            query.setParameter(5, modifyUserId);
            query.setParameter(8, null);
            query.setParameter(9, null);
            query.setParameter(10, LocalDateTime.now());
            query.setParameter(11, operationContext);

            query.execute();

            String message = (String) query.getOutputParameterValue(7);

            if (!"OK".equals(message)) {
                log.error("Error happened in automatic offsetting message is [%s]".formatted(message));
                throw new RuntimeException("Direct receivable offsetting failed: " + message);
            }

        } catch (Exception e) {
            log.info(e.getMessage());
            throw new RuntimeException("Direct receivable offsetting failed : " + e.getMessage());
        }
    }
}