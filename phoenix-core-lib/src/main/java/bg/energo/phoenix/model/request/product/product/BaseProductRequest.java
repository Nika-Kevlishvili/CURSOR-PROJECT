package bg.energo.phoenix.model.request.product.product;

import bg.energo.phoenix.model.customAnotations.DuplicatedValuesValidator;
import bg.energo.phoenix.model.customAnotations.product.product.*;
import bg.energo.phoenix.model.enums.product.product.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SaleDateAvailabilityValidator
@InstallmentNumbersValidator
@ProductAmountValidator
@CapacityLimitValidation
@ProductTermsValidator
@ProductMonthlyInstallmentValidator
@IndividualProductValidator
@GuaranteeAmountValidator
@ProductAdditionalParamsValidator
@ProductTemplateValidator
public abstract class BaseProductRequest {
    private Boolean isIndividual;

    @Size(min = 1, message = "basicSettings.customerIdentifier-Customer identifier must be at least {min} symbols;")
    @Pattern(regexp = "^[A-Z\\d]+$", message = "basicSettings.customerIdentifier-Invalid format;")
    private String customerIdentifier;

    private Long consumerBalancingProductNameId;

    private Long generatorBalancingProductNameId;

    @Size(min = 1, max = 1024, message = "basicSettings.name-Name size does not match allowed length: range [{min}:{max}];")
    private String name;

    @Size(min = 1, max = 1024, message = "basicSettings.nameTransliterated-Transliterated Name size does not match allowed length: range [{min}:{max}];")
    private String nameTransliterated;

    @Size(min = 1, max = 1024, message = "basicSettings.printingName-Printing Name size does not match allowed length: range [{min}:{max}];")
    @NotBlank(message = "basicSettings.printingName-Printing Name must not be blank;")
    private String printingName;

    @Size(min = 1, max = 1024, message = "basicSettings.printingNameTransliterated-Transliterated Printing Name size does not match allowed length: range [{min}:{max}];")
    @NotBlank(message = "basicSettings.printingNameTransliterated-Transliterated Printing Name must not be blank;")
    private String printingNameTransliterated;

    @NotNull(message = "basicSettings.productStatus-Must not be null;")
    private ProductDetailStatus productStatus;

    private Boolean availableForSale;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDateTime availableFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDateTime availableTo;

    @Size(min = 1, max = 8192, message = "basicSettings.shortDescription-Short Description size does not match allowed length: range [{min}:{max}];")
    private String shortDescription;

    @Size(min = 1, max = 32768, message = "basicSettings.fullDescription-Full Description size does not match allowed length: range [{min}:{max}];")
    private String fullDescription;

    @Size(min = 1, message = "basicSettings.productTerms-productTerms can not be empty;")
    @NotNull(message = "basicSettings.productTerms-productTerms can not be null;")
    private List<@Valid BaseProductTermsRequest> productTerms;

    private Long productGroupId;

    @Size(min = 1, max = 256, message = "basicSettings.otherSystemConnectionCode-OtherSystemConnectionCode size does not match allowed length: range [{min}:{max}];")
    private String otherSystemConnectionCode;

    @NotNull(message = "basicSettings.productTypeId-ProductTypeId must not be null;")
    private Long productTypeId;

    @Size(min = 1, message = "basicSettings.contractTypes-Contract Type array must have at least one object;")
    @NotNull(message = "basicSettings.contractType-Contract Type must not be null;")
    private Set<ContractType> contractTypes;

    @Size(min = 1, message = "basicSettings.paymentGuarantees-PaymentGuarantees array must have at least one object;")
    @NotNull(message = "basicSettings.paymentGuarantees-PaymentGuarantees must not be null;")
    private Set<PaymentGuarantee> paymentGuarantees;

    @DecimalMin(value = "0.01", message = "basicSettings.cashDepositAmount-Cash deposit amount minimum value is [{value}];")
    @DecimalMax(value = "99999999.99", message = "basicSettings.cashDepositAmount-Cash deposit amount maximum value if [{value}];")
    @Digits(integer = 8, fraction = 2, message = "basicSettings.cashDepositAmount-Invalid Decimal fractions: max [{integer}] digits and [{fraction}] fractions;")
    private BigDecimal cashDepositAmount;

    private Long cashDepositCurrencyId;

    @DecimalMin(value = "0.01", message = "basicSettings.bankGuaranteeAmount-Bank guarantee amount minimum value is [{value}];")
    @DecimalMax(value = "99999999.99", message = "basicSettings.bankGuaranteeAmount-Bank guarantee amount maximum value if [{value}];")
    @Digits(integer = 8, fraction = 2, message = "basicSettings.bankGuaranteeAmount-Invalid Decimal fractions: max [{integer}] digits and [{fraction}] fractions;")
    private BigDecimal bankGuaranteeAmount;

    private Long bankGuaranteeCurrencyId;

