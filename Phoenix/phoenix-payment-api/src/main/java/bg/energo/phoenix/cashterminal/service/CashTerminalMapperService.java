package bg.energo.phoenix.cashterminal.service;

import bg.energo.phoenix.cashterminal.model.CashTerminalInvoiceResponse;
import bg.energo.phoenix.cashterminal.model.entity.CashTerminalPaymentDetailsEntity;
import bg.energo.phoenix.cashterminal.model.entity.CashTerminalPaymentEntity;
import bg.energo.phoenix.cashterminal.model.entity.CashTerminalVerifyDetailsEntity;
import bg.energo.phoenix.cashterminal.model.entity.CashTerminalVerifyEntity;
import bg.energo.phoenix.cashterminal.model.enums.CashTerminalPaymentStatus;
import bg.energo.phoenix.cashterminal.model.enums.CashTerminalRequestType;
import bg.energo.phoenix.model.CheckLiabilityDetails;
import bg.energo.phoenix.model.entity.customer.Customer;
import bg.energo.phoenix.model.entity.receivable.collectionChannel.CollectionChannel;
import bg.energo.phoenix.model.entity.receivable.paymentPackage.PaymentPackage;
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
public class CashTerminalMapperService {

    public <T> List<T> mapToDetailEntities(
            List<CheckLiabilityDetails> liabilities,
            Function<CheckLiabilityDetails, T> mapperFunction
    ) {
        return liabilities
                .stream()
                .map(mapperFunction)
                .collect(Collectors.toList());
    }

    public CashTerminalPaymentDetailsEntity mapToPaymentDetailEntity(CheckLiabilityDetails current) {
        BigDecimal totalAmount = EPBPaymentCalculationUtils.detectTotalAmount(
                current.getTotalAmount(),
                current.getCurrentAmount()
        );

        return CashTerminalPaymentDetailsEntity
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

    public CashTerminalVerifyDetailsEntity mapToVerifyDetailEntity(CheckLiabilityDetails current) {
        BigDecimal totalAmount = EPBPaymentCalculationUtils.detectTotalAmount(
                current.getTotalAmount(),
                current.getCurrentAmount()
        );

        return CashTerminalVerifyDetailsEntity
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

    public CashTerminalPaymentEntity buildCashTerminalPaymentEntity(
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
            CashTerminalRequestType requestType,
            CashTerminalPaymentStatus paymentStatus
    ) {
        return CashTerminalPaymentEntity
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

    public CashTerminalVerifyEntity buildCashTerminalVerifyEntity(
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
            CashTerminalRequestType requestType
    ) {
        return CashTerminalVerifyEntity
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

    public List<CashTerminalInvoiceResponse> mapToInvoicesResponse(
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

    public CashTerminalInvoiceResponse mapToInvoiceResponse(
            CheckLiabilityDetails liability,
            CustomerPaymentInfo customerPaymentInfo,
            String customerNumber
    ) {
        CashTerminalInvoiceResponse response = new CashTerminalInvoiceResponse();
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

