package bg.energo.phoenix.service;

import bg.energo.phoenix.model.CheckLiabilityDetails;
import bg.energo.phoenix.model.CustomerWrapper;
import bg.energo.phoenix.model.entity.Configuration;
import bg.energo.phoenix.model.entity.EasyPayPaymentEntity;
import bg.energo.phoenix.model.entity.customer.Customer;
import bg.energo.phoenix.model.entity.receivable.collectionChannel.CollectionChannel;
import bg.energo.phoenix.model.enums.EasyPayRequestType;
import bg.energo.phoenix.model.enums.customer.CustomerStatus;
import bg.energo.phoenix.model.response.receivable.customerLiability.LiabilityInvoiceDate;
import bg.energo.phoenix.repository.ConfigurationRepository;
import bg.energo.phoenix.repository.EasyPayPaymentDetailsRepository;
import bg.energo.phoenix.repository.EasyPayPaymentRepository;
import bg.energo.phoenix.repository.customer.CustomerDetailsRepository;
import bg.energo.phoenix.repository.customer.CustomerRepository;
import bg.energo.phoenix.repository.receivable.collectionChannel.CollectionChannelRepository;
import bg.energo.phoenix.repository.receivable.customerLiability.CustomerLiabilityRepository;
import bg.energo.phoenix.utils.EPBDatabaseFunctionUtils;
import bg.energo.phoenix.utils.EPBFunctionUtils;
import bg.energo.phoenix.utils.EPBPaymentCalculationUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.CollectionUtils;

