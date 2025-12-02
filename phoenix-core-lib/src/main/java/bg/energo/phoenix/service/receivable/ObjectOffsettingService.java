package bg.energo.phoenix.service.receivable;

import bg.energo.phoenix.model.ObjectOffsettingDetail;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.nomenclature.product.Currency;
import bg.energo.phoenix.model.enums.receivable.OperationContext;
import bg.energo.phoenix.model.enums.receivable.offsetting.ObjectOffsettingDisplayColor;
import bg.energo.phoenix.model.enums.receivable.offsetting.ObjectOffsettingRole;
import bg.energo.phoenix.model.enums.receivable.offsetting.ObjectOffsettingType;
import bg.energo.phoenix.model.enums.receivable.offsetting.OffsettingOperationType;
import bg.energo.phoenix.repository.nomenclature.product.CurrencyRepository;
import bg.energo.phoenix.util.epb.EPBDatabaseFunctionUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
@RequiredArgsConstructor
public class ObjectOffsettingService {

    @PersistenceContext
    private final EntityManager entityManager;

    private final CurrencyRepository currencyRepository;

    /**
     * Fetches a list of {@link ObjectOffsettingDetail} objects based on the provided {@link ObjectOffsettingType}
     * and objectId. The method retrieves data from the database by executing a stored procedure
     * through a {@link CallableStatement}. The results are then mapped into {@link ObjectOffsettingDetail} objects
     * and returned in a list.
     * <p>
     * This method utilizes a transaction with the {@link Transactional} annotation to ensure that the database
     * operations are executed within the scope of a transaction. If an exception occurs during database interaction,
     * it is logged, and the method will return an empty list.
     *
     * @param objectOffsettingType The type of the object offsetting to filter the result set by.
     * @param objectId             The ID of the object for which offsetting details are to be fetched.
     * @return A list of {@link ObjectOffsettingDetail} objects populated from the result set, or an empty list
     * if no results are found or an error occurs.
     * @throws SQLException If an SQL error occurs during the execution of the stored procedure or result set processing.
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public List<ObjectOffsettingDetail> fetchObjectOffsettings(
            ObjectOffsettingType objectOffsettingType,
            Long objectId
    ) {
        log.info("Starting fetchObjectOffsettings for id: {}, transaction object type: {}", objectId, objectOffsettingType);
        AtomicReference<List<ObjectOffsettingDetail>> listAtomicReference = new AtomicReference<>(new ArrayList<>());

        try (Session session = entityManager.unwrap(Session.class)) {
            log.debug("Session unwrapped from EntityManager successfully.");
            session.doWork(connection -> {
                        log.debug("Inside session.doWork with connection: {}", connection);
                        try (CallableStatement callableStatement = createObjectOffsettingsStatement(
                                connection,
                                objectOffsettingType,
                                objectId
                        )) {
                            log.debug("Offsetting callableStatement created successfully for objectId: {}, objectType: {}", objectId, objectOffsettingType);
                            boolean hasResultSet = callableStatement.execute();
                            // Process the result set if present
                            if (hasResultSet) {
                                try (ResultSet resultSet = callableStatement.getResultSet()) {
                                    log.debug("ResultSet obtained, starting to populate result list.");
                                    populateObjectOffsettingResultSet(resultSet, listAtomicReference.get());
                                    log.debug("Result list populated successfully. Total items: {}", listAtomicReference.get().size());
                                }
                            } else {
                                log.warn("Offsetting callableStatement executed but no result set was returned.");
                            }
                        }
                    }
            );
        } catch (Exception e) {
            log.error("An error occurred during fetchObjectTransactions for objectId: {}", objectId, e);
        }
        log.info("Completed fetchObjectOffsettings for id: {}, total results: {}", objectId, listAtomicReference.get().size());
        return listAtomicReference.get();
    }

    /**
     * Populates a list of {@link ObjectOffsettingDetail} objects from the provided {@link ResultSet}.
     * <p>
     * This method iterates through the rows of the {@link ResultSet} and for each row, it extracts values
     * from the columns and sets them into a new {@link ObjectOffsettingDetail} object. The object is then
     * added to the provided list of {@link ObjectOffsettingDetail} objects.
     * <p>
     * The method utilizes {@link Optional} to safely handle nullable columns and ensures that values are
     * mapped appropriately to their respective types (e.g., converting {@link Date} to {@link java.time.LocalDate},
     * and strings to enum values).
     *
     * @param resultSet           The {@link ResultSet} from which data is extracted to populate {@link ObjectOffsettingDetail} objects.
     * @param offsetObjectDetails The list to which the populated {@link ObjectOffsettingDetail} objects are added.
     * @throws SQLException If there is an error retrieving data from the {@link ResultSet}.
     */
    private void populateObjectOffsettingResultSet(
            ResultSet resultSet,
            List<ObjectOffsettingDetail> offsetObjectDetails
    ) throws SQLException {
        while (resultSet.next()) {
            ObjectOffsettingDetail offsetObjectDetail = new ObjectOffsettingDetail();
            offsetObjectDetail.setObjectId(resultSet.getLong("object_id"));

            Optional.ofNullable(
                            resultSet.getString("object_type")
                    )
                    .map(String::toUpperCase)
                    .map(ObjectOffsettingType::valueOf)
                    .ifPresent(offsetObjectDetail::setObjectType);

            offsetObjectDetail.setOffsettingAmount(resultSet.getBigDecimal("offsetting_amount"));
            offsetObjectDetail.setCurrencyId(resultSet.getLong("currency_id"));

            Optional.ofNullable(
                            resultSet.getDate("operation_date")
                    )
                    .map(Date::toLocalDate)
                    .ifPresent(offsetObjectDetail::setOperationDate);

            Optional.ofNullable(
                            resultSet.getString("operation_type")
                    )
                    .map(OffsettingOperationType::valueOf)
                    .ifPresent(offsetObjectDetail::setOperationType);

            Optional.ofNullable(
                            resultSet.getString("status")
                    )
                    .map(EntityStatus::valueOf)
                    .ifPresent(offsetObjectDetail::setStatus);

            Optional.ofNullable(
                            resultSet.getString("display_color")
                    )
                    .map(ObjectOffsettingDisplayColor::valueOf)
                    .ifPresent(offsetObjectDetail::setDisplayColor);

            Optional.ofNullable(
                            resultSet.getString("operation_context")
                    )
                    .map(OperationContext::valueOf)
                    .ifPresent(offsetObjectDetail::setOperationContext);

            Optional.ofNullable(
                            resultSet.getString("object_role")
                    )
                    .map(ObjectOffsettingRole::valueOf)
                    .ifPresent(offsetObjectDetail::setObjectRole);

            if (offsetObjectDetail.getCurrencyId() != null) {
                String currencyName = currencyRepository
                        .findById(offsetObjectDetail.getCurrencyId())
                        .map(Currency::getName)
                        .orElse(null);
                offsetObjectDetail.setCurrencyName(currencyName);
            }
            offsetObjectDetails.add(offsetObjectDetail);
        }
    }

