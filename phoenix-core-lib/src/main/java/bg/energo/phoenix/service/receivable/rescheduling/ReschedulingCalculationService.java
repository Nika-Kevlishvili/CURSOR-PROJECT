package bg.energo.phoenix.service.receivable.rescheduling;

import bg.energo.phoenix.model.enums.receivable.rescheduling.ReschedulingInterestType;
import bg.energo.phoenix.model.request.receivable.rescheduling.ReschedulingCalculateRequest;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Types;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReschedulingCalculationService {

    private final EntityManager entityManager;

    public String calculate(ReschedulingCalculateRequest calculateRequest) throws Exception {
        AtomicReference<Exception> atomicException = new AtomicReference<>();
        final AtomicReference<String> calculationResult = new AtomicReference<>("");
        try (Session session = entityManager.unwrap(Session.class)) {
            session.doWork(connection -> {
                        CallableStatement callableStatement;
                        if (calculateRequest.getReschedulingInterestType().equals(ReschedulingInterestType.INTEREST_WITH_THE_FIRST_INSTALLMENT)) {
                            callableStatement = connection.prepareCall("{? = CALL receivable.calculate_rescheduling_fip_json(?,now()::date,?,?,?,?,?)}");
                        } else if (calculateRequest.getReschedulingInterestType().equals(ReschedulingInterestType.INTEREST_WITH_LAST_INSTALLMENT)) {
                            callableStatement = connection.prepareCall("{? = CALL receivable.calculate_rescheduling_li_json(?,now()::date,?,?,?,?,?)}");
                        } else if (calculateRequest.getReschedulingInterestType().equals(ReschedulingInterestType.FIRST_INSTALLMENT_INTEREST_ONLY)) {
                            callableStatement = connection.prepareCall("{? = CALL receivable.calculate_rescheduling_fi_json(?,now()::date,?,?,?,?,?)}");
                        } else {
                            callableStatement = connection.prepareCall("{? = CALL receivable.calculate_rescheduling_ei_json(?,now()::date,?,?,?,?,?)}");
                        }

                        callableStatement.registerOutParameter(1, Types.OTHER);

                        List<Long> liabilityIdsList = calculateRequest.getLiabilityIds();
                        Long[] liabilityIdsArray = liabilityIdsList.toArray(new Long[0]);

                        callableStatement.setArray(2, connection.createArrayOf("bigint", liabilityIdsArray));
                        if (calculateRequest.getInstallmentAmount() == null || calculateRequest.getInstallmentAmount().equals(BigDecimal.ZERO)) {
                            callableStatement.setNull(3, Types.NUMERIC);
                        } else {
                            callableStatement.setBigDecimal(3, calculateRequest.getInstallmentAmount());
                        }

                        if (calculateRequest.getInstallmentCount() == null || calculateRequest.getInstallmentCount() == 0) {
                            callableStatement.setNull(4, Types.INTEGER);
                        } else {
                            callableStatement.setInt(4, calculateRequest.getInstallmentCount());
                        }

                        callableStatement.setInt(5, calculateRequest.getInstallmentCurrencyId());
                        callableStatement.setDate(6, java.sql.Date.valueOf(calculateRequest.getInstallmentDate()));

                        if (calculateRequest.getReplaceInstallmentRateId() == null) {
                            callableStatement.setNull(7, Types.BIGINT);
                        } else {
                            callableStatement.setLong(7, calculateRequest.getReplaceInstallmentRateId());
                        }

                        try {
                            callableStatement.execute();
                            Object jsonbResult = callableStatement.getObject(1);

                            if (jsonbResult != null) {
                                calculationResult.getAndSet(jsonbResult.toString());
                            }
                        } catch (Exception e) {
                            log.error("Some exception happened in calculate_rescheduling_fi_json procedure ", e);
                            atomicException.set(e);
                        }
                    }
            );
        }
        if (atomicException.get() != null) {
            throw atomicException.get();
        }

        return calculationResult.get();
    }

}
