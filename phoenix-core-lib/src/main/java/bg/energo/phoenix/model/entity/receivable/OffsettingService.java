package bg.energo.phoenix.model.entity.receivable;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.CallableStatement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class OffsettingService {

    private final EntityManager entityManager;

    @Transactional
    public void reverseOffsetting(Long offsettingId, LocalDateTime reversalDate) {
        try (Session session = entityManager.unwrap(Session.class)) {
            session.doWork(connection -> {
                CallableStatement callableStatement = connection.prepareCall("CALL receivable.reverse_offsetting(?,?,?,?,?)");
                callableStatement.setLong(1, offsettingId);
                callableStatement.setTimestamp(2, Timestamp.valueOf(reversalDate));
                callableStatement.setString(3, "system");
                callableStatement.setString(4, "system");

                callableStatement.registerOutParameter(5, Types.VARCHAR); // o_message
                callableStatement.execute();

                String message = callableStatement.getString(5);
                if (!message.equals("OK")) {
                    log.error("Error happened in reverse offsetting message is [%s]".formatted(message));
                }
            });
        } catch (Exception exception) {
            log.error("Some exception happened in automatic_payment_offsetting procedure %s".formatted(exception.getMessage()));
        }
    }
}
