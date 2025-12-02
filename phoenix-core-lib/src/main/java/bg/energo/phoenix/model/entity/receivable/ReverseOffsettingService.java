package bg.energo.phoenix.model.entity.receivable;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.sql.CallableStatement;
import java.sql.Date;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReverseOffsettingService {
    private final EntityManager entityManager;


    public void reverseOffsetting(
            Long offsettingId,
            LocalDateTime reversalDate,
            String systemUserId,
            String modifySystemUserId) {

        try (Session session = entityManager.unwrap(Session.class)) {
            session.doWork(connection -> {
                CallableStatement callableStatement = connection.prepareCall(
                        "CALL receivable.reverse_offsetting(" +
                                "CAST(? AS BIGINT), " +
                                "CAST(? AS DATE), " +
                                "CAST(? AS VARCHAR), " +
                                "CAST(? AS VARCHAR), " +
                                "CAST(? AS VARCHAR))"
                );

                callableStatement.setLong(1, offsettingId);

                if (reversalDate != null) {
                    callableStatement.setDate(2, Date.valueOf(reversalDate.toLocalDate()));
                } else {
                    callableStatement.setDate(2,Date.valueOf(LocalDate.now()));
                }

                callableStatement.setString(3, systemUserId);
                callableStatement.setString(4, modifySystemUserId);

                callableStatement.registerOutParameter(5, Types.VARCHAR);

                callableStatement.execute();

                String message = callableStatement.getString(5);
                if (StringUtils.hasText(message) && !message.equals("OK")) {
                    log.error("Error in reverse offsetting: {}", message);
                    throw new RuntimeException("Reverse offsetting failed: " + message);
                }

                log.debug("Offsetting reversal completed successfully for offsetting ID: {}", offsettingId);
            });
        }
    }
}
