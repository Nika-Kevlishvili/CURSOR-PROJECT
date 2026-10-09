package bg.energo.phoenix.cashterminal.service;

import bg.energo.phoenix.cashterminal.model.entity.CashTerminalPaymentEntity;
import bg.energo.phoenix.cashterminal.model.enums.CashTerminalRequestType;
import bg.energo.phoenix.cashterminal.repository.CashTerminalPaymentDetailsRepository;
import bg.energo.phoenix.cashterminal.repository.CashTerminalPaymentRepository;
import bg.energo.phoenix.model.CheckLiabilityDetails;
import bg.energo.phoenix.model.CustomerWrapper;
import bg.energo.phoenix.model.entity.customer.Customer;
import bg.energo.phoenix.model.entity.receivable.collectionChannel.CollectionChannel;
import bg.energo.phoenix.model.enums.customer.CustomerStatus;
import bg.energo.phoenix.repository.customer.CustomerDetailsRepository;
import bg.energo.phoenix.repository.customer.CustomerRepository;
import bg.energo.phoenix.repository.receivable.collectionChannel.CollectionChannelRepository;
import bg.energo.phoenix.utils.EPBDatabaseFunctionUtils;
import bg.energo.phoenix.utils.EPBFunctionUtils;
import bg.energo.phoenix.utils.EPBPaymentCalculationUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Value;

import java.sql.*;
import java.sql.Date;
import java.time.LocalDate;
import java.util.*;

@RequiredArgsConstructor
public abstract class CashTerminalBaseService {

    protected final CashTerminalPaymentDetailsRepository cashTerminalPaymentDetailRepository;
    protected final CashTerminalPaymentRepository cashTerminalPaymentRepository;
    protected final CollectionChannelRepository collectionChannelRepository;
    protected final CustomerDetailsRepository customerDetailsRepository;
    protected final CustomerRepository customerRepository;
    protected final CashTerminalMapperService cashTerminalMapperService;
    @PersistenceContext
    protected EntityManager entityManager;
    @Value("${cashterminal.secret.key}")
    protected String secretKey;
    @Value("${cashterminal.merchant.id}")
    protected String merchantCode;
    @Value("${cashterminal.collection.channel.name}")
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

    protected CashTerminalPaymentEntity findPaymentByTransactionId(String tid) {
        return cashTerminalPaymentRepository.findByTransactionId(tid).orElse(null);
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

                            if (hasResultSet) {
                                try (ResultSet resultSet = callableStatement.getResultSet()) {
                                    populateLiabilityDetails(resultSet, checkLiabilityDetailsList);
                                }
                            }
                        }
                    }
            );
        }

        return checkLiabilityDetailsList.stream().filter(EPBPaymentCalculationUtils::isValidLiability).toList();
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
        return !Objects.equals(merchantId, merchantCode);
    }

    protected boolean isBillingRequestWithInvalidTid(CashTerminalRequestType requestType, String tid) {
        if (CashTerminalRequestType.BILLING.equals(requestType)) {
            return StringUtils.isBlank(tid) || !EPBFunctionUtils.validateTransactionId(tid);
        } else {
            return false;
        }
    }

    protected boolean isCheckRequestWithTid(CashTerminalRequestType requestType, String tid) {
        return CashTerminalRequestType.CHECK.equals(requestType) && StringUtils.isNotBlank(tid);
    }

    protected boolean isCorrectBillingRequest(CashTerminalRequestType requestType, String tid) {
        return CashTerminalRequestType.BILLING.equals(requestType) && StringUtils.isNotBlank(tid) && EPBFunctionUtils.validateTransactionId(tid);
    }
}

