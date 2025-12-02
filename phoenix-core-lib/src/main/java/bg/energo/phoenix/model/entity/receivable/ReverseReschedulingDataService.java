package bg.energo.phoenix.model.entity.receivable;

import jakarta.persistence.EntityManager;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.sql.Array;
import java.sql.CallableStatement;
import java.sql.Date;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReverseReschedulingDataService {
    private final EntityManager entityManager;

    @Getter
    @Setter
    @AllArgsConstructor
    public static class ReverseReschedulingResult {
        private Long[] oldLiabilities;
        private Long[] restoredReceivables;
        private Long[] installmentLiabilities;
    }

    public ReverseReschedulingResult getReverseReschedulingData(
            Long reschedulingId,
            LocalDateTime reversalDate,
            String systemUserId,
            String modifySystemUserId
    ) {
        try (Session session = entityManager.unwrap(Session.class)) {
            AtomicReference<ReverseReschedulingResult> result = new AtomicReference<>();

            session.doWork(connection -> {
                CallableStatement callableStatement = connection.prepareCall(
                        "CALL receivable.get_reverse_rescheduling_data(" +
                                "CAST(? AS BIGINT), " +
                                "CAST(? AS DATE), " +
                                "CAST(? AS VARCHAR), " +
                                "CAST(? AS VARCHAR), " +
                                "CAST(? AS BIGINT[]), " +
                                "CAST(? AS BIGINT[]), " +
                                "CAST(? AS BIGINT[]), " +
                                "CAST(? AS VARCHAR))"
                );

                callableStatement.setLong(1, reschedulingId);
                callableStatement.setDate(2, Date.valueOf(reversalDate.toLocalDate()));
                callableStatement.setString(3, systemUserId);
                callableStatement.setString(4, modifySystemUserId);

                callableStatement.registerOutParameter(5, Types.ARRAY);
                callableStatement.registerOutParameter(6, Types.ARRAY);
                callableStatement.registerOutParameter(7, Types.ARRAY);
                callableStatement.registerOutParameter(8, Types.VARCHAR);

                callableStatement.execute();

                Array oldLiabilitiesArray = callableStatement.getArray(5);
                Array restoredReceivablesArray = callableStatement.getArray(6);
                Array installmentLiabilitiesArray = callableStatement.getArray(7);
                String message = callableStatement.getString(8);

                Long[] oldLiabilities = oldLiabilitiesArray != null ?
                        (Long[]) oldLiabilitiesArray.getArray() : new Long[0];
                Long[] restoredReceivables = restoredReceivablesArray != null ?
                        (Long[]) restoredReceivablesArray.getArray() : new Long[0];
                Long[] installmentLiabilities = installmentLiabilitiesArray != null ?
                        (Long[]) installmentLiabilitiesArray.getArray() : new Long[0];

                if (StringUtils.hasText(message) && !message.equals("OK")) {
                    log.error("Error in get reverse rescheduling data: {}", message);
                    throw new RuntimeException("Get reverse rescheduling data failed: " + message);
                }

                result.set(new ReverseReschedulingResult(
                        oldLiabilities,
                        restoredReceivables,
                        installmentLiabilities));

                log.debug("Get reverse rescheduling data completed successfully for rescheduling ID: {}", reschedulingId);
            });

            return result.get();
        }
    }
}
