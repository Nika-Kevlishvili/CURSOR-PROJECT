package bg.energo.phoenix.model.entity.receivable;

import bg.energo.phoenix.model.enums.receivable.DirectOffsettingSourceType;
import bg.energo.phoenix.model.enums.receivable.OperationContext;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Date;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * <p><b>Vato's</b></p>
 * Service responsible for handling the direct offsetting of receivables in the system.
 * This service directly interacts with the database to perform the offsetting
 * operation using a stored procedure call.
 * <p>
 * The `directReceivableOffsetting` method facilitates mapping a specific receivable
 * against a source with a specified amount, currency, and offsetting date, while maintaining
 * audit details and operation context. The operation results in the execution of the
 * referenced stored procedure and manages any potential errors during execution.
 * <p>
 * Dependencies:
 * - EntityManager: Manages database interactions required for unwrapping a Hibernate session
 * and performing database work.
 * <p>
 * Logging:
 * - Logs informational messages in the state of the operation and any encountered errors.
 * <p>
 * Thread Safety:
 * - This class is marked as a Spring service (`@Service`) and relies on dependency injection
 * to instantiate the required objects. It is expected to be thread-safe as the main state
 * manipulation happens in a transactional scope.
 */
@RequiredArgsConstructor
@Slf4j
@Service
public class DirectReceivableOffsettingService {
    private final EntityManager entityManager;

    public void directReceivableOffsetting(
            DirectOffsettingSourceType sourceType,
            Long sourceId,
            Long receivableId,
            String systemUserId,
            String modifySystemUserId,
            BigDecimal amount,
            Long amountCurrencyId,
            LocalDateTime offsetingDate,
            OperationContext operationContext) {

        try (Session session = entityManager.unwrap(Session.class)) {
            session.doWork(connection -> {
                CallableStatement callableStatement = connection.prepareCall(
                        "CALL receivable.direct_receivable_offsetting(" +
                                "CAST(? AS VARCHAR), " +
                                "CAST(? AS BIGINT), " +
                                "CAST(? AS BIGINT), " +
                                "CAST(? AS VARCHAR), " +
                                "CAST(? AS VARCHAR), " +
                                "CAST(? AS BIGINT), " +
                                "CAST(? AS VARCHAR), " +
                                "CAST(? AS NUMERIC), " +
                                "CAST(? AS INTEGER), " +
                                "CAST(? AS DATE)," +
                                "CAST(? AS VARCHAR))"
                );

                callableStatement.setString(1, sourceType.name());
                callableStatement.setLong(2, sourceId);
                callableStatement.setLong(3, receivableId);
                callableStatement.setString(4, systemUserId);
                callableStatement.setString(5, modifySystemUserId);

                callableStatement.registerOutParameter(6, Types.BIGINT);
                callableStatement.registerOutParameter(7, Types.VARCHAR);

                if (amount != null) {
                    callableStatement.setBigDecimal(8, amount);
                } else {
                    callableStatement.setNull(8, Types.NUMERIC);
                }

                if (amountCurrencyId != null) {
                    callableStatement.setLong(9, amountCurrencyId);
                } else {
                    callableStatement.setNull(9, Types.INTEGER);
                }

                if (offsetingDate != null) {
                    callableStatement.setDate(10, Date.valueOf(offsetingDate.toLocalDate()));
                } else {
                    callableStatement.setDate(10, Date.valueOf(LocalDate.now()));
                }

                if (operationContext != null) {
                    callableStatement.setString(11, operationContext.name());
                } else {
                    callableStatement.setNull(11, Types.VARCHAR);
                }

                callableStatement.execute();

                String message = callableStatement.getString(7);
                if (StringUtils.hasText(message) && !message.equals("OK")) {
                    log.error("Error in direct receivable offsetting: {}", message);
                    throw new RuntimeException("Direct receivable offsetting failed: " + message);
                }

                Long transactionId = callableStatement.getLong(6);
                log.debug("Receivable offsetting completed. Transaction ID: {}", transactionId);
            });
        }
    }
}
