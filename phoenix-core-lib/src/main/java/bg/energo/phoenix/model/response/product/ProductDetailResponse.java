package bg.energo.phoenix.model.response.product;

import bg.energo.phoenix.model.enums.product.product.*;
import bg.energo.phoenix.model.response.interimAdvancePayment.InterimAdvancePaymentGroupShortResponse;
import bg.energo.phoenix.model.response.interimAdvancePayment.InterimAdvancePaymentShortResponse;
import bg.energo.phoenix.model.response.nomenclature.customer.SegmentResponse;
import bg.energo.phoenix.model.response.nomenclature.product.*;
import bg.energo.phoenix.model.response.nomenclature.product.currency.CurrencyResponse;
import bg.energo.phoenix.model.response.penalty.PenaltyShortResponse;
import bg.energo.phoenix.model.response.penaltyGroup.PenaltyGroupShortResponse;
import bg.energo.phoenix.model.response.priceComponent.PriceComponentGroupShortResponse;
import bg.energo.phoenix.model.response.priceComponent.PriceComponentShortResponse;
import bg.energo.phoenix.model.response.shared.FileWithStatusesResponse;
import bg.energo.phoenix.model.response.shared.ShortResponse;
import bg.energo.phoenix.model.response.terminationGroup.TerminationGroupShortResponse;
import bg.energo.phoenix.model.response.terminations.TerminationShortResponse;
import bg.energo.phoenix.model.response.terms.TermsShortResponse;
import bg.energo.phoenix.model.response.termsGroup.TermsGroupsShortResponse;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ProductDetailResponse {
    private Long id;
    private Boolean isIndividual;
    private String customerIdentifier;
    private Long detailsId;
    private Long version;
    private String name;
    private String nameTransliterated;
    private String printingName;
    private String printingNameTransliterated;
    private ProductStatus productStatus;
    private Boolean availableForSale;
    private LocalDateTime availableFrom;
    private LocalDateTime availableTo;
    private String shortDescription;
    private String fullDescription;
    private String invoiceAndTemplatesText;
    private String invoiceAndTemplatesTextTransliterated;
    private String otherSystemConnectionCode;
    private ProductForBalancingShortResponse balancingProductNameConsumer;
    private ProductForBalancingShortResponse balancingProductNameGenerator;

    private String incomeAccountNumber;
    private String costCenterControllingOrder;
    private Boolean equalMonthlyInstallmentsActivation;
    private Short installmentNumber;
    private Short installmentNumberFrom;
    private Short installmentNumberTo;
    private BigDecimal amount;
    private BigDecimal amountFrom;
    private BigDecimal amountTo;
    private List<ScheduleRegistrations> scheduleRegistrations;
    private List<Forecasting> forecasting;
    private List<TakingOverBalancingCosts> takingOverBalancingCosts;

    private CurrencyResponse currency;
    private ProductGroupsResponse productGroups;
    private ProductTypesResponse productType;
    private VatRateResponse vatRate;
    private ElectricityPriceTypeResponse electricityPriceType;
    private List<PaymentChannels> ineligiblePaymentChannel;
    private BigDecimal cashDepositAmount;
    private CurrencyResponse cashDepositAmountCurrency;
    private BigDecimal bankGuaranteeAmount;
    private CurrencyResponse bankGuaranteeAmountCurrency;
    private List<ContractType> contractTypes;
    private List<PaymentGuarantee> paymentGuarantees;
    private List<PurposeOfConsumption> consumptionPurposes;
    private List<MeteringTypeOfThePointOfDelivery> meteringTypeOfThePointOfDeliveries;
    private List<VoltageLevel> voltageLevels;
    private List<ProductPodType> typePointsOfDelivery;

    private ProductDetailStatus detailStatus;

    private Boolean globalVatRate;
    private Boolean globalSalesArea;
    private Boolean globalSalesChannel;
    private Boolean globalSegment;
    private Boolean globalGridOperator;

    private ProvidedCapacityLimitType capacityLimitType;
    private Short capacityLimitAmount;

    private List<SalesAreaResponse> salesAreas;
    private List<SalesChannelResponse> salesChannels;
    private List<SegmentResponse> segments;

    private String additionalInfo1;
    private String additionalInfo2;
    private String additionalInfo3;
    private String additionalInfo4;
    private String additionalInfo5;
    private String additionalInfo6;
    private String additionalInfo7;
    private String additionalInfo8;
    private String additionalInfo9;
    private String additionalInfo10;

    private List<ProductAdditionalParamsResponse> productAdditionalParams;

    private TermsShortResponse terms;
    private TermsGroupsShortResponse termsGroups;

    private List<ProductContractTermsResponse> productContractTerms;
    private List<GridOperatorResponse> gridOperatorResponses;
    private List<PenaltyShortResponse> penalties;
    private List<PenaltyGroupShortResponse> penaltyGroups;

    private List<PriceComponentGroupShortResponse> priceComponentGroups;
    private List<TerminationGroupShortResponse> terminationGroups;
    private List<PriceComponentShortResponse> priceComponents;
    private List<TerminationShortResponse> terminations;
    private List<ProductRelatedEntityShortResponse> relatedEntities;
    private List<InterimAdvancePaymentShortResponse> interimAdvancePayments;
    private List<InterimAdvancePaymentGroupShortResponse> interimAdvancePaymentGroups;
    private List<FileWithStatusesResponse> productFiles;

    private List<ProductVersion> productVersions;
    private List<ShortResponse> collectionChannels;



    private boolean isLocked;

    private List<ProductServiceTemplateShortResponse> templateResponses;
}
