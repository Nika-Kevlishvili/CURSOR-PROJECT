package bg.energo.phoenix.service;

import bg.energo.phoenix.model.CheckLiabilityDetails;
import bg.energo.phoenix.model.EasyPayInvoiceResponse;
import bg.energo.phoenix.model.entity.EasyPayPaymentDetailsEntity;
import bg.energo.phoenix.model.entity.EasyPayPaymentEntity;
import bg.energo.phoenix.model.entity.EasyPayVerifyDetailsEntity;
import bg.energo.phoenix.model.entity.EasyPayVerifyEntity;
import bg.energo.phoenix.model.entity.customer.Customer;
import bg.energo.phoenix.model.entity.receivable.collectionChannel.CollectionChannel;
import bg.energo.phoenix.model.entity.receivable.paymentPackage.PaymentPackage;
import bg.energo.phoenix.model.enums.EasyPayPaymentStatus;
import bg.energo.phoenix.model.enums.EasyPayRequestType;
import bg.energo.phoenix.model.request.receivable.payment.CreatePaymentRequest;
import bg.energo.phoenix.model.response.customer.CustomerPaymentInfo;
import bg.energo.phoenix.utils.EPBFunctionUtils;
import bg.energo.phoenix.utils.EPBPaymentCalculationUtils;
import bg.energo.phoenix.utils.EPBStringUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class EasyPayMapperService {

    public <T> List<T> mapToDetailEntities(
            List<CheckLiabilityDetails> liabilities,
            Function<CheckLiabilityDetails, T> mapperFunction
    ) {
        return liabilities
                .stream()
                .map(mapperFunction)
                .collect(Collectors.toList());
    }

    public EasyPayPaymentDetailsEntity mapToPaymentDetailEntity(CheckLiabilityDetails current) {
        BigDecimal totalAmount = EPBPaymentCalculationUtils.detectTotalAmount(
                current.getTotalAmount(),
                current.getCurrentAmount()
        );

        return EasyPayPaymentDetailsEntity
                .builder()
                .liabilityId(current.getLiabilityId())
                .amount(current.getCurrentAmount())
                .lfpAmount(
                        EPBPaymentCalculationUtils.calculateLatePaymentFineAmount(
                                current.getTotalAmount(),
                                current.getCurrentAmount()
                        )
                )
                .totalAmount(totalAmount)
                .totalAmountInCoins(
                        EPBPaymentCalculationUtils.convertToCoinAmount(
                                totalAmount
                        )
                )
                .build();
    }

    public EasyPayVerifyDetailsEntity mapToVerifyDetailEntity(CheckLiabilityDetails current) {
        BigDecimal totalAmount = EPBPaymentCalculationUtils.detectTotalAmount(
                current.getTotalAmount(),
                current.getCurrentAmount()
        );

        return EasyPayVerifyDetailsEntity
                .builder()
                .liabilityId(current.getLiabilityId())
                .amount(current.getCurrentAmount())
                .lfpAmount(
                        EPBPaymentCalculationUtils.calculateLatePaymentFineAmount(
                                current.getTotalAmount(),
                                current.getCurrentAmount()
                        )
                )
                .totalAmount(totalAmount)
                .totalAmountInCoins(
                        EPBPaymentCalculationUtils.convertToCoinAmount(
                                totalAmount
                        )
                )
                .build();
    }

    public EasyPayPaymentEntity buildEasyPayPaymentEntity(
            String tid,
            Long collectionChannelId,
            Long currencyId,
            Long customerId,
            String merchantId,
            String customerIdentifier,
            String billingGroupNumber,
            BigDecimal totalAmount,
            BigDecimal totalLfpAmount,
            BigDecimal totalSumAmount,
            Long convertedCoinTotalAmount,
            Boolean isCombined,
            String statusCode,
            EasyPayRequestType requestType,
            EasyPayPaymentStatus paymentStatus
    ) {
        return EasyPayPaymentEntity
                .builder()
                .transactionId(tid)
                .collectionChannelId(collectionChannelId)
                .currencyId(currencyId)
                .merchantCode(merchantId)
                .customerId(customerId)
                .customerIdentifier(customerIdentifier)
                .billingGroupNumber(billingGroupNumber)
                .initAmount(totalAmount)
                .initLfpAmount(totalLfpAmount)
                .initTotalAmount(totalSumAmount)
                .initTotalAmountInCoins(convertedCoinTotalAmount)
                .statusCode(statusCode)
                .isCombined(isCombined != null && isCombined)
                .initDate(LocalDateTime.now())
                .requestType(requestType)
                .paymentStatus(paymentStatus)
                .build();
    }

    public EasyPayVerifyEntity buildEasyPayVerifyEntity(
            Long collectionChannelId,
            Long currencyId,
            String merchantId,
            String customerIdentifier,
            String billingGroupNumber,
            BigDecimal totalAmount,
            BigDecimal totalLfpAmount,
            BigDecimal totalSumAmount,
            Long convertedCoinTotalAmount,
            String statusCode,
            EasyPayRequestType requestType
    ) {
        return EasyPayVerifyEntity
                .builder()
                .collectionChannelId(collectionChannelId)
                .currencyId(currencyId)
                .merchantCode(merchantId)
                .customerIdentifier(customerIdentifier)
                .billingGroupNumber(billingGroupNumber)
                .amount(totalAmount)
                .lfpAmount(totalLfpAmount)
                .totalAmount(totalSumAmount)
                .totalAmountInCoins(convertedCoinTotalAmount)
                .statusCode(statusCode)
                .verifyDate(LocalDateTime.now())
                .requestType(requestType)
                .build();
    }

    public List<EasyPayInvoiceResponse> mapToInvoicesResponse(
            List<CheckLiabilityDetails> liabilities,
            CustomerPaymentInfo customerPaymentInfo,
            String customerNumber
    ) {
        return liabilities
                .stream()
                .map(liability ->
                        mapToInvoiceResponse(
                                liability,
                                customerPaymentInfo,
                                customerNumber
                        )
                )
                .collect(Collectors.toList());
    }

    public EasyPayInvoiceResponse mapToInvoiceResponse(
            CheckLiabilityDetails liability,
            CustomerPaymentInfo customerPaymentInfo,
            String customerNumber
    ) {
        EasyPayInvoiceResponse easyPayInvoiceResponse = new EasyPayInvoiceResponse();
        easyPayInvoiceResponse.setIDN(
                EPBStringUtils.formatInvoiceIdentifier(
                        customerNumber,
                        liability.getLiabilityId()
                )
        );
        easyPayInvoiceResponse.setSHORTDESC(
                EPBStringUtils.formatShortDesc(customerNumber)
        );
        easyPayInvoiceResponse.setLONGDESC(
                EPBStringUtils.formatInvoiceLongDesc(
                        customerNumber,
                        customerPaymentInfo,
                        liability
                )
        );
        easyPayInvoiceResponse.setAMOUNT(
                EPBPaymentCalculationUtils.convertToCoinAmount(
                        EPBPaymentCalculationUtils.detectTotalAmount(
                                liability.getTotalAmount(),
                                liability.getCurrentAmount()
                        )
                )
        );
        easyPayInvoiceResponse.setVALIDTO(
                EPBFunctionUtils.dateStringFromLocalDate(
                        EPBFunctionUtils.resolveValidToDate(liability.getDueDate(), LocalDate.now())
                )
        );
        return easyPayInvoiceResponse;
    }

    public CreatePaymentRequest buildPaymentObjectCreateRequest(
            Customer customer,
            CollectionChannel collectionChannel,
            PaymentPackage paymentPackage,
            BigDecimal amount,
            Long billingGroupId,
            LocalDate paymentDate
    ) {
        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setAccountPeriodId(paymentPackage.getAccountingPeriodId());
        request.setPaymentPackageId(paymentPackage.getId());
        request.setCollectionChannelId(collectionChannel.getId());
        request.setCurrencyId(collectionChannel.getCurrencyId());
        request.setContractBillingGroupId(billingGroupId);
        request.setPaymentDate(paymentDate);
        request.setCustomerId(customer.getId());
        request.setInitialAmount(amount);
        return request;
    }

}
