package bg.energo.phoenix.service;

import bg.energo.phoenix.model.CheckLiabilityDetails;
import bg.energo.phoenix.model.CustomerWrapper;
import bg.energo.phoenix.model.entity.*;
import bg.energo.phoenix.model.entity.customer.Customer;
import bg.energo.phoenix.model.entity.receivable.collectionChannel.CollectionChannel;
import bg.energo.phoenix.model.enums.EPayStatusCode;
import bg.energo.phoenix.model.enums.EasyPayPaymentStatus;
import bg.energo.phoenix.model.enums.EasyPayRequestType;
import bg.energo.phoenix.model.request.InitPayRequest;
import bg.energo.phoenix.model.response.EasyPayInitPaymentResponse;
import bg.energo.phoenix.model.response.customer.CustomerPaymentInfo;
import bg.energo.phoenix.repository.*;
import bg.energo.phoenix.repository.customer.CustomerDetailsRepository;
import bg.energo.phoenix.repository.customer.CustomerRepository;
import bg.energo.phoenix.repository.receivable.collectionChannel.CollectionChannelRepository;
import bg.energo.phoenix.repository.receivable.customerLiability.CustomerLiabilityRepository;
import bg.energo.phoenix.util.epb.EPBJsonUtils;
import bg.energo.phoenix.utils.EPBFunctionUtils;
import bg.energo.phoenix.utils.EPBPaymentCalculationUtils;
import bg.energo.phoenix.utils.EPBSignatureUtils;
import bg.energo.phoenix.utils.EPBStringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
public class EasyPayInitPaymentService extends EasyPayBaseService {

    protected final EasyPayVerifyRepository easyPayVerifyRepository;
    protected final EasyPayVerifyDetailsRepository easyPayVerifyDetailsRepository;

    public EasyPayInitPaymentService(
            EasyPayPaymentDetailsRepository easyPayPaymentDetailRepository,
            EasyPayPaymentRepository easyPayPaymentRepository,
            CollectionChannelRepository collectionChannelRepository,
            CustomerDetailsRepository customerDetailsRepository,
            CustomerRepository customerRepository,
            EasyPayMapperService easyPayMapperService,
            EasyPayVerifyRepository easyPayVerifyRepository,
            EasyPayVerifyDetailsRepository easyPayVerifyDetailsRepository,
            CustomerLiabilityRepository customerLiabilityRepository,
            ConfigurationRepository configurationRepository
    ) {
        super(
                easyPayPaymentDetailRepository,
                easyPayPaymentRepository,
                collectionChannelRepository,
                customerDetailsRepository,
                customerRepository,
                easyPayMapperService,
                customerLiabilityRepository,
                configurationRepository
        );
        this.easyPayVerifyRepository = easyPayVerifyRepository;
        this.easyPayVerifyDetailsRepository = easyPayVerifyDetailsRepository;
    }