    @Size(min = 1, message = "basicSettings.purposeOfConsumptions-PurposeOfConsumptions array must have at least one object;")
    @NotNull(message = "basicSettings.purposeOfConsumptions-PurposeOfConsumptions must not be null;")
    private Set<PurposeOfConsumption> purposeOfConsumptions;

    @Size(min = 1, message = "basicSettings.meteringTypeOfThePointOfDeliveries-MeteringTypeOfThePointOfDeliveries array must have at least one object;")
    @NotNull(message = "basicSettings.meteringTypeOfThePointOfDeliveries-MeteringTypeOfThePointOfDeliveries must not be null;")
    private Set<MeteringTypeOfThePointOfDelivery> meteringTypeOfThePointOfDeliveries;

    @Size(min = 1, message = "basicSettings.voltageLevels-VoltageLevels array must have at least one object;")
    @NotNull(message = "basicSettings.voltageLevels-VoltageLevels must not be null;")
    private Set<VoltageLevel> voltageLevels;

    @Size(min = 1, message = "basicSettings.typePointsOfDelivery-Points of delivery array must have at least one object;")
    @NotNull(message = "basicSettings.typePointsOfDelivery-Points of delivery must not be null;")
    private Set<ProductPodType> typePointsOfDelivery;

    private Boolean globalGridOperator;

    @DuplicatedValuesValidator(fieldPath = "basicSettings.gridOperatorIds")
    private List<Long> gridOperatorIds;

    @Size(max = 2048, message = "basicSettings.invoiceAndTemplatesText-Invoice And Templates Text max size should be:{max};")
    private String invoiceAndTemplatesText;

    @Size(max = 2048, message = "basicSettings.invoiceAndTemplatesTextTransliterated-Invoice And Templates Transliterated Text max size should be:{max};")
    private String invoiceAndTemplatesTextTransliterated;

    @NotBlank(message = "basicSettings.incomeAccountNumber-incomeAccountNumber must not be blank;")
    @Size(max = 32, message = "basicSettings.incomeAccountNumber-Income Account Number size does not match allowed length: range [{min}:{max}];")
    private String incomeAccountNumber;

    @NotBlank(message = "basicSettings.costCenterControllingOrder-costCenterControllingOrder must not be blank;")
    @Size(max = 32, message = "basicSettings.costCenterControllingOrder-Cost Center Controlling Order size does not match allowed length: range [{min}:{max}];")
    private String costCenterControllingOrder;

    @NotNull(message = "basicSettings.globalVatRate-Global Vat Rate must not be null;")
    private Boolean globalVatRate;

    private Long vatRateId;

    private Boolean globalSalesChannel;

    @DuplicatedValuesValidator(fieldPath = "basicSettings.salesChannelIds")
    private List<Long> salesChannelIds;

    private Boolean globalSalesArea;

    @DuplicatedValuesValidator(fieldPath = "basicSettings.salesAreasIds")
    private List<Long> salesAreasIds;

    private Boolean globalSegment;

    @DuplicatedValuesValidator(fieldPath = "basicSettings.segmentIds")
    private List<Long> segmentIds;

    @NotNull(message = "priceSettings.electricityPriceTypeId-Cannot be null;")
    private Long electricityPriceTypeId;

    @NotNull(message = "priceSettings.equalMonthlyInstallmentsActivation-Cannot be null;")
    private Boolean equalMonthlyInstallmentsActivation;

    @Min(value = 1, message = "priceSettings.installmentNumber-Installment Number should be in range: [{1:9999}];")
    @Max(value = 9999, message = "priceSettings.installmentNumber-Installment Number should be in range: [{1:9999}];")
    private Short installmentNumber;

    @Min(value = 1, message = "priceSettings.installmentNumberFrom-Installment Number From should be in range: [{1:9999}];")
    @Max(value = 9999, message = "priceSettings.installmentNumberFrom-Installment Number From should be in range: [{1:9999}];")
    private Short installmentNumberFrom;

    @Min(value = 1, message = "priceSettings.installmentNumberTo-Installment Number To should be in range: [{1:9999}];")
    @Max(value = 9999, message = "priceSettings.installmentNumberTo-Installment Number To should be in range: [{1:9999}];")
    private Short installmentNumberTo;

    @DecimalMin(value = "0.01", message = "priceSettings.amount-Amount minimum value is [{value}];")
    @DecimalMax(value = "999999999999.99", message = "priceSettings.amount-Amount maximum value if [{value}];")
    @Digits(integer = 12, fraction = 2, message = "priceSettings.amount-Invalid Decimal fractions: max [{integer}] digits and [{fraction}] fractions;")
    private BigDecimal amount;

    @DecimalMin(value = "0.01", message = "priceSettings.amountFrom-Amount From minimum value is [{value}];")
    @DecimalMax(value = "999999999999.99", message = "priceSettings.amountFrom-Amount From maximum value if [{value}];")
    @Digits(integer = 12, fraction = 2, message = "priceSettings.amount-Invalid Decimal fractions: max [{integer}] digits and [{fraction}] fractions;")
    private BigDecimal amountFrom;

