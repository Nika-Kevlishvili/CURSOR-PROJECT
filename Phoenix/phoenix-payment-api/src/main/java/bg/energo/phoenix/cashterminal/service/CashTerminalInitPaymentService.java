package bg.energo.phoenix.cashterminal.service;

import bg.energo.phoenix.cashterminal.model.enums.CashTerminalPaymentStatus;
import bg.energo.phoenix.cashterminal.model.enums.CashTerminalRequestType;
import bg.energo.phoenix.cashterminal.model.enums.CashTerminalStatusCode;
import bg.energo.phoenix.cashterminal.model.request.CashTerminalInitPayRequest;
import bg.energo.phoenix.cashterminal.model.response.CashTerminalInitPaymentResponse;
import bg.energo.phoenix.cashterminal.repository.CashTerminalPaymentDetailsRepository;
import bg.energo.phoenix.cashterminal.repository.CashTerminalPaymentRepository;
import bg.energo.phoenix.cashterminal.repository.CashTerminalVerifyDetailsRepository;
import bg.energo.phoenix.cashterminal.repository.CashTerminalVerifyRepository;
import bg.energo.phoenix.model.CheckLiabilityDetails;
import bg.energo.phoenix.model.CustomerWrapper;
import bg.energo.phoenix.model.entity.customer.Customer;
import bg.energo.phoenix.model.entity.receivable.collectionChannel.CollectionChannel;
import bg.energo.phoenix.model.response.customer.CustomerPaymentInfo;
import bg.energo.phoenix.repository.customer.CustomerDetailsRepository;
import bg.energo.phoenix.repository.customer.CustomerRepository;
import bg.energo.phoenix.repository.receivable.collectionChannel.CollectionChannelRepository;
import bg.energo.phoenix.util.epb.EPBJsonUtils;
import bg.energo.phoenix.utils.EPBFunctionUtils;
import bg.energo.phoenix.utils.EPBPaymentCalculationUtils;
import bg.energo.phoenix.utils.EPBSignatureUtils;
import bg.energo.phoenix.utils.EPBStringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Slf4j
@Service
public class CashTerminalInitPaymentService extends CashTerminalBaseService {

    private final CashTerminalVerifyRepository cashTerminalVerifyRepository;
    private final CashTerminalVerifyDetailsRepository cashTerminalVerifyDetailsRepository;

    public CashTerminalInitPaymentService(
            CashTerminalPaymentDetailsRepository cashTerminalPaymentDetailRepository,
            CashTerminalPaymentRepository cashTerminalPaymentRepository,
            CollectionChannelRepository collectionChannelRepository,
            CustomerDetailsRepository customerDetailsRepository,
            CustomerRepository customerRepository,
            CashTerminalMapperService cashTerminalMapperService,
            CashTerminalVerifyRepository cashTerminalVerifyRepository,
            CashTerminalVerifyDetailsRepository cashTerminalVerifyDetailsRepository
    ) {
        super(
                cashTerminalPaymentDetailRepository,
                cashTerminalPaymentRepository,
                collectionChannelRepository,
                customerDetailsRepository,
                customerRepository,
                cashTerminalMapperService
        );
        this.cashTerminalVerifyRepository = cashTerminalVerifyRepository;
        this.cashTerminalVerifyDetailsRepository = cashTerminalVerifyDetailsRepository;
    }

