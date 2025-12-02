package bg.energo.phoenix.service.receivable.payment;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.model.enums.receivable.DirectOffsettingSourceType;
import bg.energo.phoenix.model.enums.receivable.OperationContext;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.CallableStatement;
import java.sql.Date;
import java.sql.Types;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicReference;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentOffsettingService {

    private final EntityManager entityManager;

    @Transactional
    public String directNegativePaymentOffsetting(
            DirectOffsettingSourceType sourceType,
            Long sourceId,
            Long negativePaymentId,
            String systemUserId,
            String modifySystemUserId,
            OperationContext operationContext
    ) {
        AtomicReference<String> message = new AtomicReference<>("UNKNOWN");
        try {
            Session session = entityManager.unwrap(Session.class);
            session.doWork(connection -> {
                CallableStatement callableStatement = connection.prepareCall(
                        "CALL receivable.direct_negative_payment_offsetting(?,?,?,?,?,?,?,?,?)");

                callableStatement.setString(1, sourceType.name());
                callableStatement.setLong(2, sourceId);
                callableStatement.setLong(3, negativePaymentId);
                callableStatement.setString(4, systemUserId);
                callableStatement.setString(5, modifySystemUserId);

                callableStatement.registerOutParameter(6, Types.BIGINT);
                callableStatement.registerOutParameter(7, Types.VARCHAR);

                callableStatement.setDate(8, Date.valueOf(LocalDate.now()));
                callableStatement.setString(9, operationContext.name());

                callableStatement.execute();
                message.set(callableStatement.getString(7));
            });
        } catch (Exception e) {
            throw new ClientException("Error happened in direct_negative_payment_offsetting procedure [%s]".formatted(e.getMessage()), ErrorCode.APPLICATION_ERROR);
        }
        return message.get();
    }
}
