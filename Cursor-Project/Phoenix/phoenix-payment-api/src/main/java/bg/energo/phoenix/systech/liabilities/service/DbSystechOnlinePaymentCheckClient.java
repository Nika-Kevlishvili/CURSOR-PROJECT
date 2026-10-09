package bg.energo.phoenix.systech.liabilities.service;

import bg.energo.phoenix.model.CheckLiabilityDetails;
import bg.energo.phoenix.utils.EPBDatabaseFunctionUtils;
import bg.energo.phoenix.utils.EPBPaymentCalculationUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Session;
import org.springframework.stereotype.Service;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
public class DbSystechOnlinePaymentCheckClient implements SystechOnlinePaymentCheckClient {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<CheckLiabilityDetails> check(
            Long collectionChannelId,
            Long customerId,
            String billingGroupNumber,
            LocalDate calculateDate
    ) {
        List<CheckLiabilityDetails> results = new ArrayList<>();

        try (Session session = entityManager.unwrap(Session.class)) {
            session.doWork(connection -> {
                try (CallableStatement callableStatement = createLiabilityCallableStatement(
                        connection,
                        collectionChannelId,
                        customerId,
                        billingGroupNumber,
                        calculateDate
                )) {
                    boolean hasResultSet = callableStatement.execute();
                    if (hasResultSet) {
                        try (ResultSet resultSet = callableStatement.getResultSet()) {
                            populateLiabilityDetails(resultSet, results);
                        }
                    }
                }
            });
        }

        return results.stream().filter(EPBPaymentCalculationUtils::isValidLiability).toList();
    }

    private CallableStatement createLiabilityCallableStatement(
            Connection connection,
            Long collectionChannelId,
            Long customerId,
            String billingGroupNumber,
            LocalDate calculateDate
    ) throws SQLException {
        CallableStatement callableStatement = connection.prepareCall("{CALL receivable.online_payment_check(?, ?, ?, ?)}");
        EPBDatabaseFunctionUtils.nullSafeSetLong(callableStatement, collectionChannelId, 1);
        EPBDatabaseFunctionUtils.nullSafeSetLong(callableStatement, customerId, 2);
        EPBDatabaseFunctionUtils.nullSafeSetString(callableStatement, billingGroupNumber, 3);
        EPBDatabaseFunctionUtils.nullSafeSetLocalDate(callableStatement, calculateDate, 4);
        return callableStatement;
    }

    private void populateLiabilityDetails(ResultSet resultSet, List<CheckLiabilityDetails> liabilities) throws SQLException {
        while (resultSet.next()) {
            CheckLiabilityDetails liabilityDetails = new CheckLiabilityDetails();
            liabilityDetails.setOutgoingDocumentFromExternalSystem(resultSet.getString("outgoing_document_from_external_system"));
            liabilityDetails.setCurrentAmount(resultSet.getBigDecimal("current_amount"));
            liabilityDetails.setTotalAmount(resultSet.getBigDecimal("amount_to_pay"));
            liabilityDetails.setLfpAmount(resultSet.getBigDecimal("lpf_def_curr"));
            liabilityDetails.setLiabilityId(resultSet.getLong("liability_id"));
            liabilityDetails.setCurrencyId(resultSet.getLong("currency_id"));

            if (liabilityDetails.getLiabilityId() != null && (liabilityDetails.getCurrentAmount() != null || liabilityDetails.getTotalAmount() != null)) {
                Optional.ofNullable(resultSet.getString("pod_identifiers"))
                        .filter(StringUtils::isNotBlank)
                        .map(podIdentifiers -> Arrays.asList(podIdentifiers.split(",")))
                        .ifPresent(liabilityDetails::setPodIdentifiers);

                Optional.ofNullable(resultSet.getDate("due_date"))
                        .map(Date::toLocalDate)
                        .ifPresent(liabilityDetails::setDueDate);

                liabilities.add(liabilityDetails);
            }
        }
    }
}