    @DecimalMin(value = "0.01", message = "priceSettings.amountTo-Amount To minimum value is [{value}];")
    @DecimalMax(value = "999999999999.99", message = "priceSettings.amountTo-Amount To maximum value if [{value}];")
    @Digits(integer = 12, fraction = 2, message = "priceSettings.amountTo-Invalid Decimal fractions: max [{integer}] digits and [{fraction}] fractions;")
    private BigDecimal amountTo;

    private Long currencyId;

    private Set<ScheduleRegistrations> scheduleRegistrations;

    private Set<Forecasting> forecasting;

    private Set<TakingOverBalancingCosts> takingOverBalancingCosts;

    private ProvidedCapacityLimitType capacityLimitType;

    @Min(value = 1, message = "basicSettings.capacityLimitAmount-Capacity Limit Amount should be in range: [{1:9999}];")
    @Max(value = 9999, message = "basicSettings.capacityLimitAmount-Capacity Limit Amount should be in range: [{1:9999}];")
    private Short capacityLimitAmount;

    @DuplicatedValuesValidator
    private List<Long> collectionChannelIds;

    private Long termId;

    private Long termGroupId;

    @DuplicatedValuesValidator(fieldPath = "additionalSettings.terminationIds")
    private List<Long> terminationIds;

    @DuplicatedValuesValidator(fieldPath = "additionalSettings.terminationGroupIds")
    private List<Long> terminationGroupIds;

    @DuplicatedValuesValidator(fieldPath = "additionalSettings.penaltyIds")
    private List<Long> penaltyIds;

    @DuplicatedValuesValidator(fieldPath = "additionalSettings.penaltyGroupIds")
    private List<Long> penaltyGroupIds;

    @DuplicatedValuesValidator(fieldPath = "priceSettings.priceComponentIds")
    private List<Long> priceComponentIds;

    @DuplicatedValuesValidator(fieldPath = "priceSettings.priceComponentGroupIds")
    private List<Long> priceComponentGroupIds;

    @DuplicatedValuesValidator(fieldPath = "basicSettings.interimAdvancePayments")
    private List<Long> interimAdvancePayments;

    @DuplicatedValuesValidator(fieldPath = "basicSettings.interimAdvancePaymentGroups")
    private List<Long> interimAdvancePaymentGroups;

    @Size(min = 1, max = 1024, message = "additionalSettings.additionalInfo1-Additional Info field length must be in range: [{min}:{max}]")
    private String additionalInfo1;

    @Size(min = 1, max = 1024, message = "additionalSettings.additionalInfo2-Additional Info field length must be in range: [{min}:{max}]")
    private String additionalInfo2;

    @Size(min = 1, max = 1024, message = "additionalSettings.additionalInfo3-Additional Info field length must be in range: [{min}:{max}]")
    private String additionalInfo3;

    @Size(min = 1, max = 1024, message = "additionalSettings.additionalInfo4-Additional Info field length must be in range: [{min}:{max}]")
    private String additionalInfo4;

    @Size(min = 1, max = 1024, message = "additionalSettings.additionalInfo5-Additional Info field length must be in range: [{min}:{max}]")
    private String additionalInfo5;

    @Size(min = 1, max = 1024, message = "additionalSettings.additionalInfo6-Additional Info field length must be in range: [{min}:{max}]")
    private String additionalInfo6;

    @Size(min = 1, max = 1024, message = "additionalSettings.additionalInfo7-Additional Info field length must be in range: [{min}:{max}]")
    private String additionalInfo7;

    @Size(min = 1, max = 1024, message = "additionalSettings.additionalInfo8-Additional Info field length must be in range: [{min}:{max}]")
    private String additionalInfo8;

    @Size(min = 1, max = 1024, message = "additionalSettings.additionalInfo9-Additional Info field length must be in range: [{min}:{max}]")
    private String additionalInfo9;

    @Size(min = 1, max = 1024, message = "additionalSettings.additionalInfo10-Additional Info field length must be in range: [{min}:{max}]")
    private String additionalInfo10;

    @ProductRelatedEntitiesValidator
    private List<ProductRelatedEntityRequest> relatedEntities;

    @DuplicatedValuesValidator(fieldPath = "basicSettings.productFileIds")
    private List<Long> productFileIds;

    private List<ProductsAdditionalParamsRequest> productAdditionalParams;
    private Set<TemplateSubObjectRequest> templateIds;
    @JsonIgnore
    @AssertTrue(message = "basicSettings.gridOperatorIds-Grid Operator IDs array must have at least one object when globalGridOperator not chosen;")
    private boolean isValidGridOperators() {
        if (globalGridOperator == null || !globalGridOperator) {
            return gridOperatorIds != null && !gridOperatorIds.isEmpty();

        }
        return true;
    }
}