    /**
     * Creates a {@link CallableStatement} for executing the stored procedure
     * `receivable.get_object_offsettings` with the specified parameters.
     * <p>
     * This method prepares the CallableStatement using the provided database connection
     * and sets the parameters for the stored procedure. The parameters set are:
     * - The name of the {@link ObjectOffsettingType}.
     * - The ID of the object for which offsetting details are to be fetched.
     * <p>
     * The method uses utility methods {@link EPBDatabaseFunctionUtils#nullSafeSetString}
     * and {@link EPBDatabaseFunctionUtils#nullSafeSetLong} to safely set the parameters
     * for the CallableStatement.
     *
     * @param connection           The database connection to be used for creating the callable statement.
     * @param objectOffsettingType The type of object offsetting (used in the first parameter of the stored procedure).
     * @param objectId             The ID of the object to fetch offsetting details for (used in the second parameter of the stored procedure).
     * @return A {@link CallableStatement} that has been prepared with the specified SQL and parameters.
     * @throws SQLException If a database access error occurs or the SQL is invalid.
     */
    private CallableStatement createObjectOffsettingsStatement(
            Connection connection,
            ObjectOffsettingType objectOffsettingType,
            Long objectId
    ) throws SQLException {
        CallableStatement callableStatement = connection.prepareCall("{CALL receivable.get_object_offsettings(?, ?)}");
        log.debug("CallableStatement created with SQL: {CALL receivable.get_object_offsettings(?, ?)}");
        EPBDatabaseFunctionUtils.nullSafeSetString(callableStatement, objectOffsettingType.name(), 1);
        EPBDatabaseFunctionUtils.nullSafeSetLong(callableStatement, objectId, 2);
        log.debug("Parameters set successfully for CallableStatement.");
        return callableStatement;
    }

}