    public CashTerminalInitPaymentResponse initPay(CashTerminalInitPayRequest request, String requestCheckSum) {
        log.info("Initializing cash terminal payment with request: {}", request);

        CashTerminalInitPaymentResponse validationResponse = validateRequest(request, requestCheckSum);
        if (validationResponse != null) {
            log.error("Validation failed: {}", validationResponse.getSTATUS());
            return validationResponse;
        }

        CustomerWrapper customerWrapper = processCustomerDetails(request.getIDN());
        if (customerWrapper == null) {
            return createErrorResponse(
                    CashTerminalStatusCode.INVALID_SUBSCRIBER_NUMBER,
                    "No customer found for the identifier",
                    request.getIDN()
            );
        }
        Customer customer = customerWrapper.customer();
        log.info("Customer found: {}. Fetching collection channel...", customer.getIdentifier());

        CollectionChannel collectionChannel = fetchCollectionChannel();
        if (collectionChannel == null) {
            return createErrorResponse(
                    CashTerminalStatusCode.GENERAL_ERROR,
                    "No collection channel found for this request",
                    EPBJsonUtils.asJsonString(request)
            );
        }
        log.info("Collection channel ID fetched: {}", collectionChannel.getId());

        List<CheckLiabilityDetails> liabilities = fetchLiabilities(
                collectionChannel.getId(),
                customer.getId(),
                customerWrapper.billingGroupNumber()
        );
        if (CollectionUtils.isEmpty(liabilities)) {
            return createErrorResponse(
                    CashTerminalStatusCode.NO_OBLIGATION,
                    "No liabilities found for customer identifier",
                    request.getIDN()
            );
        }
        log.info("Liabilities found: {}. Calculating total sum...", liabilities.size());

        long sumAmountOfLiabilities = EPBPaymentCalculationUtils.computeScaledLiabilitiesAmount(liabilities);
        if (sumAmountOfLiabilities <= 0) {
            return createErrorResponse(
                    CashTerminalStatusCode.NO_OBLIGATION,
                    "The total liability amount must be greater than zero for the customer with identifier",
                    request.getIDN()
            );
        }
        log.info("Total liabilities amount: {}. Proceeding with request type check...", sumAmountOfLiabilities);

        saveRequestDetails(
                request.getTYPE(),
                request.getTID(),
                collectionChannel,
                customerWrapper,
                liabilities,
                sumAmountOfLiabilities
        );
        log.info("Payment initialization completed successfully. Returning success response.");

        return createSuccessResponse(
                customerWrapper,
                liabilities,
                collectionChannel,
                sumAmountOfLiabilities
        );
    }

    private void saveRequestDetails(
            CashTerminalRequestType requestType,
            String tid,
            CollectionChannel collectionChannel,
            CustomerWrapper customerWrapper,
            List<CheckLiabilityDetails> liabilities,
            Long convertedCoinTotalAmount
    ) {
        if (CashTerminalRequestType.BILLING.equals(requestType)) {
            savePaymentDetails(
                    tid,
                    collectionChannel,
                    customerWrapper,
                    liabilities,
                    CashTerminalStatusCode.OK,
                    convertedCoinTotalAmount,
                    requestType
            );
        } else if (CashTerminalRequestType.CHECK.equals(requestType)) {
            saveVerifyDetails(
                    collectionChannel.getId(),
                    customerWrapper.customer().getIdentifier(),
                    customerWrapper.billingGroupNumber(),
                    liabilities,
                    CashTerminalStatusCode.OK,
                    convertedCoinTotalAmount,
                    requestType
            );
        }
    }

    private CashTerminalInitPaymentResponse validateRequest(CashTerminalInitPayRequest request, String requestCheckSum) {
        log.info("Validating request checksum...");
        if (EPBSignatureUtils.isInvalidCashTerminalInitSignature(request, requestCheckSum, secretKey)) {
            return createErrorResponse(
                    CashTerminalStatusCode.INVALID_CHECKSUM,
                    "The checksum for the request is invalid.",
                    EPBJsonUtils.asJsonString(request)
            );
        }

        log.info("Validating merchant ID...");
        if (isInvalidMerchantId(request.getMERCHANTID())) {
            return createErrorResponse(
                    CashTerminalStatusCode.GENERAL_ERROR,
                    "Merchant not found for ID",
                    request.getMERCHANTID()
            );
        }

        log.info("Validating request type and transaction ID...");
        if (isCheckRequestWithTid(request.getTYPE(), request.getTID())) {
            return createErrorResponse(
                    CashTerminalStatusCode.GENERAL_ERROR,
                    "Invalid request: Request type is CHECK, but a TID was provided",
                    EPBJsonUtils.asJsonString(request)
            );
        }

        if (isBillingRequestWithInvalidTid(request.getTYPE(), request.getTID())) {
            return createErrorResponse(
                    CashTerminalStatusCode.GENERAL_ERROR,
                    "A 'BILLING' request type requires a valid TID",
                    EPBJsonUtils.asJsonString(request)
            );
        }

        log.info("Request validation successful.");
        return null;
    }