import java.sql.Date;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public abstract class EasyPayBaseService {

    protected final EasyPayPaymentDetailsRepository easyPayPaymentDetailRepository;
    protected final EasyPayPaymentRepository easyPayPaymentRepository;
    protected final CollectionChannelRepository collectionChannelRepository;
    protected final CustomerDetailsRepository customerDetailsRepository;
    protected final CustomerRepository customerRepository;
    protected final EasyPayMapperService easyPayMapperService;
    protected final CustomerLiabilityRepository customerLiabilityRepository;
    protected final ConfigurationRepository configurationRepository;
    @PersistenceContext
    protected EntityManager entityManager;
    @Value("${easypay.secret.key}")
    protected String secretKey;
    @Value("${easypay.merchant.id}")
    protected String easyPayMerchantCode;
    @Value("${easypay.collection.channel.name}")
    protected String collectionChannelName;

    protected String[] splitCustomerIdn(String inputString) throws Exception {
        if (StringUtils.isBlank(inputString)) {
            throw new IllegalArgumentException("Input string cannot be blank.");
        }

        if (inputString.length() < 10) {
            throw new IllegalArgumentException("Input string must be at least 10 characters.");
        }

        if (!inputString.matches("\\d+")) {
            throw new IllegalArgumentException("Input string must contain only numbers.");
        }

        if (inputString.length() > 10) {
            String remainingPart = inputString.substring(10);
            if (remainingPart.length() != 4) {
                throw new IllegalArgumentException("If the idn is longer than 10 characters, it must contain exactly 4 characters after the 10th position.");
            }
        }

        String customerNumber = inputString.substring(0, 10);
        String billingGroupNumber = inputString.length() > 10 ? inputString.substring(10) : null;

        return new String[]{customerNumber, billingGroupNumber};
    }

    protected EasyPayPaymentEntity findEasyPayPaymentByTransactionId(String tid) {
        return easyPayPaymentRepository.findByTransactionId(tid).orElse(null);
    }

    protected String fetchCustomerDetails(Long customerDetailId) {
        return customerDetailsRepository
                .findByCustomerDetailsId(customerDetailId)
                .map(shortResponse -> String.join(";", shortResponse.name(), String.valueOf(shortResponse.id())))
                .orElse(null);
    }

    protected CollectionChannel fetchCollectionChannel() {
        return collectionChannelRepository.findCollectionChanel(collectionChannelName).orElse(null);
    }

    protected CustomerWrapper processCustomerDetails(String customerInfo) {
        String[] result;
        try {
            result = splitCustomerIdn(customerInfo);
        } catch (Exception e) {
            return null;
        }
        String customerNumberString = result[0];
        String billingGroupNumber = StringUtils.isBlank(result[1]) ? null : result[1];

        log.info("billingGroupNumber from IDN - {}", billingGroupNumber);
        if (billingGroupNumber != null) {
            log.info("Valid billingGroupNumber from IDN - {}", billingGroupNumber);
        }

        Long customerNumber = Long.valueOf(customerNumberString);

        Customer customer = fetchCustomer(customerNumber);
        if (customer != null) {
            String customerShortDetails = fetchCustomerDetails(customer.getLastCustomerDetailId());
            if (StringUtils.isNotBlank(customerShortDetails)) {
                return new CustomerWrapper(customer, customerShortDetails, billingGroupNumber);
            }
        }
        return null;
    }

    protected List<CheckLiabilityDetails> fetchLiabilities(
            Long collectionChannelId,
            Long customerId,
            String billingGroupNumber
    ) {
        List<CheckLiabilityDetails> checkLiabilityDetailsList = new ArrayList<>();

        try (Session session = entityManager.unwrap(Session.class)) {
            session.doWork(connection -> {
                        try (CallableStatement callableStatement = createLiabilityCallableStatement(
                                connection,
                                collectionChannelId,
                                customerId,
                                billingGroupNumber,
                                LocalDate.now()
                        )) {
                            boolean hasResultSet = callableStatement.execute();

                            // Process the result set if present
                            if (hasResultSet) {
                                try (ResultSet resultSet = callableStatement.getResultSet()) {
                                    populateLiabilityDetails(resultSet, checkLiabilityDetailsList);
                                }
                            }
                        }
                    }
            );
        }

        List<CheckLiabilityDetails> validLiabilities = checkLiabilityDetailsList.stream()
                .filter(EPBPaymentCalculationUtils::isValidLiability)
                .toList();

        // Populate invoice dates for liabilities
        if (!validLiabilities.isEmpty()) {
            List<Long> liabilityIds = validLiabilities.stream()
                    .map(CheckLiabilityDetails::getLiabilityId)
                    .filter(Objects::nonNull)
                    .toList();

            if (!liabilityIds.isEmpty()) {
                Map<Long, LiabilityInvoiceDate> invoiceDataMap = customerLiabilityRepository
                        .findInvoiceDatesByLiabilityIds(liabilityIds)
                        .stream()
                        .collect(java.util.stream.Collectors.toMap(
                                LiabilityInvoiceDate::getLiabilityId,
                                java.util.function.Function.identity()
                        ));

                validLiabilities.forEach(liability -> {
                    LiabilityInvoiceDate invoiceData = invoiceDataMap.get(liability.getLiabilityId());
                    if (invoiceData != null) {
                        liability.setDocumentDate(invoiceData.getdocumentDate());
                        liability.setDocumentNumber(invoiceData.getdocumentNumber());
                    }
                });
            }
        }

        return validLiabilities;
    }

    /**
     * Returns liability IDs from the given set that belong to pro-forma reconnection invoices
     * ({@code document_type = PROFORMA_INVOICE} and {@code invoice_type = RECONNECTION}).
     */
    @SuppressWarnings("unchecked")
    protected Set<Long> findProformaReconnectionLiabilityIds(List<Long> liabilityIds) {
        if (CollectionUtils.isEmpty(liabilityIds)) {
            return Set.of();
        }

        List<Number> rows = entityManager.createNativeQuery("""
                        SELECT cl.id
                        FROM receivable.customer_liabilities cl
                                 JOIN invoice.invoices i ON i.id = cl.invoice_id
                        WHERE cl.id IN (:liabilityIds)
                          AND i.document_type = 'PROFORMA_INVOICE'
                          AND i.type = 'RECONNECTION'
                        """)
                .setParameter("liabilityIds", liabilityIds)
                .getResultList();

        return rows.stream()
                .map(Number::longValue)
                .collect(Collectors.toSet());
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

    protected Customer fetchCustomer(Long customerNumber) {
        return customerRepository
                .findByCustomerNumberAndStatus(customerNumber, CustomerStatus.ACTIVE)
                .orElse(null);
    }

    protected boolean isInvalidMerchantId(String merchantId) {
        Optional<Configuration> currentConfiguration = configurationRepository.findLatestConfiguration();
        String merchantIdFromConfig = null;
        if (currentConfiguration.isPresent()) {
            merchantIdFromConfig = currentConfiguration.get().getEasyPayMerchantId();
        }
        String finalMerchantId = merchantIdFromConfig == null ? easyPayMerchantCode : merchantIdFromConfig;
        log.info("finalMerchantId- {}", finalMerchantId);
        return !Objects.equals(merchantId, finalMerchantId);
    }

    protected boolean isBillingRequestWithInvalidTid(EasyPayRequestType easyPayRequestType, String tid) {
        if (EasyPayRequestType.BILLING.equals(easyPayRequestType)) {
            return StringUtils.isBlank(tid) || !EPBFunctionUtils.validateTransactionId(tid);
        } else {
            return false;
        }
    }

    protected boolean isCheckRequestWithTid(EasyPayRequestType easyPayRequestType, String tid) {
        return EasyPayRequestType.CHECK.equals(easyPayRequestType) && StringUtils.isNotBlank(tid);
    }

    protected boolean isCorrectBillingRequest(EasyPayRequestType easyPayRequestType, String tid) {
        return EasyPayRequestType.BILLING.equals(easyPayRequestType) && StringUtils.isNotBlank(tid) && EPBFunctionUtils.validateTransactionId(tid);
    }

}
