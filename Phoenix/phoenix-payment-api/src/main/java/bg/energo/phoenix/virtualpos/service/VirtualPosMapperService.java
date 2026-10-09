package bg.energo.phoenix.virtualpos.service;

import bg.energo.phoenix.model.CheckLiabilityDetails;
import bg.energo.phoenix.model.entity.receivable.collectionChannel.CollectionChannel;
import bg.energo.phoenix.model.entity.receivable.paymentPackage.PaymentPackage;
import bg.energo.phoenix.model.request.receivable.payment.CreatePaymentRequest;
import bg.energo.phoenix.model.response.customer.CustomerPaymentInfo;
import bg.energo.phoenix.utils.EPBFunctionUtils;
import bg.energo.phoenix.utils.EPBPaymentCalculationUtils;
import bg.energo.phoenix.utils.EPBStringUtils;
import bg.energo.phoenix.virtualpos.model.VirtualPosInvoiceResponse;
import bg.energo.phoenix.virtualpos.model.entity.VirtualPosPaymentDetailsEntity;
import bg.energo.phoenix.virtualpos.model.entity.VirtualPosPaymentEntity;
import bg.energo.phoenix.virtualpos.model.entity.VirtualPosVerifyDetailsEntity;
import bg.energo.phoenix.virtualpos.model.entity.VirtualPosVerifyEntity;
import bg.energo.phoenix.virtualpos.model.enums.VirtualPosPaymentStatus;
import bg.energo.phoenix.virtualpos.model.enums.VirtualPosRequestType;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class VirtualPosMapperService {

    public <T> List<T> mapToDetailEntities(
            List<CheckLiabilityDetails> liabilities,
            Function<CheckLiabilityDetails, T> mapperFunction
    ) {
        return liabilities
                .stream()
                .map(mapperFunction)
                .collect(Collectors.toList());
    }

    public VirtualPosPaymentDetailsEntity mapToPaymentDetailEntity(CheckLiabilityDetails current) {
        BigDecimal totalAmount = EPBPaymentCalculationUtils.detectTotalAmount(
                current.getTotalAmount(),
                current.getCurrentAmount()
        );

        return VirtualPosPaymentDetailsEntity
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

    public VirtualPosVerifyDetailsEntity mapToVerifyDetailEntity(CheckLiabilityDetails current) {
        BigDecimal totalAmount = EPBPaymentCalculationUtils.detectTotalAmount(
                current.getTotalAmount(),
                current.getCurrentAmount()
        );

        return VirtualPosVerifyDetailsEntity
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

    public VirtualPosPaymentEntity buildVirtualPosPaymentEntity(
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
            VirtualPosRequestType requestType,
            VirtualPosPaymentStatus paymentStatus
    ) {
        return VirtualPosPaymentEntity
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

    public VirtualPosVerifyEntity buildVirtualPosVerifyEntity(
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
            VirtualPosRequestType requestType
    ) {
        return VirtualPosVerifyEntity
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

    public List<VirtualPosInvoiceResponse> mapToInvoicesResponse(
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

    public VirtualPosInvoiceResponse mapToInvoiceResponse(
            CheckLiabilityDetails liability,
            CustomerPaymentInfo customerPaymentInfo,
            String customerNumber
    ) {
        VirtualPosInvoiceResponse response = new VirtualPosInvoiceResponse();
        response.setIDN(
                EPBStringUtils.formatInvoiceIdentifier(
                        customerNumber,
                        liability.getLiabilityId()
                )
        );
        response.setSHORTDESC(
                EPBStringUtils.formatShortDesc(customerNumber)
        );
        response.setLONGDESC(
                EPBStringUtils.formatInvoiceLongDesc(
                        customerNumber,
                        customerPaymentInfo,
                        liability
                )
        );
        response.setAMOUNT(
                EPBPaymentCalculationUtils.convertToCoinAmount(
                        EPBPaymentCalculationUtils.detectTotalAmount(
                                liability.getTotalAmount(),
                                liability.getCurrentAmount()
                        )
                )
        );
        response.setVALIDTO(
                liability.getDueDate() == null ? "0" : EPBFunctionUtils.dateStringFromLocalDate(liability.getDueDate())
        );
        return response;
    }

    public CreatePaymentRequest buildPaymentObjectCreateRequest(
            bg.energo.phoenix.model.entity.customer.Customer customer,
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


