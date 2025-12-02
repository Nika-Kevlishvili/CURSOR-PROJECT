package bg.energo.phoenix.service.contract.product;

import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.contract.product.*;
import bg.energo.phoenix.model.entity.nomenclature.product.Currency;
import bg.energo.phoenix.model.entity.nomenclature.product.terms.Calendar;
import bg.energo.phoenix.model.entity.product.iap.interimAdvancePayment.InterimAdvancePayment;
import bg.energo.phoenix.model.entity.product.price.priceComponent.PriceComponent;
import bg.energo.phoenix.model.entity.product.price.priceComponent.PriceComponentFormulaVariable;
import bg.energo.phoenix.model.entity.product.product.*;
import bg.energo.phoenix.model.entity.product.term.terms.InvoicePaymentTerms;
import bg.energo.phoenix.model.entity.product.term.terms.Terms;
import bg.energo.phoenix.model.enums.contract.products.ContractDetailsStatus;
import bg.energo.phoenix.model.enums.contract.products.ContractSubObjectStatus;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.product.iap.interimAdvancePayment.*;
import bg.energo.phoenix.model.enums.product.product.*;
import bg.energo.phoenix.model.enums.product.term.terms.*;
import bg.energo.phoenix.model.request.contract.express.ExpressContractProductParametersRequest;
import bg.energo.phoenix.model.request.contract.express.ExpressContractRequest;
import bg.energo.phoenix.model.request.contract.product.*;
import bg.energo.phoenix.model.request.product.price.priceComponent.PriceComponentProjectionForIap;
import bg.energo.phoenix.model.response.contract.priceComponent.PriceComponentForContractResponse;
import bg.energo.phoenix.model.response.contract.productContract.*;
import bg.energo.phoenix.model.response.contract.productContract.priceComponentFormula.*;
import bg.energo.phoenix.model.response.interimAdvancePayment.InterimAdvancePaymentResponse;
import bg.energo.phoenix.model.response.interimAdvancePayment.InterimAdvancePaymentTermsResponse;
import bg.energo.phoenix.model.response.nomenclature.product.currency.CurrencyResponse;
import bg.energo.phoenix.model.response.product.ProductContractTermsResponse;
import bg.energo.phoenix.model.response.terms.InvoicePaymentTermsResponse;
import bg.energo.phoenix.repository.contract.product.*;
import bg.energo.phoenix.repository.nomenclature.product.CurrencyRepository;
import bg.energo.phoenix.repository.nomenclature.product.terms.CalendarRepository;
import bg.energo.phoenix.repository.product.iap.interimAdvancePayment.InterimAdvancePaymentRepository;
import bg.energo.phoenix.repository.product.price.priceComponent.PriceComponentFormulaVariableRepository;
import bg.energo.phoenix.repository.product.price.priceComponent.PriceComponentRepository;
import bg.energo.phoenix.repository.product.product.ProductAdditionalParamsRepository;
import bg.energo.phoenix.repository.product.product.ProductContractTermRepository;
import bg.energo.phoenix.repository.product.product.ProductDetailsRepository;
import bg.energo.phoenix.repository.product.product.ProductPriceComponentRepository;
import bg.energo.phoenix.repository.product.term.terms.InvoicePaymentTermsRepository;
import bg.energo.phoenix.repository.product.term.terms.TermsRepository;
import bg.energo.phoenix.service.product.iap.interimAdvancePayment.InterimAdvancePaymentTermsService;
import bg.energo.phoenix.util.epb.EPBChainedExceptionTriggerUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductContractProductParametersService {

    @Value("${contract.without_term.value}")
    private String maxDate;

    private final ProductContractTermRepository productContractTermRepository;
    private final InvoicePaymentTermsRepository invoicePaymentTermsRepository;
    private final InterimAdvancePaymentTermsService interimAdvancePaymentTermsService;
    private final InterimAdvancePaymentRepository advancePaymentRepository;

    private final PriceComponentFormulaVariableRepository priceComponentFormulaVariableRepository;
    private final ProductDetailsRepository productDetailsRepository;
    private final ProductContractRepository productContractRepository;
    private final ProductContractValidatorService validatorService;
    private final ProductContractPriceComponentRepository productContractPriceComponentRepository;
    private final ProductContractInterimAdvancePaymentsRepository productContractInterimAdvancePaymentsRepository;
    private final CurrencyRepository currencyRepository;
    private final CalendarRepository calendarRepository;
    private final ProductContractInterimPriceFormulaRepository interimPriceFormulaRepository;
    private final TermsRepository termsRepository;
    private final PriceComponentRepository priceComponentRepository;
    private final ProductAdditionalParamsRepository productAdditionalParamsRepository;
    private final ProductContractAdditionalParamsRepository productContractAdditionalParamsRepository;

    private final ProductPriceComponentRepository productPriceComponentRepository;
    private final ProductContractDateService contractDateService;

    @Transactional(propagation = Propagation.MANDATORY)
    public boolean create(ProductContractCreateRequest request, ProductContractDetails productContractDetails, ProductContract productContract, List<String> errorMessages) {
        ProductContractProductParametersCreateRequest productParameters = request.getProductParameters();
        ProductContractBasicParametersCreateRequest basicParameters = request.getBasicParameters();
        //validation
        ProductContractThirdPageFields sourceView = thirdTabFields(productContractDetails.getProductDetailId());

        validateAndSet(productContractDetails, productContract, productParameters, basicParameters, sourceView);
        contractDateService.validateDates(productContract,
                productContractDetails,
                basicParameters,
                productParameters,
                sourceView,
                errorMessages);
        contractDateService.validateSourceView(sourceView,
                basicParameters,
                productParameters,
                errorMessages);
        LocalDate contractTermEndDate = contractDateService.validateContractTerm(
                productContract,
                productContractDetails,
                sourceView,
                basicParameters,
                productParameters,
                errorMessages
        );
        if (!basicParameters.getStatus().equals(ContractDetailsStatus.DRAFT)) {
            productContract.setContractTermEndDate(contractTermEndDate);
        }
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
        validateDates(productContract, request.getBasicParameters().getEntryInForceDate(),errorMessages);
        fillSupplyActivation(request.getBasicParameters(),request.getProductParameters(),productContract,productContractDetails,errorMessages);
        return true;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public boolean update(ProductContractUpdateRequest request, ProductContractDetails productContractDetails, ProductContract productContract, List<String> errorMessages) {
        ProductContractProductParametersCreateRequest productParameters = request.getProductParameters();
        ProductContractBasicParametersUpdateRequest basicParameters = request.getBasicParameters();
        //validation
        ProductContractThirdPageFields sourceView = thirdTabFields(productContractDetails.getProductDetailId());
        contractDateService.validateDates(productContract,
                productContractDetails,
                basicParameters,
                productParameters,
                sourceView,
                errorMessages);
        contractDateService.validateSourceView(sourceView,
                basicParameters,
                productParameters,
                errorMessages);
        LocalDate termEndDate = contractDateService.validateContractTerm(
                productContract,
                productContractDetails,
                sourceView,
                basicParameters,
                productParameters,
                errorMessages
        );
        contractDateService.setContractTermEndDate(productContract, basicParameters, termEndDate);
        contractDateService.validatePerpetuity(
                productContract,
                sourceView,
                basicParameters,
                productParameters,
                errorMessages
        );
        validateAndSet(productContractDetails, productContract, productParameters, basicParameters, sourceView);
        validateDates(productContract,request.getBasicParameters().getEntryInForceDate(), errorMessages);
        fillSupplyActivation(request.getBasicParameters(),request.getProductParameters(),productContract,productContractDetails,errorMessages);
        return true;
    }


    @Transactional(propagation = Propagation.MANDATORY)
    public void updateSubObjects(ProductContractUpdateRequest request, ProductContractDetails productContractDetails, List<String> errorMessages) {
        ProductContractProductParametersCreateRequest productParameters = request.getProductParameters();
        updateFormulaVariables(productContractDetails, productParameters);
        updateInterimAdvancePayments(productContractDetails, productParameters);
    }



    private void updateInterimAdvancePayments(ProductContractDetails productContractDetails, ProductContractProductParametersCreateRequest productParameters) {
        List<ContractInterimAdvancePaymentsRequest> interimAdvancePayments = productParameters.getInterimAdvancePayments();
        List<ProductContractInterimAdvancePayments> interims = productContractInterimAdvancePaymentsRepository.findAllByContractDetailIdAndStatusIn(productContractDetails.getId(), List.of(ContractSubObjectStatus.ACTIVE));
        List<ProductContractInterimAdvancePayments> updatedContractInterims = new ArrayList<>();

        Map<Long, ProductContractInterimAdvancePayments> interimsMap = interims.stream().collect(Collectors.toMap(ProductContractInterimAdvancePayments::getInterimAdvancePaymentId, j -> j));
        List<ProductContractInterimPriceFormula> formulasToSave = new ArrayList<>();
        for (ContractInterimAdvancePaymentsRequest interimAdvancePayment : interimAdvancePayments) {
            ProductContractInterimAdvancePayments iap = interimsMap.remove(interimAdvancePayment.getInterimAdvancePaymentId());
            if (iap == null) {
                ProductContractInterimAdvancePayments contractIap = createContractIap(productContractDetails, interimAdvancePayment);
                //save explicitly
                productContractInterimAdvancePaymentsRepository.save(contractIap);
                createIapFormula(formulasToSave, interimAdvancePayment, contractIap);
            } else {
                iap.setValue(interimAdvancePayment.getValue());
                iap.setIssueDate(interimAdvancePayment.getIssueDate());
                iap.setTermValue(interimAdvancePayment.getTermValue());
                updateIapFormula(formulasToSave, interimAdvancePayment, iap);
                updatedContractInterims.add(iap);
            }

        }
        Collection<ProductContractInterimAdvancePayments> values = interimsMap.values();
        List<Long> contractInterimIds = new ArrayList<>();
        for (ProductContractInterimAdvancePayments value : values) {
            value.setStatus(ContractSubObjectStatus.DELETED);
            contractInterimIds.add(value.getId());
            updatedContractInterims.add(value);
        }
        deleteIapFormulas(contractInterimIds, formulasToSave);
        interimPriceFormulaRepository.saveAll(formulasToSave);
        productContractInterimAdvancePaymentsRepository.saveAll(updatedContractInterims);
    }

    private void updateIapFormula(List<ProductContractInterimPriceFormula> formulasToSave, ContractInterimAdvancePaymentsRequest request, ProductContractInterimAdvancePayments contractIaps) {
        List<ProductContractInterimPriceFormula> contractFormulas = interimPriceFormulaRepository.findByContractIapIdAndStatus(contractIaps.getId(), List.of(EntityStatus.ACTIVE));
        Map<Long, ProductContractInterimPriceFormula> formulaMap = contractFormulas.stream().collect(Collectors.toMap(ProductContractInterimPriceFormula::getFormulaId, j -> j));
        List<PriceComponentContractFormula> formulaRequests = request.getContractFormulas();
        for (PriceComponentContractFormula formulaRequest : formulaRequests) {
            ProductContractInterimPriceFormula formula = formulaMap.remove(formulaRequest.getFormulaVariableId());
            if (formula == null) {
                formulasToSave.add(new ProductContractInterimPriceFormula(formulaRequest.getValue(), formulaRequest.getFormulaVariableId(), contractIaps.getId()));
            } else {
                formula.setValue(formulaRequest.getValue());
                formulasToSave.add(formula);
            }
        }
        Collection<ProductContractInterimPriceFormula> values = formulaMap.values();
        for (ProductContractInterimPriceFormula value : values) {
            value.setStatus(EntityStatus.DELETED);
            formulasToSave.add(value);
        }

    }

    public void deleteIapFormulas(List<Long> productContractIapId, List<ProductContractInterimPriceFormula> formulasToSave) {
        List<ProductContractInterimPriceFormula> allById = interimPriceFormulaRepository.findAllByContractInterimAdvancePaymentIdInAndStatusIn(productContractIapId, List.of(EntityStatus.ACTIVE));
        for (ProductContractInterimPriceFormula formula : allById) {
            formula.setStatus(EntityStatus.DELETED);
            formulasToSave.add(formula);
        }

    }

    private void updateFormulaVariables(ProductContractDetails productContractDetails, ProductContractProductParametersCreateRequest productParameters) {
        List<PriceComponentContractFormula> contractFormulas = productParameters.getContractFormulas();
        List<ProductContractPriceComponents> contractPriceComponents = productContractPriceComponentRepository.findByContractDetailIdAndStatusIn(productContractDetails.getId(), List.of(ContractSubObjectStatus.ACTIVE));

        List<ProductContractPriceComponents> updatedPriceComponents = new ArrayList<>();
        Map<Long, ProductContractPriceComponents> priceComponentsMap = contractPriceComponents.stream().collect(Collectors.toMap(ProductContractPriceComponents::getPriceComponentFormulaVariableId, j -> j));
        for (PriceComponentContractFormula contractFormula : contractFormulas) {
            ProductContractPriceComponents priceComponents = priceComponentsMap.remove(contractFormula.getFormulaVariableId());
            if (priceComponents == null) {
                ProductContractPriceComponents newPriceComponents = createContractPriceComponent(productContractDetails, contractFormula);
                updatedPriceComponents.add(newPriceComponents);
            } else {
                priceComponents.setValue(contractFormula.getValue());
                updatedPriceComponents.add(priceComponents);
            }
        }
        Collection<ProductContractPriceComponents> deletedValues = priceComponentsMap.values();
        for (ProductContractPriceComponents value : deletedValues) {
            value.setStatus(ContractSubObjectStatus.DELETED);
            updatedPriceComponents.add(value);
        }
        productContractPriceComponentRepository.saveAll(updatedPriceComponents);

    }

    private void validateAndSet(ProductContractDetails productContractDetails,
                                ProductContract productContract,
                                ProductContractProductParametersCreateRequest productParameters,
                                ProductContractBasicParametersCreateRequest basicParameters
            , ProductContractThirdPageFields sourceView) {
        List<String> messages = new ArrayList<>();
        validatorService.validateBaseCreateRequest(productParameters, sourceView, messages);
        validatorService.validateEqualMonthlyInstallment(sourceView, messages, productParameters);
        validateContractStatus(productContract, sourceView, basicParameters, productParameters, messages);
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(messages, log);

        //map and save
        fillContractDetails(productContractDetails, productParameters);
        fillOptionalFields(productContractDetails, productParameters);
    }

    private void validateContractStatus(ProductContract productContract, ProductContractThirdPageFields sourceView, ProductContractBasicParametersCreateRequest basicParameters, ProductContractProductParametersCreateRequest productParameters, List<String> errorMessages) {
        if (!productContract.getContractStatus().equals(ContractDetailsStatus.ACTIVE_IN_PERPETUITY) && basicParameters.getStatus().equals(ContractDetailsStatus.ACTIVE_IN_PERPETUITY)) {
            Map<Long, ProductContractTermsResponse> collect = sourceView.getProductContractTerms().stream().collect(Collectors.toMap(ProductContractTermsResponse::getId, j -> j));
            ProductContractTermsResponse productContractTermsResponse = collect.get(productParameters.getProductContractTermId());
            if (productContractTermsResponse == null) {
                return;
            }
            if (!productContractTermsResponse.isPerpetuityCause()) {
                errorMessages.add("basicParameters.status-status can not be changed to active in perpetuity because term is not perpetuity clause!;");
            }
        }
    }

    private boolean hasProductAdditionalParamUnfilledValues(List<ProductContractAdditionalParamsResponse> productAdditionalParamsFromProduct) {
        return productAdditionalParamsFromProduct.stream()
                .anyMatch(param -> StringUtils.isEmpty(param.value()) && StringUtils.isNotEmpty(param.label()));
    }

    public void createProductAdditionalParams(ProductContractProductParametersCreateRequest productParameters, ProductContractDetails productContractDetails, List<String> messages) {
        List<ProductContractAdditionalParamsResponse> productAdditionalParamsFromProduct = productAdditionalParamsRepository.findProductFilledAdditionalParamsByProductDetailId(productContractDetails.getProductDetailId());
        List<ContractProductAdditionalParamsRequest> productAdditionalParamsFromReq = productParameters.getProductAdditionalParams();

        if (CollectionUtils.isEmpty(productAdditionalParamsFromReq)) {
            if (hasProductAdditionalParamUnfilledValues(productAdditionalParamsFromProduct)) {
                messages.add("Unfilled product additional params must be filled from contract");
                return;
            }
            for (ProductContractAdditionalParamsResponse productParam : productAdditionalParamsFromProduct) {
                ProductContractAdditionalParams productContractParam = new ProductContractAdditionalParams(
                        null,
                        productParam.label(),
                        productParam.value(),
                        productParam.id(),
                        productContractDetails.getId()
                );
                productContractAdditionalParamsRepository.save(productContractParam);
            }

        } else {
            for (ProductContractAdditionalParamsResponse productParam : productAdditionalParamsFromProduct) {
                Optional<ContractProductAdditionalParamsRequest> conValue = productAdditionalParamsFromReq
                        .stream()
                        .filter(req -> req.getId().equals(productParam.id()))
                        .findFirst();

                if (conValue.isEmpty()) {
                    messages.add("No value present in contract additional params");
                    return;
                }
                String value = conValue.get().getValue();
                ProductContractAdditionalParams productContractParam = new ProductContractAdditionalParams(
                        null,
                        productParam.label(),
                        value,
                        productParam.id(),
                        productContractDetails.getId()
                );
                productContractAdditionalParamsRepository.save(productContractParam);
            }
        }
    }

    public void fillProductAdditionalParams(ProductContractProductParametersCreateRequest productParameters, ProductContractDetails productContractDetails, List<String> messages) {
        List<ProductContractAdditionalParamsResponse> productAdditionalParamsResponse = productAdditionalParamsRepository
                .findProductFilledAdditionalParamsByProductDetailId(productContractDetails.getProductDetailId());

        List<ProductContractAdditionalParams> paramsFromContract = productContractAdditionalParamsRepository
                .findAllByContractDetailId(productContractDetails.getId());
        findRedundantAdditionalParamsInContract(productAdditionalParamsResponse, paramsFromContract);

        List<ContractProductAdditionalParamsRequest> productAdditionalParams = productParameters.getProductAdditionalParams();

        int index = 0;
        for (ContractProductAdditionalParamsRequest paramsRequest : productAdditionalParams) {
            ProductContractAdditionalParamsResponse productContractAdditionalParamsResponse = productAdditionalParamsResponse.stream().filter(p -> p.id().equals(paramsRequest.getId())).findFirst().orElse(null);
            if (productContractAdditionalParamsResponse == null) {
                messages.add("productParameters.productAdditionalParams[%s]-additional params with id %s not found".formatted(index, paramsRequest.getId()));
            } else {
                Optional<ProductContractAdditionalParams> paramOptional = productContractAdditionalParamsRepository.findByContractDetailIdAndProductAdditionalParamId(productContractDetails.getId(), paramsRequest.getId());
                if (paramOptional.isEmpty()) {
                    ProductContractAdditionalParams additionalParams = getContractProductAdditionalParams(productContractDetails, paramsRequest, productContractAdditionalParamsResponse);
                    productContractAdditionalParamsRepository.save(additionalParams);
                } else {
                    ProductContractAdditionalParams additionalParams = paramOptional.get();
                    additionalParams.setValue(paramsRequest.getValue());
                    productContractAdditionalParamsRepository.save(additionalParams);
                }
            }
            index++;
        }
    }

    private void findRedundantAdditionalParamsInContract(List<ProductContractAdditionalParamsResponse> paramsFromProduct,
                                                         List<ProductContractAdditionalParams> paramsFromContract) {

        if (CollectionUtils.isEmpty(paramsFromContract)) {
            return;
        }
        List<ProductContractAdditionalParams> redundantParamsInContract = new ArrayList<>();

        List<Long> productAdditionalParamsIdsFromProduct = paramsFromProduct
                .stream()
                .map(ProductContractAdditionalParamsResponse::id)
                .toList();
        for (ProductContractAdditionalParams paramFromContract : paramsFromContract) {
            if (!productAdditionalParamsIdsFromProduct.contains(paramFromContract.getProductAdditionalParamId())) {
                redundantParamsInContract.add(paramFromContract);
            }
        }
        if (CollectionUtils.isNotEmpty(redundantParamsInContract)) {
            productContractAdditionalParamsRepository.deleteAll(redundantParamsInContract);
        }
    }

    private ProductContractAdditionalParams getContractProductAdditionalParams(ProductContractDetails productContractDetails, ContractProductAdditionalParamsRequest paramsRequest, ProductContractAdditionalParamsResponse productContractAdditionalParamsResponse) {
        ProductContractAdditionalParams additionalParams = new ProductContractAdditionalParams();
        additionalParams.setContractDetailId(productContractDetails.getId());
        additionalParams.setProductAdditionalParamId(paramsRequest.getId());
        additionalParams.setValue(paramsRequest.getValue());
        additionalParams.setLabel(productContractAdditionalParamsResponse.label());
        return additionalParams;
    }

    private void validateDates(ProductContract productContract,LocalDate entryIntoForceDate, List<String> messages) {
        validateEntryIntoForceDate(productContract, messages);
        validateContractTermEndDate(productContract, messages);
        validateContractTermStartDate(productContract,entryIntoForceDate, messages);
    }

    private void validateContractTermStartDate(ProductContract productContract,LocalDate entryIntoForceDate, List<String> messages) {
        LocalDate signingDate = productContract.getSigningDate();
        LocalDate startOfInitialTerm = productContract.getInitialTermDate();
        LocalDate terminationDate = productContract.getTerminationDate();
        LocalDate perpetuityDate = productContract.getPerpetuityDate();
        if (startOfInitialTerm != null) {
            if (signingDate != null && startOfInitialTerm.isBefore(signingDate)) {
                messages.add("basicParameters.singingDate- Signing date should be after contract term start date!;");
            }

            if (terminationDate != null && startOfInitialTerm.isAfter(terminationDate)) {
                messages.add("basicParameters.terminationDate- terminationDate should be before contract term start date!;");
            }
            if (perpetuityDate != null && !startOfInitialTerm.isBefore(perpetuityDate)) {
                messages.add("basicParameters.singingDate- perpetuityDate should be before contract term start date!;");
            }
            if(entryIntoForceDate != null){
                if(!entryIntoForceDate.isEqual(startOfInitialTerm)){
                    if(!entryIntoForceDate.isBefore(startOfInitialTerm)){
                        messages.add("basicParameters.startOfInitialTerm- startOfInitialTerm should be after startOfInitialTerm!;");
                    }
                }
            }
        }

    }

    private void validateContractTermEndDate(ProductContract productContract, List<String> messages) {
        LocalDate signingDate = productContract.getSigningDate();
        LocalDate startOfInitialTerm = productContract.getInitialTermDate();
        LocalDate contractTermEndDate = productContract.getContractTermEndDate();
        LocalDate perpetuityDate = productContract.getPerpetuityDate();
        LocalDate activationDate = productContract.getActivationDate();
        if (contractTermEndDate != null) {
            if (signingDate != null && contractTermEndDate.isBefore(signingDate)) {
                messages.add("basicParameters.signingDate- Signing date should be less than contract term end date!;");
            }
//            if (startOfInitialTerm != null && !contractTermEndDate.isAfter(startOfInitialTerm)) {
//                messages.add("basicParameters.startOfInitialTerm-contract term start date should be less than contract term end date!;");
//            }

            if (perpetuityDate != null && contractTermEndDate.isBefore(perpetuityDate)) {
                messages.add("basicParameters.perpetuityDate-Perpetuity date should be before or equal to contract term end date;");
            }
            if (activationDate != null && contractTermEndDate.isBefore(activationDate)) {
                messages.add("basicParameters.contractTermEndDate-Contract term end date should be more or equal to activation date ;");
            }
        }
    }

    private void validateEntryIntoForceDate(ProductContract productContract, List<String> messages) {
        LocalDate signingDate = productContract.getSigningDate();
        LocalDate entryIntoForceDate = productContract.getEntryIntoForceDate();
        LocalDate contractTermEndDate = productContract.getContractTermEndDate();
        LocalDate perpetuityDate = productContract.getPerpetuityDate();
        LocalDate terminationDate = productContract.getTerminationDate();
        LocalDate activationDate = productContract.getActivationDate();
        if (entryIntoForceDate != null) {
            if (signingDate != null && entryIntoForceDate.isBefore(signingDate)) {
                messages.add("basicParameters.entryInForceDate- Signing date can not be after Entry into force date!;");
            }
            if (contractTermEndDate != null && entryIntoForceDate.isAfter(contractTermEndDate)) {
                messages.add("basicParameters.entryInForceDate-entry into force date should be less or equal to contract term end date!;");
            }
            if (perpetuityDate != null && entryIntoForceDate.isAfter(perpetuityDate)) {
                messages.add("basicParameters.entryInForceDate-perpetuityDate date should be before or equal to contract term end date!;");
            }
            if (terminationDate != null && entryIntoForceDate.isAfter(terminationDate)) {
                messages.add("basicParameters.entryInForceDate-terminationDate date should be before or equal to contract term end date!;");
            }

        }

    }



    private void fillContractTermEndDate(ProductParameterBaseRequest productParameters, ProductContractThirdPageFields sourceView, ProductContract productContract, ProductContractDetails productContractDetails, List<String> messages) {
        Map<Long, ProductContractTermsResponse> collect = sourceView.getProductContractTerms().stream().collect(Collectors.toMap(ProductContractTermsResponse::getId, j -> j));
        ProductContractTermsResponse termsResponse = collect.get(productParameters.getProductContractTermId());
        if (termsResponse.getTypeOfTerms().equals(ProductTermPeriodType.PERIOD) && productContract.getContractTermEndDate() == null) {
            if (productContract.getInitialTermDate() != null) {
                LocalDate termEndDate = productContract.getInitialTermDate().plus(termsResponse.getValue(), termsResponse.getPeriodType().getUnit()).minusDays(1);
                LocalDate maximumDate = LocalDate.parse(maxDate);
                productContract.setContractTermEndDate(termEndDate.isBefore(maximumDate) ? termEndDate : maximumDate);
            }
        } else if (termsResponse.getTypeOfTerms().equals(ProductTermPeriodType.CERTAIN_DATE)) {
            productContract.setContractTermEndDate(productParameters.getContractTermEndDate());
            productContractDetails.setContractTermEndDate(productParameters.getContractTermEndDate());
        } else if (termsResponse.getTypeOfTerms().equals(ProductTermPeriodType.WITHOUT_TERM)) {
            productContract.setContractTermEndDate(LocalDate.parse(maxDate));
        }
    }

    private void fillSupplyActivation(ProductContractBasicParametersCreateRequest basicParameters, ProductContractProductParametersCreateRequest productParameters, ProductContract contract, ProductContractDetails productContractDetails, List<String> messages) {
        if (productParameters.getSupplyActivation().equals(SupplyActivation.FIRST_DAY_OF_MONTH) && contract.getSupplyActivationDate() == null) {
            LocalDate today = LocalDate.now();
            contract.setSupplyActivationDate(today
                    .plusMonths(1)
                    .withDayOfMonth(1));
        } else if (productParameters.getSupplyActivation().equals(SupplyActivation.EXACT_DATE)) {
            if (contract.getSupplyActivationDate() == null) {
                contract.setSupplyActivationDate(productParameters.getSupplyActivationValue());
            }
            productContractDetails.setSupplyActivationDate(productParameters.getSupplyActivationValue());
        }
    }

    public void fillEntryIntoForce(ProductContractBasicParametersCreateRequest basicParameters, ProductContractProductParametersCreateRequest productParameters, ProductContract contract, ProductContractDetails productContractDetails, List<String> messages) {
        ContractEntryIntoForce entryIntoForce = productParameters.getEntryIntoForce();
        LocalDate entryInForceDateRequest = basicParameters.getEntryInForceDate();
        if (!Objects.equals(entryInForceDateRequest, contract.getEntryIntoForceDate())) {
            contract.setEntryIntoForceDate(entryInForceDateRequest);
        } else if (entryIntoForce.equals(ContractEntryIntoForce.SIGNING) && contract.getEntryIntoForceDate() == null) {
            contract.setEntryIntoForceDate(basicParameters.getSigningDate());
        } else if ((entryIntoForce.equals(ContractEntryIntoForce.FIRST_DELIVERY) || entryIntoForce.equals(ContractEntryIntoForce.DATE_CHANGE_OF_CBG)) && contract.getEntryIntoForceDate() == null) {
            contract.setEntryIntoForceDate(contract.getActivationDate());
        } else if (entryIntoForce.equals(ContractEntryIntoForce.EXACT_DAY)) {
            if (contract.getEntryIntoForceDate() == null && !basicParameters.getStatus().equals(ContractDetailsStatus.DRAFT)) {
                contract.setEntryIntoForceDate(productParameters.getEntryIntoForceValue());
            }
            productContractDetails.setEntryIntoForceDate(productParameters.getEntryIntoForceValue());
        }
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public boolean createForExpress(ExpressContractRequest request, ProductContractDetails productContractDetails, ProductContract productContract, List<String> errorMessages) {
        ExpressContractProductParametersRequest productParameters = request.getProductParameters();
        //validation
        ProductContractThirdPageFields sourceView = thirdTabFields(productContractDetails.getProductDetailId());
        List<String> messages = new ArrayList<>();
        validateIssueDate(sourceView, productParameters, messages);
        validatorService.validateBaseCreateRequest(productParameters, sourceView, messages);
        validatorService.validateEqualMonthlyInstallment(sourceView, messages, productParameters);
        if (productParameters.getSupplyActivation().equals(SupplyActivation.FIRST_DAY_OF_MONTH)) {
            List<WaitForOldContractTermToExpire> waitForOld = sourceView.getWaitForOldContractTermToExpires();
            if (waitForOld.contains(WaitForOldContractTermToExpire.YES) && waitForOld.contains(WaitForOldContractTermToExpire.NO)) {
                messages.add("productParameters.supplyActivation-Supply activation can not be first day of month because wait for old contract to expires is both no and yes!;");
            }
        }
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(messages, log);
        fillStartOfInitialTermForExpress(productParameters, productContract, productContractDetails, errorMessages);
        fillSupplyActivationForExpress(productParameters, productContract, productContractDetails, errorMessages);
        fillEntryIntoForceForExpress(productParameters, productContract, productContractDetails, errorMessages);

        fillContractTermEndDate(productParameters, sourceView, productContract, productContractDetails, messages);

        //map and save
        fillContractDetails(productContractDetails, productParameters);
        return true;
    }

    private void validateIssueDate(ProductContractThirdPageFields sourceView, ExpressContractProductParametersRequest productParameters, List<String> messages) {
        Map<Long, InterimAdvancePaymentResponse> collected = sourceView.getInterimAdvancePayments()
                .stream()
                .collect(Collectors.toMap(InterimAdvancePaymentResponse::getId, j -> j));
        List<ContractInterimAdvancePaymentsRequest> interimAdvancePayments = productParameters.getInterimAdvancePayments();
        int index = 0;
        for (ContractInterimAdvancePaymentsRequest interimAdvancePayment : interimAdvancePayments) {
            InterimAdvancePaymentResponse paymentResponse = collected.get(interimAdvancePayment.getInterimAdvancePaymentId());
            if (paymentResponse.getDateOfIssueType() == DateOfIssueType.WORKING_DAYS_AFTER_INVOICE_DATE &&
                (paymentResponse.getDateOfIssueValueTo() != null || paymentResponse.getDateOfIssueValueFrom() != null
                 || paymentResponse.getDateOfIssueValue() == null)) {
                messages.add("productParameters.InterimAdvancePayments[%s]-product with interim and advance payment by id [%s] not available".formatted(index, paymentResponse.getId()));
            }
            index++;
        }
    }

    private void fillEntryIntoForceForExpress(ExpressContractProductParametersRequest productParameters, ProductContract productContract, ProductContractDetails productContractDetails, List<String> errorMessages) {
        ContractEntryIntoForce entryIntoForce = productParameters.getEntryIntoForce();
        productContractDetails.setEntryIntoForce(entryIntoForce);
        if (entryIntoForce.equals(ContractEntryIntoForce.EXACT_DAY)) {
            productContractDetails.setEntryIntoForceDate(productParameters.getEntryIntoForceValue());
            productContract.setEntryIntoForceDate(productParameters.getEntryIntoForceValue());
            if (productParameters.getEntryIntoForceValue() == null) {
                errorMessages.add("productParameters.entryIntoForceValue-Entry into force date should be provided!;");
            } else if (!productParameters.getEntryIntoForceValue().isAfter(LocalDate.now())) {
                errorMessages.add("productParameters.entryIntoForceValue-Entry into force date should be in future!;");
            }
        }

    }

    private void fillSupplyActivationForExpress(ExpressContractProductParametersRequest productParameters, ProductContract productContract, ProductContractDetails details, List<String> errorMessages) {
        if (productParameters.getSupplyActivation().equals(SupplyActivation.FIRST_DAY_OF_MONTH) && productContract.getSupplyActivationDate() == null) {
            LocalDate today = LocalDate.now();
            productContract.setSupplyActivationDate(today
                    .plusMonths(1)
                    .withDayOfMonth(1));
        } else if (productParameters.getSupplyActivation().equals(SupplyActivation.EXACT_DATE)) {
            productContract.setSupplyActivationDate(productParameters.getSupplyActivationValue());
            details.setSupplyActivationDate(productParameters.getSupplyActivationValue());
            if (productParameters.getSupplyActivationValue() == null) {
                errorMessages.add("productParameters.supplyActivationValue-Supply activation date should be provided!;");
            }
        }
        details.setSupplyActivationAfterContractResigning(productParameters.getSupplyActivation());
    }

    private void fillStartOfInitialTermForExpress(ExpressContractProductParametersRequest productParameters, ProductContract productContract, ProductContractDetails details, List<String> errorMessages) {
        StartOfContractInitialTerm term = productParameters.getStartOfContractInitialTerm();
        details.setStartInitialTerm(term);
        if (term.equals(StartOfContractInitialTerm.EXACT_DATE)) {
            productContract.setInitialTermDate(productParameters.getStartOfContractValue());
            details.setInitialTermDate(productParameters.getStartOfContractValue());
            if (productParameters.getStartOfContractValue() == null) {
                errorMessages.add("productParameters.startOfContractValue-Start of contract term  date should be provided!;");
            }
        }
    }


    private void fillOptionalFields(ProductContractDetails details, ProductContractProductParametersCreateRequest request) {


        details.setMarginalPrice(request.getMarginalPrice());
        details.setMarginalPriceValidity(request.getMarginalPriceValidity());
        details.setAvgHourlyLoadProfiles(request.getHourlyLoadProfile());
        details.setProcurementPrice(request.getProcurementPrice());
        details.setCostPriceIncreaseFromImbalances(request.getImbalancePriceIncrease());
        details.setSetMargin(request.getSetMargin());
    }

    @Transactional
    public void fillSubActivityDetails(ProductParameterBaseRequest productParameters, ProductContractDetails productContractDetails, List<String> errorMessages) {
        saveFormulaVariables(productContractDetails, productParameters);

        saveInterimAdvancePayments(productContractDetails, productParameters);
    }

    private void saveInterimAdvancePayments(ProductContractDetails productContractDetails, ProductParameterBaseRequest productParameters) {
        List<ContractInterimAdvancePaymentsRequest> interimAdvancePayments = productParameters.getInterimAdvancePayments();

        List<ProductContractInterimPriceFormula> formulasToSave = new ArrayList<>();
        for (ContractInterimAdvancePaymentsRequest interimAdvancePayment : interimAdvancePayments) {
            ProductContractInterimAdvancePayments advancePayments = productContractInterimAdvancePaymentsRepository.save(createContractIap(productContractDetails, interimAdvancePayment));
            createIapFormula(formulasToSave, interimAdvancePayment, advancePayments);
        }
        interimPriceFormulaRepository.saveAll(formulasToSave);
    }

    private static void createIapFormula(List<ProductContractInterimPriceFormula> formulasToSave, ContractInterimAdvancePaymentsRequest interimAdvancePayment, ProductContractInterimAdvancePayments advancePayments) {
        List<PriceComponentContractFormula> contractFormulas = interimAdvancePayment.getContractFormulas();
        if (CollectionUtils.isNotEmpty(contractFormulas)) {
            for (PriceComponentContractFormula contractFormula : contractFormulas) {
                formulasToSave.add(new ProductContractInterimPriceFormula(contractFormula.getValue(), contractFormula.getFormulaVariableId(), advancePayments.getId()));
            }
        }
    }

    private static ProductContractInterimAdvancePayments createContractIap(ProductContractDetails productContractDetails, ContractInterimAdvancePaymentsRequest interimAdvancePayment) {
        ProductContractInterimAdvancePayments productContractInterimAdvancePayments = new ProductContractInterimAdvancePayments();
        productContractInterimAdvancePayments.setValue(interimAdvancePayment.getValue());
        productContractInterimAdvancePayments.setIssueDate(interimAdvancePayment.getIssueDate());
        productContractInterimAdvancePayments.setStatus(ContractSubObjectStatus.ACTIVE);
        productContractInterimAdvancePayments.setTermValue(interimAdvancePayment.getTermValue());
        productContractInterimAdvancePayments.setInterimAdvancePaymentId(interimAdvancePayment.getInterimAdvancePaymentId());
        productContractInterimAdvancePayments.setContractDetailId(productContractDetails.getId());
        return productContractInterimAdvancePayments;
    }

    private void saveFormulaVariables(ProductContractDetails productContractDetails, ProductParameterBaseRequest productParameters) {
        List<PriceComponentContractFormula> contractFormulas = productParameters.getContractFormulas();
        List<ProductContractPriceComponents> priceComponents = new ArrayList<>();
        for (PriceComponentContractFormula contractFormula : contractFormulas) {
            ProductContractPriceComponents contractPriceComponents = createContractPriceComponent(productContractDetails, contractFormula);
            priceComponents.add(contractPriceComponents);
        }
        productContractPriceComponentRepository.saveAll(priceComponents);
    }

    private static ProductContractPriceComponents createContractPriceComponent(ProductContractDetails productContractDetails, PriceComponentContractFormula contractFormula) {
        ProductContractPriceComponents contractPriceComponents = new ProductContractPriceComponents();
        contractPriceComponents.setContractDetailId(productContractDetails.getId());
        contractPriceComponents.setValue(contractFormula.getValue());
        contractPriceComponents.setStatus(ContractSubObjectStatus.ACTIVE);
        contractPriceComponents.setPriceComponentFormulaVariableId(contractFormula.getFormulaVariableId());
        return contractPriceComponents;
    }

    /**
     * Retrieves the third tab fields for a given product detail ID.
     *
     * @param productDetailId The ID of the product detail.
     * @return The third page fields of the product contract {@link ProductContractThirdPageFields} .
     * @throws DomainEntityNotFoundException if no product detail is found with the given ID.
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ProductContractThirdPageFields thirdTabFields(Long productDetailId) {
        ProductContractThirdPageFields result = new ProductContractThirdPageFields();
        ProductDetails details = productDetailsRepository
                .findByIdProductDetailStatusProductStatus(productDetailId, ProductDetailStatus.ACTIVE, ProductStatus.ACTIVE)
                .orElseThrow(() -> new DomainEntityNotFoundException("productDetailId-There is no Product Detail with Give id: " + productDetailId + ";"));
        result.setContractTypes(details.getContractTypes());
        result.setPaymentGuarantees(details.getPaymentGuarantees());
        Terms currentTerm = null;
        if (details.getTerms() != null) {
            currentTerm = details.getTerms();
        } else {
            Long groupOfTermsId = details.getTermsGroups().getId();
            currentTerm = termsRepository.findById(termsRepository.getTermIdFromCurrentTermGroup(groupOfTermsId)).orElse(null);
            result.setTermFromGroup(true);
        }

        result.setContractEntryIntoForces(getContractEntryIntoForceForContractFields(currentTerm));
        result.setStartOfContractInitialTermsForContractFields(getStartOfContractInitialTermsForContractFields(currentTerm));
        result.setSupplyActivationsForContractFields(getSupplyActivationsForContractFields(currentTerm));
        result.setWaitForOldContractTermToExpires(currentTerm != null ? currentTerm.getWaitForOldContractTermToExpires() : new ArrayList<>());
        result.setInvoicePaymentTerms(currentTerm != null ? invoicePaymentTermsRepository.findDetailedByTermIdAndStatusIn(currentTerm.getId(), List.of(PaymentTermStatus.ACTIVE)) : new ArrayList<>());
        result.setInstallmentForContractFields(new InstallmentForContractFields(details.getEqualMonthlyInstallmentsActivation(), details.getInstallmentNumber(), details.getInstallmentNumberFrom(), details.getInstallmentNumberTo(), details.getAmount(), details.getAmountFrom(), details.getAmountTo()));
        result.setProductContractTerms(productContractTermRepository.findAllByProductDetailsIdAndStatusInOrderByCreateDate(productDetailId, List.of(ProductSubObjectStatus.ACTIVE)).stream().map(ProductContractTermsResponse::new).toList());
        result.setDepositResponse(new DepositResponse(details.getCashDepositAmount(), details.getCashDepositCurrency(), details.getBankGuaranteeAmount(), details.getBankGuaranteeCurrency()));

        List<InterimAdvancePaymentResponse> interimAdvancePayments = new ArrayList<>();
        Comparator<InterimAdvancePayment> interimAdvancePaymentComparator = Comparator.comparingLong(InterimAdvancePayment::getId);
        interimAdvancePayments.addAll(getFromDirectIap(details.getInterimAndAdvancePayments().stream().filter(in -> in.getProductSubObjectStatus() == ProductSubObjectStatus.ACTIVE).map(ProductInterimAndAdvancePayments::getInterimAdvancePayment).filter(e -> e.getStatus() == InterimAdvancePaymentStatus.ACTIVE).sorted(interimAdvancePaymentComparator).toList()));
        interimAdvancePayments.addAll(getFromGroupsIap(productContractRepository.getIapSByProductDetailIdAndCurrentDate(productDetailId)));
        getAndSetIapFormulas(interimAdvancePayments);
        result.setInterimAdvancePayments(interimAdvancePayments);

        List<PriceComponentFormula> formulaVariables = new ArrayList<>();
        List<PriceComponent> priceComponents = productPriceComponentRepository.findPriceComponentsForProductDetails(details.getId());
        if (priceComponents != null) {
            formulaVariables.addAll(convertToPriceComponentFormula(priceComponents));
        }
        formulaVariables.addAll(new ArrayList<>());
        formulaVariables.addAll(convertToPriceComponentFormulaFromGroups(productDetailId));
        result.setFormulaVariables(formulaVariables);
        result.setProductAdditionalParams(
                productAdditionalParamsRepository.findProductFilledAdditionalParamsByProductDetailId(productDetailId));
        return result;
    }

    @Transactional
    public void thirdTabFieldsForMassImport(ProductContractProductParametersCreateRequest result, Long productId, Long versionId, List<String> errorMessages) {

        ProductDetails details = productDetailsRepository
                .findByProductIdAndVersion(productId, versionId)
                .orElseThrow(() -> new DomainEntityNotFoundException("productDetailId-There is no Product Detail with Give id: " + productId + ";"));
        Terms currentTerm = null;
        if (details.getTerms() != null) {
            currentTerm = details.getTerms();
        } else {
            Long groupOfTermsId = details.getTermsGroups().getId();
            currentTerm = termsRepository.findById(termsRepository.getTermIdFromCurrentTermGroup(groupOfTermsId)).orElse(null);
        }
        List<ProductContractTerms> contractTerms = productContractTermRepository.findAllByProductDetailsIdAndStatusInOrderByCreateDate(details.getId(), List.of(ProductSubObjectStatus.ACTIVE));
        if (contractTerms.size() != 1) {
            errorMessages.add("Wrong product, Contract term size is greater than 1");
            return;
        }
        ProductContractTerms productContractTerms = contractTerms.stream().findFirst().get();
        result.setProductContractTermId(productContractTerms.getId());

        List<InvoicePaymentTermsResponse> invoicePaymentTerms = invoicePaymentTermsRepository.findDetailedByTermIdAndStatusIn(currentTerm.getId(), List.of(PaymentTermStatus.ACTIVE));
        if (invoicePaymentTerms.size() != 1) {
            errorMessages.add("Wrong product, invoice payment term size is greater than 1;");
            return;
        }
        InvoicePaymentTermsResponse invoicePaymentTermsResponse = invoicePaymentTerms.stream().findFirst().get();
        result.setInvoicePaymentTermId(invoicePaymentTermsResponse.getId());
        result.setInvoicePaymentTermValue(invoicePaymentTermsResponse.getValue());

        List<ContractInterimAdvancePaymentsRequest> interimAdvancePayments = new ArrayList<>();
        result.setInterimAdvancePayments(interimAdvancePayments);
        Comparator<InterimAdvancePayment> interimAdvancePaymentComparator = Comparator.comparingLong(InterimAdvancePayment::getId);
        AtomicInteger index = new AtomicInteger(0);
        details.getInterimAndAdvancePayments().stream()
                .filter(in -> in.getProductSubObjectStatus() == ProductSubObjectStatus.ACTIVE)
                .map(ProductInterimAndAdvancePayments::getInterimAdvancePayment)
                .filter(e -> e.getStatus() == InterimAdvancePaymentStatus.ACTIVE)
                .sorted(interimAdvancePaymentComparator).forEach(iap -> {
                    boolean isValid = true;
                    if (Objects.equals(iap.getPaymentType(), PaymentType.OBLIGATORY)) {
                        if (iap.getDateOfIssueType().equals(DateOfIssueType.WORKING_DAYS_AFTER_INVOICE_DATE)) {
                            if (iap.getDateOfIssueValue() == null) {
                                errorMessages.add("productDetails.interimAdvancePayments[%s]-interim and advance payment with dateOfIssueType WORKING DAYS AFTER INVOICE DATE must have dateOfIssueValue filled".formatted(index));
                                isValid = false;
                            }
                            if (iap.getDateOfIssueValueFrom() != null) {
                                errorMessages.add("productDetails.interimAdvancePayments[%s]-interim and advance payment with dateOfIssueType WORKING DAYS AFTER INVOICE DATE must not have dateOfIssueValueFrom filled".formatted(index));
                                isValid = false;
                            }
                            if (iap.getDateOfIssueValueTo() != null) {
                                errorMessages.add("productDetails.interimAdvancePayments[%s]-interim and advance payment with dateOfIssueType WORKING DAYS AFTER INVOICE DATE must not have dateOfIssueValueTo filled".formatted(index));
                                isValid = false;
                            }
                        }
                    } else {
                        errorMessages.add("productDetails.interimAdvancePayments[%s]-Payment type of interim and advance payment must be OBLIGATORY".formatted(index));
                        isValid = false;
                    }
                    if (isValid) {
                        interimAdvancePayments.add(mapIapToEditRequest(iap));
                    }
                    index.getAndIncrement();
                });

        interimAdvancePayments.addAll(getIapEditRequest(productContractRepository.getIapSByProductDetailIdAndCurrentDate(details.getId())));
        getAndSetIapFormulasForMi(interimAdvancePayments);

        List<PriceComponentContractFormula> formulaVariables = new ArrayList<>();
        formulaVariables.addAll(convertToPriceComponentFormula(details.getPriceComponents().stream().filter(pc -> pc.getProductSubObjectStatus() == ProductSubObjectStatus.ACTIVE).map(ProductPriceComponents::getPriceComponent).toList()).stream().flatMap(x -> x.getVariables().stream()).map(this::mapToRequestFormula).toList());
        formulaVariables.addAll(new ArrayList<>());
        formulaVariables.addAll(convertToPriceComponentFormulaFromGroups(details.getId()).stream().flatMap(x -> x.getVariables().stream()).map(this::mapToRequestFormula).toList());
        result.setContractFormulas(formulaVariables);

        List<ProductAdditionalParams> productAdditionalParamsByProductDetailId = productAdditionalParamsRepository.findProductAdditionalParamsByProductDetailId(details.getId());
        for (ProductAdditionalParams params : productAdditionalParamsByProductDetailId) {
            if (params.getValue() == null && StringUtils.isNotEmpty(params.getLabel())) {
                errorMessages.add("productAdditionalParams-all additional param values must be filled;");
                return;
            }
        }
    }

    private PriceComponentContractFormula mapToRequestFormula(PriceComponentFormulaVariables formula) {
        PriceComponentContractFormula priceComponentContractFormula = new PriceComponentContractFormula();
        priceComponentContractFormula.setFormulaVariableId(formula.getFormulaVariableId());
        priceComponentContractFormula.setValue(formula.getValue());
        return priceComponentContractFormula;
    }

    private void getAndSetIapFormulasForMi(List<ContractInterimAdvancePaymentsRequest> interimAdvancePayments) {
        Map<Long, ContractInterimAdvancePaymentsRequest> collected = interimAdvancePayments.stream().collect(Collectors.toMap(ContractInterimAdvancePaymentsRequest::getInterimAdvancePaymentId, j -> j));
        Map<Long, List<PriceComponentProjectionForIap>> groupedBy = priceComponentFormulaVariableRepository
                .findAllByIapIds(collected.keySet())
                .stream()
                .collect(Collectors.groupingBy(PriceComponentProjectionForIap::getIapId));
        for (ContractInterimAdvancePaymentsRequest interimAdvancePayment : interimAdvancePayments) {
            List<PriceComponentProjectionForIap> formulas = groupedBy.get(interimAdvancePayment.getInterimAdvancePaymentId());
            if (formulas == null) {
                continue;
            }
            interimAdvancePayment.setContractFormulas(formulas.stream().map(PriceComponentContractFormula::new).toList());
        }
    }

    private ContractInterimAdvancePaymentsRequest mapIapToEditRequest(InterimAdvancePayment response) {
        ContractInterimAdvancePaymentsRequest request = new ContractInterimAdvancePaymentsRequest();
        if (request.getValue() != null) {
            request.setValue(response.getValue());
        }
        request.setInterimAdvancePaymentId(response.getId());
        return request;
    }

    private List<ContractInterimAdvancePaymentsRequest> getIapEditRequest(List<IapResponseFromNativeQuery> requests) {
        List<ContractInterimAdvancePaymentsRequest> requestList = new ArrayList<>();
        for (IapResponseFromNativeQuery request : requests) {
            ContractInterimAdvancePaymentsRequest iapRequest = new ContractInterimAdvancePaymentsRequest();
            if (request.getValue() != null) {
                iapRequest.setValue(request.getValue());
            }
            iapRequest.setInterimAdvancePaymentId(request.getId());
            requestList.add(iapRequest);
        }
        return requestList;
    }

    private void getAndSetIapFormulas(List<InterimAdvancePaymentResponse> interimAdvancePayments) {
        Map<Long, InterimAdvancePaymentResponse> collected = interimAdvancePayments.stream().collect(Collectors.toMap(InterimAdvancePaymentResponse::getId, j -> j));
        Map<Long, List<PriceComponentProjectionForIap>> groupedBy = priceComponentFormulaVariableRepository
                .findAllByIapIds(collected.keySet())
                .stream()
                .collect(Collectors.groupingBy(PriceComponentProjectionForIap::getIapId));
        for (InterimAdvancePaymentResponse interimAdvancePayment : interimAdvancePayments) {
            List<PriceComponentProjectionForIap> formulas = groupedBy.get(interimAdvancePayment.getId());
            if (formulas == null) {
                continue;
            }
            interimAdvancePayment.getFormula().setVariables(formulas.stream().map(PriceComponentFormulaVariables::new).toList());
        }
    }

    private List<InterimAdvancePaymentResponse> getFromDirectIap(List<InterimAdvancePayment> iaps) {
        if (iaps == null)
            return new ArrayList<>();
        List<InterimAdvancePaymentResponse> result = new ArrayList<>();
        for (InterimAdvancePayment iap : iaps) {
            InterimAdvancePaymentTermsResponse interimAdvancePaymentTermsResponse = interimAdvancePaymentTermsService.findByInterimAdvancePaymentIdAndStatusIn(iap.getId(), List.of(InterimAdvancePaymentSubObjectStatus.ACTIVE));
            result.add(new InterimAdvancePaymentResponse(iap, interimAdvancePaymentTermsResponse));
        }
        return result;
    }

    private List<InterimAdvancePaymentResponse> getFromGroupsIap(List<IapResponseFromNativeQuery> iaps) {
        if (iaps == null)
            return new ArrayList<>();
        List<InterimAdvancePaymentResponse> result = new ArrayList<>();
        for (IapResponseFromNativeQuery iap : iaps) {
            InterimAdvancePaymentTermsResponse interimAdvancePaymentTermsResponse = interimAdvancePaymentTermsService.findByInterimAdvancePaymentIdAndStatusIn(iap.getId(), List.of(InterimAdvancePaymentSubObjectStatus.ACTIVE));
            InterimAdvancePaymentResponse iapResponse = new InterimAdvancePaymentResponse(iap, interimAdvancePaymentTermsResponse);
            iapResponse.setFromGroup(true);
            result.add(iapResponse);
        }
        return result;
    }

    private ContractEntryIntoForceForContractFields getContractEntryIntoForceForContractFields(Terms terms) {
        if (terms == null)
            return null;
        return new ContractEntryIntoForceForContractFields(terms.getContractEntryIntoForceFromExactDayOfMonthStartDay(), terms.getContractEntryIntoForces());
    }

    private StartOfContractInitialTermsForContractFields getStartOfContractInitialTermsForContractFields(Terms terms) {
        if (terms == null)
            return null;
        return new StartOfContractInitialTermsForContractFields(terms.getStartDayOfInitialContractTerm(),terms.getFirstDayOfTheMonthOfInitialContractTerm(), terms.getStartsOfContractInitialTerms());
    }

    private SupplyActivationsForContractFields getSupplyActivationsForContractFields(Terms terms) {
        if (terms == null)
            return null;
        return new SupplyActivationsForContractFields(terms.getSupplyActivationExactDateStartDay(), terms.getSupplyActivations(), terms.getWaitForOldContractTermToExpires());
    }

    private List<PriceComponentFormula> convertToPriceComponentFormula(List<PriceComponent> priceComponents) {
        if (priceComponents == null)
            return new ArrayList<>();
        List<Long> idList = priceComponents.stream().map(PriceComponent::getId).toList();
        Map<Long, List<PriceComponentFormulaVariable>> collect = priceComponentFormulaVariableRepository.findAllByPriceComponentIdIn(idList).stream().collect(Collectors.groupingBy(x -> x.getPriceComponent().getId()));
        return priceComponents.stream().filter(x -> collect.containsKey(x.getId())).map(e -> {
            PriceComponentFormula priceComponentFormula = new PriceComponentFormula();
            priceComponentFormula.setPriceComponentId(e.getId());
            List<PriceComponentFormulaVariables> variables = new ArrayList<>();
            List<PriceComponentFormulaVariable> priceComponentFormulaVariables = collect.get(e.getId());
            for (PriceComponentFormulaVariable v : priceComponentFormulaVariables) {
                PriceComponentFormulaVariables formulaVariable = new PriceComponentFormulaVariables();
                formulaVariable.setFormulaVariableId(v.getId());
                formulaVariable.setVariable(v.getVariable());
                formulaVariable.setValue(v.getValue());
                formulaVariable.setValueFrom(v.getValueFrom());
                formulaVariable.setValueTo(v.getValueTo());
                formulaVariable.setVariableDescription(v.getDescription());
                formulaVariable.setDisplayName(v.getDescription() + " (" + v.getVariable() + " from " + e.getName() + ")");
                variables.add(formulaVariable);
            }
            priceComponentFormula.setVariables(variables);
            return priceComponentFormula;
        }).toList();
    }

    private List<PriceComponentFormula> convertToPriceComponentFormulaFromGroups(Long productDetailId) {

        if (productDetailId == null)
            return new ArrayList<>();
        List<PriceComponentFormula> result = new ArrayList<>();
        List<PriceComponentForContractResponse> priceComponents = productContractRepository.getPriceComponentFromProductPriceComponentGroups(productDetailId);
        for (PriceComponentForContractResponse pc : priceComponents) {
            PriceComponentFormula priceComponentFormula = new PriceComponentFormula();
            priceComponentFormula.setPriceComponentId(pc.getId());
            List<PriceComponentFormulaVariables> variables = new ArrayList<>();
            List<PriceComponentFormulaVariable> formulaVariables = priceComponentFormulaVariableRepository.findAllByPriceComponentIdOrderByIdAsc(pc.getId());
            for (PriceComponentFormulaVariable v : formulaVariables) {
                PriceComponentFormulaVariables formulaVariable = new PriceComponentFormulaVariables();
                formulaVariable.setFormulaVariableId(v.getId());
                formulaVariable.setVariable(v.getVariable());
                formulaVariable.setValue(v.getValue());
                formulaVariable.setValueFrom(v.getValueFrom());
                formulaVariable.setValueTo(v.getValueTo());
                formulaVariable.setVariableDescription(v.getDescription());
                formulaVariable.setDisplayName(v.getDescription() + " (" + v.getVariable() + " from " + pc.getName() + ")");
                variables.add(formulaVariable);
            }
            priceComponentFormula.setVariables(variables);
            if (!variables.isEmpty()) {
                priceComponentFormula.setFromGroup(true);
                result.add(priceComponentFormula);
            }
        }

        return result;
    }

    private void fillContractDetails(ProductContractDetails details, ProductParameterBaseRequest request) {
        details.setContractType(request.getContractType());
        details.setProductContractTermId(request.getProductContractTermId());
        details.setPaymentGuarantee(request.getPaymentGuarantee());

        if (details.getPaymentGuarantee().equals(PaymentGuarantee.BANK) || details.getPaymentGuarantee().equals(PaymentGuarantee.CASH_DEPOSIT_AND_BANK)) {
            details.setBankGuaranteeAmount(request.getBankGuarantee());
            details.setBankGuaranteeCurrencyId(request.getBankGuaranteeCurrencyId());
        }
        if (details.getPaymentGuarantee().equals(PaymentGuarantee.CASH_DEPOSIT) || details.getPaymentGuarantee().equals(PaymentGuarantee.CASH_DEPOSIT_AND_BANK)) {
            details.setCashDepositAmount(request.getCashDeposit());
            details.setCashDepositCurrencyId(request.getCashDepositCurrencyId());

        }
        details.setGuaranteeContract(request.isGuaranteeContract());
        details.setGuaranteeContractInfo(request.getGuaranteeInformation());

        details.setEqualMonthlyInstallmentAmount(request.getMonthlyInstallmentAmount());
        details.setEqualMonthlyInstallmentNumber(request.getMonthlyInstallmentValue());

        details.setInvoicePaymentTermId(request.getInvoicePaymentTermId());
        details.setInvoicePaymentTermValue(request.getInvoicePaymentTermValue());
        details.setWaitContractExpire(request.getProductContractWaitForOldContractTermToExpires());
    }


    public ThirdPagePreview thirdPagePreview(ProductContractDetails details, ProductContract contract, ProductContractThirdPageFields sourceView) {
        ThirdPagePreview thirdPagePreview = fillPreviewResponse(details, contract);
        fillInvoicePaymentTermResponse(details, thirdPagePreview, sourceView);
        fillContractTermResponse(details, thirdPagePreview);
        fillProductContractResponse(details, contract, thirdPagePreview);
        fillFormulaVariables(details, thirdPagePreview, sourceView);
        fillContractIaps(details, thirdPagePreview, sourceView);
        fillContractProductAdditionalParams(details, thirdPagePreview, sourceView);
        return thirdPagePreview;
    }

    private void fillContractProductAdditionalParams(ProductContractDetails details, ThirdPagePreview thirdPagePreview, ProductContractThirdPageFields sourceView) {
        List<ProductContractAdditionalParams> contractAdditionalParams = productContractAdditionalParamsRepository.findAllByContractDetailId(details.getId());

        List<ProductContractAdditionalParamsResponse> paramsFromProd = productAdditionalParamsRepository
                .findProductFilledAdditionalParamsByProductDetailId(details.getProductDetailId());

        List<ProductContractProductAdditionalParamsResponse> contractAdditionalParamsResp = new ArrayList<>();
        for (ProductContractAdditionalParams fromContract : contractAdditionalParams) {
            Optional<ProductContractAdditionalParamsResponse> fromProdOptional = paramsFromProd
                    .stream()
                    .filter(prod ->
                            prod.id().equals(fromContract.getProductAdditionalParamId())
                                    && prod.value() == null)
                    .findAny();
            if (fromProdOptional.isPresent()) {
                contractAdditionalParamsResp.add(new ProductContractProductAdditionalParamsResponse(fromContract.getProductAdditionalParamId(), fromContract.getValue()));
            }
        }
        thirdPagePreview.setProductContractProductAdditionalParamsResponseList(contractAdditionalParamsResp);

    }

    private void fillContractTermResponse(ProductContractDetails details, ThirdPagePreview thirdPagePreview) {
        thirdPagePreview.setEntryIntoForce(details.getEntryIntoForce());
        thirdPagePreview.setEntryIntoForceValue(details.getEntryIntoForceDate());
        thirdPagePreview.setStartOfContractInitialTerm(details.getStartInitialTerm());
        thirdPagePreview.setStartOfContractValue(details.getInitialTermDate());
        thirdPagePreview.setSupplyActivation(details.getSupplyActivationAfterContractResigning());
        thirdPagePreview.setSupplyActivationValue(details.getSupplyActivationDate());
        thirdPagePreview.setProductContractWaitForOldContractTermToExpires(details.getWaitContractExpire());
    }

    private void fillContractIaps(ProductContractDetails details, ThirdPagePreview thirdPagePreview, ProductContractThirdPageFields sourceView) {
        Map<Long, ProductContractInterimAdvancePayments> collect = productContractInterimAdvancePaymentsRepository.findAllByContractDetailIdAndStatusIn(details.getId(), List.of(ContractSubObjectStatus.ACTIVE))
                .stream().collect(Collectors.toMap(ProductContractInterimAdvancePayments::getInterimAdvancePaymentId, x -> x));
        List<InterimAdvancePayment> iaps = advancePaymentRepository.findAllByIdIn(collect.keySet());
        List<ProductContractIAPResponse> interimAdvancePayments = new ArrayList<>();
        for (InterimAdvancePayment iap : iaps) {
            ProductContractInterimAdvancePayments productContractInterimAdvancePayments = collect.get(iap.getId());
            ProductContractIAPResponse response = new ProductContractIAPResponse();
            response.setName(iap.getName());
            response.setContractIapId(productContractInterimAdvancePayments.getId());
            response.setInterimAdvancePaymentId(iap.getId());
            response.setIssueDate(productContractInterimAdvancePayments.getIssueDate());
            response.setTermValue(productContractInterimAdvancePayments.getTermValue());
            response.setValue(productContractInterimAdvancePayments.getValue());
            InterimAdvancePaymentTermsResponse termResponse = interimAdvancePaymentTermsService.findByInterimAdvancePaymentIdAndStatusIn(iap.getId(), List.of(InterimAdvancePaymentSubObjectStatus.ACTIVE));
            response.setIapTerms(termResponse);
            interimAdvancePayments.add(response);
            if (iap.getValueType().equals(ValueType.PRICE_COMPONENT)) {
                response.setFormulas(interimPriceFormulaRepository.getPriceComponentFormulaByContractIapIdAndStatus(productContractInterimAdvancePayments.getId(), List.of(EntityStatus.ACTIVE)));
            }
        }
        List<InterimAdvancePaymentResponse> fromGroup = sourceView.getInterimAdvancePayments().stream().filter(x -> x.isFromGroup()).toList();
        for (InterimAdvancePaymentResponse iap : fromGroup) {

            ProductContractIAPResponse response = new ProductContractIAPResponse();
            response.setName(iap.getName());

            response.setInterimAdvancePaymentId(iap.getId());
            response.setIssueDate(iap.getDateOfIssueValue());

            response.setValue(iap.getValue());
            InterimAdvancePaymentTermsResponse interimAdvancePaymentTerm = iap.getInterimAdvancePaymentTerm();
            response.setIapTerms(interimAdvancePaymentTerm);
            response.setTermValue(interimAdvancePaymentTerm.getValue());
            interimAdvancePayments.add(response);
        }
        thirdPagePreview.setInterimAdvancePayments(interimAdvancePayments);
    }

    private void fillFormulaVariables(ProductContractDetails details, ThirdPagePreview thirdPagePreview, ProductContractThirdPageFields sourceView) {
        Map<Long, ProductContractPriceComponents> collect = productContractPriceComponentRepository.findByContractDetailIdAndStatusIn(details.getId(), List.of(ContractSubObjectStatus.ACTIVE))
                .stream().collect(Collectors.toMap(ProductContractPriceComponents::getPriceComponentFormulaVariableId, x -> x));
        List<ContractPriceComponentResponse> priceComponentResponses = new ArrayList<>();
        List<PriceComponentFormulaVariable> priceComponentFormulas = priceComponentFormulaVariableRepository.findAllByIdIn(collect.keySet());
        for (PriceComponentFormulaVariable priceComponentFormula : priceComponentFormulas) {
            ProductContractPriceComponents contractPriceComponents = collect.get(priceComponentFormula.getId());
            ContractPriceComponentResponse response = new ContractPriceComponentResponse();
            response.setValue(contractPriceComponents.getValue());
            response.setFormulaVariableId(priceComponentFormula.getId());
            response.setVariableDescription(priceComponentFormula.getDescription());
            priceComponentResponses.add(response);
        }
        List<PriceComponentFormulaVariables> list = sourceView.getFormulaVariables().stream().filter(PriceComponentFormula::isFromGroup).flatMap(x -> x.getVariables().stream()).toList();
        for (PriceComponentFormulaVariables priceComponentFormula : list) {
            ContractPriceComponentResponse response = new ContractPriceComponentResponse();
            response.setValue(priceComponentFormula.getValue());
            response.setFormulaVariableId(priceComponentFormula.getFormulaVariableId());
            response.setVariableDescription(priceComponentFormula.getVariableDescription());
            priceComponentResponses.add(response);
        }

        thirdPagePreview.setPriceComponents(priceComponentResponses);
    }

    private void fillProductContractResponse(ProductContractDetails details, ProductContract contract, ThirdPagePreview thirdPagePreview) {
        ProductContractTerms productContractTerms = productContractTermRepository.findById(details.getProductContractTermId())
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Product payment term not found!;"));
        thirdPagePreview.setContractTerm(new ProductContractTermsResponse(productContractTerms));
        thirdPagePreview.setContractTermDate(details.getContractTermEndDate());
    }

    private void fillInvoicePaymentTermResponse(ProductContractDetails details, ThirdPagePreview thirdPagePreview, ProductContractThirdPageFields sourceView) {
        Long invoicePaymentTermId = details.getInvoicePaymentTermId();
        if (!sourceView.isTermFromGroup()) {
            InvoicePaymentTerms invoicePaymentTerms = invoicePaymentTermsRepository.findById(invoicePaymentTermId)
                    .orElseThrow(() -> new DomainEntityNotFoundException("id-Invoice not found!;"));
            if (invoicePaymentTerms.getCalendarId() != null) {
                Calendar calendar = calendarRepository.findByIdAndStatusIsIn(invoicePaymentTerms.getCalendarId(), List.of(NomenclatureItemStatus.ACTIVE, NomenclatureItemStatus.INACTIVE))
                        .orElseThrow(() -> new DomainEntityNotFoundException("id-Calendar  not found for existing term;"));
                thirdPagePreview.setInvoicePaymentTerm(new InvoicePaymentTermsResponse(invoicePaymentTerms, calendar));
                thirdPagePreview.setInvoicePaymentTermValue(details.getInvoicePaymentTermValue());
            }
        } else {
            InvoicePaymentTermsResponse invoicePaymentTerm = sourceView.getInvoicePaymentTerms().stream().findFirst().orElse(null);
            thirdPagePreview.setInvoicePaymentTerm(invoicePaymentTerm);
            thirdPagePreview.setInvoicePaymentTermValue(invoicePaymentTerm == null ? null : invoicePaymentTerm.getValue());
        }
    }

    private ThirdPagePreview fillPreviewResponse(ProductContractDetails details, ProductContract contract) {
        ThirdPagePreview thirdPagePreview = new ThirdPagePreview();
        thirdPagePreview.setContractType(details.getContractType());
        PaymentGuarantee paymentGuarantee = details.getPaymentGuarantee();
        thirdPagePreview.setPaymentGuarantee(paymentGuarantee);
        if (paymentGuarantee != null && paymentGuarantee.equals(PaymentGuarantee.CASH_DEPOSIT) || paymentGuarantee.equals(PaymentGuarantee.CASH_DEPOSIT_AND_BANK)) {
            thirdPagePreview.setCashDeposit(details.getCashDepositAmount());
            Currency cashDepositCurrency = currencyRepository.findByIdAndStatus(details.getCashDepositCurrencyId(), List.of(NomenclatureItemStatus.ACTIVE, NomenclatureItemStatus.INACTIVE))
                    .orElseThrow(() -> new DomainEntityNotFoundException("id-Currency not found for cash deposit!;"));
            thirdPagePreview.setCashDepositCurrency(new CurrencyResponse(cashDepositCurrency));
        }
        if (paymentGuarantee != null && paymentGuarantee.equals(PaymentGuarantee.BANK) || paymentGuarantee.equals(PaymentGuarantee.CASH_DEPOSIT_AND_BANK)) {
            thirdPagePreview.setBankGuarantee(details.getBankGuaranteeAmount());
            Currency bankGuarantee = currencyRepository.findByIdAndStatus(details.getBankGuaranteeCurrencyId(), List.of(NomenclatureItemStatus.ACTIVE, NomenclatureItemStatus.INACTIVE))
                    .orElseThrow(() -> new DomainEntityNotFoundException("id-Currency not found for Bank guarantee!;"));
            thirdPagePreview.setBankDepositCurrency(new CurrencyResponse(bankGuarantee));
        }
        thirdPagePreview.setGuaranteeInformation(details.getGuaranteeContractInfo());
        thirdPagePreview.setGuaranteeContract(details.getGuaranteeContract());


        thirdPagePreview.setMonthlyInstallmentValue(details.getEqualMonthlyInstallmentNumber());
        thirdPagePreview.setMonthlyInstallmentAmount(details.getEqualMonthlyInstallmentAmount());

        thirdPagePreview.setMarginalPrice(details.getMarginalPrice());
        thirdPagePreview.setMarginalPriceValidity(details.getMarginalPriceValidity());
        thirdPagePreview.setHourlyLoadProfile(details.getAvgHourlyLoadProfiles());
        thirdPagePreview.setProcurementPrice(details.getProcurementPrice());
        thirdPagePreview.setImbalancePriceIncrease(details.getCostPriceIncreaseFromImbalances());
        thirdPagePreview.setSetMargin(details.getSetMargin());

        return thirdPagePreview;
    }
}
