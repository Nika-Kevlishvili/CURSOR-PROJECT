package bg.energo.phoenix.model.entity.receivable;

import bg.energo.phoenix.model.ReverseReschedulingOffsettingResult;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class ReverseReschedulingOffsettingService {
    private final EntityManager entityManager;

    /**
     * Gets data for reversing a rescheduling offsetting
     *
     * @param reschedulingId the ID of the rescheduling
     * @param receivableId the ID of the receivable
     * @param liabilityId the ID of the liability
     * @param reversalDate the date of the reversal
     * @return ReverseReschedulingOffsettingResult containing offsetting amount and currency ID
     */
    public ReverseReschedulingOffsettingResult getReversalData(
            Long reschedulingId,
            Long receivableId,
            Long liabilityId,
            LocalDateTime reversalDate) {

        try (Session session = entityManager.unwrap(Session.class)) {
            final ReverseReschedulingOffsettingResult result = new ReverseReschedulingOffsettingResult();

            session.doWork(connection -> {
                CallableStatement callableStatement = connection.prepareCall(
                        "CALL receivable.get_reverse_rescheduling_offsetting_data(" +
                                "CAST(? AS BIGINT), " +
                                "CAST(? AS BIGINT), " +
                                "CAST(? AS BIGINT), " +
                                "CAST(? AS DATE), " +
                                "CAST(? AS NUMERIC), " +
                                "CAST(? AS INTEGER), " +
                                "CAST(? AS VARCHAR))"
                );

                callableStatement.setLong(1, reschedulingId);
                callableStatement.setLong(2, receivableId);
                callableStatement.setLong(3, liabilityId);

                if (reversalDate != null) {
                    callableStatement.setDate(4, Date.valueOf(reversalDate.toLocalDate()));
                } else {
                    callableStatement.setDate(4,Date.valueOf(LocalDate.now()));
                }

                callableStatement.registerOutParameter(5, Types.NUMERIC);
                callableStatement.registerOutParameter(6, Types.INTEGER);
                callableStatement.registerOutParameter(7, Types.VARCHAR);

                callableStatement.execute();

                BigDecimal offsettingAmount = callableStatement.getBigDecimal(5);
                Integer offsettingCurrencyId = callableStatement.getInt(6);
                String message = callableStatement.getString(7);

                result.setOffsettingAmount(offsettingAmount);
                result.setOffsettingCurrencyId(offsettingCurrencyId);
                result.setMessage(message);

                if (StringUtils.hasText(message) && !"OK".equals(message)) {
                    log.error("Error in get reverse rescheduling offsetting data: {}", message);
                    throw new RuntimeException("Get reverse rescheduling offsetting data failed: " + message);
                }

                log.debug("Successfully retrieved reverse rescheduling offsetting data for rescheduling ID: {}", reschedulingId);
            });

            return result;
        }
    }
}
