package bg.energo.phoenix.service.product.product;

import bg.energo.phoenix.model.entity.contract.product.ProductContractAdditionalParams;
import bg.energo.phoenix.model.entity.contract.product.ProductContractDetails;
import bg.energo.phoenix.model.entity.contract.product.ProductContractInterimAdvancePayments;
import bg.energo.phoenix.model.entity.contract.product.ProductContractPriceComponents;
import bg.energo.phoenix.model.entity.nomenclature.product.Currency;
import bg.energo.phoenix.model.entity.product.iap.interimAdvancePayment.InterimAdvancePayment;
import bg.energo.phoenix.model.entity.product.iap.interimAdvancePayment.InterimAdvancePaymentTerms;
import bg.energo.phoenix.model.entity.product.price.priceComponent.PriceComponent;
import bg.energo.phoenix.model.entity.product.price.priceComponent.PriceComponentFormulaVariable;
import bg.energo.phoenix.model.entity.product.price.priceComponentGroup.PriceComponentGroup;
import bg.energo.phoenix.model.entity.product.product.ProductAdditionalParams;
import bg.energo.phoenix.model.entity.product.product.ProductContractTerms;
import bg.energo.phoenix.model.entity.product.product.ProductDetails;
import bg.energo.phoenix.model.entity.product.term.terms.InvoicePaymentTerms;
import bg.energo.phoenix.model.entity.product.term.terms.Terms;
import bg.energo.phoenix.model.entity.product.term.termsGroups.TermsGroups;
import bg.energo.phoenix.model.enums.contract.products.ContractSubObjectStatus;
import bg.energo.phoenix.model.enums.product.iap.advancedPaymentGroup.AdvancedPaymentGroupStatus;
import bg.energo.phoenix.model.enums.product.iap.interimAdvancePayment.InterimAdvancePaymentStatus;
import bg.energo.phoenix.model.enums.product.iap.interimAdvancePayment.InterimAdvancePaymentSubObjectStatus;
import bg.energo.phoenix.model.enums.product.iap.interimAdvancePayment.PaymentType;
import bg.energo.phoenix.model.enums.product.iap.interimAdvancePayment.ValueType;
import bg.energo.phoenix.model.enums.product.price.priceComponent.PriceComponentStatus;
import bg.energo.phoenix.model.enums.product.price.priceComponentGroup.PriceComponentGroupStatus;
import bg.energo.phoenix.model.enums.product.product.PaymentGuarantee;
import bg.energo.phoenix.model.enums.product.product.ProductDetailStatus;
import bg.energo.phoenix.model.enums.product.product.ProductSubObjectStatus;
import bg.energo.phoenix.model.enums.product.product.ProductTermPeriodType;
import bg.energo.phoenix.model.enums.product.term.terms.*;
import bg.energo.phoenix.model.request.product.product.BaseProductTermsRequest;
import bg.energo.phoenix.model.request.product.product.ProductEditRequest;
import bg.energo.phoenix.repository.contract.product.ProductContractAdditionalParamsRepository;
import bg.energo.phoenix.repository.contract.product.ProductContractDetailsRepository;
import bg.energo.phoenix.repository.contract.product.ProductContractInterimAdvancePaymentsRepository;
import bg.energo.phoenix.repository.contract.product.ProductContractPriceComponentRepository;
import bg.energo.phoenix.repository.product.iap.interimAdvancePayment.InterimAdvancePaymentRepository;
import bg.energo.phoenix.repository.product.iap.interimAdvancePayment.InterimAdvancePaymentTermsRepository;
import bg.energo.phoenix.repository.product.price.priceComponent.PriceComponentFormulaVariableRepository;
import bg.energo.phoenix.repository.product.price.priceComponent.PriceComponentRepository;
import bg.energo.phoenix.repository.product.price.priceComponentGroup.PriceComponentGroupRepository;
import bg.energo.phoenix.repository.product.product.*;
import bg.energo.phoenix.repository.product.term.terms.InvoicePaymentTermsRepository;
import bg.energo.phoenix.repository.product.term.terms.TermsRepository;
import bg.energo.phoenix.security.PermissionService;
import bg.energo.phoenix.service.contract.newVersionEvent.ProductContractCreateNewVersionEvent;
import bg.energo.phoenix.service.contract.newVersionEvent.ProductContractCreateNewVersionEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import static bg.energo.phoenix.permissions.PermissionContextEnum.PRODUCT_CONTRACTS;
import static bg.energo.phoenix.permissions.PermissionEnum.PRODUCT_CONTRACT_EDIT_LOCKED;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductRelatedContractUpdateService {
    private final TermsRepository termsRepository;
    private final PriceComponentRepository priceComponentRepository;
    private final ProductDetailsRepository productDetailsRepository;
    private final PriceComponentGroupRepository priceComponentGroupRepository;
    private final InvoicePaymentTermsRepository invoicePaymentTermsRepository;
    private final ProductContractTermRepository productContractTermRepository;
    private final InterimAdvancePaymentRepository interimAdvancePaymentRepository;
    private final ProductContractDetailsRepository productContractDetailsRepository;
    private final InterimAdvancePaymentTermsRepository interimAdvancePaymentTermsRepository;
    private final ProductPriceComponentGroupRepository productPriceComponentGroupRepository;
    private final PriceComponentFormulaVariableRepository priceComponentFormulaVariableRepository;
    private final ProductContractPriceComponentRepository productContractPriceComponentRepository;
    private final ProductContractAdditionalParamsRepository productContractAdditionalParamsRepository;
    private final ProductGroupOfInterimAndAdvancePaymentsRepository interimAndAdvancePaymentsGroupRepository;
    private final ProductContractInterimAdvancePaymentsRepository productContractInterimAdvancePaymentsRepository;
    private final ProductContractCreateNewVersionEventPublisher eventPublisher;
    private final ProductAdditionalParamsRepository productAdditionalParamsRepository;
    private final PermissionService permissionService;

    /**
     * Updates the product contracts based on the current product details and the updated related product contracts.
     *
     * @param currentProductDetails                            the current product details
     * @param updatedProductDetailsWithRelatedProductContracts the updated related product contracts
     * @param exceptionMessages                                the list to store any exception messages occurred during the update process
     */
    public void updateProductContracts(ProductDetails currentProductDetails,
                                       List<Long> updatedProductDetailsWithRelatedProductContracts,
                                       List<String> exceptionMessages) {
        List<String> relatedProductContractsExceptionMessagesContext = new ArrayList<>();

        if (hasProductDetailFixedParameters(currentProductDetails, relatedProductContractsExceptionMessagesContext)) {
            updateRelatedContracts(
                    currentProductDetails,
                    updatedProductDetailsWithRelatedProductContracts,
                    relatedProductContractsExceptionMessagesContext
            );
        }

        exceptionMessages.addAll(relatedProductContractsExceptionMessagesContext);
    }

    /**
     * Checks if a product is valid for updating related contracts.
     *
     * @param editRequest The product edit request object that contains the updated product information.
     * @return {@code true} if the product is valid for updating related contracts, {@code false} otherwise.
     */
    public boolean validateProductForUpdateRelatedContracts(ProductEditRequest editRequest) {
        List<String> exceptionMessages = new ArrayList<>();
        validateProductFixedParameters(editRequest, exceptionMessages);
        return exceptionMessages.isEmpty();
    }

    /**
     * Validates the product for updating related contracts.
     *
     * @param editRequest The product edit request.
     * @return A list of exception messages if any validation fails, otherwise an empty list.
     */
    public List<String> validateProductForUpdateRelatedContractsTest(ProductEditRequest editRequest) {
        List<String> exceptionMessages = new ArrayList<>();
        validateProductFixedParameters(editRequest, exceptionMessages);
        return exceptionMessages;
    }

    /**
     * Validates the fixed parameters of a product for editing.
     *
     * @param editRequest       The product edit request.
     * @param exceptionMessages The list to store any exception messages.
     */
    private void validateProductFixedParameters(ProductEditRequest editRequest, List<String> exceptionMessages) {
        Terms terms = fetchValidTerm(editRequest, exceptionMessages);
        if (!CollectionUtils.isNotEmpty(exceptionMessages)) {
            validateProductDetailsStatus(editRequest, exceptionMessages);
            validateIndividualProduct(editRequest, exceptionMessages);
            validateContractTypes(editRequest, exceptionMessages);
            validateProductContractTerms(editRequest, exceptionMessages);
            validateInvoicePaymentTerm(editRequest, exceptionMessages);
            validatePaymentGuarantee(editRequest, exceptionMessages);
            validateEnteringIntroForce(terms, exceptionMessages);
            validateStartOfInitialTerm(terms, exceptionMessages);
            validateSupplyActivationAfterContractResigning(terms, exceptionMessages);
            validateEqualMonthlyInstallments(editRequest, exceptionMessages);
            List<InterimAdvancePayment> interimAdvancePayments = validateInterimAdvancePayments(editRequest, exceptionMessages);
            validatePriceComponents(editRequest, interimAdvancePayments, exceptionMessages);
            validateAdditionalParams(editRequest, exceptionMessages);
        }
    }

    /**
     * Validates the additional parameters in the provided ProductEditRequest.
     *
     * @param request           The ProductEditRequest object containing the additional parameters.
     * @param exceptionMessages The list to store any exception messages.
     */
    private void validateAdditionalParams(ProductEditRequest request,
                                          List<String> exceptionMessages) {
        if (CollectionUtils.isNotEmpty(request.getProductAdditionalParams())) {
            request
                    .getProductAdditionalParams()
                    .stream()
                    .filter(pai -> Objects.isNull(pai.value()) && Objects.nonNull(pai.label()))
                    .forEach(pai -> newException("Cannot update related Product Contracts, Product Additional Information with ordering id: [%s] value is not defined;".formatted(pai.orderingId()), exceptionMessages));
        }
    }

    private List<InterimAdvancePayment> validateInterimAdvancePayments(ProductEditRequest editRequest, List<String> exceptionMessages) {
        List<InterimAdvancePayment> validInterimAdvancePaymentContext = new ArrayList<>();

        if (CollectionUtils.isNotEmpty(editRequest.getInterimAdvancePayments())) {
            editRequest
                    .getInterimAdvancePayments()
                    .forEach(iapId -> {
                        Optional<InterimAdvancePayment> interimAdvancePaymentOptional = interimAdvancePaymentRepository
                                .findByIdAndStatus(iapId, InterimAdvancePaymentStatus.ACTIVE);

                        if (interimAdvancePaymentOptional.isEmpty()) {
                            newException("Cannot update related Product Contracts, Interim Advance Payment with presented id: [%s] not found;".formatted(iapId), exceptionMessages);
                        } else {
                            validInterimAdvancePaymentContext.add(interimAdvancePaymentOptional.get());
                        }
                    });
        }

        if (CollectionUtils.isNotEmpty(editRequest.getInterimAdvancePaymentGroups())) {
            editRequest
                    .getInterimAdvancePaymentGroups()
                    .forEach(iapGroupId -> {
                        Optional<InterimAdvancePayment> respectiveInterimAdvancePaymentOptional = interimAdvancePaymentRepository
                                .findRespectiveByInterimAdvancePaymentGroupId(iapGroupId, List.of(AdvancedPaymentGroupStatus.ACTIVE), PageRequest.of(0, 1));

                        if (respectiveInterimAdvancePaymentOptional.isEmpty()) {
                            newException("Cannot update related Product Contracts, Respective Interim Advance Payment Group for presented group id: [%s] not found;".formatted(iapGroupId), exceptionMessages);
                        } else {
                            validInterimAdvancePaymentContext.add(respectiveInterimAdvancePaymentOptional.get());
                        }
                    });
        }

        validInterimAdvancePaymentContext
                .forEach(iap -> {
                    if (Objects.equals(iap.getPaymentType(), PaymentType.AT_LEAST_ONE)) {
                        newException("Cannot update related Product Contracts, Interim Advance Payment with id: [%s] has Payment Type [AT_LEAST_ONE];".formatted(iap.getId()), exceptionMessages);
                    }

                    if (Objects.isNull(iap.getPaymentType())) {
                        newException("Cannot update related Product Contracts, interim advance payment without payment type is selected;", exceptionMessages);
                    }

                    if (iap.getValueType() != null) {
                        switch (iap.getValueType()) {
                            case PERCENT_FROM_PREVIOUS_INVOICE_AMOUNT, EXACT_AMOUNT -> {
                                if (Objects.isNull(iap.getValue())) {
                                    newException("Cannot update related Product Contracts, Interim Advance Payment with id: [%s] value has not defined;".formatted(iap.getId()), exceptionMessages);
                                }
                            }
                        }
                    }

                    if (iap.getDateOfIssueType() != null) {
                        switch (iap.getDateOfIssueType()) {
                            case DATE_OF_THE_MONTH, WORKING_DAYS_AFTER_INVOICE_DATE -> {
                                if (Objects.isNull(iap.getDateOfIssueValue())) {
                                    newException("Cannot update related Product Contracts, Interim Advance Payment with id: [%s] date of issue value has not defined;".formatted(iap.getId()), exceptionMessages);
                                }
                            }
                        }
                    }

                    Optional<InterimAdvancePaymentTerms> interimAdvancePaymentTermsOptional = interimAdvancePaymentTermsRepository
                            .findByInterimAdvancePaymentIdAndStatusInOrderByCreateDate(iap.getId(), List.of(InterimAdvancePaymentSubObjectStatus.ACTIVE));
                    if (interimAdvancePaymentTermsOptional.isEmpty()) {
                        if (!iap.getMatchTermOfStandardInvoice()) {
                            newException("Cannot update related Product Contracts, Interim Advance Payment with id: [%s] payment term is not defined;".formatted(iap.getId()), exceptionMessages);
                        }
                    } else {
                        InterimAdvancePaymentTerms interimAdvancePaymentTerms = interimAdvancePaymentTermsOptional.get();
                        if (Objects.isNull(interimAdvancePaymentTerms.getValue())) {
                            newException("Cannot update related Product Contracts, Interim Advance Payment with id: [%s] payment term value is not defined;".formatted(iap.getId()), exceptionMessages);
                        }
                    }
                });

        return validInterimAdvancePaymentContext;
    }

    private void validatePriceComponents(ProductEditRequest editRequest, List<InterimAdvancePayment> interimAdvancePayments, List<String> exceptionMessages) {
        List<Long> validPriceComponentsContext = new ArrayList<>();

        if (CollectionUtils.isNotEmpty(editRequest.getPriceComponentIds())) {
            editRequest
                    .getPriceComponentIds()
                    .forEach(pcId -> {
                        Optional<PriceComponent> priceComponentOptional = priceComponentRepository
                                .findByIdAndStatusIn(pcId, List.of(PriceComponentStatus.ACTIVE));

                        if (priceComponentOptional.isEmpty()) {
                            newException("Cannot update related Product Contracts, Price Component with presented id: [%s] not found;".formatted(pcId), exceptionMessages);
                        } else {
                            validPriceComponentsContext.add(pcId);
                        }
                    });
        }

        if (CollectionUtils.isNotEmpty(editRequest.getPriceComponentGroupIds())) {
            editRequest
                    .getPriceComponentGroupIds()
                    .forEach(pcgId -> {
                        Optional<PriceComponentGroup> priceComponentGroupOptional = priceComponentGroupRepository
                                .findByIdAndStatusIn(pcgId, List.of(PriceComponentGroupStatus.ACTIVE));
                        if (priceComponentGroupOptional.isEmpty()) {
                            newException("Cannot update related Product Contracts, Price Component Group with presented id: [%s] not found;".formatted(pcgId), exceptionMessages);
                        } else {
                            Optional<Long> respectivePriceComponentByGroupId = priceComponentRepository
                                    .findRespectivePriceComponentByGroupId(priceComponentGroupOptional.get().getId(), PageRequest.of(0, 1));
                            if (respectivePriceComponentByGroupId.isEmpty()) {
                                newException("Cannot found respective Price Component while trying to determinate from Price Component Group with id: [%s]".formatted(pcgId), exceptionMessages);
                            } else {
                                validPriceComponentsContext.add(respectivePriceComponentByGroupId.get());
                            }
                        }
                    });
        }

        interimAdvancePayments
                .forEach(iap -> {
                    if (iap.getValueType().equals(ValueType.PRICE_COMPONENT)) {
                        validPriceComponentsContext.add(iap.getPriceComponent().getId());
                    }
                });

        List<PriceComponentFormulaVariable> priceComponentFormulaVariables = priceComponentFormulaVariableRepository
                .findAllByPriceComponentIdIn(validPriceComponentsContext);

        priceComponentFormulaVariables
                .stream()
                .filter(pcf -> Objects.isNull(pcf.getValue()))
                .forEach(pcf -> newException("Value in Price Component with id: [%s-%s] is not defined;".formatted(pcf.getPriceComponent().getId(), pcf.getPriceComponent().getName()), exceptionMessages));
    }

    private Terms fetchValidTerm(ProductEditRequest editRequest,
                                 List<String> exceptionMessages) {
        Terms terms = null;
        if (Objects.nonNull(editRequest.getTermId())) {
            Optional<Terms> termsOptional = termsRepository
                    .findById(editRequest.getTermId());
            if (termsOptional.isEmpty()) {
                exceptionMessages.add("Term not found with id: [%s]".formatted(editRequest.getTermId()));
            } else {
                terms = termsOptional.get();
            }
        } else if (Objects.nonNull(editRequest.getTermGroupId())) {
            Optional<Terms> termsOptional = termsRepository
                    .findRespectiveTermsByTermsGroupId(
                            editRequest.getTermGroupId(),
                            LocalDateTime.now(),
                            PageRequest.of(0, 1)
                    );
            if (termsOptional.isEmpty()) {
                exceptionMessages.add("Respective term not found for group with id: [%s]".formatted(editRequest.getTermGroupId()));
            } else {
                terms = termsOptional.get();
            }
        } else {
            exceptionMessages.add("Cannot update related Product Contracts, Product Contract term not found;");
        }
        return terms;
    }

    /**
     * Checks if the product details have fixed parameters.
     *
     * @param currentProductDetails the current product details
     * @param exceptionMessages     the list to store any exception messages occurred during the update process
     * @return true if the product details have fixed parameters, false otherwise
     */
    private boolean hasProductDetailFixedParameters(ProductDetails currentProductDetails,
                                                    List<String> exceptionMessages) {
        Terms productContractValidTerm = findProductDetailValidTerm(currentProductDetails, exceptionMessages);

        validateProductDetailsStatus(currentProductDetails, exceptionMessages);
        validateIndividualProduct(currentProductDetails, exceptionMessages);
        validateContractTypes(currentProductDetails, exceptionMessages);
        validateProductContractTerms(currentProductDetails, exceptionMessages);
        validateInvoicePaymentTerm(currentProductDetails, exceptionMessages);
        validatePaymentGuarantee(currentProductDetails, exceptionMessages);
        validateEnteringIntroForce(productContractValidTerm, exceptionMessages);
        validateStartOfInitialTerm(productContractValidTerm, exceptionMessages);
        validateSupplyActivationAfterContractResigning(productContractValidTerm, exceptionMessages);
        validateEqualMonthlyInstallments(currentProductDetails, exceptionMessages);
        validateAdditionalParams(currentProductDetails, exceptionMessages);

        return exceptionMessages.isEmpty();
    }

    private void validateAdditionalParams(ProductDetails details, List<String> exceptionMessages) {
        List<ProductAdditionalParams> productAdditionalParams = details.getProductAdditionalParams();
        if (CollectionUtils.isNotEmpty(productAdditionalParams)) {
            productAdditionalParams
                    .stream()
                    .filter(pai -> Objects.isNull(pai.getValue()))
                    .forEach(pai -> newException("Cannot update related Product Contracts, Product Additional Information with ordering id: [%s] value is not defined;".formatted(pai.getOrderingId()), exceptionMessages));
        }
    }

    /**
     * Update the related contracts for the current product details.
     *
     * @param currentProductDetails                            The current product details.
     * @param updatedProductDetailsWithRelatedProductContracts The updated product details with related product contracts.
     * @param exceptionMessagesContext                         The exception messages.
     */
    private void updateRelatedContracts(ProductDetails currentProductDetails,
                                        List<Long> updatedProductDetailsWithRelatedProductContracts,
                                        List<String> exceptionMessagesContext) {
        Terms productContractValidTerm = findProductDetailValidTerm(currentProductDetails, exceptionMessagesContext);

        List<ProductDetails> productDetails = validateExistingProductDetails(updatedProductDetailsWithRelatedProductContracts, exceptionMessagesContext);

        for (ProductDetails productDetail : productDetails) {
            List<ProductContractDetails> productRelatedContracts = productContractDetailsRepository
                    .findAllbyProductDetailId(productDetail.getId());

            for (ProductContractDetails productRelatedContract : productRelatedContracts) {
                Boolean isContractCurrent = productContractDetailsRepository.isProductContractCurrent(productRelatedContract.getId(), productRelatedContract.getContractId());
                if (isContractCurrent != null && isContractCurrent && !productRelatedContract.getStartDate().equals(LocalDate.now())) {
                    Boolean hasEditLockedPermission = permissionService.permissionContextContainsPermissions(
                            PRODUCT_CONTRACTS,
                            List.of(PRODUCT_CONTRACT_EDIT_LOCKED));
                    eventPublisher.publishProductContractCreateNewVersionEvent(
                            new ProductContractCreateNewVersionEvent(
                                    currentProductDetails,
                                    exceptionMessagesContext,
                                    productContractValidTerm,
                                    productDetail,
                                    productRelatedContract.getContractId(),
                                    productRelatedContract.getVersionId(),
                                    productRelatedContract.getCustomerDetailId(),
                                    hasEditLockedPermission,
                                    SecurityContextHolder.getContext()
                            )
                    );
                } else {
                    fillProductContractDetail(productRelatedContract, currentProductDetails,
                            exceptionMessagesContext, productContractValidTerm,
                            productDetail);
                }
            }
        }
    }


    public void fillProductContractDetail(ProductContractDetails productRelatedContract, ProductDetails currentProductDetails,
                                          List<String> exceptionMessagesContext, Terms productContractValidTerm,
                                          ProductDetails productDetail) {
        productRelatedContract.setProductDetailId(currentProductDetails.getId());
        productRelatedContract.setContractType(currentProductDetails.getContractTypes().get(0));
        setProductContractTerms(currentProductDetails, productRelatedContract, exceptionMessagesContext);
        setInvoicePaymentTerms(productRelatedContract, productContractValidTerm);
        setProductContractAdditionalParams(productRelatedContract, productDetail);
        productRelatedContract.setPaymentGuarantee(currentProductDetails.getPaymentGuarantees().get(0));
        productRelatedContract.setEntryIntoForce(productContractValidTerm.getContractEntryIntoForces().get(0));
        productRelatedContract.setStartInitialTerm(productContractValidTerm.getStartsOfContractInitialTerms().get(0));
        productRelatedContract.setSupplyActivationAfterContractResigning(productContractValidTerm.getSupplyActivations().get(0));
        List<InterimAdvancePayment> interimAdvancePayments = setInterimAdvancePayments(currentProductDetails, productRelatedContract, exceptionMessagesContext);
        setPriceComponents(currentProductDetails, productRelatedContract, interimAdvancePayments, exceptionMessagesContext);
        productRelatedContract.setEqualMonthlyInstallmentAmount(currentProductDetails.getAmount());
        productRelatedContract.setEqualMonthlyInstallmentNumber(currentProductDetails.getInstallmentNumber());
        if (!productContractValidTerm.getWaitForOldContractTermToExpires().isEmpty()) {
            productRelatedContract.setWaitContractExpire(productContractValidTerm.getWaitForOldContractTermToExpires().get(0));
        }

        Currency cashDepositCurrency = currentProductDetails.getCashDepositCurrency();
        if (Objects.nonNull(cashDepositCurrency)) {
            productRelatedContract.setCashDepositCurrencyId(cashDepositCurrency.getId());
            productRelatedContract.setCashDepositAmount(currentProductDetails.getCashDepositAmount());
        }

        Currency bankGuaranteeCurrency = currentProductDetails.getBankGuaranteeCurrency();
        if (Objects.nonNull(bankGuaranteeCurrency)) {
            productRelatedContract.setBankGuaranteeCurrencyId(bankGuaranteeCurrency.getId());
            productRelatedContract.setBankGuaranteeAmount(currentProductDetails.getBankGuaranteeAmount());
        }
        // productContractDetailsRepository.save(productRelatedContract);

    }


    /**
     * Sets the valid interim advance payments for a product contract.
     *
     * @param currentProductDetails  The current product details.
     * @param productRelatedContract The product related contract.
     * @param exceptionContext       The exception context.
     */
    private List<InterimAdvancePayment> setInterimAdvancePayments(ProductDetails currentProductDetails,
                                                                  ProductContractDetails productRelatedContract,
                                                                  List<String> exceptionContext) {
        List<InterimAdvancePayment> validInterimAdvancePaymentsContext = new ArrayList<>(
                interimAdvancePaymentRepository.findByProductDetailIdAndStatusIn(
                        currentProductDetails.getId(),
                        List.of(InterimAdvancePaymentStatus.ACTIVE)
                )
        );

        interimAndAdvancePaymentsGroupRepository
                .findByProductDetailsIdAndProductSubObjectStatusIn(
                        currentProductDetails.getId(),
                        List.of(ProductSubObjectStatus.ACTIVE)
                ).forEach(piap -> {
                    Optional<InterimAdvancePayment> respectiveInterimAdvancePaymentByGroup = interimAdvancePaymentRepository
                            .findRespectiveByInterimAdvancePaymentGroupId(
                                    piap.getInterimAdvancePaymentGroup().getId(),
                                    List.of(AdvancedPaymentGroupStatus.ACTIVE),
                                    PageRequest.of(0, 1)
                            );
                    if (respectiveInterimAdvancePaymentByGroup.isEmpty()) {
                        exceptionContext.add("Cannot find respective Interim Advance Payment while trying to determinate from Interim Advance Payment Group with id: [%s]".formatted(piap.getInterimAdvancePaymentGroup().getId()));
                    } else {
                        validInterimAdvancePaymentsContext.add(respectiveInterimAdvancePaymentByGroup.get());
                    }
                });

        productContractInterimAdvancePaymentsRepository
                .findAllByContractDetailIdAndStatusIn(productRelatedContract.getId(), List.of(ContractSubObjectStatus.ACTIVE))
                .forEach(pciap -> pciap.setStatus(ContractSubObjectStatus.DELETED));

        productContractInterimAdvancePaymentsRepository
                .saveAll(
                        validInterimAdvancePaymentsContext
                                .stream()
                                .map(viap -> new ProductContractInterimAdvancePayments(
                                        null,
                                        viap.getDateOfIssueValue(),
                                        viap.getValue(),
                                        viap.getId(),
                                        ContractSubObjectStatus.ACTIVE,
                                        productRelatedContract.getId(),
                                        null
                                ))
                                .toList()
                );

        return validInterimAdvancePaymentsContext;
    }

    /**
     * Sets the price components for a product contract.
     *
     * @param currentProductDetails  The current product details.
     * @param productRelatedContract The product related contract.
     * @param exceptionsContext      The list of exception messages.
     */
    private void setPriceComponents(ProductDetails currentProductDetails,
                                    ProductContractDetails productRelatedContract,
                                    List<InterimAdvancePayment> interimAdvancePayments,
                                    List<String> exceptionsContext) {

        List<Long> validPriceComponentsContext = new ArrayList<>(
                priceComponentRepository
                        .findActivePriceComponentsByProductDetailId(currentProductDetails.getId())
        );

        interimAdvancePayments
                .stream()
                .map(InterimAdvancePayment::getPriceComponent)
                .filter(Objects::nonNull)
                .map(PriceComponent::getId)
                .forEach(validPriceComponentsContext::add);

        productPriceComponentGroupRepository
                .findByProductDetailsIdAndProductSubObjectStatusIn(
                        currentProductDetails.getId(),
                        List.of(ProductSubObjectStatus.ACTIVE)
                ).forEach(ppcg -> {
                    Optional<Long> respectivePriceComponentByGroupId = priceComponentRepository
                            .findRespectivePriceComponentByGroupId(ppcg.getPriceComponentGroup().getId(), PageRequest.of(0, 1));
                    if (respectivePriceComponentByGroupId.isEmpty()) {
                        newException("Cannot found respective Price Component while trying to determinate from Price Component Group with id: [%s]".formatted(ppcg.getPriceComponentGroup().getId()), exceptionsContext);
                    } else {
                        validPriceComponentsContext.add(respectivePriceComponentByGroupId.get());
                    }
                });

        productContractPriceComponentRepository
                .findByContractDetailIdAndStatusIn(productRelatedContract.getId(), List.of(ContractSubObjectStatus.ACTIVE))
                .forEach(cpc -> cpc.setStatus(ContractSubObjectStatus.DELETED));

        List<PriceComponentFormulaVariable> priceComponentFormulaVariables = priceComponentFormulaVariableRepository
                .findAllByPriceComponentIdIn(validPriceComponentsContext);

        List<ProductContractPriceComponents> productContractPriceComponents = priceComponentFormulaVariables
                .stream()
                .map(pcfv -> {
                    if (pcfv.getValue() == null) {
                        newException("Value in Price Component with id: [%s] is not defined;".formatted(pcfv.getPriceComponent().getName()), exceptionsContext);
                        return null;
                    } else {
                        return new ProductContractPriceComponents(
                                null,
                                pcfv.getValue(),
                                pcfv.getId(),
                                ContractSubObjectStatus.ACTIVE,
                                productRelatedContract.getId()
                        );
                    }
                })
                .toList();

        productContractPriceComponentRepository.saveAll(productContractPriceComponents.stream().filter(Objects::nonNull).toList());
    }

    /**
     * Sets additional parameters for a product contract.
     *
     * @param productRelatedContract The product contract details object.
     * @param productDetails         The product details object.
     */
    private void setProductContractAdditionalParams(ProductContractDetails productRelatedContract,
                                                    ProductDetails productDetails) {
        List<ProductAdditionalParams> productAdditionalParamsFromDb = productAdditionalParamsRepository.findProductAdditionalParamsByProductDetailId(productDetails.getId());
        List<ProductAdditionalParams> productAdditionalParams = new ArrayList<>();
        if (productAdditionalParamsFromDb != null && !productAdditionalParamsFromDb.isEmpty()) {
            productAdditionalParams = productAdditionalParamsFromDb
                    .stream()
                    .filter(adPar -> adPar.getLabel() != null)
                    .toList();
        }
        List<ProductContractAdditionalParams> paramsFromContract = productContractAdditionalParamsRepository
                .findAllByContractDetailId(productRelatedContract.getId());

        findRedundantAdditionalParamsInContract(productAdditionalParams, paramsFromContract);

        for (ProductContractAdditionalParams contractParam : paramsFromContract) {
            productAdditionalParams
                    .stream()
                    .filter(prPar ->
                            prPar.getId().equals(contractParam.getProductAdditionalParamId())
                                    && prPar.getValue() != null)
                    .findAny()
                    .ifPresent(filledPar -> contractParam.setValue(filledPar.getValue()));
            productContractAdditionalParamsRepository.save(contractParam);
        }
    }

    private void findRedundantAdditionalParamsInContract(List<ProductAdditionalParams> productAdditionalParams,
                                                         List<ProductContractAdditionalParams> paramsFromContract) {

        if (CollectionUtils.isEmpty(paramsFromContract)) {
            return;
        }
        List<ProductContractAdditionalParams> redundantParamsInContract = new ArrayList<>();

        List<Long> productAdditionalParamsIdsFromProduct = productAdditionalParams
                .stream()
                .map(ProductAdditionalParams::getId)
                .toList();
        for (ProductContractAdditionalParams paramFromContract : paramsFromContract) {
            if (!productAdditionalParamsIdsFromProduct.contains(paramFromContract.getProductAdditionalParamId())) {
                redundantParamsInContract.add(paramFromContract);
            }
        }
        if (CollectionUtils.isNotEmpty(redundantParamsInContract)) {
            paramsFromContract.removeAll(redundantParamsInContract);
            productContractAdditionalParamsRepository.deleteAll(redundantParamsInContract);
        }
    }

    /**
     * Sets the invoice payment terms for the given product contract details.
     *
     * @param productRelatedContract   The product contract details to set the invoice payment terms for.
     * @param productContractValidTerm The valid term to use for finding the active invoice payment terms.
     */
    private void setInvoicePaymentTerms(ProductContractDetails productRelatedContract, Terms productContractValidTerm) {
        List<InvoicePaymentTerms> activeInvoicePaymentTerms = invoicePaymentTermsRepository
                .findInvoicePaymentTermsByTermIdAndStatusIn(productContractValidTerm.getId(), List.of(PaymentTermStatus.ACTIVE));
        productRelatedContract.setInvoicePaymentTermId(activeInvoicePaymentTerms.get(0).getId());
        productRelatedContract.setInvoicePaymentTermValue(activeInvoicePaymentTerms.get(0).getValue());
    }

    /**
     * Sets the product contract terms for the given product details and product related contract.
     *
     * @param currentProductDetails  The current product details.
     * @param productRelatedContract The product related contract.
     * @param exceptionMessages      The list to store any exception messages.
     */
    private void setProductContractTerms(ProductDetails currentProductDetails,
                                         ProductContractDetails productRelatedContract,
                                         List<String> exceptionMessages) {
        List<ProductContractTerms> latestProductContractTerm = productContractTermRepository
                .findAllByProductDetailsIdAndStatusInOrderByCreateDate(currentProductDetails.getId(), List.of(ProductSubObjectStatus.ACTIVE));
        if (latestProductContractTerm.isEmpty()) {
            newException("Cannot update related Product Contracts, Product Contract term not found;", exceptionMessages);
        } else {
            ProductContractTerms productContractTerm = latestProductContractTerm.get(0);

            productRelatedContract.setProductContractTermId(productContractTerm.getId());
        }
    }

    /**
     * Validates the existing product details based on the given list of updated product details with related product contracts.
     *
     * @param updatedProductDetailsWithRelatedProductContracts a list of Long representing the updated product details with related product contracts
     * @param exceptionMessages                                a list of String to store any exception messages
     * @return a List of ProductDetails containing the existing product details
     */
    private List<ProductDetails> validateExistingProductDetails(List<Long> updatedProductDetailsWithRelatedProductContracts,
                                                                List<String> exceptionMessages) {
        List<ProductDetails> productDetails = productDetailsRepository
                .findAllById(updatedProductDetailsWithRelatedProductContracts);

        List<Long> productDetailIds = productDetails
                .stream()
                .map(ProductDetails::getId)
                .toList();

        List<Long> nonExistingProductContractDetailIds = updatedProductDetailsWithRelatedProductContracts
                .stream()
                .filter(id -> !productDetailIds.contains(id))
                .toList();

        for (Long nonExistingProductContractDetailId : nonExistingProductContractDetailIds) {
            newException("Product Detail with id: [%s] not found;".formatted(nonExistingProductContractDetailId), exceptionMessages);
        }

        return productDetails;
    }

    /**
     * Validates if the equal monthly installments are selected for the current product details.
     * If not selected, it throws an exception message.
     * Also checks if the installment number and amount are defined for the equal monthly installments.
     * If not defined, it throws an exception message.
     *
     * @param currentProductDetails The current product details object
     * @param exceptionMessages     The list to store exception messages
     */
    private void validateEqualMonthlyInstallments(ProductDetails currentProductDetails,
                                                  List<String> exceptionMessages) {
        if (currentProductDetails.getEqualMonthlyInstallmentsActivation()) {
            if (Objects.isNull(currentProductDetails.getInstallmentNumber())) {
                newException("Cannot update related Product Contracts, current Product equal monthly installments number should be defined;", exceptionMessages);
            }

            if (Objects.isNull(currentProductDetails.getAmount())) {
                newException("Cannot update related Product Contracts, current Product equal monthly installments amount should be defined;", exceptionMessages);
            }
        }
    }

    private void validateEqualMonthlyInstallments(ProductEditRequest request,
                                                  List<String> exceptionMessages) {
        if (request.getEqualMonthlyInstallmentsActivation()) {
            if (Objects.isNull(request.getInstallmentNumber())) {
                newException("Cannot update related Product Contracts, current Product equal monthly installments number should be defined;", exceptionMessages);
            }

            if (Objects.isNull(request.getAmount())) {
                newException("Cannot update related Product Contracts, current Product equal monthly installments amount should be defined;", exceptionMessages);
            }
        }
    }

    /**
     * Validates the supply activation after contract resigning.
     *
     * @param term              the Terms object containing supply activations and wait for old contract term to expires options
     * @param exceptionMessages the list to store any exception messages
     */
    private void validateSupplyActivationAfterContractResigning(Terms term,
                                                                List<String> exceptionMessages) {
        List<SupplyActivation> supplyActivations = term.getSupplyActivations();
        if (supplyActivations.size() != 1) {
            newException("Cannot update related Product Contracts, current Product term supply activations must be fixed for single option only;", exceptionMessages);
        } else {
            SupplyActivation supplyActivation = supplyActivations.get(0);

            switch (supplyActivation) {
                case FIRST_DAY_OF_MONTH -> {
                    List<WaitForOldContractTermToExpire> waitForOldContractTermToExpires = term.getWaitForOldContractTermToExpires();
                    if (waitForOldContractTermToExpires.size() != 1) {
                        newException("Cannot update related Product Contracts, current Product term supply activation wait for old contract term to expire must be fixed for single option only;", exceptionMessages);
                    }
                }
                case EXACT_DATE ->
                        newException("Cannot update related Product Contracts, current Product term supply activations must be [FIRST_DAY_OF_MONTH, MANUAL];", exceptionMessages);
            }
        }
    }

    /**
     * Validates the start of the initial term for a given Terms object.
     *
     * @param term              The Terms object to validate.
     * @param exceptionMessages The list to store any exception messages.
     */
    private void validateStartOfInitialTerm(Terms term,
                                            List<String> exceptionMessages) {
        List<StartOfContractInitialTerm> startsOfContractInitialTerms = term.getStartsOfContractInitialTerms();
        if (startsOfContractInitialTerms.size() != 1) {
            newException("Cannot update related Product Contracts, current Product term start of initial terms must be fixed for single option only;", exceptionMessages);
        } else {
            StartOfContractInitialTerm startOfContractInitialTerm = startsOfContractInitialTerms.get(0);

            switch (startOfContractInitialTerm) {
                case EXACT_DATE, MANUAL ->
                        newException("Cannot update related Product Contracts, current Product term start of initial terms type must be [SIGNING, DATE_OF_CHANGE_OF_CBG, FIRST_DELIVERY];", exceptionMessages);
            }
        }
    }

    /**
     * Validates the entering into force of a term in a contract.
     * If the term's entering into force is not in the correct format or type,
     * an exception message is added to the list of exception messages.
     *
     * @param term              the term whose entering into force needs to be validated
     * @param exceptionMessages the list of exception messages to which the invalid entering into force error message
     *                          will be added if the validation fails
     */
    private void validateEnteringIntroForce(Terms term,
                                            List<String> exceptionMessages) {
        List<ContractEntryIntoForce> contractEntryIntoForces = term.getContractEntryIntoForces();
        if (contractEntryIntoForces.size() != 1) {
            newException("Cannot update related Product Contracts, current Product term entering into force must be fixed for single option only;", exceptionMessages);
        } else {
            ContractEntryIntoForce contractEntryIntoForce = contractEntryIntoForces.get(0);
            switch (contractEntryIntoForce) {
                case EXACT_DAY, MANUAL -> {
                    newException("Cannot update related Product Contracts, current Product term entry into force type must be [SIGNING, DATE_OF_CHANGE_OF_CBG, FIRST_DELIVERY];", exceptionMessages);
                }
            }
        }
    }

    /**
     * Validates the payment guarantee for a given product.
     *
     * @param currentProductDetails The product details containing the payment guarantees.
     * @param exceptionMessages     The list to store exception messages if any validation errors occur.
     */
    private void validatePaymentGuarantee(ProductDetails currentProductDetails,
                                          List<String> exceptionMessages) {
        List<PaymentGuarantee> paymentGuarantees = currentProductDetails.getPaymentGuarantees();
        if (paymentGuarantees.size() != 1) {
            newException("Cannot update related Product Contracts, current Product payment guarantees must be fixed for single option only;", exceptionMessages);
        } else {
            PaymentGuarantee paymentGuarantee = paymentGuarantees.get(0);

            switch (paymentGuarantee) {
                case CASH_DEPOSIT -> {
                    if (Objects.isNull(currentProductDetails.getCashDepositAmount())) {
                        newException("Cannot update related Product Contracts, current Product cash deposit amount should be defined while payment guarantee is defined;", exceptionMessages);
                    }

                    if (Objects.isNull(currentProductDetails.getCashDepositCurrency())) {
                        newException("Cannot update related Product Contracts, current Product cash deposit currency should be defined while payment guarantee is defined;", exceptionMessages);
                    }
                }
                case BANK -> {
                    if (Objects.isNull(currentProductDetails.getBankGuaranteeCurrency())) {
                        newException("Cannot update related Product Contracts, current Product bank guarantee currency should be defined while payment guarantee is defined;", exceptionMessages);
                    }

                    if (Objects.isNull(currentProductDetails.getBankGuaranteeAmount())) {
                        newException("Cannot update related Product Contracts, current Product bank guarantee amount should be defined while payment guarantee is defined;", exceptionMessages);
                    }
                }
                case CASH_DEPOSIT_AND_BANK -> {
                    if (Stream.of(currentProductDetails.getBankGuaranteeCurrency(), currentProductDetails.getCashDepositCurrency()).anyMatch(Objects::isNull)) {
                        newException("Cannot update related Product Contracts, current Product cash deposit and bank guarantee currency should be defined while payment guarantee is defined;", exceptionMessages);
                    }

                    if (Stream.of(currentProductDetails.getBankGuaranteeAmount(), currentProductDetails.getCashDepositAmount()).anyMatch(Objects::isNull)) {
                        newException("Cannot update related Product Contracts, current Product cash deposit and bank guarantee amount should be defined while payment guarantee is defined;", exceptionMessages);
                    }
                }
            }
        }
    }

    private void validatePaymentGuarantee(ProductEditRequest request,
                                          List<String> exceptionMessages) {
        List<PaymentGuarantee> paymentGuarantees = request.getPaymentGuarantees().stream().toList();
        if (paymentGuarantees.size() != 1) {
            newException("Cannot update related Product Contracts, current Product payment guarantees must be fixed for single option only;", exceptionMessages);
        } else {
            PaymentGuarantee paymentGuarantee = paymentGuarantees.get(0);

            switch (paymentGuarantee) {
                case CASH_DEPOSIT -> {
                    if (Objects.isNull(request.getCashDepositAmount())) {
                        newException("Cannot update related Product Contracts, current Product cash deposit amount should be defined while payment guarantee is defined;", exceptionMessages);
                    }

                    if (Objects.isNull(request.getCashDepositCurrencyId())) {
                        newException("Cannot update related Product Contracts, current Product cash deposit currency should be defined while payment guarantee is defined;", exceptionMessages);
                    }
                }
                case BANK -> {
                    if (Objects.isNull(request.getBankGuaranteeCurrencyId())) {
                        newException("Cannot update related Product Contracts, current Product bank guarantee currency should be defined while payment guarantee is defined;", exceptionMessages);
                    }

                    if (Objects.isNull(request.getBankGuaranteeAmount())) {
                        newException("Cannot update related Product Contracts, current Product bank guarantee amount should be defined while payment guarantee is defined;", exceptionMessages);
                    }
                }
                case CASH_DEPOSIT_AND_BANK -> {
                    if (Stream.of(request.getBankGuaranteeCurrencyId(), request.getCashDepositCurrencyId()).anyMatch(Objects::isNull)) {
                        newException("Cannot update related Product Contracts, current Product cash deposit and bank guarantee currency should be defined while payment guarantee is defined;", exceptionMessages);
                    }

                    if (Stream.of(request.getBankGuaranteeAmount(), request.getCashDepositAmount()).anyMatch(Objects::isNull)) {
                        newException("Cannot update related Product Contracts, current Product cash deposit and bank guarantee amount should be defined while payment guarantee is defined;", exceptionMessages);
                    }
                }
            }
        }
    }

    /**
     * Validates the status of the given product details.
     *
     * @param currentProductDetails The product details to validate.
     * @param exceptionMessages     A list to store any exception messages encountered during validation.
     */
    private void validateProductDetailsStatus(ProductDetails currentProductDetails,
                                              List<String> exceptionMessages) {
        LocalDateTime now = LocalDateTime.now();
        if (!currentProductDetails.getProductDetailStatus().equals(ProductDetailStatus.ACTIVE)) {
            newException("Cannot update related Product Contracts, current Product is not [ACTIVE];", exceptionMessages);
        }

        validateAvailableForSale(
                exceptionMessages,
                now,
                currentProductDetails.getAvailableForSale(),
                currentProductDetails.getAvailableFrom(),
                currentProductDetails.getAvailableTo()
        );
    }

    private void validateProductDetailsStatus(ProductEditRequest request,
                                              List<String> exceptionMessages) {
        LocalDateTime now = LocalDateTime.now();

        validateAvailableForSale(
                exceptionMessages,
                now,
                request.getAvailableForSale(),
                request.getAvailableFrom(),
                request.getAvailableTo()
        );
    }

    private void validateAvailableForSale(List<String> exceptionMessages,
                                          LocalDateTime now,
                                          Boolean availableForSale,
                                          LocalDateTime availableFrom,
                                          LocalDateTime availableTo) {
        if (!availableForSale) {
            newException("Cannot update related Product Contracts, current Product is not available for sale;", exceptionMessages);
        } else {
            if (Objects.nonNull(availableFrom) && (availableFrom.isAfter(now))) {
                newException("Cannot update related Product Contracts, current Product available from is defined in future;", exceptionMessages);
            }
            if (Objects.nonNull(availableTo) && (availableTo.isBefore(now))) {
                newException("Cannot update related Product Contracts, current Product available to is defined in past;", exceptionMessages);
            }
        }
    }

    /**
     * Validates individual product details.
     *
     * @param currentProductDetails The product details to be validated.
     * @param exceptionMessages     A list to store any exception messages encountered during validation.
     */
    private void validateIndividualProduct(ProductDetails currentProductDetails,
                                           List<String> exceptionMessages) {
        if (StringUtils.isNotEmpty(currentProductDetails.getProduct().getCustomerIdentifier())) {
            newException("Cannot update related Product Contracts, current Product is individual;", exceptionMessages);
        }
    }

    private void validateIndividualProduct(ProductEditRequest request,
                                           List<String> exceptionMessages) {
        if (StringUtils.isNotEmpty(request.getCustomerIdentifier())) {
            newException("Cannot update related Product Contracts, current Product is individual;", exceptionMessages);
        }
    }

    /**
     * Validates the contract types of a given product.
     *
     * @param currentProductDetails the current product details
     * @param exceptionMessages     the list to store the exception messages if validation fails
     */
    private void validateContractTypes(ProductDetails currentProductDetails,
                                       List<String> exceptionMessages) {
        if (currentProductDetails.getContractTypes().size() != 1) {
            newException("Cannot update related Product Contracts, current Product contract types must be fixed for single option only;", exceptionMessages);
        }
    }

    private void validateContractTypes(ProductEditRequest request,
                                       List<String> exceptionMessages) {
        if (request.getContractTypes().size() != 1) {
            newException("Cannot update related Product Contracts, current Product contract types must be fixed for single option only;", exceptionMessages);
        }
    }

    /**
     * Validates the product contract terms for a given product.
     *
     * @param currentProductDetails The current product details.
     * @param exceptionMessages     The list to store any exception messages.
     */
    private void validateProductContractTerms(ProductDetails currentProductDetails,
                                              List<String> exceptionMessages) {
        List<ProductContractTerms> activeProductContractTerms = productContractTermRepository
                .findAllByProductDetailsIdAndStatusInOrderByCreateDate(currentProductDetails.getId(), List.of(ProductSubObjectStatus.ACTIVE));

        if (activeProductContractTerms.size() != 1) {
            newException("Cannot update related Product Contracts, current Product contract terms must be fixed for single option only;", exceptionMessages);
        } else {
            ProductContractTerms productContractTerms = activeProductContractTerms.get(0);

            if (productContractTerms.getPeriodType() == ProductTermPeriodType.CERTAIN_DATE) {
                newException("Cannot update related Product Contracts, current Product contract term type must be [PERIOD, WITHOUT_TERM, OTHER];", exceptionMessages);
            }
        }
    }

    private void validateProductContractTerms(ProductEditRequest request,
                                              List<String> exceptionMessages) {
        List<BaseProductTermsRequest> contractTerms = request.getProductTerms();

        if (contractTerms.size() != 1) {
            newException("Cannot update related Product Contracts, current Product contract terms must be fixed for single option only;", exceptionMessages);
        } else {
            BaseProductTermsRequest productTermsRequest = contractTerms.get(0);

            if (productTermsRequest.getTypeOfTerms() == ProductTermPeriodType.CERTAIN_DATE) {
                newException("Cannot update related Product Contracts, current Product contract term type must be [PERIOD, WITHOUT_TERM, OTHER];", exceptionMessages);
            }
        }
    }

    /**
     * Validates the invoice payment term for the current product details.
     *
     * @param currentProductDetails The current product details.
     * @param exceptionMessages     The list to hold any exception messages.
     */
    private void validateInvoicePaymentTerm(ProductDetails currentProductDetails,
                                            List<String> exceptionMessages) {
        Terms terms = currentProductDetails.getTerms();
        TermsGroups termsGroups = currentProductDetails.getTermsGroups();

        if (Objects.nonNull(terms)) {
            List<InvoicePaymentTerms> activeInvoicePaymentTerms = invoicePaymentTermsRepository
                    .findInvoicePaymentTermsByTermIdAndStatusIn(terms.getId(), List.of(PaymentTermStatus.ACTIVE));

            if (activeInvoicePaymentTerms.size() != 1) {
                newException("Cannot update related Product Contracts, current Product terms invoice payment terms must be fixed for single option only;", exceptionMessages);
            } else {
                InvoicePaymentTerms invoicePaymentTerms = activeInvoicePaymentTerms.get(0);

                if (Objects.isNull(invoicePaymentTerms.getValue())) {
                    newException("Cannot update related Product Contracts, current Product terms invoice payment terms value should be defined;", exceptionMessages);
                }
            }
        } else {
            if (Objects.isNull(termsGroups)) {
                newException("Cannot update related Product Contracts, terms/terms group is not defined;", exceptionMessages);
            }
        }
    }

    private void validateInvoicePaymentTerm(ProductEditRequest request,
                                            List<String> exceptionMessages) {
        if (Objects.nonNull(request.getTermId())) {
            List<InvoicePaymentTerms> activeInvoicePaymentTerms = invoicePaymentTermsRepository
                    .findInvoicePaymentTermsByTermIdAndStatusIn(request.getTermId(), List.of(PaymentTermStatus.ACTIVE));

            if (activeInvoicePaymentTerms.size() != 1) {
                newException("Cannot update related Product Contracts, current Product terms invoice payment terms must be fixed for single option only;", exceptionMessages);
            } else {
                InvoicePaymentTerms invoicePaymentTerms = activeInvoicePaymentTerms.get(0);

                if (Objects.isNull(invoicePaymentTerms.getValue())) {
                    newException("Cannot update related Product Contracts, current Product terms invoice payment terms value should be defined;", exceptionMessages);
                }
            }
        } else {
            if (Objects.isNull(request.getTermGroupId())) {
                newException("Cannot update related Product Contracts, terms/terms group is not defined;", exceptionMessages);
            }
        }
    }

    /**
     * Finds the valid term of the product contract for the given product details.
     *
     * @param currentProductDetails The current product details.
     * @param exceptionMessages     The list to store any exception messages that occur during execution.
     * @return The valid term of the product contract. Returns null if no valid term is found.
     */
    private Terms findProductDetailValidTerm(ProductDetails currentProductDetails,
                                             List<String> exceptionMessages) {
        Terms terms = currentProductDetails.getTerms();
        TermsGroups termsGroups = currentProductDetails.getTermsGroups();

        Terms targetTerm = null;
        if (Objects.nonNull(terms)) {
            targetTerm = terms;
        } else if (Objects.nonNull(termsGroups)) {
            Optional<Terms> termsOptional = termsRepository
                    .findRespectiveTermsByTermsGroupId(termsGroups.getId(), LocalDateTime.now(), PageRequest.of(0, 1));
            if (termsOptional.isEmpty()) {
                newException("Cannot update related Product Contracts, current Product term is not defined;", exceptionMessages);
            } else {
                targetTerm = termsOptional.get();
            }
        }

        return targetTerm;
    }

    private void newException(String exceptionMessage, List<String> context) {
        log.error(exceptionMessage);
        context.add(exceptionMessage);
    }
}
