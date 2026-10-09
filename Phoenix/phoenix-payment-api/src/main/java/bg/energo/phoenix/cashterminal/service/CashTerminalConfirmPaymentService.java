package bg.energo.phoenix.cashterminal.service;

import bg.energo.phoenix.cashterminal.model.entity.CashTerminalPaymentDetailsEntity;
import bg.energo.phoenix.cashterminal.model.entity.CashTerminalPaymentEntity;
import bg.energo.phoenix.cashterminal.model.enums.CashTerminalPaymentStatus;
import bg.energo.phoenix.cashterminal.model.enums.CashTerminalStatusCode;
import bg.energo.phoenix.cashterminal.model.request.CashTerminalConfirmPayRequest;
import bg.energo.phoenix.cashterminal.model.response.CashTerminalConfirmPaymentResponse;
import bg.energo.phoenix.cashterminal.repository.CashTerminalPaymentDetailsRepository;
import bg.energo.phoenix.cashterminal.repository.CashTerminalPaymentRepository;
import bg.energo.phoenix.model.CustomerWrapper;
import bg.energo.phoenix.model.entity.receivable.collectionChannel.CollectionChannel;
import bg.energo.phoenix.repository.customer.CustomerDetailsRepository;
import bg.energo.phoenix.repository.customer.CustomerRepository;
import bg.energo.phoenix.repository.receivable.collectionChannel.CollectionChannelRepository;
import bg.energo.phoenix.repository.receivable.customerLiability.CustomerLiabilityRepository;
import bg.energo.phoenix.service.BillingGroupHelperService;
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
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class CashTerminalConfirmPaymentService extends CashTerminalBaseService {

    private final CashTerminalPaymentOffsettingTransactionalService paymentOffsettingTransactionalService;
    private final CashTerminalPaymentCreationTransactionalService paymentCreationTransactionalService;
    private final CustomerLiabilityRepository customerLiabilityRepository;
    private final BillingGroupHelperService billingGroupHelperService;
    private final CashTerminalBillingGroupHelperService cashTerminalBillingGroupHelperService;

    public CashTerminalConfirmPaymentService(
            CashTerminalPaymentDetailsRepository cashTerminalPaymentDetailRepository,
            CashTerminalPaymentRepository cashTerminalPaymentRepository,
            CollectionChannelRepository collectionChannelRepository,
            CustomerDetailsRepository customerDetailsRepository,
            CustomerRepository customerRepository,
            CashTerminalMapperService cashTerminalMapperService,
            CashTerminalPaymentCreationTransactionalService paymentCreationTransactionalService,
            CustomerLiabilityRepository customerLiabilityRepository,
            CashTerminalPaymentOffsettingTransactionalService paymentOffsettingTransactionalService,
            BillingGroupHelperService billingGroupHelperService,
            CashTerminalBillingGroupHelperService cashTerminalBillingGroupHelperService
    ) {
        super(
                cashTerminalPaymentDetailRepository,
                cashTerminalPaymentRepository,
                collectionChannelRepository,
                customerDetailsRepository,
                customerRepository,
                cashTerminalMapperService
        );
        this.paymentCreationTransactionalService = paymentCreationTransactionalService;
        this.paymentOffsettingTransactionalService = paymentOffsettingTransactionalService;
        this.customerLiabilityRepository = customerLiabilityRepository;
        this.billingGroupHelperService = billingGroupHelperService;
        this.cashTerminalBillingGroupHelperService = cashTerminalBillingGroupHelperService;
    }

    @Transactional
    public CashTerminalConfirmPaymentResponse confirmPay(CashTerminalConfirmPayRequest request, String requestCheckSum) {
        log.info("Received confirm pay request: {}, requestCheckSum: {}", EPBJsonUtils.asJsonString(request), requestCheckSum);

        CashTerminalConfirmPaymentResponse responseAfterValidation = validateRequest(request, requestCheckSum);
        if (responseAfterValidation != null) {
            log.error("Validation failed: {}", responseAfterValidation.getSTATUS());
            return responseAfterValidation;
        }

        String tid = request.getTID();
        log.info("Validation passed for TID: {}", tid);

        CustomerWrapper customerWrapper = processCustomerDetails(request.getIDN());
        if (customerWrapper == null) {
            return createErrorResponse(
                    CashTerminalStatusCode.INVALID_SUBSCRIBER_NUMBER,
                    tid,
                    "Customer not found for the provided identifier."
            );
        }

        CollectionChannel collectionChannel = fetchCollectionChannel();
        if (collectionChannel == null) {
            return createErrorResponse(
                    CashTerminalStatusCode.GENERAL_ERROR,
                    tid,
                    "Collection channel not found for the request."
            );
        }

        CashTerminalPaymentEntity paymentEntity = findPaymentByTransactionId(tid);
        if (paymentEntity == null) {
            return createErrorResponse(
                    CashTerminalStatusCode.GENERAL_ERROR,
                    tid,
                    "No payment record found for the provided Transaction ID (TID)."
            );
        }

        if (paymentEntity.getPaymentStatus() == CashTerminalPaymentStatus.CONFIRMED) {
            return createErrorResponse(
                    CashTerminalStatusCode.REPEAT_NOTIFICATION,
                    tid,
                    "Repeat of already received notification: Payment with selected TID already exists"
            );
        }

        log.info("Proceeding to handle payment for TID: {}", tid);
        return handlePaymentRequest(
                request,
                paymentEntity,
                customerWrapper,
                collectionChannel
        );
    }

    @Transactional
    public CashTerminalConfirmPaymentResponse handlePaymentRequest(
            CashTerminalConfirmPayRequest request,
            CashTerminalPaymentEntity paymentEntity,
            CustomerWrapper customerWrapper,
            CollectionChannel collectionChannel
    ) {
        String tid = request.getTID();

        // Step 1: Check if the total amount matches
        if (paymentEntity.getInitTotalAmountInCoins().equals(request.getTOTAL())) {
            log.info("Total amount matches for combined payment for TID: {}", tid);
            return processFullPayment(request, paymentEntity, customerWrapper, collectionChannel);
        }

        // Step 2: Handle combined payment or missing data
        if (paymentEntity.getIsCombined()) {
            return createErrorResponse(
                    CashTerminalStatusCode.GENERAL_ERROR,
                    tid,
                    "The amount is incorrect for this combined payment."
            );
        }

        // Step 3: Validate invoices and associated details
        if (request.getInvoiceNumbers().isEmpty()) {
            return createErrorResponse(
                    CashTerminalStatusCode.GENERAL_ERROR,
                    tid,
                    "Invoice numbers are missing. Please provide valid invoices."
            );
        }

        List<CashTerminalPaymentDetailsEntity> paymentDetailsEntities = cashTerminalPaymentDetailRepository.findByCashTerminalPaymentId(paymentEntity.getId());
        if (paymentDetailsEntities.isEmpty()) {
            return createErrorResponse(
                    CashTerminalStatusCode.GENERAL_ERROR,
                    tid,
                    "Payment details are missing. Please ensure all payment details are provided."
            );
        }

        log.info("Processing partial payment for TID: {}", tid);
        return processPartialPayment(request, paymentEntity, customerWrapper, collectionChannel, paymentDetailsEntities);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public CashTerminalConfirmPaymentResponse processFullPayment(
            CashTerminalConfirmPayRequest request,
            CashTerminalPaymentEntity paymentEntity,
            CustomerWrapper customerWrapper,
            CollectionChannel collectionChannel
    ) {
        log.info("Processing full payment for TID: {}", request.getTID());
        List<CashTerminalPaymentDetailsEntity> paymentDetailsEntities = cashTerminalPaymentDetailRepository.findByCashTerminalPaymentId(
                paymentEntity.getId()
        );
        log.info("Found {} payment details for TID: {}", paymentDetailsEntities.size(), request.getTID());
        LocalDateTime date = EPBFunctionUtils.localDateTimeFromDateString(request.getDATE());
        Long paymentId = paymentCreationTransactionalService.createOnlinePayment(
                request.getTOTAL(),
                customerWrapper.customer(),
                date.toLocalDate(),
                billingGroupHelperService.fetchAccountPeriodId(date),
                collectionChannel,
                cashTerminalBillingGroupHelperService.fetchContractBillingGroupIdForCustomer(
                        customerWrapper.billingGroupNumber(),
                        paymentDetailsEntities
                )
        );
        if (paymentId != null && paymentId > 0) {
            log.info("Successfully created online payment for TID: {} with payment ID: {}", request.getTID(), paymentId);
            paymentCreationTransactionalService.saveCashTerminalPaymentConfirmationDetails(
                    paymentEntity,
                    request.getDATE(),
                    request.getTOTAL(),
                    paymentId
            );
            log.info("Payment confirmation details saved for TID: {} with payment ID: {}", request.getTID(), paymentId);

            CompletableFuture.runAsync(
                    () -> paymentOffsettingTransactionalService.startPaymentOffsetting(
                            paymentDetailsEntities,
                            paymentEntity,
                            paymentId
                    )
            );
            log.info("Payment and liabilities offsetting process started for TID: {}", request.getTID());

            return new CashTerminalConfirmPaymentResponse(
                    CashTerminalStatusCode.OK.getCode(),
                    CashTerminalStatusCode.OK.getDescription(),
                    "No additional info provided"
            );
        }

        return createErrorResponse(
                CashTerminalStatusCode.GENERAL_ERROR,
                request.getTID(),
                "Unable to create the payment. Please verify the request details."
        );
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public CashTerminalConfirmPaymentResponse processPartialPayment(
            CashTerminalConfirmPayRequest request,
            CashTerminalPaymentEntity paymentEntity,
            CustomerWrapper customerWrapper,
            CollectionChannel collectionChannel,
            List<CashTerminalPaymentDetailsEntity> paymentDetailsEntities
    ) {
        java.util.List<Long> partialPaidLiabilityIds = request.getLiabilityIds();
        String tid = request.getTID();

        if (!customerLiabilityRepository.existsAllByLiabilityIdsAndCustomerId(
                partialPaidLiabilityIds.size(),
                partialPaidLiabilityIds,
                customerWrapper.customer().getId()
        )) {
            return createErrorResponse(
                    CashTerminalStatusCode.INVALID_AMOUNT,
                    tid,
                    "The selected customer does not have the specified liabilities."
            );
        }

        java.util.List<CashTerminalPaymentDetailsEntity> partialPaidDetailEntities = collectPartialPaidDetailEntities(
                paymentDetailsEntities,
                partialPaidLiabilityIds,
                paymentEntity.getId(),
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
                    cashTerminalBillingGroupHelperService.fetchContractBillingGroupIdForCustomer(
                            paymentEntity.getBillingGroupNumber(),
                            partialPaidDetailEntities
                    )
            );

            if (paymentId != null && paymentId > 0) {
                log.info("Online payment created successfully for TID: {} with payment ID: {}", tid, paymentId);
                paymentCreationTransactionalService.saveCashTerminalPaymentConfirmationDetails(
                        paymentEntity,
                        request.getDATE(),
                        request.getTOTAL(),
                        paymentId
                );
                log.info("Payment confirmation details saved for TID: {}", tid);

                CompletableFuture.runAsync(
                        () -> paymentOffsettingTransactionalService.startPaymentOffsetting(
                                partialPaidDetailEntities,
                                paymentEntity,
                                paymentId
                        )
                );
                log.info("Payment and liabilities offsetting process started for TID: {}", tid);

                return new CashTerminalConfirmPaymentResponse(
                        CashTerminalStatusCode.OK.getCode(),
                        CashTerminalStatusCode.OK.getDescription(),
                        "No additional info provided"
                );
            }

        } else {
            return createErrorResponse(
                    CashTerminalStatusCode.GENERAL_ERROR,
                    tid,
                    "The partial payment amount or invoices are incorrect."
            );
        }

        return createErrorResponse(
                CashTerminalStatusCode.GENERAL_ERROR,
                tid,
                "Unable to create the payment. Please check the request details."
        );
    }

    private java.util.List<CashTerminalPaymentDetailsEntity> collectPartialPaidDetailEntities(
            java.util.List<CashTerminalPaymentDetailsEntity> paymentDetailsEntities,
            java.util.List<Long> partialPaidLiabilities,
            Long cashTerminalPaymentId,
            Long totalRequestAmount
    ) {
        Long totalPaidAmount = 0L;
        for (CashTerminalPaymentDetailsEntity paymentDetailsEntity : paymentDetailsEntities) {
            if (partialPaidLiabilities.contains(paymentDetailsEntity.getLiabilityId())) {
                totalPaidAmount += paymentDetailsEntity.getTotalAmountInCoins();
            }
        }
        if (totalPaidAmount.equals(totalRequestAmount)) {
            return cashTerminalPaymentDetailRepository.findByLiabilityIdInAndCashTerminalPaymentId(
                    partialPaidLiabilities,
                    cashTerminalPaymentId
            );
        }
        return null;
    }

    private CashTerminalConfirmPaymentResponse createErrorResponse(
            CashTerminalStatusCode statusCode,
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

        return new CashTerminalConfirmPaymentResponse(
                statusCode.getCode(),
                statusCode.getDescription(),
                additionalInfo
        );
    }

    private CashTerminalConfirmPaymentResponse validateRequest(CashTerminalConfirmPayRequest request, String requestCheckSum) {
        log.info("Validating request checksum...");
        String tid = request.getTID();

        if (EPBSignatureUtils.isInvalidCashTerminalPaySignature(request, requestCheckSum, secretKey)) {
            return createErrorResponse(
                    CashTerminalStatusCode.INVALID_CHECKSUM,
                    tid,
                    "Invalid checksum in the request. The signature does not match the expected value."
            );
        }

        log.info("Validating merchant ID...");
        if (isInvalidMerchantId(request.getMERCHANTID())) {
            return createErrorResponse(
                    CashTerminalStatusCode.GENERAL_ERROR,
                    tid,
                    "Merchant not found for the provided merchant ID."
            );
        }

        if (!isCorrectBillingRequest(request.getTYPE(), tid)) {
            return createErrorResponse(
                    CashTerminalStatusCode.GENERAL_ERROR,
                    tid,
                    "A 'BILLING' request requires a valid Transaction ID (TID)."
            );
        }

        LocalDateTime localDateTime = EPBFunctionUtils.localDateTimeFromDateString(request.getDATE());
        if (localDateTime == null) {
            return createErrorResponse(
                    CashTerminalStatusCode.GENERAL_ERROR,
                    tid,
                    "Invalid date format. Please provide a valid date."
            );
        }

        if (localDateTime.isAfter(LocalDateTime.now(ZoneId.of("Europe/Sofia")))) {
            return createErrorResponse(
                    CashTerminalStatusCode.GENERAL_ERROR,
                    tid,
                    "The provided date and time is in the future. Please provide a valid date."
            );
        }

        log.info("Request validation successful.");
        return null;
    }
}

