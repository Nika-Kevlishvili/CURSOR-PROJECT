package bg.energo.phoenix.model.response.service;

import bg.energo.phoenix.model.enums.product.product.PaymentGuarantee;
import bg.energo.phoenix.model.enums.product.service.*;
import bg.energo.phoenix.model.response.nomenclature.customer.SegmentResponse;
import bg.energo.phoenix.model.response.nomenclature.product.GridOperatorResponse;
import bg.energo.phoenix.model.response.nomenclature.product.SalesAreaResponse;
import bg.energo.phoenix.model.response.nomenclature.product.SalesChannelResponse;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Data
public class ServiceBasicSettingsResponse {

    private Boolean isIndividual;
    private String customerIdentifier;

    private String name;
    private String nameTransliterated;

    private String printingName;
    private String printingNameTransliterated;

    private Long serviceGroupId;
    private String serviceGroupName;
    private String serviceGroupNameTransl;

    private String otherSystemConnectionCode;

    private Long serviceTypeId;
    private String serviceTypeName;

    private Set<ServiceSaleMethod> saleMethods;

    private Set<PaymentGuarantee> paymentGuarantees;
    private BigDecimal cashDepositAmount;
    private Long cashDepositCurrencyId;
    private String cashDepositCurrencyName;
    private BigDecimal bankGuaranteeAmount;
    private Long bankGuaranteeCurrencyId;
    private String bankGuaranteeCurrencyName;

    private Set<ServiceConsumptionPurpose> consumptionPurposes;

    private Set<ServicePODMeteringType> podMeteringTypes;

    private Set<ServiceVoltageLevel> voltageLevels;

    private Set<ServicePodType> typePointsOfDelivery;

    private ServiceDetailStatus serviceDetailStatus;

    private Boolean availableForSale;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDateTime availableFrom;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDateTime availableTo;

    private String shortDescription;
    private String fullDescription;

    private String invoiceAndTemplatesText;
    private String invoiceAndTemplatesTextTransliterated;

    private String incomeAccountNumber;

    private String costCenterControllingOrder;

    private Boolean globalVatRate;
    private Long vatRateId;
    private String vatRateName;

    private Long serviceUnitId;
    private String serviceUnitName;

    private Boolean globalSalesChannel;
    private List<SalesChannelResponse> salesChannels;
    private Boolean globalSalesAreas;
    private List<SalesAreaResponse> salesAreas;
    private Boolean globalSegment;
    private List<SegmentResponse> segments;

    private Boolean globalGridOperator;
    private List<GridOperatorResponse> gridOperators;

    private ServiceProvidedCapacityLimitType capacityLimitType;
    private Short capacityLimitAmount;

}
