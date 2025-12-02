package bg.energo.phoenix.model.request.product.service;

import bg.energo.phoenix.model.customAnotations.product.service.ValidIndividualService;
import bg.energo.phoenix.model.customAnotations.product.service.ValidServiceAvailableSaleDates;
import bg.energo.phoenix.model.customAnotations.product.service.ValidServiceCapacityLimit;
import bg.energo.phoenix.model.customAnotations.product.service.ValidServicePaymentGuarantee;
import bg.energo.phoenix.model.enums.product.product.PaymentGuarantee;
import bg.energo.phoenix.model.enums.product.service.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ValidServiceAvailableSaleDates
@ValidServicePaymentGuarantee
@ValidIndividualService
@ValidServiceCapacityLimit
public class ServiceBasicSettingsRequest {

    private Boolean isIndividual;

    @Size(min = 1, message = "basicSettings.customerIdentifier-Customer identifier must be at least {min} symbols;")
    @Pattern(regexp = "^[A-Z\\d]+$", message = "basicSettings.customerIdentifier-Invalid format. Allowed symbols: A-Z 0-9;")
    private String customerIdentifier;

    @Size(min = 1, max = 1024, message = "basicSettings.name-Name should match allowed length [{min}:{max}];")
    private String name;

    @Size(min = 1, max = 1024, message = "basicSettings.nameTransliterated-Transliterated Name should match allowed length [{min}:{max}];")
    private String nameTransliterated;

    @Size(min = 1, max = 1024, message = "basicSettings.printingName-Printing Name should not be blank and size should match allowed length [{min}:{max}];")
    @NotBlank(message = "basicSettings.printingName-Printing Name must not be blank;")
    private String printingName;

    @Size(min = 1, max = 1024, message = "basicSettings.printingNameTransliterated-Transliterated Printing Name should not be blank and size should match allowed length [{min}:{max}];")
    @NotBlank(message = "basicSettings.printingNameTransliterated-Transliterated Printing Name must not be blank;")
    private String printingNameTransliterated;

    private Long serviceGroupId;

    @Size(min = 1, max = 256, message = "basicSettings.otherSystemConnectionCode-OtherSystemConnectionCode should not be blank and size should match allowed length [{min}:{max}];")
    private String otherSystemConnectionCode;

    @NotNull(message = "basicSettings.serviceType-Service type ID must not be null;")
    private Long serviceTypeId;

    @NotEmpty(message = "basicSettings.saleMethods-Sale methods must not be empty;")
    private Set<ServiceSaleMethod> saleMethods;

    @NotEmpty(message = "basicSettings.paymentGuarantees-Payment guarantees must not be empty;")
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

    private Set<ServiceConsumptionPurpose> consumptionPurposes;

    private Set<ServicePODMeteringType> podMeteringTypes;

    private Set<ServiceVoltageLevel> voltageLevels;

    private Set<ServicePodType> typePointsOfDelivery;

    @NotNull(message = "basicSettings.serviceDetailStatus-Service detail status must not be null;")
    private ServiceDetailStatus serviceDetailStatus;

    @NotNull(message = "basicSettings.availableForSale-[Available for sale] field must not be null;")
    private Boolean availableForSale;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDateTime availableFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDateTime availableTo;

    @Size(min = 1, max = 8192, message = "basicSettings.shortDescription-Short Description should not be blank and size should match allowed length [{min}:{max}];")
    private String shortDescription;

    @Size(min = 1, max = 32768, message = "basicSettings.fullDescription-Full Description size should match allowed length [{min}:{max}];")
    private String fullDescription;

    private String invoiceAndTemplatesText;

    private String invoiceAndTemplatesTextTransliterated;

    @NotBlank(message = "basicSettings.incomeAccountNumber-Must not be blank;")
    @Size(max = 32, message = "basicSettings.incomeAccountNumber-Income Account Number size should match allowed length [{min}:{max}];")
    private String incomeAccountNumber;

    @NotBlank(message = "basicSettings.costCenterControllingOrder-Must not be blank;")
    @Size(max = 32, message = "basicSettings.costCenterControllingOrder-Cost center controlling order number size should match allowed length [{min}:{max}];")
    private String costCenterControllingOrder;

    private ServiceProvidedCapacityLimitType capacityLimitType;

    @Min(value = 1, message = "basicSettings.capacityLimitAmount-Capacity Limit Amount should be in range: [{1:9999}];")
    @Max(value = 9999, message = "basicSettings.capacityLimitAmount-Capacity Limit Amount should be in range: [{1:9999}];")
    private Short capacityLimitAmount;

    private Boolean globalVatRate;

    private Long vatRateId;

    private Long serviceUnitId;

    private Boolean globalSalesChannel;

    private Set<Long> salesChannels;

    private Boolean globalSalesAreas;

    private Set<Long> salesAreas;

    private Boolean globalSegment;

    private Set<Long> segments;

    private Boolean globalGridOperator;

    private Set<Long> gridOperators;

}
