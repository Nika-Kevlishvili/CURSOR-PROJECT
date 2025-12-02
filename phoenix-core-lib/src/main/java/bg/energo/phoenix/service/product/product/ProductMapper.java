package bg.energo.phoenix.service.product.product;

import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.nomenclature.product.Currency;
import bg.energo.phoenix.model.entity.nomenclature.product.ElectricityPriceType;
import bg.energo.phoenix.model.entity.nomenclature.product.VatRate;
import bg.energo.phoenix.model.entity.nomenclature.product.product.ProductGroups;
import bg.energo.phoenix.model.entity.nomenclature.product.product.ProductTypes;
import bg.energo.phoenix.model.entity.product.iap.advancedPaymentGroup.AdvancedPaymentGroup;
import bg.energo.phoenix.model.entity.product.iap.advancedPaymentGroup.AdvancedPaymentGroupDetails;
import bg.energo.phoenix.model.entity.product.iap.interimAdvancePayment.InterimAdvancePayment;
import bg.energo.phoenix.model.entity.product.penalty.penalty.Penalty;
import bg.energo.phoenix.model.entity.product.price.priceComponent.PriceComponentFormulaVariable;
import bg.energo.phoenix.model.entity.product.product.*;
import bg.energo.phoenix.model.entity.product.service.ServiceDetails;
import bg.energo.phoenix.model.entity.product.term.terms.Terms;
import bg.energo.phoenix.model.entity.product.term.termsGroups.TermGroupDetails;
import bg.energo.phoenix.model.entity.product.term.termsGroups.TermsGroups;
import bg.energo.phoenix.model.entity.product.termination.terminations.Termination;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.product.price.priceComponent.PriceComponentMathVariableName;
import bg.energo.phoenix.model.enums.product.product.ProductDetailStatus;
import bg.energo.phoenix.model.enums.product.product.ProductSubObjectStatus;
import bg.energo.phoenix.model.enums.product.service.ServiceDetailStatus;
import bg.energo.phoenix.model.request.contract.product.ContractInterimAdvancePaymentsRequest;
import bg.energo.phoenix.model.request.contract.product.PriceComponentContractFormula;
import bg.energo.phoenix.model.request.contract.product.ProductParameterBaseRequest;
import bg.energo.phoenix.model.request.product.product.ProductCreateRequest;
import bg.energo.phoenix.model.request.product.product.ProductEditRequest;
import bg.energo.phoenix.model.response.interimAdvancePayment.InterimAdvancePaymentGroupShortResponse;
import bg.energo.phoenix.model.response.interimAdvancePayment.InterimAdvancePaymentShortResponse;
import bg.energo.phoenix.model.response.nomenclature.product.*;
import bg.energo.phoenix.model.response.nomenclature.product.currency.CurrencyResponse;
import bg.energo.phoenix.model.response.penalty.PenaltyShortResponse;
import bg.energo.phoenix.model.response.penaltyGroup.PenaltyGroupShortResponse;
import bg.energo.phoenix.model.response.priceComponent.PriceComponentGroupShortResponse;
import bg.energo.phoenix.model.response.priceComponent.PriceComponentShortResponse;
import bg.energo.phoenix.model.response.product.*;
import bg.energo.phoenix.model.response.shared.FileWithStatusesResponse;
import bg.energo.phoenix.model.response.terminationGroup.TerminationGroupShortResponse;
import bg.energo.phoenix.model.response.terminations.TerminationShortResponse;
import bg.energo.phoenix.model.response.terms.TermsShortResponse;
import bg.energo.phoenix.model.response.termsGroup.TermsGroupsShortResponse;
import bg.energo.phoenix.repository.customer.AccountManagerRepository;
import bg.energo.phoenix.repository.product.iap.advancedPaymentGroup.AdvancedPaymentGroupDetailsRepository;
import bg.energo.phoenix.repository.product.penalty.penaltyGroup.PenaltyGroupDetailsRepository;
import bg.energo.phoenix.repository.product.price.priceComponentGroup.PriceComponentGroupDetailsRepository;
import bg.energo.phoenix.repository.product.product.ProductAdditionalParamsRepository;
import bg.energo.phoenix.repository.product.product.ProductContractTermRepository;
import bg.energo.phoenix.repository.product.product.ProductDetailsRepository;
import bg.energo.phoenix.repository.product.service.ServiceDetailsRepository;
import bg.energo.phoenix.repository.product.term.terms.TermsRepository;
import bg.energo.phoenix.repository.product.term.termsGroups.TermsGroupDetailsRepository;
import bg.energo.phoenix.repository.product.term.termsGroups.TermsGroupTermsRepository;
import bg.energo.phoenix.repository.product.term.termsGroups.TermsGroupsRepository;
import bg.energo.phoenix.repository.product.termination.terminationGroup.TerminationGroupDetailsRepository;
import bg.energo.phoenix.service.product.iap.advancedPaymentGroup.AdvancedPaymentGroupService;
import bg.energo.phoenix.service.product.iap.interimAdvancePayment.InterimAdvancePaymentService;
import bg.energo.phoenix.service.product.penalty.penalty.PenaltyService;
import bg.energo.phoenix.service.product.penalty.penaltyGroups.PenaltyGroupService;
import bg.energo.phoenix.service.product.price.priceComponent.PriceComponentService;
import bg.energo.phoenix.service.product.term.terms.TermsService;
import bg.energo.phoenix.service.product.termination.terminationGroup.TerminationGroupService;
import bg.energo.phoenix.service.product.termination.terminations.TerminationsService;
import lombok.NonNull;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ProductMapper {
    public AccountManagerRepository accountManagerRepository;
    public TermsGroupDetailsRepository termsGroupDetailsRepository;
    public ProductContractTermRepository productContractTermRepository;
    public PenaltyGroupDetailsRepository penaltyGroupDetailsRepository;
    public PriceComponentGroupDetailsRepository pcgDetailRepository;
    public TerminationGroupDetailsRepository terminationGroupDetailsRepository;
    public ServiceDetailsRepository serviceDetailsRepository;
    public ProductDetailsRepository productDetailsRepository;
    public AdvancedPaymentGroupDetailsRepository advancedPaymentGroupDetailsRepository;
    public ProductAdditionalParamsRepository productAdditionalParamsRepository;
    public AdvancedPaymentGroupService advancedPaymentGroupService;
    public InterimAdvancePaymentService interimAdvancePaymentService;
    public PriceComponentService priceComponentService;
    public TerminationGroupService terminationGroupService;
    public PenaltyGroupService penaltyGroupService;
    public PenaltyService penaltyService;
    public TerminationsService terminationsService;
    public TermsGroupTermsRepository termsGroupTermsRepository;
    public TermsGroupsRepository termsGroupsRepository;
    public TermsService termsService;
    public TermsRepository termsRepository;

    @Autowired
    public ProductMapper(TermsGroupDetailsRepository termsGroupDetailsRepository,
                         ProductContractTermRepository productContractTermRepository,
                         PenaltyGroupDetailsRepository penaltyGroupDetailsRepository,
                         PriceComponentGroupDetailsRepository pcgDetailRepository,
                         TerminationGroupDetailsRepository terminationGroupDetailsRepository,
                         ServiceDetailsRepository serviceDetailsRepository,
                         ProductDetailsRepository productDetailsRepository,
                         AdvancedPaymentGroupDetailsRepository advancedPaymentGroupDetailsRepository,
                         AdvancedPaymentGroupService advancedPaymentGroupService,
                         InterimAdvancePaymentService interimAdvancePaymentService,
                         TerminationGroupService terminationGroupService,
                         PenaltyGroupService penaltyGroupService,
                         PenaltyService penaltyService,
                         TerminationsService terminationsService,
                         TermsGroupTermsRepository termsGroupTermsRepository,
                         TermsGroupsRepository termsGroupsRepository,
                         TermsService termsService,
                         TermsRepository termsRepository,
                         PriceComponentService priceComponentService,
                         ProductAdditionalParamsRepository productAdditionalParamsRepository, AccountManagerRepository accountManagerRepository) {
        this.termsGroupDetailsRepository = termsGroupDetailsRepository;
        this.productContractTermRepository = productContractTermRepository;
        this.penaltyGroupDetailsRepository = penaltyGroupDetailsRepository;
        this.pcgDetailRepository = pcgDetailRepository;
        this.terminationGroupDetailsRepository = terminationGroupDetailsRepository;
        this.serviceDetailsRepository = serviceDetailsRepository;
        this.productDetailsRepository = productDetailsRepository;
        this.advancedPaymentGroupDetailsRepository = advancedPaymentGroupDetailsRepository;
        this.advancedPaymentGroupService = advancedPaymentGroupService;
        this.interimAdvancePaymentService = interimAdvancePaymentService;
        this.terminationGroupService = terminationGroupService;
        this.penaltyGroupService = penaltyGroupService;
        this.penaltyService = penaltyService;
        this.terminationsService = terminationsService;
        this.termsGroupTermsRepository = termsGroupTermsRepository;
        this.termsGroupsRepository = termsGroupsRepository;
        this.termsService = termsService;
        this.termsRepository = termsRepository;
        this.priceComponentService = priceComponentService;
        this.productAdditionalParamsRepository = productAdditionalParamsRepository;
        this.accountManagerRepository = accountManagerRepository;
    }

    public ProductDetails mapProductEntityToProductDetailsEntity(Product product,
                                                                 ProductCreateRequest request,
                                                                 Long version,
                                                                 ProductGroups productGroups,
                                                                 ProductTypes productType,
                                                                 VatRate vatRate,
                                                                 ElectricityPriceType electricityPriceType,
                                                                 Currency currency,
                                                                 Terms terms,
                                                                 TermsGroups termsGroups,
                                                                 ProductForBalancing productForConsumerBalancing,
                                                                 ProductForBalancing productForGeneratorBalancing) {
        return ProductDetails
                .builder()
                .version(version)
                .product(product)
                .productBalancingIdForConsumer(productForConsumerBalancing)
                .productBalancingIdForGenerator(productForGeneratorBalancing)
                .productDetailStatus(request.getProductStatus())
                .name(BooleanUtils.isTrue(request.getIsIndividual()) ? product.getId().toString() : request.getName())
                .nameTransliterated(BooleanUtils.isTrue(request.getIsIndividual()) ? product.getId().toString() : request.getNameTransliterated())
                .printingName(request.getPrintingName())
                .printingNameTransliterated(request.getPrintingNameTransliterated())
                .availableForSale(request.getAvailableForSale())
                .availableFrom(request.getAvailableFrom())
                .availableTo(request.getAvailableTo())
                .shortDescription(request.getShortDescription())
                .fullDescription(request.getFullDescription())
                .invoiceAndTemplatesText(request.getInvoiceAndTemplatesText())
                .invoiceAndTemplatesTextTransliterated(request.getInvoiceAndTemplatesTextTransliterated())
                .productGroups(productGroups)
                .otherSystemConnectionCode(request.getOtherSystemConnectionCode())
                .productType(productType)
                .incomeAccountNumber(request.getIncomeAccountNumber())
                .costCenterControllingOrder(request.getCostCenterControllingOrder())
                .globalVatRate(request.getGlobalVatRate())
                .vatRate(vatRate)
                .globalSalesArea(request.getGlobalSalesArea())
                .globalSalesChannel(request.getGlobalSalesChannel())
                .globalSegment(request.getGlobalSegment())
                .globalGridOperators(request.getGlobalGridOperator())
                .electricityPriceType(electricityPriceType)
                .equalMonthlyInstallmentsActivation(request.getEqualMonthlyInstallmentsActivation())
                .installmentNumber(request.getInstallmentNumber())
                .installmentNumberFrom(request.getInstallmentNumberFrom())
                .installmentNumberTo(request.getInstallmentNumberTo())
                .amount(request.getAmount())
                .amountFrom(request.getAmountFrom())
                .amountTo(request.getAmountTo())
                .currency(currency)
                .capacityLimitType(request.getCapacityLimitType())
                .capacityLimitAmount(request.getCapacityLimitAmount())
                .contractTypes(request.getContractTypes().stream().toList())
                .paymentGuarantees(request.getPaymentGuarantees().stream().toList())
                .cashDepositAmount(request.getCashDepositAmount())
                .bankGuaranteeAmount(request.getBankGuaranteeAmount())
                .consumptionPurposes(request.getPurposeOfConsumptions().stream().toList())
                .meteringTypeOfThePointOfDeliveries(request.getMeteringTypeOfThePointOfDeliveries().stream().toList())
                .voltageLevels(request.getVoltageLevels().stream().toList())
                .productPodTypes(request.getTypePointsOfDelivery().stream().toList())
                .scheduleRegistrations(request.getScheduleRegistrations() == null ? null : request.getScheduleRegistrations().stream().toList())
                .forecasting(request.getForecasting() == null ? null : request.getForecasting().stream().toList())
                .takingOverBalancingCosts(request.getTakingOverBalancingCosts() == null ? null : request.getTakingOverBalancingCosts().stream().toList())
                .terms(terms)
                .termsGroups(termsGroups)
                .additionalInfo1(request.getAdditionalInfo1())
                .additionalInfo2(request.getAdditionalInfo2())
                .additionalInfo3(request.getAdditionalInfo3())
                .additionalInfo4(request.getAdditionalInfo4())
                .additionalInfo5(request.getAdditionalInfo5())
                .additionalInfo6(request.getAdditionalInfo6())
                .additionalInfo7(request.getAdditionalInfo7())
                .additionalInfo8(request.getAdditionalInfo8())
                .additionalInfo9(request.getAdditionalInfo9())
                .additionalInfo10(request.getAdditionalInfo10())
                .build();
    }

    public List<PriceComponentShortResponse> createPriceComponentResponse(ProductDetails details) {
        List<ProductPriceComponents> priceComponents = details.getPriceComponents();
        return priceComponents.stream().filter(x -> x.getProductSubObjectStatus().equals(ProductSubObjectStatus.ACTIVE))
                .sorted(Comparator.comparing(BaseEntity::getCreateDate))
                .map(ProductPriceComponents::getPriceComponent)
                .map(PriceComponentShortResponse::new)
                .toList();
    }

    public List<TerminationShortResponse> createTerminationResponse(ProductDetails details) {
        List<ProductTerminations> terminations = details.getTerminations();
        return terminations.stream().filter(x -> x.getProductSubObjectStatus().equals(ProductSubObjectStatus.ACTIVE))
                .sorted(Comparator.comparing(BaseEntity::getCreateDate))
                .map(ProductTerminations::getTermination)
                .map(TerminationShortResponse::new)
                .toList();
    }

    public List<GridOperatorResponse> createGridOperatorResponse(ProductDetails details, List<NomenclatureItemStatus> statuses) {
        List<ProductGridOperator> gridOperator = details.getGridOperator();
        return gridOperator.stream()
                .filter(x -> x.getProductSubObjectStatus().equals(ProductSubObjectStatus.ACTIVE))
                .map(ProductGridOperator::getGridOperator)
                .filter(x -> statuses.contains(x.getStatus()))
                .map(GridOperatorResponse::new).toList();
    }

    public List<InterimAdvancePaymentShortResponse> createIAPShortResponse(ProductDetails details) {
        List<ProductInterimAndAdvancePayments> interimAndAdvancePayments = details.getInterimAndAdvancePayments();
        return interimAndAdvancePayments.stream()
                .filter(x -> x.getProductSubObjectStatus().equals(ProductSubObjectStatus.ACTIVE))
                .sorted(Comparator.comparing(BaseEntity::getCreateDate))
                .map(ProductInterimAndAdvancePayments::getInterimAdvancePayment)
                .map(InterimAdvancePaymentShortResponse::new).toList();
    }

    public List<PenaltyShortResponse> createPenaltyResponse(ProductDetails details) {
        List<ProductPenalty> penalties = details.getPenalties();
        return penalties.stream()
                .filter(x -> x.getProductSubObjectStatus().equals(ProductSubObjectStatus.ACTIVE))
                .sorted(Comparator.comparing(BaseEntity::getCreateDate))
                .map(ProductPenalty::getPenalty)
                .map(PenaltyShortResponse::new)
                .toList();
    }

    public ProductDetailCopyResponse mapToCopyResponse(Product product, ProductDetails details) {
        ProductDetailCopyResponse response = new ProductDetailCopyResponse();
        response.setName(details.getName());
        response.setNameTransliterated(details.getNameTransliterated());
        response.setIsIndividual(StringUtils.isNotEmpty(product.getCustomerIdentifier()));
        response.setCustomerIdentifier(product.getCustomerIdentifier());
        response.setAvailableForSale(details.getAvailableForSale());
        response.setAvailableFrom(details.getAvailableFrom());
        response.setAvailableTo(details.getAvailableTo());
        response.setPrintingName(details.getPrintingName());
        response.setProductStatus(product.getProductStatus());
        response.setDetailStatus(details.getProductDetailStatus());
        response.setPrintingNameTransliterated(details.getPrintingNameTransliterated());
        response.setShortDescription(details.getShortDescription());
        response.setFullDescription(details.getFullDescription());
        response.setInvoiceAndTemplatesText(details.getInvoiceAndTemplatesText());
        response.setInvoiceAndTemplatesTextTransliterated(details.getInvoiceAndTemplatesTextTransliterated());
        response.setIneligiblePaymentChannel(details.getPaymentChannels());
        ProductGroups productGroups = details.getProductGroups();
        if (productGroups != null && Objects.equals(NomenclatureItemStatus.ACTIVE, productGroups.getStatus())) {
            response.setProductGroups(new ProductGroupsResponse(productGroups));
        }
        response.setOtherSystemConnectionCode(details.getOtherSystemConnectionCode());
        ProductTypes productType = details.getProductType();
        if (Objects.equals(NomenclatureItemStatus.ACTIVE, productType.getStatus())) {
            response.setProductType(new ProductTypesResponse(productType));
        }
        response.setContractTypes(details.getContractTypes());
        response.setPaymentGuarantees(details.getPaymentGuarantees());
        response.setCashDepositAmount(details.getCashDepositAmount());
        Currency cashDepositCurrency = details.getCashDepositCurrency();
        if (cashDepositCurrency != null && Objects.equals(NomenclatureItemStatus.ACTIVE, cashDepositCurrency.getStatus())) {
            response.setCashDepositAmountCurrency(new CurrencyResponse(cashDepositCurrency));
        }
        response.setBankGuaranteeAmount(details.getBankGuaranteeAmount());
        Currency bankGuaranteeCurrency = details.getBankGuaranteeCurrency();
        if (bankGuaranteeCurrency != null && Objects.equals(NomenclatureItemStatus.ACTIVE, bankGuaranteeCurrency.getStatus())) {
            response.setBankGuaranteeAmountCurrency(new CurrencyResponse(bankGuaranteeCurrency));
        }
        response.setConsumptionPurposes(details.getConsumptionPurposes());
        response.setMeteringTypeOfThePointOfDeliveries(details.getMeteringTypeOfThePointOfDeliveries());
        response.setVoltageLevels(details.getVoltageLevels());
        response.setTypePointsOfDelivery(details.getProductPodTypes());
        response.setIncomeAccountNumber(details.getIncomeAccountNumber());
        response.setCostCenterControllingOrder(details.getCostCenterControllingOrder());
        response.setGlobalVatRate(details.getGlobalVatRate());
        response.setGlobalSalesArea(details.getGlobalSalesArea());
        response.setGlobalSalesChannel(details.getGlobalSalesChannel());
        response.setGlobalSegment(details.getGlobalSegment());
        response.setEqualMonthlyInstallmentsActivation(details.getEqualMonthlyInstallmentsActivation());
        response.setInstallmentNumber(details.getInstallmentNumber());
        response.setInstallmentNumberFrom(details.getInstallmentNumberFrom());
        response.setInstallmentNumberTo(details.getInstallmentNumberTo());
        response.setAmount(details.getAmount());
        response.setAmountFrom(details.getAmountFrom());
        response.setAmountTo(details.getAmountTo());
        response.setScheduleRegistrations(details.getScheduleRegistrations());
        response.setForecasting(details.getForecasting());
        response.setTakingOverBalancingCosts(details.getTakingOverBalancingCosts());
        response.setCapacityLimitType(details.getCapacityLimitType());
        response.setCapacityLimitAmount(details.getCapacityLimitAmount());
        Currency currency = details.getCurrency();
        if (currency != null && Objects.equals(NomenclatureItemStatus.ACTIVE, currency.getStatus())) {
            response.setCurrency(new CurrencyResponse(currency));
        }
        if ((details.getGlobalVatRate() == null || !details.getGlobalVatRate()) && Objects.equals(NomenclatureItemStatus.ACTIVE, details.getVatRate().getStatus())) {
            response.setVatRate(new VatRateResponse(details.getVatRate()));
        }
        if (Objects.equals(NomenclatureItemStatus.ACTIVE, details.getElectricityPriceType().getStatus())) {
            response.setElectricityPriceType(new ElectricityPriceTypeResponse(details.getElectricityPriceType()));
        }
        response.setAdditionalInfo1(details.getAdditionalInfo1());
        response.setAdditionalInfo2(details.getAdditionalInfo2());
        response.setAdditionalInfo3(details.getAdditionalInfo3());
        response.setAdditionalInfo4(details.getAdditionalInfo4());
        response.setAdditionalInfo5(details.getAdditionalInfo5());
        response.setAdditionalInfo6(details.getAdditionalInfo6());
        response.setAdditionalInfo7(details.getAdditionalInfo7());
        response.setAdditionalInfo8(details.getAdditionalInfo8());
        response.setAdditionalInfo9(details.getAdditionalInfo9());
        response.setAdditionalInfo10(details.getAdditionalInfo10());

        response.setInterimAdvancePaymentGroups(createIAPGroupShortResponse(details.getInterimAndAdvancePaymentGroups()));
        response.setTermsGroups(getTermsGroupShortResponse(details.getTermsGroups()));
        response.setPenaltyGroups(createPenaltyGroupResponse(details));
        response.setTerminationGroups(createTerminationGroupResponse(details));
        response.setPriceComponentGroups(createPriceComponentGroupResponse(details));
        ProductForBalancing productBalancingIdForConsumer = details.getProductBalancingIdForConsumer();
        if (Objects.nonNull(productBalancingIdForConsumer)) {
            response.setBalancingProductNameConsumer(new ProductForBalancingShortResponse(productBalancingIdForConsumer));
        }
        ProductForBalancing productBalancingIdForGenerator = details.getProductBalancingIdForGenerator();
        if (Objects.nonNull(productBalancingIdForGenerator)) {
            response.setBalancingProductNameGenerator(new ProductForBalancingShortResponse(productBalancingIdForGenerator));
        }
        response.setProductAdditionalParams(mapAdditionalParams(details));
        return response;
    }

    public void updateVersion(long version,
                              ProductDetails productDetails,
                              ProductEditRequest request,
                              ProductGroups productGroups,
                              ProductTypes productType,
                              VatRate vatRate,
                              ElectricityPriceType electricityPriceType,
                              Currency currency,
                              Terms terms,
                              TermsGroups termsGroups,
                              ProductForBalancing productForBalancingConsumer,
                              ProductForBalancing productForBalancingGenerator,
                              Boolean updateExistingVersion) {
        if (updateExistingVersion != null) {
            if (!updateExistingVersion) {
                productDetails.getProduct().setLastProductDetail(productDetails.getId());
            }
        }
        productDetails.getProduct().setCustomerIdentifier(request.getCustomerIdentifier());
        productDetails.setProductBalancingIdForConsumer(productForBalancingConsumer);
        productDetails.setProductBalancingIdForGenerator(productForBalancingGenerator);
        productDetails.setVersion(version);
        productDetails.setProductDetailStatus(request.getProductStatus());
        productDetails.setName(request.getName());
        productDetails.setNameTransliterated(request.getNameTransliterated());
        productDetails.setPrintingName(request.getPrintingName());
        productDetails.setPrintingNameTransliterated(request.getPrintingNameTransliterated());
        productDetails.setAvailableForSale(request.getAvailableForSale());
        productDetails.setAvailableFrom(request.getAvailableFrom());
        productDetails.setAvailableTo(request.getAvailableTo());
        productDetails.setShortDescription(request.getShortDescription());
        productDetails.setFullDescription(request.getFullDescription());
        productDetails.setInvoiceAndTemplatesText(request.getInvoiceAndTemplatesText());
        productDetails.setInvoiceAndTemplatesTextTransliterated(request.getInvoiceAndTemplatesTextTransliterated());
        productDetails.setProductGroups(productGroups);
        productDetails.setOtherSystemConnectionCode(request.getOtherSystemConnectionCode());
        productDetails.setProductType(productType);
        productDetails.setIncomeAccountNumber(request.getIncomeAccountNumber());
        productDetails.setCostCenterControllingOrder(request.getCostCenterControllingOrder());
        productDetails.setGlobalVatRate(request.getGlobalVatRate());
        productDetails.setVatRate(vatRate);
        productDetails.setGlobalSalesArea(request.getGlobalSalesArea());
        productDetails.setGlobalSalesChannel(request.getGlobalSalesChannel());
        productDetails.setGlobalSegment(request.getGlobalSegment());
        productDetails.setGlobalGridOperators(request.getGlobalGridOperator());
        productDetails.setElectricityPriceType(electricityPriceType);
        productDetails.setEqualMonthlyInstallmentsActivation(request.getEqualMonthlyInstallmentsActivation());
        productDetails.setInstallmentNumber(request.getInstallmentNumber());
        productDetails.setInstallmentNumberFrom(request.getInstallmentNumberFrom());
        productDetails.setInstallmentNumberTo(request.getInstallmentNumberTo());
        productDetails.setAmount(request.getAmount());
        productDetails.setAmountFrom(request.getAmountFrom());
        productDetails.setAmountTo(request.getAmountTo());
        productDetails.setCurrency(currency);
        productDetails.setCapacityLimitType(request.getCapacityLimitType());
        productDetails.setCapacityLimitAmount(request.getCapacityLimitAmount());
        productDetails.setContractTypes(request.getContractTypes().stream().toList());
        productDetails.setPaymentGuarantees(request.getPaymentGuarantees().stream().toList());
        productDetails.setCashDepositAmount(request.getCashDepositAmount());
        productDetails.setBankGuaranteeAmount(request.getBankGuaranteeAmount());
        productDetails.setConsumptionPurposes(request.getPurposeOfConsumptions().stream().toList());
        productDetails.setMeteringTypeOfThePointOfDeliveries(request.getMeteringTypeOfThePointOfDeliveries().stream().toList());
        productDetails.setVoltageLevels(request.getVoltageLevels().stream().toList());
        productDetails.setProductPodTypes(request.getTypePointsOfDelivery().stream().toList());
        productDetails.setScheduleRegistrations(request.getScheduleRegistrations() == null ? null : request.getScheduleRegistrations().stream().toList());
        productDetails.setForecasting(request.getForecasting() == null ? null : request.getForecasting().stream().toList());
        productDetails.setTakingOverBalancingCosts(request.getTakingOverBalancingCosts() == null ? null : request.getTakingOverBalancingCosts().stream().toList());
        productDetails.setTerms(terms);
        productDetails.setTermsGroups(termsGroups);
        productDetails.setAdditionalInfo1(request.getAdditionalInfo1());
        productDetails.setAdditionalInfo2(request.getAdditionalInfo2());
        productDetails.setAdditionalInfo3(request.getAdditionalInfo3());
        productDetails.setAdditionalInfo4(request.getAdditionalInfo4());
        productDetails.setAdditionalInfo5(request.getAdditionalInfo5());
        productDetails.setAdditionalInfo6(request.getAdditionalInfo6());
        productDetails.setAdditionalInfo7(request.getAdditionalInfo7());
        productDetails.setAdditionalInfo8(request.getAdditionalInfo8());
        productDetails.setAdditionalInfo9(request.getAdditionalInfo9());
        productDetails.setAdditionalInfo10(request.getAdditionalInfo10());
    }


    public ProductDetails newInstance(long version, ProductDetails productDetails) {
        return new ProductDetails(
                null,
                version,
                productDetails.getProduct(),
                productDetails.getProductBalancingIdForConsumer(),
                productDetails.getProductBalancingIdForGenerator(),
                productDetails.getProductDetailStatus(),
                productDetails.getName(),
                productDetails.getNameTransliterated(),
                productDetails.getAvailableForSale(),
                productDetails.getAvailableFrom(),
                productDetails.getAvailableTo(),
                productDetails.getPrintingName(),
                productDetails.getPrintingNameTransliterated(),
                productDetails.getShortDescription(),
                productDetails.getFullDescription(),
                productDetails.getInvoiceAndTemplatesText(),
                productDetails.getInvoiceAndTemplatesTextTransliterated(),
                productDetails.getProductGroups(),
                productDetails.getOtherSystemConnectionCode(),
                productDetails.getProductType(),
                productDetails.getContractTypes(),
                productDetails.getPaymentGuarantees(),
                productDetails.getCashDepositAmount(),
                productDetails.getCashDepositCurrency(),
                productDetails.getBankGuaranteeAmount(),
                productDetails.getBankGuaranteeCurrency(),
                productDetails.getConsumptionPurposes(),
                productDetails.getMeteringTypeOfThePointOfDeliveries(),
                productDetails.getVoltageLevels(),
                productDetails.getProductPodTypes(),
                productDetails.getIncomeAccountNumber(),
                productDetails.getCostCenterControllingOrder(),
                productDetails.getGlobalVatRate(),
                productDetails.getVatRate(),
                productDetails.getGlobalSalesArea(),
                productDetails.getGlobalSalesChannel(),
                productDetails.getGlobalSegment(),
                productDetails.getGlobalGridOperators(),
                productDetails.getElectricityPriceType(),
                productDetails.getEqualMonthlyInstallmentsActivation(),
                productDetails.getInstallmentNumber(),
                productDetails.getInstallmentNumberFrom(),
                productDetails.getInstallmentNumberTo(),
                productDetails.getAmount(),
                productDetails.getAmountFrom(),
                productDetails.getAmountTo(),
                productDetails.getCurrency(),
                productDetails.getScheduleRegistrations(),
                productDetails.getForecasting(),
                productDetails.getTakingOverBalancingCosts(),
                productDetails.getCapacityLimitType(),
                productDetails.getPaymentChannels(),
                productDetails.getCapacityLimitAmount(),
                productDetails.getAdditionalInfo1(),
                productDetails.getAdditionalInfo2(),
                productDetails.getAdditionalInfo3(),
                productDetails.getAdditionalInfo4(),
                productDetails.getAdditionalInfo5(),
                productDetails.getAdditionalInfo6(),
                productDetails.getAdditionalInfo7(),
                productDetails.getAdditionalInfo8(),
                productDetails.getAdditionalInfo9(),
                productDetails.getAdditionalInfo10(),
                productDetails.getTerms(),
                productDetails.getTermsGroups(),
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>()
        );
    }

    public List<FileWithStatusesResponse> createProductFileShortResponse(List<ProductFile> productFiles) {
        return productFiles
                .stream()
                .filter(productFile -> productFile.getStatus().equals(EntityStatus.ACTIVE))
                .sorted(Comparator.comparing(BaseEntity::getCreateDate))
                .map(file -> new FileWithStatusesResponse(file, accountManagerRepository.findByUserName(file.getSystemUserId())
                        .map(manager -> " (".concat(manager.getDisplayName()).concat(")")).orElse(""))).toList();
    }

    public ProductDetailResponse mapToResponse(Product product, ProductDetails details) {
        ProductDetailResponse productDetailResponse = new ProductDetailResponse();
        productDetailResponse.setId(product.getId());
        productDetailResponse.setProductStatus(product.getProductStatus());
        productDetailResponse.setDetailsId(details.getId());
        productDetailResponse.setVersion(details.getVersion());
        productDetailResponse.setIsIndividual(StringUtils.isNotEmpty(product.getCustomerIdentifier()));
        productDetailResponse.setCustomerIdentifier(product.getCustomerIdentifier());
        productDetailResponse.setDetailStatus(details.getProductDetailStatus());
        productDetailResponse.setName(details.getName());
        productDetailResponse.setNameTransliterated(details.getNameTransliterated());
        productDetailResponse.setAvailableForSale(details.getAvailableForSale());
        productDetailResponse.setAvailableFrom(details.getAvailableFrom());
        productDetailResponse.setAvailableTo(details.getAvailableTo());
        productDetailResponse.setPrintingName(details.getPrintingName());
        productDetailResponse.setPrintingNameTransliterated(details.getPrintingNameTransliterated());
        productDetailResponse.setShortDescription(details.getShortDescription());
        productDetailResponse.setFullDescription(details.getFullDescription());
        productDetailResponse.setInvoiceAndTemplatesText(details.getInvoiceAndTemplatesText());
        productDetailResponse.setInvoiceAndTemplatesTextTransliterated(details.getInvoiceAndTemplatesTextTransliterated());
        ProductGroups productGroups = details.getProductGroups();
        if (productGroups != null) {
            productDetailResponse.setProductGroups(new ProductGroupsResponse(productGroups));
        }
        productDetailResponse.setOtherSystemConnectionCode(details.getOtherSystemConnectionCode());
        productDetailResponse.setProductType(new ProductTypesResponse(details.getProductType()));
        productDetailResponse.setContractTypes(details.getContractTypes());
        productDetailResponse.setPaymentGuarantees(details.getPaymentGuarantees());
        productDetailResponse.setCashDepositAmount(details.getCashDepositAmount());
        Currency cashDepositCurrency = details.getCashDepositCurrency();
        productDetailResponse.setCashDepositAmountCurrency(cashDepositCurrency == null ? null : new CurrencyResponse(cashDepositCurrency));
        productDetailResponse.setBankGuaranteeAmount(details.getBankGuaranteeAmount());
        Currency bankGuaranteeCurrency = details.getBankGuaranteeCurrency();
        productDetailResponse.setBankGuaranteeAmountCurrency(bankGuaranteeCurrency == null ? null : new CurrencyResponse(bankGuaranteeCurrency));
        productDetailResponse.setConsumptionPurposes(details.getConsumptionPurposes());
        productDetailResponse.setMeteringTypeOfThePointOfDeliveries(details.getMeteringTypeOfThePointOfDeliveries());
        productDetailResponse.setVoltageLevels(details.getVoltageLevels());
        productDetailResponse.setTypePointsOfDelivery(details.getProductPodTypes());
        productDetailResponse.setIncomeAccountNumber(details.getIncomeAccountNumber());
        productDetailResponse.setCostCenterControllingOrder(details.getCostCenterControllingOrder());
        productDetailResponse.setGlobalVatRate(details.getGlobalVatRate());
        productDetailResponse.setGlobalSalesArea(details.getGlobalSalesArea());
        productDetailResponse.setGlobalSalesChannel(details.getGlobalSalesChannel());
        productDetailResponse.setGlobalSegment(details.getGlobalSegment());
        productDetailResponse.setGlobalGridOperator(details.getGlobalGridOperators());
        productDetailResponse.setEqualMonthlyInstallmentsActivation(details.getEqualMonthlyInstallmentsActivation());
        productDetailResponse.setInstallmentNumber(details.getInstallmentNumber());
        productDetailResponse.setInstallmentNumberFrom(details.getInstallmentNumberFrom());
        productDetailResponse.setInstallmentNumberTo(details.getInstallmentNumberTo());
        productDetailResponse.setAmount(details.getAmount());
        productDetailResponse.setAmountFrom(details.getAmountFrom());
        productDetailResponse.setAmountTo(details.getAmountTo());
        productDetailResponse.setScheduleRegistrations(details.getScheduleRegistrations());
        productDetailResponse.setForecasting(details.getForecasting());
        productDetailResponse.setTakingOverBalancingCosts(details.getTakingOverBalancingCosts());
        productDetailResponse.setCapacityLimitType(details.getCapacityLimitType());
        productDetailResponse.setCapacityLimitAmount(details.getCapacityLimitAmount());
        productDetailResponse.setCurrency(details.getCurrency() == null ? null : new CurrencyResponse(details.getCurrency()));
        if (details.getGlobalVatRate() == null || !details.getGlobalVatRate()) {
            productDetailResponse.setVatRate(new VatRateResponse(details.getVatRate()));
        }
        productDetailResponse.setElectricityPriceType(new ElectricityPriceTypeResponse(details.getElectricityPriceType()));
        productDetailResponse.setIneligiblePaymentChannel(details.getPaymentChannels());
        productDetailResponse.setAdditionalInfo1(details.getAdditionalInfo1());
        productDetailResponse.setAdditionalInfo2(details.getAdditionalInfo2());
        productDetailResponse.setAdditionalInfo3(details.getAdditionalInfo3());
        productDetailResponse.setAdditionalInfo4(details.getAdditionalInfo4());
        productDetailResponse.setAdditionalInfo5(details.getAdditionalInfo5());
        productDetailResponse.setAdditionalInfo6(details.getAdditionalInfo6());
        productDetailResponse.setAdditionalInfo7(details.getAdditionalInfo7());
        productDetailResponse.setAdditionalInfo8(details.getAdditionalInfo8());
        productDetailResponse.setAdditionalInfo9(details.getAdditionalInfo9());
        productDetailResponse.setAdditionalInfo10(details.getAdditionalInfo10());
        productDetailResponse.setProductAdditionalParams(
                mapAdditionalParams(details)
        );
        Terms terms = details.getTerms();
        if (terms != null) {
            productDetailResponse.setTerms(new TermsShortResponse(details.getTerms()));
        }
        productDetailResponse.setTermsGroups(getTermsGroupShortResponse(details.getTermsGroups()));
        productDetailResponse.setProductContractTerms(createProductTermsResponse(details));
        productDetailResponse.setPenalties(createPenaltyResponse(details));
        productDetailResponse.setPenaltyGroups(createPenaltyGroupResponse(details));
        productDetailResponse.setPriceComponentGroups(createPriceComponentGroupResponse(details));
        productDetailResponse.setTerminationGroups(createTerminationGroupResponse(details));
        productDetailResponse.setPriceComponents(createPriceComponentResponse(details));
        productDetailResponse.setTerminations(createTerminationResponse(details));
        productDetailResponse.setRelatedEntities(createLinkedEntitiesShortResponse(details.getLinkedProducts(), details.getLinkedServices()));
        productDetailResponse.setInterimAdvancePayments(createIAPShortResponse(details));
        productDetailResponse.setInterimAdvancePaymentGroups(createIAPGroupShortResponse(details.getInterimAndAdvancePaymentGroups()));
        productDetailResponse.setProductFiles(createProductFileShortResponse(details.getProductFiles()));
        productDetailResponse.setDetailStatus(details.getProductDetailStatus());
        ProductForBalancing productForBalancingConsumer = details.getProductBalancingIdForConsumer();
        if (Objects.nonNull(productForBalancingConsumer)) {
            productDetailResponse.setBalancingProductNameConsumer(new ProductForBalancingShortResponse(productForBalancingConsumer));
        }
        ProductForBalancing productForBalancingGenerator = details.getProductBalancingIdForGenerator();
        if (Objects.nonNull(productForBalancingGenerator)) {
            productDetailResponse.setBalancingProductNameGenerator(new ProductForBalancingShortResponse(productForBalancingGenerator));
        }

        return productDetailResponse;
    }

    public List<InterimAdvancePaymentGroupShortResponse> createIAPGroupShortResponse(List<ProductGroupOfInterimAndAdvancePayments> advancePaymentGroups) {
        List<AdvancedPaymentGroup> advancedPaymentGroups = advancePaymentGroups.stream()
                .filter(x -> x.getProductSubObjectStatus().equals(ProductSubObjectStatus.ACTIVE))
                .sorted(Comparator.comparing(BaseEntity::getCreateDate))
                .map(ProductGroupOfInterimAndAdvancePayments::getInterimAdvancePaymentGroup)
                .toList();
        List<AdvancedPaymentGroupDetails> details = new ArrayList<>();
        for (AdvancedPaymentGroup advancedPaymentGroup : advancedPaymentGroups) {
            advancedPaymentGroupDetailsRepository.findFirstByAdvancedPaymentGroupIdAndStartDateLessThanEqualOrderByStartDateDesc(advancedPaymentGroup.getId(), LocalDate.now())
                    .ifPresent(details::add);
        }
        return details.stream().map(InterimAdvancePaymentGroupShortResponse::new).toList();
    }

    public List<ProductRelatedEntityShortResponse> createLinkedEntitiesShortResponse(List<ProductLinkToProduct> linkedProducts, List<ProductLinkToService> linkedServices) {
        List<ProductRelatedEntityShortResponse> productLinkedEntityShortResponse = new ArrayList<>(linkedProducts
                .stream()
                .filter(p -> p.getProductSubObjectStatus().equals(ProductSubObjectStatus.ACTIVE))
                .map(productLinkToProduct -> {
                    Optional<ProductDetails> productDetailsOptional = productDetailsRepository
                            .findLatestDetails(productLinkToProduct.getLinkedProduct().getId(),
                                    List.of(ProductDetailStatus.ACTIVE, ProductDetailStatus.INACTIVE),
                                    Sort.by(Sort.Direction.DESC, "version"));

                    return productDetailsOptional
                            .map(productDetails ->
                                    new ProductRelatedEntityShortResponse(productLinkToProduct, productDetails))
                            .orElseGet(() ->
                                    new ProductRelatedEntityShortResponse(productLinkToProduct, null));
                })
                .toList());

        productLinkedEntityShortResponse
                .addAll(linkedServices
                        .stream()
                        .filter(s -> s.getProductSubObjectStatus().equals(ProductSubObjectStatus.ACTIVE))
                        .map(productLinkToService -> {
                            Optional<ServiceDetails> serviceDetailsOptional = serviceDetailsRepository
                                    .findLastDetailByServiceId(productLinkToService.getLinkedService().getId(),
                                            List.of(ServiceDetailStatus.ACTIVE, ServiceDetailStatus.INACTIVE),
                                            Sort.by(Sort.Direction.DESC, "version"));

                            return serviceDetailsOptional
                                    .map(serviceDetails ->
                                            new ProductRelatedEntityShortResponse(productLinkToService, serviceDetails))
                                    .orElseGet(() ->
                                            new ProductRelatedEntityShortResponse(productLinkToService, null));
                        })
                        .toList());

        return productLinkedEntityShortResponse
                .stream()
                .sorted(Comparator.comparing(ProductRelatedEntityShortResponse::getCreateDate))
                .toList();
    }

    public TermsGroupsShortResponse getTermsGroupShortResponse(TermsGroups termsGroups) {
        if (termsGroups == null) {
            return null;
        }
        TermGroupDetails termGroupDetails = termsGroupDetailsRepository.findFirstByGroupIdAndStartDateLessThanEqualOrderByStartDateDesc(termsGroups.getId(), LocalDateTime.now())
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Term group not found with ID %s;".formatted(termsGroups.getId())));
        return new TermsGroupsShortResponse(termsGroups, termGroupDetails);
    }

    public List<ProductContractTermsResponse> createProductTermsResponse(ProductDetails productDetails) {
        List<ProductContractTerms> productContractTerms = productContractTermRepository
                .findAllByProductDetailsIdAndStatusInOrderByCreateDate(productDetails.getId(), List.of(ProductSubObjectStatus.ACTIVE));
        return productContractTerms.stream().map(ProductContractTermsResponse::new).toList();
    }


    public List<PenaltyGroupShortResponse> createPenaltyGroupResponse(ProductDetails details) {
        return details.getPenaltyGroups()
                .stream().filter(x -> x.getProductSubObjectStatus().equals(ProductSubObjectStatus.ACTIVE))
                .map(ProductPenaltyGroups::getPenaltyGroup)
                .map(x -> penaltyGroupDetailsRepository.findFirstByPenaltyGroupIdAndStartDateLessThanEqualOrderByStartDateDesc(x.getId(), LocalDate.now())
                        .orElseThrow(() -> new DomainEntityNotFoundException(String.format("Penalty Group details for penalty group with id: %s do not exists!", x.getId()))))
                .map(PenaltyGroupShortResponse::new)
                .toList();
    }

    public List<PriceComponentGroupShortResponse> createPriceComponentGroupResponse(ProductDetails details) {
        List<ProductPriceComponentGroups> priceComponentGroups = details.getPriceComponentGroups();
        return priceComponentGroups.stream().filter(x -> x.getProductSubObjectStatus().equals(ProductSubObjectStatus.ACTIVE))
                .sorted(Comparator.comparing(BaseEntity::getCreateDate))
                .map(ProductPriceComponentGroups::getPriceComponentGroup)
                .map(x -> pcgDetailRepository.findFirstByPriceComponentGroupIdAndStartDateLessThanEqualOrderByStartDateDesc(x.getId(), LocalDate.now())
                        .orElseThrow(() -> new DomainEntityNotFoundException(String.format("Price component group details not found for group: %s", x.getId()))))
                .map(PriceComponentGroupShortResponse::new)
                .toList();
    }

    public List<TerminationGroupShortResponse> createTerminationGroupResponse(ProductDetails details) {
        List<ProductTerminationGroups> terminationGroups = details.getTerminationGroups();
        return terminationGroups.stream().filter(x -> x.getProductSubObjectStatus().equals(ProductSubObjectStatus.ACTIVE))
                .sorted(Comparator.comparing(BaseEntity::getCreateDate))
                .map(ProductTerminationGroups::getTerminationGroup)
                .map(x -> terminationGroupDetailsRepository.findFirstByTerminationGroupIdAndStartDateLessThanEqualOrderByStartDateDesc(x.getId(), LocalDate.now())
                        .orElseThrow(() -> new DomainEntityNotFoundException(String.format("Termination Group details with group id %s do not exists", x.getId()))))
                .map(TerminationGroupShortResponse::new)
                .toList();
    }


    public List<InterimAdvancePaymentShortResponse> copyInterimAdvancePayments(List<ProductInterimAndAdvancePayments> interimAndAdvancePayments) {
        return interimAdvancePaymentService
                .copyIAPForSubObjects(interimAndAdvancePayments
                        .stream()
                        .filter(x -> x.getProductSubObjectStatus().equals(ProductSubObjectStatus.ACTIVE))
                        .map(ProductInterimAndAdvancePayments::getInterimAdvancePayment)
                        .toList())
                .stream()
                .filter(Objects::nonNull)
                .map(InterimAdvancePaymentShortResponse::new)
                .toList();
    }


    public List<PenaltyShortResponse> copyPenalties(List<ProductPenalty> penalties) {
        List<Penalty> list = penalties.stream()
                .filter(x -> x.getProductSubObjectStatus().equals(ProductSubObjectStatus.ACTIVE))
                .map(ProductPenalty::getPenalty)
                .toList();
        return penaltyService.clonePenaltiesForSubObjects(list).stream().map(PenaltyShortResponse::new).toList();
    }

    public List<TerminationShortResponse> copyTerminations(List<ProductTerminations> terminations) {
        List<Termination> list = terminations.stream()
                .filter(x -> x.getProductSubObjectStatus().equals(ProductSubObjectStatus.ACTIVE))
                .map(ProductTerminations::getTermination)
                .toList();
        return terminationsService.copyTerminationsForSubObjects(list).stream().map(TerminationShortResponse::new).toList();
    }

    public List<PriceComponentShortResponse> copyPriceComponents(List<ProductPriceComponents> priceComponents) {
        return priceComponentService
                .copyPriceComponentsForSubObjects(priceComponents
                        .stream()
                        .filter(x -> x.getProductSubObjectStatus().equals(ProductSubObjectStatus.ACTIVE))
                        .map(ProductPriceComponents::getPriceComponent)
                        .toList())
                .stream()
                .filter(Objects::nonNull)
                .map(PriceComponentShortResponse::new)
                .toList();

    }

    public void createProductClone(@NonNull ProductDetails originalDetails, @NonNull Product clone, @NonNull ProductDetails cloneDetails) {
        cloneDetails.setProduct(clone);
        cloneDetails.setProductBalancingIdForConsumer(originalDetails.getProductBalancingIdForConsumer());
        cloneDetails.setProductBalancingIdForGenerator(originalDetails.getProductBalancingIdForGenerator());
        cloneDetails.setName(originalDetails.getName());
        cloneDetails.setNameTransliterated(originalDetails.getNameTransliterated());
        cloneDetails.setAvailableForSale(originalDetails.getAvailableForSale());
        cloneDetails.setAvailableFrom(originalDetails.getAvailableFrom());
        cloneDetails.setAvailableTo(originalDetails.getAvailableTo());
        cloneDetails.setPrintingName(originalDetails.getPrintingName());
        cloneDetails.setProductDetailStatus(originalDetails.getProductDetailStatus());
        cloneDetails.setPrintingNameTransliterated(originalDetails.getPrintingNameTransliterated());
        cloneDetails.setShortDescription(originalDetails.getShortDescription());
        cloneDetails.setFullDescription(originalDetails.getFullDescription());
        cloneDetails.setInvoiceAndTemplatesText(originalDetails.getInvoiceAndTemplatesText());
        cloneDetails.setInvoiceAndTemplatesTextTransliterated(originalDetails.getInvoiceAndTemplatesTextTransliterated());
        cloneDetails.setPaymentChannels(originalDetails.getPaymentChannels());
        cloneDetails.setProductGroups(originalDetails.getProductGroups());

        cloneDetails.setOtherSystemConnectionCode(originalDetails.getOtherSystemConnectionCode());
        cloneDetails.setProductType(originalDetails.getProductType());

        cloneDetails.setContractTypes(originalDetails.getContractTypes());
        cloneDetails.setPaymentGuarantees(originalDetails.getPaymentGuarantees());
        cloneDetails.setCashDepositAmount(originalDetails.getCashDepositAmount());

        cloneDetails.setCashDepositCurrency(originalDetails.getCashDepositCurrency());

        cloneDetails.setBankGuaranteeAmount(originalDetails.getBankGuaranteeAmount());
        cloneDetails.setBankGuaranteeCurrency(originalDetails.getBankGuaranteeCurrency());
        cloneDetails.setConsumptionPurposes(originalDetails.getConsumptionPurposes());
        cloneDetails.setMeteringTypeOfThePointOfDeliveries(originalDetails.getMeteringTypeOfThePointOfDeliveries());
        cloneDetails.setVoltageLevels(originalDetails.getVoltageLevels());
        cloneDetails.setProductPodTypes(originalDetails.getProductPodTypes());
        cloneDetails.setIncomeAccountNumber(originalDetails.getIncomeAccountNumber());
        cloneDetails.setCostCenterControllingOrder(originalDetails.getCostCenterControllingOrder());
        cloneDetails.setGlobalVatRate(originalDetails.getGlobalVatRate());
        cloneDetails.setGlobalSalesArea(originalDetails.getGlobalSalesArea());
        cloneDetails.setGlobalSalesChannel(originalDetails.getGlobalSalesChannel());
        cloneDetails.setGlobalSegment(originalDetails.getGlobalSegment());
        cloneDetails.setEqualMonthlyInstallmentsActivation(originalDetails.getEqualMonthlyInstallmentsActivation());
        cloneDetails.setInstallmentNumber(originalDetails.getInstallmentNumber());
        cloneDetails.setInstallmentNumberFrom(originalDetails.getInstallmentNumberFrom());
        cloneDetails.setInstallmentNumberTo(originalDetails.getInstallmentNumberTo());
        cloneDetails.setAmount(originalDetails.getAmount());
        cloneDetails.setAmountFrom(originalDetails.getAmountFrom());
        cloneDetails.setAmountTo(originalDetails.getAmountTo());
        cloneDetails.setScheduleRegistrations(originalDetails.getScheduleRegistrations());
        cloneDetails.setForecasting(originalDetails.getForecasting());
        cloneDetails.setTakingOverBalancingCosts(originalDetails.getTakingOverBalancingCosts());
        cloneDetails.setCapacityLimitType(originalDetails.getCapacityLimitType());
        cloneDetails.setCapacityLimitAmount(originalDetails.getCapacityLimitAmount());

        cloneDetails.setCurrency(originalDetails.getCurrency());
        cloneDetails.setGlobalVatRate(originalDetails.getGlobalVatRate());
        cloneDetails.setVatRate(originalDetails.getVatRate());
        cloneDetails.setElectricityPriceType(originalDetails.getElectricityPriceType());

        cloneDetails.setAdditionalInfo1(originalDetails.getAdditionalInfo1());
        cloneDetails.setAdditionalInfo2(originalDetails.getAdditionalInfo2());
        cloneDetails.setAdditionalInfo3(originalDetails.getAdditionalInfo3());
        cloneDetails.setAdditionalInfo4(originalDetails.getAdditionalInfo4());
        cloneDetails.setAdditionalInfo5(originalDetails.getAdditionalInfo5());
        cloneDetails.setAdditionalInfo6(originalDetails.getAdditionalInfo6());
        cloneDetails.setAdditionalInfo7(originalDetails.getAdditionalInfo7());
        cloneDetails.setAdditionalInfo8(originalDetails.getAdditionalInfo8());
        cloneDetails.setAdditionalInfo9(originalDetails.getAdditionalInfo9());
        cloneDetails.setAdditionalInfo10(originalDetails.getAdditionalInfo10());
        cloneDetails.setVersion(1L);

        ProductDetails savedCloneDetails = productDetailsRepository.saveAndFlush(cloneDetails);

        List<ProductAdditionalParams> newAdditionalParamsList = new ArrayList<>();
        for (ProductAdditionalParams params : originalDetails.getProductAdditionalParams()) {
            ProductAdditionalParams newParams = new ProductAdditionalParams();
            newParams.setProductDetailId(savedCloneDetails.getId());
            newParams.setLabel(params.getLabel());
            newParams.setValue(params.getValue());
            newParams.setOrderingId(params.getOrderingId());
            newAdditionalParamsList.add(newParams);
        }
        productAdditionalParamsRepository.saveAll(newAdditionalParamsList);

        cloneDetails.setInterimAndAdvancePaymentGroups(createProductIAPGroupClone(originalDetails.getInterimAndAdvancePaymentGroups(), cloneDetails));
        if (originalDetails.getTermsGroups() != null) {
            cloneDetails.setTermsGroups(originalDetails.getTermsGroups());
        }
        cloneDetails.setPenaltyGroups(createProductPenaltyGroupClones(originalDetails.getPenaltyGroups(), cloneDetails));
        cloneDetails.setTerminationGroups(createTerminationGroupClones(originalDetails.getTerminationGroups(), cloneDetails));
        cloneDetails.setPriceComponentGroups(createPriceComponentGroupClones(originalDetails.getPriceComponentGroups(), cloneDetails));

        cloneDetails.setTerminations(createProductTerminationClones(originalDetails.getTerminations(), cloneDetails));
        cloneDetails.setPenalties(createProductPenaltyClones(originalDetails.getPenalties(), cloneDetails));


        cloneDetails.setProductFiles(cloneProductFile(originalDetails.getProductFiles(), cloneDetails));
        cloneDetails.setLinkedProducts(cloneLinkedProducts(originalDetails.getLinkedProducts(), cloneDetails));
        cloneDetails.setLinkedServices(cloneLinkedService(originalDetails.getLinkedServices(), cloneDetails));
    }

    private List<ProductLinkToService> cloneLinkedService(List<ProductLinkToService> linkedServices, ProductDetails cloneDetails) {
        List<ProductLinkToService> productLinkToServices = new ArrayList<>();
        for (ProductLinkToService linkedService : linkedServices) {
            if (linkedService.getProductSubObjectStatus().equals(ProductSubObjectStatus.ACTIVE)) {
                ProductLinkToService productLinkToService = new ProductLinkToService();
                productLinkToService.setProductDetails(cloneDetails);
                productLinkToService.setLinkedService(linkedService.getLinkedService());
                productLinkToService.setObligatory(linkedService.getObligatory());
                productLinkToService.setAllowSalesUnder(linkedService.getAllowSalesUnder());
                productLinkToService.setProductSubObjectStatus(ProductSubObjectStatus.ACTIVE);
                productLinkToServices.add(productLinkToService);
            }
        }
        return productLinkToServices;
    }

    private List<ProductLinkToProduct> cloneLinkedProducts(List<ProductLinkToProduct> linkedProducts, ProductDetails cloneDetails) {
        List<ProductLinkToProduct> productLinkToProducts = new ArrayList<>();
        for (ProductLinkToProduct linkedProduct : linkedProducts) {
            if (linkedProduct.getProductSubObjectStatus().equals(ProductSubObjectStatus.ACTIVE)) {
                ProductLinkToProduct productLinkToProduct = new ProductLinkToProduct();
                productLinkToProduct.setProductDetails(cloneDetails);
                productLinkToProduct.setLinkedProduct(linkedProduct.getLinkedProduct());
                productLinkToProduct.setProductSubObjectStatus(ProductSubObjectStatus.ACTIVE);
                productLinkToProduct.setObligatory(linkedProduct.getObligatory());
                productLinkToProduct.setAllowSalesUnder(linkedProduct.getAllowSalesUnder());
                productLinkToProducts.add(productLinkToProduct);
            }
        }
        return productLinkToProducts;
    }

    private List<ProductFile> cloneProductFile(List<ProductFile> productFiles, ProductDetails cloneDetails) {
        List<ProductFile> files = new ArrayList<>();
        for (ProductFile productFile : productFiles) {
            if (productFile.getStatus().equals(EntityStatus.ACTIVE)) {
                ProductFile file = new ProductFile();
                file.setFileType(productFile.getFileType());
                file.setLocalFileUrl(productFile.getLocalFileUrl());
                file.setName(productFile.getName());
                file.setStatus(productFile.getStatus());
                file.setFileStatuses(productFile.getFileStatuses());
                file.setProductDetailId(cloneDetails.getId());
                files.add(file);
            }
        }
        return files;
    }

    public List<ProductInterimAndAdvancePayments> createProductIapClone(List<ProductInterimAndAdvancePayments> interimAndAdvancePayments, ProductDetails cloneDetails) {
        List<ProductInterimAndAdvancePayments> advancePayments = new ArrayList<>();
        for (ProductInterimAndAdvancePayments interimAndAdvancePayment : interimAndAdvancePayments) {
            if (interimAndAdvancePayment.getProductSubObjectStatus().equals(ProductSubObjectStatus.ACTIVE)) {
                ProductInterimAndAdvancePayments iapClone = new ProductInterimAndAdvancePayments();
                iapClone.setProductSubObjectStatus(ProductSubObjectStatus.ACTIVE);
                iapClone.setProductDetails(cloneDetails);
                iapClone.setInterimAdvancePayment(interimAdvancePaymentService.cloneInterimAdvancePayment(interimAndAdvancePayment.getInterimAdvancePayment().getId()));
                advancePayments.add(iapClone);

            }
        }
        return advancePayments;
    }

    public List<ProductInterimAndAdvancePayments> createProductIapCloneForProductContract(List<ProductInterimAndAdvancePayments> interimAndAdvancePayments,
                                                                                          ProductDetails cloneDetails,
                                                                                          ProductParameterBaseRequest baseRequest) {
        Map<Long, ContractInterimAdvancePaymentsRequest> interimMap = baseRequest.getInterimAdvancePayments().stream().collect(Collectors.toMap(ContractInterimAdvancePaymentsRequest::getInterimAdvancePaymentId, j -> j));
        List<ProductInterimAndAdvancePayments> advancePayments = new ArrayList<>();
        for (ProductInterimAndAdvancePayments sourceInterim : interimAndAdvancePayments) {
            if (sourceInterim.getProductSubObjectStatus().equals(ProductSubObjectStatus.ACTIVE)) {
                InterimAdvancePayment sourceInterimAdvancePayment = sourceInterim.getInterimAdvancePayment();

                ProductInterimAndAdvancePayments iapClone = new ProductInterimAndAdvancePayments();
                iapClone.setProductSubObjectStatus(ProductSubObjectStatus.ACTIVE);
                iapClone.setProductDetails(cloneDetails);
                Long sourceInterimId = sourceInterimAdvancePayment.getId();
                InterimAdvancePayment interimAdvancePayment = interimAdvancePaymentService.cloneInterimAdvancePayment(sourceInterimId);

                if (interimMap.containsKey(sourceInterimId)) {
                    ContractInterimAdvancePaymentsRequest interim = interimMap.get(sourceInterimId);
                    interim.setInterimAdvancePaymentId(interimAdvancePayment.getId());
                    if (sourceInterimAdvancePayment.getPriceComponent() != null) {
                        Map<PriceComponentMathVariableName, PriceComponentFormulaVariable> clonedFormulaMap = interimAdvancePayment.getPriceComponent().getFormulaVariables().stream().filter(x -> !x.getVariable().equals(PriceComponentMathVariableName.PRICE_PROFILE)).collect(Collectors.toMap(PriceComponentFormulaVariable::getVariable, j -> j));
                        Map<Long, PriceComponentFormulaVariable> formulaMap = sourceInterimAdvancePayment.getPriceComponent().getFormulaVariables().stream().collect(Collectors.toMap(PriceComponentFormulaVariable::getId, j -> j));
                        List<PriceComponentContractFormula> contractFormulas = interim.getContractFormulas();
                        if (CollectionUtils.isNotEmpty(contractFormulas)) {
                            for (PriceComponentContractFormula contractFormula : contractFormulas) {
                                if (!formulaMap.containsKey(contractFormula.getFormulaVariableId())) {
                                    continue;
                                }
                                PriceComponentFormulaVariable priceComponentFormulaVariable = formulaMap.get(contractFormula.getFormulaVariableId());
                                PriceComponentFormulaVariable cloneVariable = clonedFormulaMap.get(priceComponentFormulaVariable.getVariable());
                                if (cloneVariable == null) {
                                    continue;
                                }
                                contractFormula.setFormulaVariableId(cloneVariable.getId());
                            }
                        }
                    }
                }


                iapClone.setInterimAdvancePayment(interimAdvancePayment);
                advancePayments.add(iapClone);
            }
        }
        return advancePayments;
    }

    private List<ProductPenalty> createProductPenaltyClones(List<ProductPenalty> penalties, ProductDetails cloneDetails) {
        List<ProductPenalty> productPenalties = new ArrayList<>();
        for (ProductPenalty penalty : penalties) {
            if (penalty.getProductSubObjectStatus().equals(ProductSubObjectStatus.ACTIVE)) {
                ProductPenalty productPenalty = new ProductPenalty();
                productPenalty.setProductDetails(cloneDetails);
                productPenalty.setProductSubObjectStatus(ProductSubObjectStatus.ACTIVE);
                productPenalty.setPenalty(penaltyService.clonePenalty(penalty.getPenalty().getId()));
                productPenalties.add(productPenalty);
            }
        }
        return productPenalties;
    }

    public List<ProductPriceComponents> createPriceComponentClones(List<ProductPriceComponents> priceComponents, ProductDetails cloneDetails) {
        List<ProductPriceComponents> productPriceComponents = new ArrayList<>();
        for (ProductPriceComponents priceComponent : priceComponents) {
            if (priceComponent.getProductSubObjectStatus().equals(ProductSubObjectStatus.ACTIVE)) {
                ProductPriceComponents clonedProductPc = new ProductPriceComponents();
                clonedProductPc.setProductDetails(cloneDetails);
                clonedProductPc.setProductSubObjectStatus(ProductSubObjectStatus.ACTIVE);
                clonedProductPc.setPriceComponent(priceComponentService.clonePriceComponent(priceComponent.getPriceComponent().getId()));
                productPriceComponents.add(clonedProductPc);
            }
        }
        return productPriceComponents;
    }

    private List<ProductTerminations> createProductTerminationClones(List<ProductTerminations> terminations, ProductDetails cloneDetails) {
        List<ProductTerminations> productTerminations = new ArrayList<>();
        for (ProductTerminations termination : terminations) {
            if (termination.getProductSubObjectStatus().equals(ProductSubObjectStatus.ACTIVE)) {
                ProductTerminations clonedProductTermination = new ProductTerminations();
                clonedProductTermination.setTermination(terminationsService.cloneTermination(termination.getTermination().getId()));
                clonedProductTermination.setProductSubObjectStatus(ProductSubObjectStatus.ACTIVE);
                clonedProductTermination.setProductDetails(cloneDetails);
                productTerminations.add(clonedProductTermination);
            }
        }
        return productTerminations;
    }

    public List<ProductGridOperator> createGridOperatorClones(List<ProductGridOperator> productGridOperators, ProductDetails clonedDetails) {
        List<ProductGridOperator> gridOperators = new ArrayList<>();
        for (ProductGridOperator gridOperator : productGridOperators) {
            if (gridOperator.getProductSubObjectStatus().equals(ProductSubObjectStatus.ACTIVE)) {
                ProductGridOperator productGridOperator = new ProductGridOperator();
                productGridOperator.setGridOperator(gridOperator.getGridOperator());
                productGridOperator.setProductDetails(clonedDetails);
                productGridOperator.setProductSubObjectStatus(ProductSubObjectStatus.ACTIVE);
                gridOperators.add(productGridOperator);
            }
        }
        return gridOperators;
    }

    private List<ProductPriceComponentGroups> createPriceComponentGroupClones(List<ProductPriceComponentGroups> priceComponentGroups, ProductDetails cloneDetails) {
        List<ProductPriceComponentGroups> clonedPriceComponentGroups = new ArrayList<>();
        for (ProductPriceComponentGroups priceComponentGroup : priceComponentGroups) {
            if (priceComponentGroup.getProductSubObjectStatus().equals(ProductSubObjectStatus.ACTIVE)) {
                ProductPriceComponentGroups productPriceComponentGroups = new ProductPriceComponentGroups();
                productPriceComponentGroups.setProductSubObjectStatus(ProductSubObjectStatus.ACTIVE);
                productPriceComponentGroups.setProductDetails(cloneDetails);
                productPriceComponentGroups.setPriceComponentGroup(priceComponentGroup.getPriceComponentGroup());
                clonedPriceComponentGroups.add(productPriceComponentGroups);
            }
        }
        return clonedPriceComponentGroups;
    }

    private List<ProductTerminationGroups> createTerminationGroupClones(List<ProductTerminationGroups> terminationGroups, ProductDetails cloneDetails) {
        List<ProductTerminationGroups> clonedTerminationGroups = new ArrayList<>();
        for (ProductTerminationGroups terminationGroup : terminationGroups) {
            if (terminationGroup.getProductSubObjectStatus().equals(ProductSubObjectStatus.ACTIVE)) {
                ProductTerminationGroups productTerminationGroups = new ProductTerminationGroups();
                productTerminationGroups.setTerminationGroup(terminationGroup.getTerminationGroup());
                productTerminationGroups.setProductDetails(cloneDetails);
                productTerminationGroups.setProductSubObjectStatus(ProductSubObjectStatus.ACTIVE);
                clonedTerminationGroups.add(productTerminationGroups);
            }
        }
        return clonedTerminationGroups;
    }

    private List<ProductPenaltyGroups> createProductPenaltyGroupClones(List<ProductPenaltyGroups> penaltyGroups, ProductDetails clonedDetails) {
        List<ProductPenaltyGroups> clonedPenaltyGroups = new ArrayList<>();
        for (ProductPenaltyGroups penaltyGroup : penaltyGroups) {
            if (penaltyGroup.getProductSubObjectStatus().equals(ProductSubObjectStatus.ACTIVE)) {
                ProductPenaltyGroups productPenaltyGroups = new ProductPenaltyGroups();
                productPenaltyGroups.setPenaltyGroup(penaltyGroup.getPenaltyGroup());
                productPenaltyGroups.setProductDetails(clonedDetails);
                productPenaltyGroups.setProductSubObjectStatus(ProductSubObjectStatus.ACTIVE);
                clonedPenaltyGroups.add(productPenaltyGroups);
            }
        }
        return clonedPenaltyGroups;
    }


    private List<ProductGroupOfInterimAndAdvancePayments> createProductIAPGroupClone(List<ProductGroupOfInterimAndAdvancePayments> interimAndAdvancePaymentGroups, ProductDetails cloneDetails) {
        List<ProductGroupOfInterimAndAdvancePayments> clonedIaps = new ArrayList<>();
        for (ProductGroupOfInterimAndAdvancePayments interimAndAdvancePaymentGroup : interimAndAdvancePaymentGroups) {
            if (interimAndAdvancePaymentGroup.getProductSubObjectStatus().equals(ProductSubObjectStatus.ACTIVE)) {
                ProductGroupOfInterimAndAdvancePayments productIapGroup = new ProductGroupOfInterimAndAdvancePayments();
                productIapGroup.setProductDetails(cloneDetails);
                productIapGroup.setProductSubObjectStatus(ProductSubObjectStatus.ACTIVE);
                productIapGroup.setInterimAdvancePaymentGroup(interimAndAdvancePaymentGroup.getInterimAdvancePaymentGroup());
                clonedIaps.add(productIapGroup);
            }
        }
        return clonedIaps;
    }

    public ProductSegments cloneSegment(ProductSegments x, ProductDetails clonedProductDetails) {
        ProductSegments productSegments = new ProductSegments();
        productSegments.setProductSubObjectStatus(x.getProductSubObjectStatus());
        productSegments.setSegment(x.getSegment());
        productSegments.setProductDetails(clonedProductDetails);
        return productSegments;
    }

    public ProductSalesArea cloneSalesArea(ProductSalesArea x, ProductDetails clonedProductDetails) {
        ProductSalesArea productSalesArea = new ProductSalesArea();
        productSalesArea.setSalesArea(x.getSalesArea());
        productSalesArea.setProductSubObjectStatus(x.getProductSubObjectStatus());
        productSalesArea.setProductDetails(clonedProductDetails);
        return productSalesArea;
    }

    public ProductSalesChannel cloneSaleChannel(ProductSalesChannel x, ProductDetails clonedProductDetails) {
        ProductSalesChannel productSalesChannel = new ProductSalesChannel();
        productSalesChannel.setProductSubObjectStatus(x.getProductSubObjectStatus());
        productSalesChannel.setSalesChannel(x.getSalesChannel());
        productSalesChannel.setProductDetails(clonedProductDetails);
        return productSalesChannel;
    }

    public List<ProductAdditionalParamsResponse> mapAdditionalParams(ProductDetails details) {
        List<ProductAdditionalParamsResponse> productAdditionalParams = new ArrayList<>();
        details.getProductAdditionalParams()
                .stream().filter(adPar -> adPar.getLabel() != null)
                .forEach(
                        it -> {
                            ProductAdditionalParamsResponse productAdditionalParam = new ProductAdditionalParamsResponse(
                                    it.getOrderingId(),
                                    it.getProductDetailId(),
                                    it.getLabel(),
                                    it.getValue()
                            );
                            productAdditionalParams.add(productAdditionalParam);
                        }
                );
        return productAdditionalParams;
    }
}
