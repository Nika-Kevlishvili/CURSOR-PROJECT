package bg.energo.phoenix.virtualpos.service;

import bg.energo.phoenix.model.CheckLiabilityDetails;
import bg.energo.phoenix.model.CustomerWrapper;
import bg.energo.phoenix.model.entity.Configuration;
import bg.energo.phoenix.model.entity.customer.Customer;
import bg.energo.phoenix.model.entity.receivable.collectionChannel.CollectionChannel;
import bg.energo.phoenix.repository.ConfigurationRepository;
import bg.energo.phoenix.model.response.customer.CustomerPaymentInfo;
import bg.energo.phoenix.repository.customer.CustomerDetailsRepository;
import bg.energo.phoenix.repository.customer.CustomerRepository;
import bg.energo.phoenix.repository.receivable.collectionChannel.CollectionChannelRepository;
import bg.energo.phoenix.util.epb.EPBJsonUtils;
import bg.energo.phoenix.utils.EPBFunctionUtils;
import bg.energo.phoenix.utils.EPBPaymentCalculationUtils;
import bg.energo.phoenix.utils.EPBSignatureUtils;
import bg.energo.phoenix.utils.EPBStringUtils;
import bg.energo.phoenix.virtualpos.model.enums.VirtualPosRequestType;
import bg.energo.phoenix.virtualpos.model.enums.VirtualPosStatusCode;
import bg.energo.phoenix.virtualpos.model.request.VirtualPosInitPayRequest;
import bg.energo.phoenix.virtualpos.model.response.VirtualPosInitPaymentResponse;
import bg.energo.phoenix.virtualpos.repository.VirtualPosPaymentDetailsRepository;
import bg.energo.phoenix.virtualpos.repository.VirtualPosPaymentRepository;
import bg.energo.phoenix.virtualpos.repository.VirtualPosVerifyDetailsRepository;
import bg.energo.phoenix.virtualpos.repository.VirtualPosVerifyRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class VirtualPosInitPaymentService extends VirtualPosBaseService {

    private final VirtualPosVerifyRepository virtualPosVerifyRepository;
    private final VirtualPosVerifyDetailsRepository virtualPosVerifyDetailsRepository;

    public VirtualPosInitPaymentService(
            VirtualPosPaymentDetailsRepository virtualPosPaymentDetailRepository,
            VirtualPosPaymentRepository virtualPosPaymentRepository,
            CollectionChannelRepository collectionChannelRepository,
            CustomerDetailsRepository customerDetailsRepository,
            CustomerRepository customerRepository,
            VirtualPosMapperService virtualPosMapperService,
            VirtualPosVerifyRepository virtualPosVerifyRepository,
            VirtualPosVerifyDetailsRepository virtualPosVerifyDetailsRepository,
            ConfigurationRepository configurationRepository
    ) {
        super(
                virtualPosPaymentDetailRepository,
                virtualPosPaymentRepository,
                collectionChannelRepository,
                customerDetailsRepository,
                customerRepository,
                virtualPosMapperService,
                configurationRepository
        );
        this.virtualPosVerifyRepository = virtualPosVerifyRepository;
        this.virtualPosVerifyDetailsRepository = virtualPosVerifyDetailsRepository;
    }

    public VirtualPosInitPaymentResponse initPay(VirtualPosInitPayRequest request, String requestCheckSum) {
        log.info("Initializing virtual POS payment with request: {}", request);

        VirtualPosInitPaymentResponse validationResponse = validateRequest(request, requestCheckSum);
        if (validationResponse != null) {
            log.error("Validation failed: {}", validationResponse.getSTATUS());
            return validationResponse;
        }

        CustomerWrapper customerWrapper = processCustomerDetails(request.getIDN());
        if (customerWrapper == null) {
            return createErrorResponse(
                    VirtualPosStatusCode.INVALID_SUBSCRIBER_NUMBER,
                    "No customer found for the identifier",
                    request.getIDN()
            );
        }
        Customer customer = customerWrapper.customer();
        log.info("Customer found: {}. Fetching collection channel...", customer.getIdentifier());

        CollectionChannel collectionChannel = fetchCollectionChannel();
        if (collectionChannel == null) {
            return createErrorResponse(
                    VirtualPosStatusCode.GENERAL_ERROR,
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
                    VirtualPosStatusCode.NO_OBLIGATION,
                    "No liabilities found for customer identifier",
                    request.getIDN()
            );
        }
        log.info("Liabilities found: {}. Calculating total sum...", liabilities.size());

        long sumAmountOfLiabilities = bg.energo.phoenix.utils.EPBPaymentCalculationUtils.computeScaledLiabilitiesAmount(liabilities);
        if (sumAmountOfLiabilities <= 0) {
            return createErrorResponse(
                    VirtualPosStatusCode.NO_OBLIGATION,
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
            VirtualPosRequestType requestType,
            String tid,
            CollectionChannel collectionChannel,
            CustomerWrapper customerWrapper,
            List<CheckLiabilityDetails> liabilities,
            Long convertedCoinTotalAmount
    ) {
        if (VirtualPosRequestType.BILLING.equals(requestType)) {
            savePaymentDetails(
                    tid,
                    collectionChannel,
                    customerWrapper,
                    liabilities,
                    VirtualPosStatusCode.OK,
                    convertedCoinTotalAmount,
                    requestType
            );
        } else if (VirtualPosRequestType.CHECK.equals(requestType)) {
            saveVerifyDetails(
                    collectionChannel.getId(),
                    customerWrapper.customer().getIdentifier(),
                    customerWrapper.billingGroupNumber(),
                    liabilities,
                    VirtualPosStatusCode.OK,
                    convertedCoinTotalAmount,
                    requestType
            );
        }
    }

    private VirtualPosInitPaymentResponse validateRequest(VirtualPosInitPayRequest request, String requestCheckSum) {
        log.info("Validating request checksum...");
        Optional<Configuration> currentConfiguration = configurationRepository.findLatestConfiguration();
        String secretKeyFromConfig = null;
        if (currentConfiguration.isPresent()) {
            secretKeyFromConfig = currentConfiguration.get().getVirtualPosSecretKey();
        }
        String finalSecretKey = secretKeyFromConfig == null ? secretKey : secretKeyFromConfig;

        if (EPBSignatureUtils.isInvalidVirtualPosInitSignature(request, requestCheckSum, finalSecretKey)) {
            return createErrorResponse(
                    VirtualPosStatusCode.INVALID_CHECKSUM,
                    "The checksum for the request is invalid.",
                    EPBJsonUtils.asJsonString(request)
            );
        }

        log.info("Validating merchant ID...");
        if (isInvalidMerchantId(request.getMERCHANTID())) {
            return createErrorResponse(
                    VirtualPosStatusCode.GENERAL_ERROR,
                    "Merchant not found for ID",
                    request.getMERCHANTID()
            );
        }

        log.info("Validating request type and transaction ID...");
        if (isCheckRequestWithTid(request.getTYPE(), request.getTID())) {
            return createErrorResponse(
                    VirtualPosStatusCode.GENERAL_ERROR,
                    "Invalid request: Request type is CHECK, but a TID was provided",
                    EPBJsonUtils.asJsonString(request)
            );
        }

        if (isBillingRequestWithInvalidTid(request.getTYPE(), request.getTID())) {
            return createErrorResponse(
                    VirtualPosStatusCode.GENERAL_ERROR,
                    "A 'BILLING' request type requires a valid TID",
                    EPBJsonUtils.asJsonString(request)
            );
        }

        log.info("Request validation successful.");
        return null; // No issues found, proceed with the payment initialization
    }

    public void savePaymentDetails(
            String tid,
            CollectionChannel collectionChannel,
            CustomerWrapper customerWrapper,
            List<CheckLiabilityDetails> liabilities,
            VirtualPosStatusCode statusCode,
            Long convertedCoinTotalAmount,
            VirtualPosRequestType requestType
    ) {
        bg.energo.phoenix.virtualpos.model.entity.VirtualPosPaymentEntity existing = findPaymentByTransactionId(tid);
        if (existing == null) {
            log.info("No existing payment details found for transaction ID: {}. Proceeding to save new payment details.", tid);

            String customerIdentifier = customerWrapper.customer().getIdentifier();
            Long customerId = customerWrapper.customer().getId();
            Long collectionChannelId = collectionChannel.getId();
            String billingGroupNumber = customerWrapper.billingGroupNumber();
            log.debug("Started saving payment details for customer: {} in channel: {} with transaction ID: {}", customerIdentifier, collectionChannelId, tid);

            java.util.List<bg.energo.phoenix.virtualpos.model.entity.VirtualPosPaymentDetailsEntity> paymentDetails = virtualPosMapperService.mapToDetailEntities(liabilities, virtualPosMapperService::mapToPaymentDetailEntity);
            log.debug("Mapped {} liability details to VirtualPosPaymentDetailsEntity", paymentDetails.size());

            java.math.BigDecimal totalAmount = EPBPaymentCalculationUtils.calculateTotalAmount(paymentDetails, bg.energo.phoenix.virtualpos.model.entity.VirtualPosPaymentDetailsEntity::getAmount);
            java.math.BigDecimal totalLfpAmount = EPBPaymentCalculationUtils.calculateTotalAmount(paymentDetails, bg.energo.phoenix.virtualpos.model.entity.VirtualPosPaymentDetailsEntity::getLfpAmount);
            java.math.BigDecimal totalSumAmount = EPBPaymentCalculationUtils.calculateTotalAmount(paymentDetails, bg.energo.phoenix.virtualpos.model.entity.VirtualPosPaymentDetailsEntity::getTotalAmount);
            log.debug("Calculated totals: Amount = {}, LFP Amount = {}, Total Amount = {}", totalAmount, totalLfpAmount, totalSumAmount);

            Optional<Configuration> currentConfiguration = configurationRepository.findLatestConfiguration();
            String merchantIdFromConfig = null;
            if (currentConfiguration.isPresent()) {
                merchantIdFromConfig = currentConfiguration.get().getVirtualPosMerchantId();
            }
            String finalMerchantId = merchantIdFromConfig == null ? merchantCode : merchantIdFromConfig;

            bg.energo.phoenix.virtualpos.model.entity.VirtualPosPaymentEntity entity = virtualPosMapperService.buildVirtualPosPaymentEntity(
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
                    bg.energo.phoenix.virtualpos.model.enums.VirtualPosPaymentStatus.INITIALIZED
            );

            log.debug("Saving VirtualPosPaymentEntity for customer: {} and channel: {} with transaction ID: {}", customerIdentifier, collectionChannelId, tid);
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
            VirtualPosStatusCode statusCode,
            Long convertedCoinTotalAmount,
            VirtualPosRequestType requestType
    ) {
        log.debug("Started saving verify details for customer: {} in channel: {}", customerIdentifier, collectionChannelId);
        java.util.List<bg.energo.phoenix.virtualpos.model.entity.VirtualPosVerifyDetailsEntity> verifyDetails = virtualPosMapperService.mapToDetailEntities(liabilities, virtualPosMapperService::mapToVerifyDetailEntity);
        log.debug("Mapped {} liability details to VirtualPosVerifyDetailsEntity", verifyDetails.size());

        java.math.BigDecimal totalAmount = EPBPaymentCalculationUtils.calculateTotalAmount(verifyDetails, bg.energo.phoenix.virtualpos.model.entity.VirtualPosVerifyDetailsEntity::getAmount);
        java.math.BigDecimal totalLfpAmount = EPBPaymentCalculationUtils.calculateTotalAmount(verifyDetails, bg.energo.phoenix.virtualpos.model.entity.VirtualPosVerifyDetailsEntity::getLfpAmount);
        java.math.BigDecimal totalSumAmount = EPBPaymentCalculationUtils.calculateTotalAmount(verifyDetails, bg.energo.phoenix.virtualpos.model.entity.VirtualPosVerifyDetailsEntity::getTotalAmount);
        log.debug("Calculated totals: Amount = {}, LFP Amount = {}, Total Amount = {}", totalAmount, totalLfpAmount, totalSumAmount);

        Optional<Configuration> currentConfiguration = configurationRepository.findLatestConfiguration();
        String merchantIdFromConfig = null;
        if (currentConfiguration.isPresent()) {
            merchantIdFromConfig = currentConfiguration.get().getVirtualPosMerchantId();
        }
        String finalMerchantId = merchantIdFromConfig == null ? merchantCode : merchantIdFromConfig;

        bg.energo.phoenix.virtualpos.model.entity.VirtualPosVerifyEntity verifyEntity = virtualPosMapperService.buildVirtualPosVerifyEntity(
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

        log.debug("Saving VirtualPosVerifyEntity for customer: {} and channel: {}", customerIdentifier, collectionChannelId);
        saveVerifyEntities(verifyEntity, verifyDetails);
    }

    private void savePaymentEntities(
            bg.energo.phoenix.virtualpos.model.entity.VirtualPosPaymentEntity paymentEntity,
            java.util.List<bg.energo.phoenix.virtualpos.model.entity.VirtualPosPaymentDetailsEntity> paymentDetailsEntities
    ) {
        virtualPosPaymentRepository.saveAndFlush(paymentEntity);

        java.util.List<bg.energo.phoenix.virtualpos.model.entity.VirtualPosPaymentDetailsEntity> collected = paymentDetailsEntities
                .stream()
                .peek(detail -> detail.setVirtualPosPaymentId(paymentEntity.getId()))
                .toList();

        log.debug("Saving VirtualPosPaymentDetailEntity for {} liabilities", collected.size());
        virtualPosPaymentDetailRepository.saveAll(collected);
    }

    private void saveVerifyEntities(
            bg.energo.phoenix.virtualpos.model.entity.VirtualPosVerifyEntity verifyEntity,
            java.util.List<bg.energo.phoenix.virtualpos.model.entity.VirtualPosVerifyDetailsEntity> verifyDetailsEntities
    ) {
        virtualPosVerifyRepository.saveAndFlush(verifyEntity);

        java.util.List<bg.energo.phoenix.virtualpos.model.entity.VirtualPosVerifyDetailsEntity> collected = verifyDetailsEntities
                .stream()
                .peek(detail -> detail.setVirtualPosVerifyId(verifyEntity.getId()))
                .toList();

        log.debug("Saving VirtualPosVerifyDetailsEntity for {} liabilities", collected.size());
        virtualPosVerifyDetailsRepository.saveAll(collected);
    }

    private VirtualPosInitPaymentResponse createErrorResponse(bg.energo.phoenix.virtualpos.model.enums.VirtualPosStatusCode statusCode, String logMessage, String idn) {
        log.error("{}: {}", logMessage, idn);
        return new VirtualPosInitPaymentResponse(statusCode);
    }

    public VirtualPosInitPaymentResponse createSuccessResponse(
            CustomerWrapper customerWrapper,
            List<CheckLiabilityDetails> liabilities,
            CollectionChannel collectionChannel,
            Long sumAmountOfLiabilities
    ) {
        String customerNumber = String.valueOf(customerWrapper.customer().getCustomerNumber());

        CustomerPaymentInfo customerPaymentInfo = customerDetailsRepository
                .findCustomerPaymentInfoByCustomerDetailId(customerWrapper.customer().getLastCustomerDetailId())
                .orElse(null);

        VirtualPosInitPaymentResponse response = new VirtualPosInitPaymentResponse();

        // Mirror EasyPay formatting behavior
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
        response.setSTATUS(bg.energo.phoenix.virtualpos.model.enums.VirtualPosStatusCode.OK.getCode());
        response.setAMOUNT(sumAmountOfLiabilities);
        response.setIDN(customerNumber);
        if (collectionChannel.getCombineLiabilities() == null || !collectionChannel.getCombineLiabilities()) {
            response.setINVOICES(
                    virtualPosMapperService.mapToInvoicesResponse(
                            liabilities,
                            customerPaymentInfo,
                            customerNumber
                    )
            );
        }
        return response;
    }
}


