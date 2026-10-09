package bg.energo.phoenix.service;

import bg.energo.phoenix.model.CustomerWrapper;
import bg.energo.phoenix.model.entity.Configuration;
import bg.energo.phoenix.model.entity.EasyPayPaymentDetailsEntity;
import bg.energo.phoenix.model.entity.EasyPayPaymentEntity;
import bg.energo.phoenix.model.entity.receivable.collectionChannel.CollectionChannel;
import bg.energo.phoenix.model.enums.EPayStatusCode;
import bg.energo.phoenix.model.enums.EasyPayPaymentStatus;
import bg.energo.phoenix.model.request.ConfirmPayRequest;
import bg.energo.phoenix.model.response.EasyPayConfirmPaymentResponse;
import bg.energo.phoenix.repository.ConfigurationRepository;
import bg.energo.phoenix.repository.EasyPayPaymentDetailsRepository;
import bg.energo.phoenix.repository.EasyPayPaymentRepository;
import bg.energo.phoenix.repository.customer.CustomerDetailsRepository;
import bg.energo.phoenix.repository.customer.CustomerRepository;
import bg.energo.phoenix.repository.receivable.collectionChannel.CollectionChannelRepository;
import bg.energo.phoenix.repository.receivable.customerLiability.CustomerLiabilityRepository;
import bg.energo.phoenix.util.epb.EPBJsonUtils;
import bg.energo.phoenix.utils.EPBFunctionUtils;
import bg.energo.phoenix.utils.EPBSignatureUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class EasyPayConfirmPaymentService extends EasyPayBaseService {

    private final PaymentOffsettingTransactionalService paymentOffsettingTransactionalService;
    private final PaymentCreationTransactionalService paymentCreationTransactionalService;
    private final CustomerLiabilityRepository customerLiabilityRepository;
    private final BillingGroupHelperService billingGroupHelperService;

    public EasyPayConfirmPaymentService(
            EasyPayPaymentDetailsRepository easyPayPaymentDetailRepository,
            EasyPayPaymentRepository easyPayPaymentRepository,
            CollectionChannelRepository collectionChannelRepository,
            CustomerDetailsRepository customerDetailsRepository,
            CustomerRepository customerRepository,
            EasyPayMapperService easyPayMapperService,
            PaymentCreationTransactionalService paymentCreationTransactionalService,
            CustomerLiabilityRepository customerLiabilityRepository,
            PaymentOffsettingTransactionalService paymentOffsettingTransactionalService,
            BillingGroupHelperService billingGroupHelperService,
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
        this.paymentCreationTransactionalService = paymentCreationTransactionalService;
        this.paymentOffsettingTransactionalService = paymentOffsettingTransactionalService;
        this.customerLiabilityRepository = customerLiabilityRepository;
        this.billingGroupHelperService = billingGroupHelperService;
    }

    /**
     * Confirms a payment request by validating the input data and processing the payment.
     * This method performs several key steps to process the payment confirmation:
     * Validates the incoming payment request and checksum.
     * Verifies that the provided date format is valid.
     * Processes customer details based on the provided identifier (IDN).
     * Fetches the associated collection channel.
     * Checks if a valid payment record exists for the provided Transaction ID (TID).
     * Handles the payment request by invoking the appropriate internal logic for payment processing.
     * If any validation or processing step fails, the method will return an error response. If all steps are successful, the method proceeds to handle the payment.
     *
     * @param request         The confirm payment request containing details such as TID, IDN, and date.
     * @param requestCheckSum The checksum for validating the request's integrity.
     * @return A {@link EasyPayConfirmPaymentResponse} representing the result of the payment confirmation, either a success or an error message.
     */
    @Transactional
    public EasyPayConfirmPaymentResponse confirmPay(ConfirmPayRequest request, String requestCheckSum) {
        log.info("Received confirm pay request: {}, requestCheckSum: {}", EPBJsonUtils.asJsonString(request), requestCheckSum);

        // Validate request
        EasyPayConfirmPaymentResponse responseAfterValidation = validateRequest(request, requestCheckSum);
        if (responseAfterValidation != null) {
            log.error("Validation failed: {}", responseAfterValidation.getSTATUS());
            return responseAfterValidation;
        }

        String tid = request.getTID();
        log.info("Validation passed for TID: {}", tid);

        CustomerWrapper customerWrapper = processCustomerDetails(request.getIDN());
        if (customerWrapper == null) {
            return createErrorResponse(
                    EPayStatusCode.INVALID_SUBSCRIBER_NUMBER,
                    tid,
                    "Customer not found for the provided identifier."
            );
        }

        CollectionChannel collectionChannel = fetchCollectionChannel();
        if (collectionChannel == null) {
            return createErrorResponse(
                    EPayStatusCode.GENERAL_ERROR,
                    tid,
                    "Collection channel not found for the request."
            );
        }

        EasyPayPaymentEntity easyPayPaymentEntity = findEasyPayPaymentByTransactionId(tid);
        if (easyPayPaymentEntity == null) {
            return createErrorResponse(
                    EPayStatusCode.GENERAL_ERROR,
                    tid,
                    "No payment record found for the provided Transaction ID (TID)."
            );
        }

        if (easyPayPaymentEntity.getPaymentStatus() == EasyPayPaymentStatus.CONFIRMED) {
            return createErrorResponse(
                    EPayStatusCode.REPEAT_NOTIFICATION,
                    tid,
                    "Repeat of already received notification: Payment with selected TID already exists"
            );
        }

        log.info("Proceeding to handle payment for TID: {}", tid);
        return handlePaymentRequest(
                request,
                easyPayPaymentEntity,
                customerWrapper,
                collectionChannel
        );
    }

    /**
     * Handles the payment request by processing full payments, partial payments, and validating the associated data.
     * This method checks if the payment amount matches the expected value and processes the payment accordingly.
     * It supports full payments, combined payments, and partial payments. It also ensures that invoice numbers
     * and payment details are provided, and validates that the partial payment is correct.
     *
     * @param request              The confirm payment request containing details such as TID, total amount, and invoice numbers.
     * @param easyPayPaymentEntity The payment entity associated with the transaction.
     * @param customerWrapper      A wrapper containing customer details.
     * @param collectionChannel    The collection channel through which the payment is processed.
     * @return A {@link EasyPayConfirmPaymentResponse} representing the result of the payment processing, either a success or an error message.
     */
    @Transactional
    public EasyPayConfirmPaymentResponse handlePaymentRequest(
            ConfirmPayRequest request,
            EasyPayPaymentEntity easyPayPaymentEntity,
            CustomerWrapper customerWrapper,
            CollectionChannel collectionChannel
    ) {
        String tid = request.getTID();

//        if (!easyPayPaymentEntity.getIsCombined() && request.getInvoiceNumbers().isEmpty()) {
//            return createErrorResponse(
//                    EPayStatusCode.GENERAL_ERROR,
//                    tid,
//                    "Invoice numbers are missing. Please provide invoices when combine liabilities is not checked."
//            );
//        }

        // Step 1: Check if the total amount matches
        if (easyPayPaymentEntity.getInitTotalAmountInCoins().equals(request.getTOTAL())) {
            log.info("Total amount matches for combined payment for TID: {}", tid);
            return processFullPayment(request, easyPayPaymentEntity, customerWrapper, collectionChannel);
        }

        // Step 2: Handle combined payment or missing data
        if (easyPayPaymentEntity.getIsCombined()) {
            return createErrorResponse(
                    EPayStatusCode.GENERAL_ERROR,
                    tid,
                    "The amount is incorrect for this combined payment."
            );
        }

        // Step 3: Validate invoices and associated details
        if (request.getInvoiceNumbers().isEmpty()) {
            return createErrorResponse(
                    EPayStatusCode.GENERAL_ERROR,
                    tid,
                    "Invoice numbers are missing. Please provide valid invoices."
            );
        }

        List<EasyPayPaymentDetailsEntity> easyPayPaymentDetailsEntities = easyPayPaymentDetailRepository.findByEasyPayPaymentId(easyPayPaymentEntity.getId());
        if (easyPayPaymentDetailsEntities.isEmpty()) {
            return createErrorResponse(
                    EPayStatusCode.GENERAL_ERROR,
                    tid,
                    "Payment details are missing. Please ensure all payment details are provided."
            );
        }

        log.info("Processing partial payment for TID: {}", tid);
        // Step 4: Check if partial payment is correct
        return processPartialPayment(request, easyPayPaymentEntity, customerWrapper, collectionChannel, easyPayPaymentDetailsEntities);
    }

    /**
     * Processes a full payment by creating the associated online payment, updating the payment entity,
     * and performing the necessary payment offsetting.
     * This method handles the following steps for processing a full payment:
     * Retrieves the payment details for the given payment entity.
     * Creates an online payment using the provided request details and payment details.
     * Updates the payment entity with the confirmation date, confirmed amount, and other relevant details.
     * Performs payment offsetting by invoking the appropriate internal logic.
     * Returns a success response if the payment creation and offsetting are successful. Otherwise, it returns an error response.
     *
     * @param request              The confirm payment request containing the total amount and other relevant payment details.
     * @param easyPayPaymentEntity The payment entity associated with the transaction.
     * @param customerWrapper      A wrapper containing customer details.
     * @param collectionChannel    The collection channel through which the payment is processed.
     * @return A {@link EasyPayConfirmPaymentResponse} representing the result of the payment processing, either a success or an error message.
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public EasyPayConfirmPaymentResponse processFullPayment(
            ConfirmPayRequest request,
            EasyPayPaymentEntity easyPayPaymentEntity,
            CustomerWrapper customerWrapper,
            CollectionChannel collectionChannel
    ) {
        log.info("Processing full payment for TID: {}", request.getTID());
        List<EasyPayPaymentDetailsEntity> easyPayPaymentDetailsEntities = easyPayPaymentDetailRepository.findByEasyPayPaymentId(
                easyPayPaymentEntity.getId()
        );
        log.info("Found {} payment details for TID: {}", easyPayPaymentDetailsEntities.size(), request.getTID());
        LocalDateTime date = EPBFunctionUtils.localDateTimeFromDateString(request.getDATE());
        Long paymentId = paymentCreationTransactionalService.createOnlinePayment(
                request.getTOTAL(),
                customerWrapper.customer(),
                date.toLocalDate(),
                billingGroupHelperService.fetchAccountPeriodId(date),
                collectionChannel,
                billingGroupHelperService.fetchContractBillingGroupIdForCustomer(customerWrapper.billingGroupNumber(), easyPayPaymentDetailsEntities)
        );
        if (paymentId != null && paymentId > 0) {
            log.info("Successfully created online payment for TID: {} with payment ID: {}", request.getTID(), paymentId);
            paymentCreationTransactionalService.saveEasyPayPaymentConfirmationDetails(
                    easyPayPaymentEntity,
                    request.getDATE(),
                    request.getTOTAL(),
                    paymentId
            );
            log.info("Payment confirmation details saved for TID: {} with payment ID: {}", request.getTID(), paymentId);

            paymentOffsettingTransactionalService.startPaymentOffsetting(
                    easyPayPaymentDetailsEntities,
                    easyPayPaymentEntity,
                    paymentId
            );
            log.info("Payment and liabilities offsetting process started for TID: {}", request.getTID());

            return new EasyPayConfirmPaymentResponse(
                    EPayStatusCode.OK.getCode(),
                    EPayStatusCode.OK.getDescription(),
                    "No additional info provided"
            );
        }

        return createErrorResponse(
                EPayStatusCode.GENERAL_ERROR,
                request.getTID(),
                "Unable to create the payment. Please verify the request details."
        );
    }

    /**
     * Processes a partial payment by validating the liabilities, checking if the partial payment amount and invoices are correct,
     * and creating the associated online payment.
     * This method handles the following steps for processing a partial payment:
     * Validates if the customer has the specified liabilities associated with the payment.
     * Checks if the partial payment amount and invoices match the expected values.
     * Collects the partial payment details and creates an online payment if the conditions are met.
     * Updates the payment entity with the confirmation date, confirmed amount, and other relevant details.
     * Performs payment offsetting by invoking the appropriate internal logic.
     * Returns a success response if the payment creation and offsetting are successful. Otherwise, it returns an error response.
     *
     * @param request                       The confirm payment request containing details such as total amount, liability IDs, and invoice details.
     * @param easyPayPaymentEntity          The payment entity associated with the transaction.
     * @param customerWrapper               A wrapper containing customer details.
     * @param collectionChannel             The collection channel through which the payment is processed.
     * @param easyPayPaymentDetailsEntities The payment details entities associated with the payment.
     * @return A {@link EasyPayConfirmPaymentResponse} representing the result of the payment processing, either a success or an error message.
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public EasyPayConfirmPaymentResponse processPartialPayment(
            ConfirmPayRequest request,
            EasyPayPaymentEntity easyPayPaymentEntity,
            CustomerWrapper customerWrapper,
            CollectionChannel collectionChannel,
            List<EasyPayPaymentDetailsEntity> easyPayPaymentDetailsEntities
    ) {
        List<Long> partialPaidLiabilityIds = request.getLiabilityIds();
        String tid = request.getTID();

        // Check if the customer has liabilities
        if (!customerLiabilityRepository.existsAllByLiabilityIdsAndCustomerId(
                partialPaidLiabilityIds.size(),
                partialPaidLiabilityIds,
                customerWrapper.customer().getId()
        )
        ) {
            return createErrorResponse(
                    EPayStatusCode.INVALID_AMOUNT,
                    tid,
                    "The selected customer does not have the specified liabilities."
            );
        }

        // Collect partial paid details and proceed
        List<EasyPayPaymentDetailsEntity> partialPaidDetailEntities = collectPartialPaidDetailEntities(
                easyPayPaymentDetailsEntities,
                partialPaidLiabilityIds,
                easyPayPaymentEntity.getId(),
                request.getTOTAL()
        );
        if (CollectionUtils.isNotEmpty(partialPaidDetailEntities)) {
            log.info("Found partial paid details for TID: {}. Creating online payment.", tid);
            LocalDateTime date = EPBFunctionUtils.localDateTimeFromDateString(request.getDATE());

            Long paymentId = paymentCreationTransactionalService.createOnlinePayment(
                    request.getTOTAL(),
                    customerWrapper.customer(),
                    date.toLocalDate(),
                    billingGroupHelperService.fetchAccountPeriodId(date),
                    collectionChannel,
                    billingGroupHelperService.fetchContractBillingGroupIdForCustomer(
                            easyPayPaymentEntity.getBillingGroupNumber(),
                            partialPaidDetailEntities
                    )
            );

            if (paymentId != null && paymentId > 0) {
                log.info("Online payment created successfully for TID: {} with payment ID: {}", tid, paymentId);
                paymentCreationTransactionalService.saveEasyPayPaymentConfirmationDetails(
                        easyPayPaymentEntity,
                        request.getDATE(),
                        request.getTOTAL(),
                        paymentId
                );
                log.info("Payment confirmation details saved for TID: {}", tid);

                paymentOffsettingTransactionalService.startPaymentOffsetting(
                        partialPaidDetailEntities,
                        easyPayPaymentEntity,
                        paymentId
                );
                log.info("Payment and liabilities offsetting process started for TID: {}", tid);

                return new EasyPayConfirmPaymentResponse(
                        EPayStatusCode.OK.getCode(),
                        EPayStatusCode.OK.getDescription(),
                        "No additional info provided"
                );
            }

        } else {
            return createErrorResponse(
                    EPayStatusCode.GENERAL_ERROR,
                    tid,
                    "The partial payment amount or invoices are incorrect."
            );
        }

        return createErrorResponse(
                EPayStatusCode.GENERAL_ERROR,
                tid,
                "Unable to create the payment. Please check the request details."
        );
    }

    /**
     * Collects the partial paid details for a given list of payment details entities,
     * based on specified liabilities and payment ID. It calculates the total paid amount
     * for the partial paid liabilities and checks if it matches the requested total amount.
     * If they match, it fetches the corresponding payment details entities from the repository.
     *
     * @param paymentDetailsEntities A list of {@link EasyPayPaymentDetailsEntity} objects,
     *                               representing the payment details.
     * @param partialPaidLiabilities A list of liability IDs that have been partially paid.
     * @param easyPayPaymentId       The ID of the EasyPay payment used to filter the payment details.
     * @param totalRequestAmount     The total amount requested for the payment, used to verify
     *                               that the partial payments match the requested amount.
     * @return A list of {@link EasyPayPaymentDetailsEntity} objects that represent the partial
     * paid details, if the total paid amount matches the requested amount and the
     * corresponding entities are found in the repository; otherwise, returns null.
     * @throws NullPointerException if any of the input parameters is null, particularly
     *                              {@code paymentDetailsEntities} or {@code partialPaidLiabilities}.
     */
    private List<EasyPayPaymentDetailsEntity> collectPartialPaidDetailEntities(
            List<EasyPayPaymentDetailsEntity> paymentDetailsEntities,
            List<Long> partialPaidLiabilities,
            Long easyPayPaymentId,
            Long totalRequestAmount
    ) {
        Long totalPaidAmount = 0L;

        for (EasyPayPaymentDetailsEntity paymentDetailsEntity : paymentDetailsEntities) {
            if (partialPaidLiabilities.contains(paymentDetailsEntity.getLiabilityId())) {
                totalPaidAmount += paymentDetailsEntity.getTotalAmountInCoins();
            }
        }

        if (totalPaidAmount.equals(totalRequestAmount)) {
            List<EasyPayPaymentDetailsEntity> partialPaidEntities = easyPayPaymentDetailRepository.findByLiabilityIdInAndEasyPayPaymentId(
                    partialPaidLiabilities,
                    easyPayPaymentId
            );

            if (CollectionUtils.isNotEmpty(partialPaidEntities) && partialPaidEntities.size() == partialPaidLiabilities.size()) {
                return partialPaidEntities;
            }
        }

        return null;
    }

    /**
     * Validates the provided request by checking the checksum, merchant ID, and transaction ID (TID).
     * If any of the checks fail, an appropriate error response is created. The method also logs each validation
     * step for debugging and monitoring purposes. If all validations are successful, the method returns `null`,
     * indicating that the request is valid and the payment process can proceed.
     * This method performs the following validations:
     * Checks if the checksum in the request matches the expected value, ensuring the integrity of the request.
     * Verifies that the merchant ID is valid and exists in the system.
     * Ensures that a valid Transaction ID (TID) is provided for 'BILLING' type requests.
     *
     * @param request         The {@link ConfirmPayRequest} object containing the payment request details.
     * @param requestCheckSum The checksum string that is used to validate the integrity of the request.
     * @return {@link EasyPayConfirmPaymentResponse} An error response if any of the validations fail. Otherwise, `null` is returned.
     */
    private EasyPayConfirmPaymentResponse validateRequest(ConfirmPayRequest request, String requestCheckSum) {
        log.info("Validating request checksum...");
        String tid = request.getTID();

        Optional<Configuration> currentConfiguration = configurationRepository.findLatestConfiguration();
        String secretKeyFromConfig = null;
        if (currentConfiguration.isPresent()) {
            secretKeyFromConfig = currentConfiguration.get().getEasyPaySecretKey();
        }
        String finalSecretKey = secretKeyFromConfig == null ? secretKey : secretKeyFromConfig;

        if (EPBSignatureUtils.isInvalidPaySignature(request, requestCheckSum, finalSecretKey)) {
            return createErrorResponse(
                    EPayStatusCode.INVALID_CHECKSUM,
                    tid,
                    "Invalid checksum in the request. The signature does not match the expected value."
            );
        }

        log.info("Validating merchant ID...");
        if (isInvalidMerchantId(request.getMERCHANTID())) {
            return createErrorResponse(
                    EPayStatusCode.GENERAL_ERROR,
                    tid,
                    "Merchant not found for the provided merchant ID."
            );
        }

        if (!isCorrectBillingRequest(request.getTYPE(), tid)) {
            return createErrorResponse(
                    EPayStatusCode.GENERAL_ERROR,
                    tid,
                    "A 'BILLING' request requires a valid Transaction ID (TID)."
            );
        }

        LocalDateTime localDateTime = EPBFunctionUtils.localDateTimeFromDateString(request.getDATE());
        if (localDateTime == null) {
            return createErrorResponse(
                    EPayStatusCode.GENERAL_ERROR,
                    tid,
                    "Invalid date format. Please provide a valid date."
            );
        }

        if (localDateTime.isAfter(LocalDateTime.now(ZoneId.of("Europe/Sofia")))) {
            return createErrorResponse(
                    EPayStatusCode.GENERAL_ERROR,
                    tid,
                    "The provided date and time is in the future. Please provide a valid date."
            );
        }

        log.info("Request validation successful.");
        return null; // No issues found, proceed with the payment initialization
    }

    /**
     * Creates an error response object containing details about a failed transaction.
     * This method generates a {@link EasyPayConfirmPaymentResponse} based on the provided status code, transaction ID,
     * and additional information. If the additional information is {@code null}, it is replaced with a default message.
     * It also logs an error message with the transaction details.
     *
     * @param statusCode     the status code representing the error (from {@link EPayStatusCode})
     * @param transactionId  the ID of the transaction that encountered an error
     * @param additionalInfo any extra information about the error, or {@code null} if none
     * @return a {@link EasyPayConfirmPaymentResponse} containing the error status code, description, and additional information
     */
    private EasyPayConfirmPaymentResponse createErrorResponse(
            EPayStatusCode statusCode,
            String transactionId,
            String additionalInfo
    ) {
        if (additionalInfo == null) {
            additionalInfo = "No additional info provided";
        }

        log.error(
                "TransactionId={}, StatusCode={}, StatusDescription={}, AdditionalInfo={}",
                transactionId,
                statusCode.getCode(),
                statusCode.getDescription(),
                additionalInfo
        );

        return new EasyPayConfirmPaymentResponse(
                statusCode.getCode(),
                statusCode.getDescription(),
                additionalInfo
        );
    }

}