    public void savePaymentDetails(
            String tid,
            CollectionChannel collectionChannel,
            CustomerWrapper customerWrapper,
            List<CheckLiabilityDetails> liabilities,
            CashTerminalStatusCode statusCode,
            Long convertedCoinTotalAmount,
            CashTerminalRequestType requestType
    ) {
        bg.energo.phoenix.cashterminal.model.entity.CashTerminalPaymentEntity existing = findPaymentByTransactionId(tid);
        if (existing == null) {
            log.info("No existing payment details found for transaction ID: {}. Proceeding to save new payment details.", tid);

            String customerIdentifier = customerWrapper.customer().getIdentifier();
            Long customerId = customerWrapper.customer().getId();
            Long collectionChannelId = collectionChannel.getId();
            String billingGroupNumber = customerWrapper.billingGroupNumber();
            log.debug("Started saving payment details for customer: {} in channel: {} with transaction ID: {}", customerIdentifier, collectionChannelId, tid);

            java.util.List<bg.energo.phoenix.cashterminal.model.entity.CashTerminalPaymentDetailsEntity> paymentDetails = cashTerminalMapperService.mapToDetailEntities(
                    liabilities,
                    cashTerminalMapperService::mapToPaymentDetailEntity
            );
            log.debug("Mapped {} liability details to CashTerminalPaymentDetailsEntity", paymentDetails.size());

            java.math.BigDecimal totalAmount = EPBPaymentCalculationUtils.calculateTotalAmount(paymentDetails, bg.energo.phoenix.cashterminal.model.entity.CashTerminalPaymentDetailsEntity::getAmount);
            java.math.BigDecimal totalLfpAmount = EPBPaymentCalculationUtils.calculateTotalAmount(paymentDetails, bg.energo.phoenix.cashterminal.model.entity.CashTerminalPaymentDetailsEntity::getLfpAmount);
            java.math.BigDecimal totalSumAmount = EPBPaymentCalculationUtils.calculateTotalAmount(paymentDetails, bg.energo.phoenix.cashterminal.model.entity.CashTerminalPaymentDetailsEntity::getTotalAmount);
            log.debug("Calculated totals: Amount = {}, LFP Amount = {}, Total Amount = {}", totalAmount, totalLfpAmount, totalSumAmount);

            bg.energo.phoenix.cashterminal.model.entity.CashTerminalPaymentEntity entity = cashTerminalMapperService.buildCashTerminalPaymentEntity(
                    tid,
                    collectionChannelId,
                    liabilities.stream().findFirst().map(CheckLiabilityDetails::getCurrencyId).orElse(null),
                    customerId,
                    merchantCode,
                    customerIdentifier,
                    billingGroupNumber,
                    totalAmount,
                    totalLfpAmount,
                    totalSumAmount,
                    convertedCoinTotalAmount,
                    collectionChannel.getCombineLiabilities(),
                    statusCode.getCode(),
                    requestType,
                    CashTerminalPaymentStatus.INITIALIZED
            );

            log.debug("Saving CashTerminalPaymentEntity for customer: {} and channel: {} with transaction ID: {}", customerIdentifier, collectionChannelId, tid);
            savePaymentEntities(entity, paymentDetails);
            log.info("Successfully saved payment details for customer: {} in channel: {} with transaction ID: {}", customerIdentifier, collectionChannelId, tid);
        } else {
            log.info("Payment details already exist for transaction ID: {}. Skipping save operation.", tid);
        }
    }

    public void saveVerifyDetails(
            Long collectionChannelId,
            String customerIdentifier,
            String billingGroupNumber,
            List<CheckLiabilityDetails> liabilities,
            CashTerminalStatusCode statusCode,
            Long convertedCoinTotalAmount,
            CashTerminalRequestType requestType
    ) {
        log.debug("Started saving verify details for customer: {} in channel: {}", customerIdentifier, collectionChannelId);
        java.util.List<bg.energo.phoenix.cashterminal.model.entity.CashTerminalVerifyDetailsEntity> verifyDetails = cashTerminalMapperService.mapToDetailEntities(
                liabilities,
                cashTerminalMapperService::mapToVerifyDetailEntity
        );
        log.debug("Mapped {} liability details to CashTerminalVerifyDetailsEntity", verifyDetails.size());

        java.math.BigDecimal totalAmount = EPBPaymentCalculationUtils.calculateTotalAmount(verifyDetails, bg.energo.phoenix.cashterminal.model.entity.CashTerminalVerifyDetailsEntity::getAmount);
        java.math.BigDecimal totalLfpAmount = EPBPaymentCalculationUtils.calculateTotalAmount(verifyDetails, bg.energo.phoenix.cashterminal.model.entity.CashTerminalVerifyDetailsEntity::getLfpAmount);
        java.math.BigDecimal totalSumAmount = EPBPaymentCalculationUtils.calculateTotalAmount(verifyDetails, bg.energo.phoenix.cashterminal.model.entity.CashTerminalVerifyDetailsEntity::getTotalAmount);
        log.debug("Calculated totals: Amount = {}, LFP Amount = {}, Total Amount = {}", totalAmount, totalLfpAmount, totalSumAmount);

        bg.energo.phoenix.cashterminal.model.entity.CashTerminalVerifyEntity verifyEntity = cashTerminalMapperService.buildCashTerminalVerifyEntity(
                collectionChannelId,
                liabilities.stream().findFirst().map(CheckLiabilityDetails::getCurrencyId).orElse(null),
                merchantCode,
                customerIdentifier,
                billingGroupNumber,
                totalAmount,
                totalLfpAmount,
                totalSumAmount,
                convertedCoinTotalAmount,
                statusCode.getCode(),
                requestType
        );

        log.debug("Saving CashTerminalVerifyEntity for customer: {} and channel: {}", customerIdentifier, collectionChannelId);
        saveVerifyEntities(verifyEntity, verifyDetails);
    }

