package bg.energo.phoenix.model.entity.receivable;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Types;

@Service
@Slf4j
@RequiredArgsConstructor
public class AutomaticOffsettingService {

    private final EntityManager entityManager;

    public void offsetOfLiabilityAndReceivable(Long receivableId, Long liabilityId, String userId, String modifyUserId) {
        try (Session session = entityManager.unwrap(Session.class)) {
            session.doWork(connection -> {
                CallableStatement callableStatement = connection.prepareCall("CALL receivable.automatic_liability_offsetting(?,?,?,?,?)");

                if (receivableId == null) {
                    callableStatement.setNull(1, Types.BIGINT);
                } else {
                    callableStatement.setLong(1, receivableId);
                }

                if (liabilityId == null) {
                    callableStatement.setNull(2, Types.BIGINT);
                } else {
                    callableStatement.setLong(2, liabilityId);
                }

                callableStatement.setString(3, userId);
                callableStatement.setString(4, modifyUserId);

                callableStatement.registerOutParameter(5, Types.VARCHAR); // o_message

                callableStatement.execute();

                String message = callableStatement.getString(5);
                if (!message.equals("OK")) {
                    log.error("Error happened in automatic offsetting message is [%s]".formatted(message));
                }

            });
        } catch (Exception exception) {
            log.error("Some exception happened in automatic_liability_offsetting procedure %s".formatted(exception.getMessage()));
        }
    }

    public BigDecimal offsetOfPayments(Long paymentId, String userId, String modifyUserId) {
        final BigDecimal[] paymentCurrentAmount = new BigDecimal[1];
        try (Session session = entityManager.unwrap(Session.class)) {
            session.doWork(connection -> {
                CallableStatement callableStatement = connection.prepareCall("CALL receivable.automatic_payment_offsetting_out(?,?,?,?,?)");

                if (paymentId == null) {
                    callableStatement.setNull(1, Types.BIGINT);
                } else {
                    callableStatement.setLong(1, paymentId);
                }

                callableStatement.setString(2, userId);
                callableStatement.setString(3, modifyUserId);

                callableStatement.registerOutParameter(4, Types.VARCHAR); // o_message
                callableStatement.registerOutParameter(5, Types.NUMERIC); // o_payment_amount

                callableStatement.execute();

                String message = callableStatement.getString(4);
                if (!message.equals("OK")) {
                    log.error("Error happened in payment offsetting message is [%s]".formatted(message));
                }

                paymentCurrentAmount[0] = callableStatement.getBigDecimal(5);
            });
        } catch (Exception exception) {
            log.error("Some exception happened in automatic_payment_offsetting procedure %s".formatted(exception.getMessage()));
        }
        return paymentCurrentAmount[0];
    }
}
