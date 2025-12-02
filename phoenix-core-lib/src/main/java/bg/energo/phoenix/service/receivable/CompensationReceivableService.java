package bg.energo.phoenix.service.receivable;

import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.model.InvoiceCompensation;
import bg.energo.phoenix.model.entity.billing.compensation.Compensations;
import bg.energo.phoenix.model.entity.billing.invoice.Invoice;
import bg.energo.phoenix.model.entity.documents.Document;
import bg.energo.phoenix.model.entity.receivable.CustomerReceivable;
import bg.energo.phoenix.model.entity.receivable.customerLiability.CustomerLiability;
import bg.energo.phoenix.model.enums.billing.compensation.CompensationStatus;
import bg.energo.phoenix.repository.billing.compensation.CompensationRepository;
import bg.energo.phoenix.repository.billing.invoice.InvoiceRepository;
import bg.energo.phoenix.repository.receivable.customerLiability.CustomerLiabilityRepository;
import bg.energo.phoenix.service.billing.billingRun.BillingRunDocumentCreationService;
import bg.energo.phoenix.service.receivable.customerLiability.CustomerLiabilityService;
import bg.energo.phoenix.service.receivable.customerReceivables.CustomerReceivableService;
import bg.energo.phoenix.util.epb.EPBDatabaseFunctionUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.StoredProcedureQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.hibernate.Session;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompensationReceivableService {

    private final CompensationRepository compensationRepository;
    private final CustomerLiabilityService customerLiabilityService;
    private final CustomerReceivableService customerReceivableService;
    private final InvoiceRepository invoiceRepository;
    private final CustomerLiabilityRepository customerLiabilityRepository;
    private final BillingRunDocumentCreationService billingRunDocumentCreationService;

    private final static String SYSTEM_USER = "system";

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Runs the invoice compensation process for a given invoice.
     * <p>
     * This method performs two main tasks:
     * <ul>
     *   <li>Reverses compensations that have already been invoiced.</li>
     *   <li>Invoicing compensations that are currently not invoiced.</li>
     * </ul>
     * It also attempts to offset the compensations, and if the offset operation fails, it throws a {@link DomainEntityNotFoundException}.
     * </p>
     *
     * @param invoice Invoice for which compensations are being processed
     * @throws DomainEntityNotFoundException if the compensations could not be offset for the given invoice ID
     */
    @Transactional
    public void runInvoiceCompensation(Invoice invoice, Long liabilityId, boolean isInitial) throws Exception {
        if (isInitial) {
            if (liabilityId != null && liabilityId > 0) {
                CustomerLiability customerLiability = customerLiabilityRepository
                        .findById(liabilityId)
                        .orElseThrow(() -> new DomainEntityNotFoundException("Customer liability with ID %d not found".formatted(liabilityId)));
                entityManager.refresh(customerLiability);
            }

            List<InvoiceCompensation> compensationsForInvoicing = fetchInvoiceCompensations(invoice.getId(), CompensationStatus.UNINVOICED, true);
            for (InvoiceCompensation invoiceCompensation : compensationsForInvoicing) {
                createInvoicedCompensationReceivableAndLiability(invoiceCompensation);
            }
        } else {
            List<InvoiceCompensation> compensationsForReversing = fetchInvoiceCompensations(invoice.getId(), CompensationStatus.INVOICED, false);
            for (InvoiceCompensation invoiceCompensation : compensationsForReversing) {
                createReversedCompensationReceivableAndLiability(invoiceCompensation);
            }

            invoice.setCompensationIndex(invoice.getCompensationIndex() + 1);
            invoiceRepository.saveAndFlush(invoice);

            List<InvoiceCompensation> compensationsForInvoicing = fetchInvoiceCompensations(invoice.getId(), CompensationStatus.UNINVOICED, false);
            for (InvoiceCompensation invoiceCompensation : compensationsForInvoicing) {
                createInvoicedCompensationReceivableAndLiability(invoiceCompensation);
            }

            billingRunDocumentCreationService.generateDocumentOnRegeneration(invoice);
        }

        String offsetCompensationsResult = offsetCompensations(invoice.getId());
        if (offsetCompensationsResult == null) {
            throw new DomainEntityNotFoundException("Offsetting compensations for invoiceId: %d failed".formatted(invoice.getId()));
        }
    }

    /**
     * Creates a reversed compensation receivable and liability from the provided invoice compensation data.
     *
     * <p>This method first creates a {@link CustomerLiability} and a {@link CustomerReceivable}
     * from the given {@link InvoiceCompensation}. It then retrieves the compensations associated with
     * the provided compensation IDs, marks them as reversed, and associates them with the newly created
     * liability and receivable. The updated compensations are then saved to the repository.</p>
     *
     * <p>The method is wrapped in a transactional context to ensure that all operations are
     * performed atomically. If any part of the process fails, the entire transaction is rolled back.</p>
     *
     * @param invoiceCompensation The invoice compensation object containing the relevant data
     *                            for creating the liability and receivable.
     * @throws DomainEntityNotFoundException If any of the related entities (such as customer
     *                                       liability, customer receivable, or compensations) cannot be found or if there are issues
     *                                       during the update process.
     * @throws RuntimeException              If there are any runtime exceptions that occur during the transaction.
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void createReversedCompensationReceivableAndLiability(InvoiceCompensation invoiceCompensation) {
        CustomerLiability customerLiability = customerLiabilityService.createLiabilityFromReverseCompensation(invoiceCompensation);
        CustomerReceivable customerReceivable = customerReceivableService.createReceivableFromReverseCompensation(invoiceCompensation);

        List<Compensations> updatedCompensations = compensationRepository
                .findAllById(invoiceCompensation.getCompensationIds())
                .stream()
                .peek(it ->
                        {
                            it.setLiabilityForCustomerId(customerLiability.getId());
                            it.setReceivableForRecipientId(customerReceivable.getId());
                        }
                )
                .toList();

        compensationRepository.saveAllAndFlush(updatedCompensations);
    }

    /**
     * Creates an invoiced compensation receivable and liability from the provided invoice compensation data.
     *
     * <p>This method first creates a {@link CustomerLiability} and a {@link CustomerReceivable}
     * using the data from the provided {@link InvoiceCompensation}. It then retrieves the compensations
     * associated with the given compensation IDs, updates them with the new liability and receivable IDs,
     * and sets their status to "INVOICED". The updated compensations are then saved to the repository.</p>
     *
     * <p>The method is wrapped in a transactional context to ensure that all operations are
     * executed atomically. If any part of the process fails, the entire transaction is rolled back.</p>
     *
     * @param invoiceCompensation The invoice compensation object containing the data used
     *                            to create the customer liability, receivable, and to update compensations.
     * @throws DomainEntityNotFoundException If any of the related entities (such as customer
     *                                       liability, customer receivable, or compensations) cannot be found during the process.
     * @throws RuntimeException              If any unexpected errors occur during the transactional process.
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void createInvoicedCompensationReceivableAndLiability(InvoiceCompensation invoiceCompensation) {
        CustomerLiability customerLiability = customerLiabilityService.createLiabilityFromInvoicedCompensation(invoiceCompensation);
        CustomerReceivable customerReceivable = customerReceivableService.createReceivableFromInvoicedCompensation(invoiceCompensation);

        List<Compensations> updatedCompensations = compensationRepository
                .findAllById(invoiceCompensation.getCompensationIds())
                .stream()
                .peek(it ->
                        {
                            it.setCompensationIndex(invoiceCompensation.getCompensationIndex());
                            it.setReceivableForCustomerId(customerReceivable.getId());
                            it.setLiabilityForRecipientId(customerLiability.getId());
                            it.setCompensationStatus(CompensationStatus.INVOICED);
                            it.setInvoiceId(invoiceCompensation.getInvoiceId());
                            it.setInvoiceUsageDate(LocalDate.now());
                        }
                )
                .toList();

        compensationRepository.saveAllAndFlush(updatedCompensations);
    }

    /**
     * Offsets the compensations for the given invoice by executing a stored procedure.
     *
     * <p>This method invokes the stored procedure `billing.offsett_compensations` to perform the compensation offset
     * for the specified invoice. It registers the required parameters for the stored procedure, executes it, and
     * retrieves the output message. Based on the output message, it logs the success or failure of the operation.
     * If an error occurs, it catches and logs the exception, returning an error message.</p>
     *
     * <p>The method is wrapped in a transactional context, ensuring that the stored procedure execution is part of
     * the transaction.</p>
     *
     * @param invoiceId The ID of the invoice for which compensations are to be offset.
     * @return A string indicating the result of the offset operation. If successful, the message will be "OK",
     * otherwise, it will contain an error message returned by the stored procedure.
     * @throws RuntimeException If any unexpected errors occur during the stored procedure execution.
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public String offsetCompensations(Long invoiceId) {
        log.info("Starting offsetCompensations for invoiceId: {}", invoiceId);

        AtomicReference<String> message = new AtomicReference<>("Error occurred");

        try {
            log.debug("Creating stored procedure query for 'billing.offsett_compensations'");
            StoredProcedureQuery query = entityManager.createStoredProcedureQuery("billing.offsett_compensations");

            log.debug("Registering stored procedure parameters");
            query.registerStoredProcedureParameter(1, Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter(2, String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter(3, String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter(4, String.class, ParameterMode.OUT);

            log.debug("Setting stored procedure parameters: invoiceId={}, user={}, system={}", invoiceId, SYSTEM_USER, SYSTEM_USER);
            query.setParameter(1, invoiceId);
            query.setParameter(2, SYSTEM_USER);
            query.setParameter(3, SYSTEM_USER);

            log.info("Executing stored procedure 'billing.offsett_compensations' for invoiceId: {}", invoiceId);
            query.execute();

            String outputMessage = (String) query.getOutputParameterValue(4);
            log.debug("Stored procedure executed, outputMessage: {}", outputMessage);

            if (StringUtils.isNotBlank(outputMessage)) {
                message.set(outputMessage);
            }

            if ("OK".equals(message.get())) {
                log.info("Successfully offsetted invoice [{}] with message: [{}]", invoiceId, message.get());
            } else {
                log.error("Error occurred during payment offsetting for invoiceId [{}], message: [{}]", invoiceId, message.get());
            }

        } catch (Exception e) {
            log.error("An error occurred during liability offsetting for invoiceId [{}]:", invoiceId, e);
        }

        log.info("Finished offsetCompensations for invoiceId [{}], result message: [{}]", invoiceId, message.get());
        return message.get();
    }

    /**
     * Fetches a list of invoice compensations for the given invoice ID and compensation status.
     *
     * <p>This method performs a database call to retrieve compensations associated with a specific invoice
     * and a given compensation status. It executes a callable statement to fetch the results from the database
     * and populates a list of {@link InvoiceCompensation} objects based on the results. The method is wrapped
     * in a transactional context to ensure consistency and proper resource management during database interaction.</p>
     *
     * @param invoiceId          The ID of the invoice for which compensations are being fetched.
     * @param compensationStatus The status of the compensations to be fetched. This is used to filter the compensations.
     * @return A list of {@link InvoiceCompensation} objects matching the provided invoice ID and compensation status.
     * If no compensations are found, an empty list will be returned.
     * @throws RuntimeException If any unexpected errors occur during the database call or result set processing.
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public List<InvoiceCompensation> fetchInvoiceCompensations(
            Long invoiceId,
            CompensationStatus compensationStatus,
            Boolean isInitial
    ) {
        log.info(
                "Starting fetchInvoiceCompensations for invoiceId: {}, compensationStatus: {}",
                invoiceId, compensationStatus
        );
        AtomicReference<List<InvoiceCompensation>> compensations = new AtomicReference<>(new ArrayList<>());

        try (Session session = entityManager.unwrap(Session.class)) {
            log.debug("Unwrapped Hibernate session from the EntityManager");
            session.doWork(connection -> {
                        log.debug("Performing database call to fetch compensations");
                        try (CallableStatement callableStatement = createInvoiceCompensationsStatement(
                                connection,
                                invoiceId,
                                compensationStatus,
                                isInitial
                        )) {
                            log.debug(
                                    "Callable statement created for invoiceId: {}, compensationStatus: {}",
                                    invoiceId, compensationStatus
                            );
                            boolean hasResultSet = callableStatement.execute();
                            log.debug("Callable statement executed, hasResultSet: {}", hasResultSet);

                            // Process the result set if present
                            if (hasResultSet) {
                                try (ResultSet resultSet = callableStatement.getResultSet()) {
                                    log.debug("Processing result set for invoiceId: {}", invoiceId);
                                    populateInvoiceCompensationsResultSet(resultSet, compensations.get());
                                    log.debug("Compensations result set processed for invoiceId: {}", invoiceId);
                                }
                            }
                        }
                    }
            );
        } catch (Exception e) {
            log.error("An error occurred during fetchInvoiceCompensations for invoiceId: {}", invoiceId, e);
        }

        log.info("Finished fetchInvoiceCompensations for invoiceId: {}, total compensations fetched: {}",
                invoiceId, compensations.get().size()
        );
        return compensations.get();
    }

    /**
     * Creates a {@link CallableStatement} to execute the stored procedure for fetching invoice compensations.
     *
     * <p>This method prepares a callable statement to execute the stored procedure `billing.get_invoice_compensations`,
     * which fetches compensations for a specific invoice ID and compensation status. The parameters for the stored procedure
     * are set safely using helper methods to avoid issues with null values.</p>
     *
     * @param connection         The database {@link Connection} object used to create the callable statement.
     * @param invoiceId          The ID of the invoice for which compensations are to be fetched.
     * @param compensationStatus The status of the compensations to be fetched (e.g., `INVOICED`, `PAID`, etc.).
     * @return A {@link CallableStatement} object configured with the appropriate parameters to call the stored procedure.
     * @throws SQLException If an error occurs while preparing or setting parameters for the callable statement.
     */
    private CallableStatement createInvoiceCompensationsStatement(
            Connection connection,
            Long invoiceId,
            CompensationStatus compensationStatus,
            boolean isInitial
    ) throws SQLException {
        CallableStatement callableStatement = connection.prepareCall("{CALL billing.get_invoice_compensations(?, ?, ?)}");
        EPBDatabaseFunctionUtils.nullSafeSetLong(callableStatement, invoiceId, 1);
        EPBDatabaseFunctionUtils.nullSafeSetString(callableStatement, compensationStatus.name(), 2);
        EPBDatabaseFunctionUtils.nullSafeSetBoolean(callableStatement, isInitial, 3);
        return callableStatement;
    }

    /**
     * Populates a list of {@link InvoiceCompensation} objects from the provided {@link ResultSet}.
     *
     * <p>This method processes the rows of the provided {@link ResultSet} and maps the result set values into {@link InvoiceCompensation} objects.
     * Each row in the result set represents one compensation record, and the method extracts relevant columns from the result set to populate
     * the fields of each {@link InvoiceCompensation} object. These objects are then added to the provided list.</p>
     *
     * @param resultSet     The {@link ResultSet} containing the result set returned by executing the stored procedure or query. It must contain
     *                      compensation data for each invoice.
     * @param compensations The list to which the populated {@link InvoiceCompensation} objects will be added.
     * @throws SQLException If an error occurs while accessing the data in the {@link ResultSet}.
     */
    private void populateInvoiceCompensationsResultSet(
            ResultSet resultSet,
            List<InvoiceCompensation> compensations
    ) throws SQLException {
        while (resultSet.next()) {
            InvoiceCompensation invoiceCompensation = new InvoiceCompensation();
            invoiceCompensation.setCompensationReceiptId(resultSet.getLong("compensation_recipient_id"));
            invoiceCompensation.setCompensationIndex(resultSet.getInt("compensation_index"));
            invoiceCompensation.setCompAmount(resultSet.getBigDecimal("comp_amount"));
            invoiceCompensation.setCurrencyId(resultSet.getLong("currency_id"));
            invoiceCompensation.setCustomerId(resultSet.getLong("customer_id"));
            invoiceCompensation.setInvoiceId(resultSet.getLong("invoice_id"));
            invoiceCompensation.setPodId(resultSet.getLong("pod_id"));
            Optional.ofNullable(
                            resultSet.getDate("compensation_document_period")
                    )
                    .map(Date::toLocalDate)
                    .ifPresent(invoiceCompensation::setCompensationDocumentPeriod);
            Optional.ofNullable(
                            resultSet.getString("compensation_ids")
                    )
                    .filter(StringUtils::isNotBlank)
                    .map(compensationIds -> Arrays
                            .stream(compensationIds
                                    .replaceAll("[{}]", "")
                                    .split(",")
                            )
                            .map(Long::parseLong)
                            .collect(Collectors.toSet())
                    )
                    .ifPresent(invoiceCompensation::setCompensationIds);
            compensations.add(invoiceCompensation);
        }
    }
}