    private void savePaymentEntities(
            bg.energo.phoenix.cashterminal.model.entity.CashTerminalPaymentEntity paymentEntity,
            java.util.List<bg.energo.phoenix.cashterminal.model.entity.CashTerminalPaymentDetailsEntity> paymentDetailsEntities
    ) {
        cashTerminalPaymentRepository.saveAndFlush(paymentEntity);

        java.util.List<bg.energo.phoenix.cashterminal.model.entity.CashTerminalPaymentDetailsEntity> collected = paymentDetailsEntities
                .stream()
                .peek(detail -> detail.setCashTerminalPaymentId(paymentEntity.getId()))
                .toList();

        log.debug("Saving CashTerminalPaymentDetailsEntity for {} liabilities", collected.size());
        cashTerminalPaymentDetailRepository.saveAll(collected);
    }

    private void saveVerifyEntities(
            bg.energo.phoenix.cashterminal.model.entity.CashTerminalVerifyEntity verifyEntity,
            java.util.List<bg.energo.phoenix.cashterminal.model.entity.CashTerminalVerifyDetailsEntity> verifyDetailsEntities
    ) {
        cashTerminalVerifyRepository.saveAndFlush(verifyEntity);

        java.util.List<bg.energo.phoenix.cashterminal.model.entity.CashTerminalVerifyDetailsEntity> collected = verifyDetailsEntities
                .stream()
                .peek(detail -> detail.setCashTerminalVerifyId(verifyEntity.getId()))
                .toList();

        log.debug("Saving CashTerminalVerifyDetailsEntity for {} liabilities", collected.size());
        cashTerminalVerifyDetailsRepository.saveAll(collected);
    }

    private CashTerminalInitPaymentResponse createErrorResponse(CashTerminalStatusCode statusCode, String logMessage, String idn) {
        log.error("{}: {}", logMessage, idn);
        return new CashTerminalInitPaymentResponse(statusCode);
    }

    public CashTerminalInitPaymentResponse createSuccessResponse(
            CustomerWrapper customerWrapper,
            List<CheckLiabilityDetails> liabilities,
            CollectionChannel collectionChannel,
            Long sumAmountOfLiabilities
    ) {
        String customerNumber = String.valueOf(customerWrapper.customer().getCustomerNumber());

        CustomerPaymentInfo customerPaymentInfo = customerDetailsRepository
                .findCustomerPaymentInfoByCustomerDetailId(customerWrapper.customer().getLastCustomerDetailId())
                .orElse(null);

        CashTerminalInitPaymentResponse response = new CashTerminalInitPaymentResponse();

        response.setLONGDESC(
                EPBStringUtils.formatCombinedLongDesc(
                        customerNumber,
                        customerPaymentInfo,
                        liabilities
                )
        );
        response.setSHORTDESC(
                EPBStringUtils.formatShortDesc(customerNumber)
        );
        response.setVALIDTO(
                EPBFunctionUtils.dateStringFromLocalDate(
                        EPBFunctionUtils.getNearestDueDate(liabilities, java.time.LocalDate.now())
                )
        );
        response.setSTATUS(CashTerminalStatusCode.OK.getCode());
        response.setAMOUNT(sumAmountOfLiabilities);
        response.setIDN(customerNumber);
        if (collectionChannel.getCombineLiabilities() == null || !collectionChannel.getCombineLiabilities()) {
            response.setINVOICES(
                    cashTerminalMapperService.mapToInvoicesResponse(
                            liabilities,
                            customerPaymentInfo,
                            customerNumber
                    )
            );
        }
        return response;
    }
}