    /**
     * Initializes the payment process by validating the request, processing customer details,
     * fetching collection channel and liabilities, and then saving the liabilities state if necessary.
     * Returns an appropriate success or error response based on the result of these operations.
     *
     * @param request         The payment initialization request, containing customer and transaction details.
     * @param requestCheckSum The checksum used to validate the request integrity.
     * @return An {@link EasyPayInitPaymentResponse} object indicating the result of the payment initialization.
     * If any validation or processing step fails, an error response is returned.
     * Otherwise, a success response is returned with the calculated total liability amount.
     */
    public EasyPayInitPaymentResponse initPay(InitPayRequest request, String requestCheckSum) {
        log.info("Initializing payment with request: {}", request);

        // Validate request
        EasyPayInitPaymentResponse responseAfterValidation = validateRequest(request, requestCheckSum);
        if (responseAfterValidation != null) {
            log.error("Validation failed: {}", responseAfterValidation.getSTATUS());
            return responseAfterValidation;
        }

        // Process customer details
        CustomerWrapper customerWrapper = processCustomerDetails(request.getIDN());
        if (customerWrapper == null) {
            return createErrorResponse(
                    EPayStatusCode.INVALID_SUBSCRIBER_NUMBER,
                    "No customer found for the identifier",
                    request.getIDN()
            );
        }
        Customer customer = customerWrapper.customer();
        log.info("Customer found: {}. Fetching collection channel...", customer.getIdentifier());

        CollectionChannel collectionChannel = fetchCollectionChannel();
        if (collectionChannel == null) {
            return createErrorResponse(
                    EPayStatusCode.GENERAL_ERROR,
                    "No collection channel found for this request",
                    EPBJsonUtils.asJsonString(request)
            );
        }
        log.info("Collection channel ID fetched: {}", collectionChannel.getId());

        // Fetch liabilities
        List<CheckLiabilityDetails> liabilities = fetchLiabilities(
                collectionChannel.getId(),
                customer.getId(),
                customerWrapper.billingGroupNumber()
        );

        // For selected payment sources (TID AID), hide pro-forma reconnection liabilities
        if (EasyPayRequestType.BILLING.equals(request.getTYPE())
                && EPBFunctionUtils.shouldHideProformaReconnectionLiabilities(request.getTID())
                && !CollectionUtils.isEmpty(liabilities)) {
            List<Long> liabilityIds = liabilities.stream()
                    .map(CheckLiabilityDetails::getLiabilityId)
                    .filter(Objects::nonNull)
                    .toList();
            Set<Long> proformaReconnectionIds = findProformaReconnectionLiabilityIds(liabilityIds);
            if (!proformaReconnectionIds.isEmpty()) {
                log.info(
                        "Excluding {} pro-forma reconnection liabilities for TID ending with {}",
                        proformaReconnectionIds.size(),
                        request.getTID().substring(request.getTID().length() - 6)
                );
                liabilities = EPBFunctionUtils.excludeLiabilitiesByIds(liabilities, proformaReconnectionIds);
            }
        }

        if (CollectionUtils.isEmpty(liabilities)) {
            return createErrorResponse(
                    EPayStatusCode.NO_OBLIGATION,
                    "No liabilities found for customer identifier",
                    request.getIDN()
            );
        }
        log.info("Liabilities found: {}. Calculating total sum...", liabilities.size());

        long sumAmountOfLiabilities = EPBPaymentCalculationUtils.computeScaledLiabilitiesAmount(liabilities);
        if (sumAmountOfLiabilities <= 0) {
            return createErrorResponse(
                    EPayStatusCode.NO_OBLIGATION,
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

        // Return success response
        return createSuccessResponse(
                customerWrapper,
                liabilities,
                collectionChannel,
                sumAmountOfLiabilities
        );
    }

    /**
     * Saves the request details based on the request type.
     * This method checks the {@link EasyPayRequestType} and calls the appropriate method to save the details.
     * If the request type is {@link EasyPayRequestType#BILLING}, it will save the payment details using the {@link #savePaymentDetails} method.
     * If the request type is {@link EasyPayRequestType#CHECK}, it will save the verification details using the {@link #saveVerifyDetails} method.
     *
     * @param requestType       The type of the request which determines whether to save payment or verification details.
     * @param tid               The transaction ID associated with the request.
     * @param collectionChannel The collection channel.
     * @param customerWrapper   The unique identifier of the customer.
     * @param liabilities       A list of {@link CheckLiabilityDetails} representing the liabilities to be processed.
     */
    private void saveRequestDetails(
            EasyPayRequestType requestType,
            String tid,
            CollectionChannel collectionChannel,
            CustomerWrapper customerWrapper,
            List<CheckLiabilityDetails> liabilities,
            Long convertedCoinTotalAmount
    ) {
        if (EasyPayRequestType.BILLING.equals(requestType)) {
            savePaymentDetails(
                    tid,
                    collectionChannel,
                    customerWrapper,
                    liabilities,
                    EPayStatusCode.OK,
                    convertedCoinTotalAmount,
                    requestType
            );
        } else if (EasyPayRequestType.CHECK.equals(requestType)) {
            saveVerifyDetails(
                    collectionChannel.getId(),
                    customerWrapper.customer().getIdentifier(),
                    customerWrapper.billingGroupNumber(),
                    liabilities,
                    EPayStatusCode.OK,
                    convertedCoinTotalAmount,
                    requestType
            );
        }
    }

    /**
     * Validates the incoming payment initialization request by checking the checksum, merchant ID,
     * request type, and transaction ID. Returns an appropriate error response if any validation fails.
     *
     * @param request         The payment initialization request to be validated.
     * @param requestCheckSum The checksum used to validate the integrity of the request.
     * @return An {@link EasyPayInitPaymentResponse} object indicating the result of the validation.
     * If any validation step fails, an error response is returned with the corresponding error code.
     * If all validations pass, returns null to indicate no issues.
     */
    private EasyPayInitPaymentResponse validateRequest(InitPayRequest request, String requestCheckSum) {
        log.info("Validating request checksum...");
        Optional<Configuration> currentConfiguration = configurationRepository.findLatestConfiguration();
        String secretKeyFromConfig = null;
        if (currentConfiguration.isPresent()) {
            secretKeyFromConfig = currentConfiguration.get().getEasyPaySecretKey();
        }
        String finalSecretKey = secretKeyFromConfig == null ? secretKey : secretKeyFromConfig;

        if (EPBSignatureUtils.isInvalidInitSignature(request, requestCheckSum, finalSecretKey)) {
            return createErrorResponse(
                    EPayStatusCode.INVALID_CHECKSUM,
                    "The checksum for the request is invalid.",
                    EPBJsonUtils.asJsonString(request)
            );
        }

        log.info("Validating merchant ID...");
        if (isInvalidMerchantId(request.getMERCHANTID())) {
            return createErrorResponse(
                    EPayStatusCode.GENERAL_ERROR,
                    "Merchant not found for ID",
                    request.getMERCHANTID()
            );
        }

        log.info("Validating request type and transaction ID...");
        if (isCheckRequestWithTid(request.getTYPE(), request.getTID())) {
            return createErrorResponse(
                    EPayStatusCode.GENERAL_ERROR,
                    "Invalid request: Request type is CHECK, but a TID was provided",
                    EPBJsonUtils.asJsonString(request)
            );
        }

        if (isBillingRequestWithInvalidTid(request.getTYPE(), request.getTID())) {
            return createErrorResponse(
                    EPayStatusCode.GENERAL_ERROR,
                    "A 'BILLING' request type requires a valid TID",
                    EPBJsonUtils.asJsonString(request)
            );
        }

        log.info("Request validation successful.");
        return null; // No issues found, proceed with the payment initialization
    }

    /**
     * Checks if the request type is BILLING and processes liabilities by calculating the total amounts
     * and saving the payment details to the repository.
     *
     * @param tid               The transaction ID.
     * @param collectionChannel The collection channel.
     * @param customerWrapper   The identifier of the customer.
     * @param liabilities       A list of liabilities details to process.
     */
    public void savePaymentDetails(
            String tid,
            CollectionChannel collectionChannel,
            CustomerWrapper customerWrapper,
            List<CheckLiabilityDetails> liabilities,
            EPayStatusCode statusCode,
            Long convertedCoinTotalAmount,
            EasyPayRequestType requestType
    ) {
        EasyPayPaymentEntity existedEasyPayPaymentEntity = findEasyPayPaymentByTransactionId(tid);
        if (existedEasyPayPaymentEntity == null) {
            log.info("No existing payment details found for transaction ID: {}. Proceeding to save new payment details.", tid);

            String customerIdentifier = customerWrapper.customer().getIdentifier();
            Long customerId = customerWrapper.customer().getId();
            Long collectionChannelId = collectionChannel.getId();
            String billingGroupNumber = customerWrapper.billingGroupNumber();
            log.debug("Started saving payment details for customer: {} in channel: {} with transaction ID: {}", customerIdentifier, collectionChannelId, tid);
            List<EasyPayPaymentDetailsEntity> paymentDetails = easyPayMapperService.mapToDetailEntities(liabilities, easyPayMapperService::mapToPaymentDetailEntity);
            log.debug("Mapped {} liability details to EasyPayPaymentDetailsEntity", paymentDetails.size());

            BigDecimal totalAmount = EPBPaymentCalculationUtils.calculateTotalAmount(paymentDetails, EasyPayPaymentDetailsEntity::getAmount);
            BigDecimal totalLfpAmount = EPBPaymentCalculationUtils.calculateTotalAmount(paymentDetails, EasyPayPaymentDetailsEntity::getLfpAmount);
            BigDecimal totalSumAmount = EPBPaymentCalculationUtils.calculateTotalAmount(paymentDetails, EasyPayPaymentDetailsEntity::getTotalAmount);
            log.debug("Calculated totals: Amount = {}, LFP Amount = {}, Total Amount = {}", totalAmount, totalLfpAmount, totalSumAmount);

            Optional<Configuration> currentConfiguration = configurationRepository.findLatestConfiguration();
            String merchantIdFromConfig = null;
            if (currentConfiguration.isPresent()) {
                merchantIdFromConfig = currentConfiguration.get().getEasyPayMerchantId();
            }
            String finalMerchantId = merchantIdFromConfig == null ? easyPayMerchantCode : merchantIdFromConfig;
            EasyPayPaymentEntity easyPayPaymentEntity = easyPayMapperService.buildEasyPayPaymentEntity(
                    tid,
                    collectionChannelId,
                    liabilities.stream().findFirst().map(CheckLiabilityDetails::getCurrencyId).orElse(null),
                    customerId,
                    finalMerchantId,
                    customerIdentifier,
                    billingGroupNumber,
                    totalAmount,
                    totalLfpAmount,
                    totalSumAmount,
                    convertedCoinTotalAmount,
                    collectionChannel.getCombineLiabilities(),
                    statusCode.getCode(),
                    requestType,
                    EasyPayPaymentStatus.INITIALIZED
            );

            log.debug("Saving EasyPayPaymentEntity for customer: {} and channel: {} with transaction ID: {}", customerIdentifier, collectionChannelId, tid);
            savePaymentEntities(easyPayPaymentEntity, paymentDetails);
            log.info("Successfully saved payment details for customer: {} in channel: {} with transaction ID: {}", customerIdentifier, collectionChannelId, tid);
        } else {
            log.info("Payment details already exist for transaction ID: {}. Skipping save operation.", tid);
        }
    }

    /**
     * Asynchronously processes and saves payment verification details.
     * <p>
     * This method maps the provided liability details to `EasyPayVerifyDetailsEntity`, calculates the total amounts
     * for the verification, and creates an `EasyPayVerifyEntity`. Finally, it saves the `EasyPayVerifyEntity` and its details
     * into the respective repositories.
     *
     * @param collectionChannelId The collection channel identifier.
     * @param customerIdentifier  The unique identifier of the customer.
     * @param liabilities         A list of `CheckLiabilityDetails` to be processed.
     */
    public void saveVerifyDetails(
            Long collectionChannelId,
            String customerIdentifier,
            String billingGroupNumber,
            List<CheckLiabilityDetails> liabilities,
            EPayStatusCode statusCode,
            Long convertedCoinTotalAmount,
            EasyPayRequestType requestType
    ) {
        log.debug("Started saving verify details for customer: {} in channel: {}", customerIdentifier, collectionChannelId);
        List<EasyPayVerifyDetailsEntity> verifyDetails = easyPayMapperService.mapToDetailEntities(liabilities, easyPayMapperService::mapToVerifyDetailEntity);
        log.debug("Mapped {} liability details to EasyPayVerifyDetailsEntity", verifyDetails.size());

        BigDecimal totalAmount = EPBPaymentCalculationUtils.calculateTotalAmount(verifyDetails, EasyPayVerifyDetailsEntity::getAmount);
        BigDecimal totalLfpAmount = EPBPaymentCalculationUtils.calculateTotalAmount(verifyDetails, EasyPayVerifyDetailsEntity::getLfpAmount);
        BigDecimal totalSumAmount = EPBPaymentCalculationUtils.calculateTotalAmount(verifyDetails, EasyPayVerifyDetailsEntity::getTotalAmount);
        log.debug("Calculated totals: Amount = {}, LFP Amount = {}, Total Amount = {}", totalAmount, totalLfpAmount, totalSumAmount);

        Optional<Configuration> currentConfiguration = configurationRepository.findLatestConfiguration();
        String merchantIdFromConfig = null;
        if (currentConfiguration.isPresent()) {
            merchantIdFromConfig = currentConfiguration.get().getEasyPayMerchantId();
        }
        String finalMerchantId = merchantIdFromConfig == null ? easyPayMerchantCode : merchantIdFromConfig;

        EasyPayVerifyEntity easyPayVerifyEntity = easyPayMapperService.buildEasyPayVerifyEntity(
                collectionChannelId,
                liabilities.stream().findFirst().map(CheckLiabilityDetails::getCurrencyId).orElse(null),
                finalMerchantId,
                customerIdentifier,
                billingGroupNumber,
                totalAmount,
                totalLfpAmount,
                totalSumAmount,
                convertedCoinTotalAmount,
                statusCode.getCode(),
                requestType
        );

        log.debug("Saving EasyPayVerifyEntity for customer: {} and channel: {}", customerIdentifier, collectionChannelId);
        saveVerifyEntities(easyPayVerifyEntity, verifyDetails);
    }

    /**
     * Saves the provided EasyPay payment entity and its associated payment detail entities.
     * <p>
     * This method first saves the main payment entity (`EasyPayPaymentEntity`) into the repository,
     * and then it updates the provided list of payment details entities (`EasyPayPaymentDetailsEntity`) by setting
     * the `easyPayPaymentId` on each entity. Finally, it saves all the updated payment details entities.
     *
     * @param paymentEntity          The main EasyPay payment entity to be saved.
     * @param paymentDetailsEntities The list of EasyPay payment details entities to be saved, associated with the payment entity.
     */
    private void savePaymentEntities(
            EasyPayPaymentEntity paymentEntity,
            List<EasyPayPaymentDetailsEntity> paymentDetailsEntities
    ) {
        easyPayPaymentRepository.saveAndFlush(paymentEntity);

        List<EasyPayPaymentDetailsEntity> collected = paymentDetailsEntities
                .stream()
                .peek(easyPayPaymentDetailsEntity -> easyPayPaymentDetailsEntity.setEasyPayPaymentId(paymentEntity.getId()))
                .toList();

        log.debug("Saving EasyPayPaymentDetailEntity for {} liabilities", collected.size());
        easyPayPaymentDetailRepository.saveAll(collected);
    }

    /**
     * Saves the provided EasyPay verify entity and its associated verify detail entities.
     * <p>
     * This method first saves the main verify entity (`EasyPayVerifyEntity`) into the repository,
     * and then it updates the provided list of verify details entities (`EasyPayVerifyDetailsEntity`) by setting
     * the `easyPayVerifyId` on each entity. Finally, it saves all the updated verify details entities.
     *
     * @param verifyEntity          The main EasyPay verify entity to be saved.
     * @param verifyDetailsEntities The list of EasyPay verify details entities to be saved, associated with the verify entity.
     */
    private void saveVerifyEntities(
            EasyPayVerifyEntity verifyEntity,
            List<EasyPayVerifyDetailsEntity> verifyDetailsEntities
    ) {
        easyPayVerifyRepository.saveAndFlush(verifyEntity);

        List<EasyPayVerifyDetailsEntity> collected = verifyDetailsEntities
                .stream()
                .peek(verifyDetailEntity -> verifyDetailEntity.setEasyPayVerifyId(verifyEntity.getId()))
                .toList();

        log.debug("Saving EasyPayVerifyDetailsEntity for {} liabilities", collected.size());
        easyPayVerifyDetailsRepository.saveAll(collected);
    }

    /**
     * Creates an error response for the payment initialization process.
     * This method logs an error message with the provided status code and identifier, and then
     * returns a new `EasyPayInitPaymentResponse` with the provided status code. The status code
     * is used to indicate the type of error that occurred.
     *
     * @param statusCode The status code representing the error type. This is an enumeration of possible error statuses.
     * @param logMessage A message to log that describes the error, including the associated identifier.
     * @param idn        The identifier (e.g., customer ID) associated with the error, which is included in the log message.
     * @return An `EasyPayInitPaymentResponse` object with the provided error status code.
     * @see EPayStatusCode
     * @see EasyPayInitPaymentResponse
     */
    private EasyPayInitPaymentResponse createErrorResponse(EPayStatusCode statusCode, String logMessage, String idn) {
        log.error("{}: {}", logMessage, idn);
        return new EasyPayInitPaymentResponse(statusCode);
    }

    /**
     * Creates a successful response for initiating an EasyPay payment.
     * This method generates an {@link EasyPayInitPaymentResponse} object with various details about the customer,
     * their liabilities, and the collection channel. It processes the customer information, liabilities, and relevant
     * collection data to generate the necessary payment response details.
     *
     * @param customerWrapper        The {@link CustomerWrapper} object containing the customer details to be used in the response.
     * @param liabilities            A list of {@link CheckLiabilityDetails} objects representing the liabilities associated with the customer.
     * @param collectionChannel      The {@link CollectionChannel} object that provides information about how the liabilities are to be collected.
     * @param sumAmountOfLiabilities The total amount of the liabilities to be paid.
     * @return An {@link EasyPayInitPaymentResponse} containing the details for initiating the EasyPay payment, including
     * long and short descriptions, due date, payment status, and invoices.
     */
    public EasyPayInitPaymentResponse createSuccessResponse(
            CustomerWrapper customerWrapper,
            List<CheckLiabilityDetails> liabilities,
            CollectionChannel collectionChannel,
            Long sumAmountOfLiabilities
    ) {
        String customerNumber = String.valueOf(customerWrapper.customer().getCustomerNumber());

        // Get customer payment info for formatting
        CustomerPaymentInfo customerPaymentInfo = customerDetailsRepository
                .findCustomerPaymentInfoByCustomerDetailId(customerWrapper.customer().getLastCustomerDetailId())
                .orElse(null);

        EasyPayInitPaymentResponse payInitPaymentResponse = new EasyPayInitPaymentResponse();

        // Format LONGDESC
        payInitPaymentResponse.setLONGDESC(
                EPBStringUtils.formatCombinedLongDesc(
                        customerNumber,
                        customerPaymentInfo,
                        liabilities
                )
        );

        // Format SHORTDESC
        payInitPaymentResponse.setSHORTDESC(
                EPBStringUtils.formatShortDesc(customerNumber)
        );
        payInitPaymentResponse.setVALIDTO(
                EPBFunctionUtils.dateStringFromLocalDate(
                        EPBFunctionUtils.getNearestDueDate(liabilities, LocalDate.now())
                )
        );
        payInitPaymentResponse.setSTATUS(EPayStatusCode.OK.getCode());
//        payInitPaymentResponse.setStatusDescription(EPayStatusCode.OK.getDescription());
        payInitPaymentResponse.setAMOUNT(sumAmountOfLiabilities);
        payInitPaymentResponse.setIDN(customerNumber);
        if (collectionChannel.getCombineLiabilities() == null || !collectionChannel.getCombineLiabilities()) {
            payInitPaymentResponse.setINVOICES(
                    easyPayMapperService.mapToInvoicesResponse(
                            liabilities,
                            customerPaymentInfo,
                            customerNumber
                    )
            );
        }
        return payInitPaymentResponse;
    }

}
