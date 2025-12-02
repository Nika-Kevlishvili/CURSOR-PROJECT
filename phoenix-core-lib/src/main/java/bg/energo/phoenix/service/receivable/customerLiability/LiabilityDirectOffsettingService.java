package bg.energo.phoenix.service.receivable.customerLiability;

import bg.energo.phoenix.model.enums.receivable.DirectOffsettingSourceType;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Date;
import java.sql.Types;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicReference;

@Service
@Slf4j
@RequiredArgsConstructor
public class LiabilityDirectOffsettingService {

    private final EntityManager entityManager;

    @Transactional(propagation = Propagation.REQUIRED)
    public Long directOffsetting(DirectOffsettingSourceType sourceType, Long sourceId, Long liabilityId,
                                 String userId, String modifyUserId,String operationContext,BigDecimal amount,Long currencyId) {
        AtomicReference<Long> transactionId = new AtomicReference<>();
        try (Session session = entityManager.unwrap(Session.class)) {
            session.doWork(connection -> {
                CallableStatement callableStatement = connection.prepareCall(
                        "CALL receivable.direct_liability_offsetting(" +
                                "CAST(? AS VARCHAR), " +
                                "CAST(? AS BIGINT), " +
                                "CAST(? AS BIGINT), " +
                                "CAST(? AS VARCHAR), " +
                                "CAST(? AS VARCHAR), " +
                                "CAST(? AS BIGINT), " +
                                "CAST(? as VARCHAR), " +
                                "CAST(? AS NUMERIC), " +
                                "CAST(? AS INTEGER), " +
                                "CAST(? AS DATE)," +
                                "CAST(? AS VARCHAR))"
                );
                callableStatement.setString(1, sourceType.name());
                callableStatement.setLong(2, sourceId);
                callableStatement.setLong(3, liabilityId);
                callableStatement.setString(4, userId);
                callableStatement.setString(5, modifyUserId);
                callableStatement.registerOutParameter(6, Types.BIGINT);
                callableStatement.registerOutParameter(7, Types.VARCHAR);
                if(amount!=null) {
                    callableStatement.setBigDecimal(8,amount);
                } else {
                    callableStatement.setNull(8, Types.NUMERIC);
                }
                if(currencyId!=null) {
                    callableStatement.setLong(9,currencyId);
                } else {
                    callableStatement.setNull(9, Types.INTEGER);
                }
                callableStatement.setDate(10, Date.valueOf(LocalDate.now()));
                if(operationContext!=null) {
                    callableStatement.setString(11, operationContext);
                } else {
                    callableStatement.setNull(11,Types.VARCHAR);
                }

                callableStatement.execute();
                String message = callableStatement.getString(7);

                transactionId.set(callableStatement.getLong(6));

                if (!message.equals("OK")) {
                    log.error("Error happened in automatic offsetting message is [%s]".formatted(message));
                    throw new RuntimeException("Direct liability offsetting failed: " + message);
                }
            });
        } catch (Exception e) {
            log.info(e.getMessage());
            throw new RuntimeException("Direct liability offsetting failed : " + e.getMessage());
        }
        return transactionId.get();
    }
}