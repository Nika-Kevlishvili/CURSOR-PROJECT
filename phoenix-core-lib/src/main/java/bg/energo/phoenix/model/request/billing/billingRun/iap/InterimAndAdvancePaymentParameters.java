package bg.energo.phoenix.model.request.billing.billingRun.iap;


import bg.energo.phoenix.model.customAnotations.billing.billingRun.InterimAndAdvancePaymentParametersValidator;
import bg.energo.phoenix.model.customAnotations.customer.ValidIBAN;
import bg.energo.phoenix.model.enums.billing.billings.PrefixType;
import bg.energo.phoenix.model.enums.contract.ContractType;
import bg.energo.phoenix.model.enums.product.iap.interimAdvancePayment.DeductionFrom;
import bg.energo.phoenix.model.enums.product.iap.interimAdvancePayment.IssuingForTheMonthToCurrent;
import bg.energo.phoenix.model.enums.product.price.priceComponent.IssuedSeparateInvoice;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@InterimAndAdvancePaymentParametersValidator
public class InterimAndAdvancePaymentParameters {

    @DecimalMin(value = "0.01",inclusive = true,message = "interimAndAdvancePaymentParameters.amountExcludingVat-[amountExcludingVat] minimum value is 0.01;")
    @DecimalMax(value = "99999999999.99",inclusive = true,message = "interimAndAdvancePaymentParameters.amountExcludingVat-[amountExcludingVat] maximum value is 99999999999.99 ;")
    @NotNull(message = "interimAndAdvancePaymentParameters.amountExcludingVat-[amountExcludingVat] must not be null;")
    private BigDecimal amountExcludingVat;

    @NotNull(message = "interimAndAdvancePaymentParameters.basisForIssuing-[basisForIssuing] should not be null;")
    @Size(min = 1, max = 1024)
    private String basisForIssuing;

    @NotNull(message = "interimAndAdvancePaymentParameters.issuingForTheMonthToCurrent-[issuingForTheMonthToCurrent] must not be null;")
    private IssuingForTheMonthToCurrent issuingForTheMonthToCurrent;
    @NotNull(message = "interimAndAdvancePaymentParameters.issuedSeparateInvoices-[issuedSeparateInvoices] mot not be null;")
    @NotEmpty(message = "interimAndAdvancePaymentParameters.issuedSeparateInvoices-[issuedSeparateInvoices] at least one should be selected;")
    private List<IssuedSeparateInvoice> issuedSeparateInvoices;

    @NotNull(message = "interimAndAdvancePaymentParameters.currencyId-[currencyId] must not be null;")
    private Long currencyId;

    @NotNull(message ="interimAndAdvancePaymentParameters.deductionForm-[deductionForm] must not be null;")
    private DeductionFrom deductionFrom;

    @Size(min = 1,max = 32, message = "interimAndAdvancePaymentParameters.numberOfIncomeAccount-[numberOfIncomeAccount] number of income account size is 1 - 32!;")
    private String numberOfIncomeAccount;

    private boolean numberOfIncomeAccountManual;

    @Size(min = 1,max = 32,message = "interimAndAdvancePaymentParameters.costCenterControllingOrder-[costCenterControllingOrder] min size is 1 and max is 32;")
    private String costCenterControllingOrder;

    private boolean costCenterControllingOrderManual;

    private Long vatRateId;

    private boolean globalVatRate;

    private boolean vatRateManual;

    private Long applicableInterestRateId;

    private boolean applicableInterestRateManual;

    private boolean directDebit;

    private boolean directDebitManual;

    private Long bankId;

    @ValidIBAN(errorMessageKey = "interimAndAdvancedPaymentParameters.iban")
    private String iban;

    private PrefixType prefixType;

    // sub objects

    @NotNull(message = "interimAndAdvancePaymentParameters.customerId-[customerId] customer id is mandatory;")
    private Long customerDetailId;

    private ContractType contractType;

    private Long contractId;

    private Set<Long> billingGroupIds;

    private Long invoiceCommunicationDataId;
}
