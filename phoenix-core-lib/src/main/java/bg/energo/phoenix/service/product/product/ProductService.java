package bg.energo.phoenix.service.product.product;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.exception.OperationNotAllowedException;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.nomenclature.customer.Segment;
import bg.energo.phoenix.model.entity.nomenclature.product.*;
import bg.energo.phoenix.model.entity.nomenclature.product.Currency;
import bg.energo.phoenix.model.entity.nomenclature.product.product.ProductGroups;
import bg.energo.phoenix.model.entity.nomenclature.product.product.ProductTypes;
import bg.energo.phoenix.model.entity.product.price.priceComponent.PriceComponent;
import bg.energo.phoenix.model.entity.product.price.priceComponent.PriceComponentFormulaVariable;
import bg.energo.phoenix.model.entity.product.product.*;
import bg.energo.phoenix.model.entity.product.term.terms.Terms;
import bg.energo.phoenix.model.entity.product.term.termsGroups.TermsGroups;
import bg.energo.phoenix.model.entity.receivable.collectionChannel.CollectionChannel;
import bg.energo.phoenix.model.entity.template.ProductTemplate;
import bg.energo.phoenix.model.enums.copy.group.CopyDomainWithVersion;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.product.price.priceComponent.PriceComponentMathVariableName;
import bg.energo.phoenix.model.enums.product.product.*;
import bg.energo.phoenix.model.enums.product.product.list.ProductListColumns;
import bg.energo.phoenix.model.enums.product.product.list.ProductListIndividualProduct;
import bg.energo.phoenix.model.enums.product.product.list.ProductParameterFilterField;
import bg.energo.phoenix.model.enums.product.term.terms.TermStatus;
import bg.energo.phoenix.model.enums.product.term.termsGroup.TermGroupStatus;
import bg.energo.phoenix.model.enums.template.ContractTemplatePurposes;
import bg.energo.phoenix.model.enums.template.ContractTemplateStatus;
import bg.energo.phoenix.model.enums.template.ContractTemplateType;
import bg.energo.phoenix.model.enums.template.ProductServiceTemplateType;
import bg.energo.phoenix.model.request.communication.edms.Attribute;
import bg.energo.phoenix.model.request.contract.product.PriceComponentContractFormula;
import bg.energo.phoenix.model.request.contract.product.ProductParameterBaseRequest;
import bg.energo.phoenix.model.request.copy.group.CopyDomainWithVersionBaseRequest;
import bg.energo.phoenix.model.request.product.product.*;
import bg.energo.phoenix.model.response.copy.group.CopyDomainWithVersionBaseResponse;
import bg.energo.phoenix.model.response.copy.group.CopyDomainWithVersionMiddleResponse;
import bg.energo.phoenix.model.response.nomenclature.customer.SegmentResponse;
import bg.energo.phoenix.model.response.nomenclature.product.SalesAreaResponse;
import bg.energo.phoenix.model.response.nomenclature.product.SalesChannelResponse;
import bg.energo.phoenix.model.response.product.*;
import bg.energo.phoenix.model.response.service.ContractTermNameResponse;
import bg.energo.phoenix.model.response.shared.FileWithStatusesResponse;
import bg.energo.phoenix.model.response.terms.TermsShortResponse;
import bg.energo.phoenix.permissions.PermissionContextEnum;
import bg.energo.phoenix.permissions.PermissionEnum;
import bg.energo.phoenix.repository.nomenclature.customer.SegmentRepository;
import bg.energo.phoenix.repository.nomenclature.product.*;
import bg.energo.phoenix.repository.nomenclature.product.product.ProductGroupsRepository;
import bg.energo.phoenix.repository.nomenclature.product.product.ProductTypeRepository;
import bg.energo.phoenix.repository.product.iap.advancedPaymentGroup.AdvancedPaymentGroupRepository;
import bg.energo.phoenix.repository.product.iap.interimAdvancePayment.InterimAdvancePaymentRepository;
import bg.energo.phoenix.repository.product.penalty.penalty.PenaltyRepository;
import bg.energo.phoenix.repository.product.penalty.penaltyGroup.PenaltyGroupRepository;
import bg.energo.phoenix.repository.product.price.priceComponent.PriceComponentFormulaVariableRepository;
import bg.energo.phoenix.repository.product.price.priceComponent.PriceComponentRepository;
import bg.energo.phoenix.repository.product.price.priceComponentGroup.PriceComponentGroupRepository;
import bg.energo.phoenix.repository.product.product.*;
import bg.energo.phoenix.repository.product.term.terms.TermsRepository;
import bg.energo.phoenix.repository.product.term.termsGroups.TermsGroupsRepository;
import bg.energo.phoenix.repository.product.termination.terminationGroup.TerminationGroupRepository;
import bg.energo.phoenix.repository.product.termination.terminations.TerminationRepository;
import bg.energo.phoenix.repository.receivable.collectionChannel.CollectionChannelRepository;
import bg.energo.phoenix.repository.template.ContractTemplateRepository;
import bg.energo.phoenix.repository.template.ProductTemplateRepository;
import bg.energo.phoenix.security.PermissionService;
import bg.energo.phoenix.service.archivation.edms.EDMSFileArchivationService;
import bg.energo.phoenix.service.archivation.edms.FileArchivationService;
import bg.energo.phoenix.service.archivation.edms.config.EDMSAttributeProperties;
import bg.energo.phoenix.service.copy.group.CopyDomainWithVersionBaseService;
import bg.energo.phoenix.service.product.iap.advancedPaymentGroup.AdvancedPaymentGroupService;
import bg.energo.phoenix.service.product.iap.interimAdvancePayment.InterimAdvancePaymentService;
import bg.energo.phoenix.service.product.penalty.penalty.PenaltyService;
import bg.energo.phoenix.service.product.penalty.penaltyGroups.PenaltyGroupService;
import bg.energo.phoenix.service.product.price.priceComponent.PriceComponentService;
import bg.energo.phoenix.service.product.price.priceComponentGroup.PriceComponentGroupService;
import bg.energo.phoenix.service.product.term.terms.TermsService;
import bg.energo.phoenix.service.product.termination.terminationGroup.TerminationGroupService;
import bg.energo.phoenix.service.product.termination.terminations.TerminationsService;
import bg.energo.phoenix.util.ByteMultiPartFile;
import bg.energo.phoenix.util.archivation.EDMSArchivationConstraints;
import bg.energo.phoenix.util.epb.EPBChainedExceptionTriggerUtil;
import bg.energo.phoenix.util.epb.EPBListUtils;
import bg.energo.phoenix.util.epb.EPBStringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService implements CopyDomainWithVersionBaseService {
    private final TermsService termsService;
    private final ProductMapper productMapper;
    private final PenaltyService penaltyService;
    private final TermsRepository termsRepository;
    private final VatRateRepository vatRateRepository;
    private final SegmentRepository segmentRepository;
    private final PermissionService permissionService;
    private final ProductRepository productRepository;
    private final PenaltyRepository penaltyRepository;
    private final ProductFileService productFileService;
    private final CurrencyRepository currencyRepository;
    private final SalesAreaRepository salesAreaRepository;
    private final ProductTermsService productTermsService;
    private final PenaltyGroupService penaltyGroupService;
    private final TerminationsService terminationsService;
    private final EDMSAttributeProperties attributeProperties;
    private final TermsGroupsRepository termsGroupsRepository;
    private final TerminationRepository terminationRepository;
    private final ProductTypeRepository productTypeRepository;
    private final ProductFileRepository productFileRepository;
    private final PriceComponentService priceComponentService;
    private final PenaltyGroupRepository penaltyGroupRepository;
    private final SalesChannelRepository salesChannelRepository;
    private final GridOperatorRepository gridOperatorRepository;
    private final FileArchivationService fileArchivationService;
    private final EDMSFileArchivationService archivationService;
    private final ProductGroupsRepository productGroupsRepository;
    private final TerminationGroupService terminationGroupService;
    private final ProductSegmentRepository productSegmentRepository;
    private final ProductDetailsRepository productDetailsRepository;
    private final PriceComponentRepository priceComponentRepository;
    private final ProductTemplateRepository productTemplateRepository;
    private final TerminationGroupRepository terminationGroupRepository;
    private final PriceComponentGroupService priceComponentGroupService;
    private final ProductSalesAreaRepository productSalesAreaRepository;
    private final ContractTemplateRepository contractTemplateRepository;
    private final AdvancedPaymentGroupService advancedPaymentGroupService;
    private final CollectionChannelRepository collectionChannelRepository;
    private final InterimAdvancePaymentService interimAdvancePaymentService;
    private final PriceComponentGroupRepository priceComponentGroupRepository;
    private final ProductContractTermRepository productContractTermRepository;
    private final ProductGridOperatorRepository productGridOperatorRepository;
    private final ProductSalesChannelRepository productSalesChannelRepository;
    private final ProductRelatedEntitiesService productRelatedEntitiesService;
    private final ProductForBalancingRepository productForBalancingRepository;
    private final ElectricityPriceTypeRepository electricityPriceTypeRepository;
    private final InterimAdvancePaymentRepository interimAdvancePaymentRepository;
    private final ProductPriceComponentRepository productPriceComponentRepository;
    private final AdvancedPaymentGroupRepository interimAdvancePaymentGroupRepository;
    private final ProductAdditionalParamsRepository productAdditionalParamsRepository;
    private final ProductRelatedContractUpdateService productRelatedContractUpdateService;
    private final PriceComponentFormulaVariableRepository priceComponentFormulaVariableRepository;
    private final ProductDetailCollectionChannelsRepository productDetailCollectionChannelsRepository;

    /**
     * Creates a new product based on the provided request.
     *
     * @param request The product creation request containing the necessary data.
     * @return The response object containing the created product details.
     * @throws ClientException If an error occurs during the creation process.
     */
    @Transactional
    public Long create(ProductCreateRequest request) {
        validateCreatePermissions(Boolean.TRUE.equals(request.getIsIndividual()));

        List<String> exceptionMessages = new ArrayList<>();

        if (!BooleanUtils.isTrue(request.getIsIndividual())) {
            validateProductNameUniqueness(request.getName(), exceptionMessages);
        }
        validatePriceComponentFields(request, exceptionMessages);

        ProductGroups productGroups = validateProductGroup(request, exceptionMessages);
        ProductTypes productTypes = validateProductType(request, exceptionMessages);
        VatRate vatRate = validateVatRate(request, exceptionMessages);
        ElectricityPriceType electricityPriceType = validateElectricityPriceType(request, exceptionMessages);
        Currency currency = validateCurrency(request, exceptionMessages);
        ProductForBalancing productForConsumerBalancing = validateConsumerBalancingProductName(request, exceptionMessages);
        ProductForBalancing productForGeneratorBalancing = validateGeneratorBalancingProductName(request, exceptionMessages);

        // Terms or Terms Group will be present in request, not both
        Terms terms = validateTerms(request, exceptionMessages);
        TermsGroups termsGroups = validateTermsGroup(request, exceptionMessages);
        List<GridOperator> gridOperators = gridOperatorsFetchOption(request, exceptionMessages);
        List<SalesChannel> salesChannels = fetchSalesChannels(request, exceptionMessages);
        List<SalesArea> salesAreas = fetchSalesAreas(request, exceptionMessages);
        List<Segment> segments = fetchSegments(request, exceptionMessages);

        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(exceptionMessages, log);

        ProductDetails productDetails =
                createProductDetailsInstance(
                        request,
                        productGroups,
                        productTypes,
                        vatRate,
                        electricityPriceType,
                        currency,
                        terms,
                        termsGroups,
                        productForConsumerBalancing,
                        productForGeneratorBalancing);

        productTermsService.create(request.getProductTerms(), productDetails.getId());
        interimAdvancePaymentService.addInterimAdvancePaymentsToProduct(request.getInterimAdvancePayments(), productDetails, exceptionMessages);
        advancedPaymentGroupService.addInterimAdvancePaymentGroupsToProduct(request.getInterimAdvancePaymentGroups(), productDetails, exceptionMessages);
        penaltyService.addPenaltiesToProduct(request.getPenaltyIds(), productDetails, exceptionMessages);
        penaltyGroupService.addPenaltyGroupsToProduct(request.getPenaltyGroupIds(), productDetails, exceptionMessages);
        terminationsService.addTerminationsToProduct(request.getTerminationIds(), productDetails, exceptionMessages);
        terminationGroupService.addTerminationGroupsToProduct(request.getTerminationGroupIds(), productDetails, exceptionMessages);
        priceComponentService.addPriceComponentsToProduct(request.getPriceComponentIds(), productDetails, exceptionMessages);
        priceComponentGroupService.addPriceComponentGroupsToProduct(request.getPriceComponentGroupIds(), productDetails, exceptionMessages);
        productRelatedEntitiesService.addRelatedProductsAndServicesToProduct(request.getRelatedEntities(), productDetails, exceptionMessages);

        List<ProductFile> productFiles = searchProductFilesAndReturnList(request.getProductFileIds(), exceptionMessages);
        updatePaymentGuaranteeCurrencies(request, productDetails, false, exceptionMessages);

        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(exceptionMessages, log);

        createProductGridOperatorsAndAssignGridOperators(productDetails, gridOperators);
        createProductSalesChannelsAndAssignSalesChannel(productDetails, salesChannels);
        createProductSalesAreasAndAssignSalesAreas(productDetails, salesAreas);
        createProductSegmentsAndAssignSegments(productDetails, segments);
        createLinkedProductFiles(productDetails, productFiles);
        archiveFiles(productDetails);

        productDetailsRepository.save(productDetails);
        saveTemplates(request.getTemplateIds(), productDetails.getId(), exceptionMessages);
        createProductAdditionalParams(productDetails, request.getProductAdditionalParams());
        createConnectedCollectionChannels(request.getCollectionChannelIds(), productDetails.getId(), exceptionMessages);
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(exceptionMessages, log);

        return productDetails.getProduct().getId();
    }

    @Transactional
    public Long edit(Long id, ProductEditRequest request) {
        validateEditPermissions(Boolean.TRUE.equals(request.getIsIndividual()));

        List<String> exceptionMessages = new ArrayList<>();

        ProductDetails currentVersion = productDetailsRepository
                .findByProductIdAndVersion(id, request.getVersion())
                .orElseThrow(() ->
                        new DomainEntityNotFoundException("id-Active product with presented id and version not found;"));

        checkForProductNameUniqueness(currentVersion.getId(), request.getName(), currentVersion.getName(), exceptionMessages);
        checkForProductStatus(currentVersion);
        validatePriceComponentFields(request, exceptionMessages);
        checkIndividualProductUpdate(currentVersion, request, exceptionMessages);

        // Terms or Terms Group will be present in request, not both
        Terms terms = validateTermsOnUpdate(request, currentVersion, exceptionMessages);
        TermsGroups termsGroups = validateTermsGroup(request, exceptionMessages);

        ProductDetails updatedProductDetailsInstance = updateProductDetailsInstance(request, currentVersion);

        ProductGroups productGroup = validateProductGroupOnUpdate(request, currentVersion, exceptionMessages);
        ProductTypes productType = validateProductTypeOnUpdate(request, currentVersion, exceptionMessages);
        VatRate vatRate = validateVatRateOnUpdate(request, currentVersion, exceptionMessages);
        ElectricityPriceType electricityPriceType = validateElectricityPriceTypeOnUpdate(request, currentVersion, exceptionMessages);
        Currency currency = validateCurrencyOnUpdate(request, currentVersion, exceptionMessages);
        ProductForBalancing productForBalancingConsumer = validateConsumerBalancingProductName(request, exceptionMessages);
        ProductForBalancing productForBalancingGenerator = validateGeneratorBalancingProductName(request, exceptionMessages);

        if (request.getUpdateExistingVersion()) {
            checkForBoundObjects(currentVersion);
            productTermsService.edit(request.getProductTerms(), updatedProductDetailsInstance, exceptionMessages);
            interimAdvancePaymentService.updateProductIAPsForExistingVersion(request.getInterimAdvancePayments(), updatedProductDetailsInstance, exceptionMessages);
            advancedPaymentGroupService.updateProductIAPGroupsForExistingVersion(request.getInterimAdvancePaymentGroups(), updatedProductDetailsInstance, exceptionMessages);
            penaltyService.updateProductPenaltiesForExistingVersion(request.getPenaltyIds(), updatedProductDetailsInstance, exceptionMessages);
            penaltyGroupService.updateProductPenaltyGroupsForExistingVersion(request.getPenaltyGroupIds(), updatedProductDetailsInstance, exceptionMessages);
            terminationsService.updateProductTerminationsForExistingVersion(request.getTerminationIds(), updatedProductDetailsInstance, exceptionMessages);
            terminationGroupService.updateProductTerminationGroupsForExistingVersion(request.getTerminationGroupIds(), updatedProductDetailsInstance, exceptionMessages);
            priceComponentService.updateProductPriceComponentsForExistingVersion(request.getPriceComponentIds(), updatedProductDetailsInstance, exceptionMessages);
            priceComponentGroupService.updateProductPriceComponentGroupsForExistingVersion(request.getPriceComponentGroupIds(), updatedProductDetailsInstance, exceptionMessages);
            productRelatedEntitiesService.updateRelatedProductsAndServicesToProduct(request.getRelatedEntities(), updatedProductDetailsInstance, exceptionMessages);
            if (BooleanUtils.isNotTrue(request.getGlobalGridOperator())) {
                updateGridOperators(updatedProductDetailsInstance, currentVersion, request.getGridOperatorIds(), exceptionMessages);
            } else {
                List<ProductGridOperator> redundantGridOperators = productGridOperatorRepository.findAllByProductDetailsIdAndProductSubObjectStatus(currentVersion.getId(), ProductSubObjectStatus.ACTIVE);
                redundantGridOperators.forEach(go -> go.setProductSubObjectStatus(ProductSubObjectStatus.DELETED));
                productGridOperatorRepository.saveAll(redundantGridOperators);
            }
            updateLinkedAdditionalParams(request, currentVersion, exceptionMessages);
            updateConnectedCollectionChannels(request.getCollectionChannelIds(), currentVersion.getId(), exceptionMessages);
            updateProductFilesForExistingVersion(request, currentVersion, exceptionMessages);
        } else {
            productTermsService.create(request.getProductTerms(), updatedProductDetailsInstance.getId());
            interimAdvancePaymentService.updateProductIAPsForNewVersion(request.getInterimAdvancePayments(), updatedProductDetailsInstance, currentVersion, exceptionMessages);
            advancedPaymentGroupService.addInterimAdvancePaymentGroupsToProduct(request.getInterimAdvancePaymentGroups(), updatedProductDetailsInstance, exceptionMessages);
            penaltyService.updateProductPenaltiesForNewVersion(request.getPenaltyIds(), updatedProductDetailsInstance, currentVersion, exceptionMessages);
            penaltyGroupService.addPenaltyGroupsToProduct(request.getPenaltyGroupIds(), updatedProductDetailsInstance, exceptionMessages);
            terminationsService.updateProductTerminationsForNewVersion(request.getTerminationIds(), updatedProductDetailsInstance, currentVersion, exceptionMessages);
            terminationGroupService.addTerminationGroupsToProduct(request.getTerminationGroupIds(), updatedProductDetailsInstance, exceptionMessages);
            priceComponentService.updateProductPriceComponentsForNewVersion(request.getPriceComponentIds(), updatedProductDetailsInstance, currentVersion, exceptionMessages);
            priceComponentGroupService.addPriceComponentGroupsToProduct(request.getPriceComponentGroupIds(), updatedProductDetailsInstance, exceptionMessages);
            productRelatedEntitiesService.addRelatedProductsAndServicesToProduct(request.getRelatedEntities(), updatedProductDetailsInstance, exceptionMessages);
            if (BooleanUtils.isNotTrue(request.getGlobalGridOperator())) {
                updateGridOperators(updatedProductDetailsInstance, currentVersion, request.getGridOperatorIds(), exceptionMessages);
            }
            createProductAdditionalParams(updatedProductDetailsInstance, request.getProductAdditionalParams());
            createConnectedCollectionChannels(request.getCollectionChannelIds(), updatedProductDetailsInstance.getId(), exceptionMessages);
            updateProductFilesForNewVersion(request, updatedProductDetailsInstance, exceptionMessages);
        }
        updateTemplates(request.getTemplateIds(), updatedProductDetailsInstance.getId(), exceptionMessages);
        updatePaymentGuaranteeCurrencies(request, updatedProductDetailsInstance, true, exceptionMessages);
        updateSalesChannels(updatedProductDetailsInstance, currentVersion, request.getSalesChannelIds(), exceptionMessages);
        updateSalesAreas(updatedProductDetailsInstance, currentVersion, request.getSalesAreasIds(), exceptionMessages);
        updateSegments(updatedProductDetailsInstance, currentVersion, request.getSegmentIds(), exceptionMessages);
        archiveFiles(updatedProductDetailsInstance);

        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(exceptionMessages, log);

        productMapper.updateVersion(
                updatedProductDetailsInstance.getVersion(),
                updatedProductDetailsInstance,
                request,
                productGroup,
                productType,
                vatRate,
                electricityPriceType,
                currency,
                terms,
                termsGroups,
                productForBalancingConsumer,
                productForBalancingGenerator,
                request.getUpdateExistingVersion());

        if (!request.getUpdateExistingVersion()) {
            updateRelatedProductContracts(updatedProductDetailsInstance, request.getProductDetailIdsForUpdatingProductContracts(), exceptionMessages);
        }

        productDetailsRepository.save(updatedProductDetailsInstance);

        return currentVersion.getProduct().getId();
    }

    /**
     * Deletes a product by id if it passes all the validations
     *
     * @param id id of the product to be deleted
     * @return id of the deleted product
     */
    @Transactional
    public Long delete(Long id) {
        Product product = productRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Product with id not found;"));

        validateDeletePermission(StringUtils.isNotBlank(product.getCustomerIdentifier()));

        if (product.getProductStatus().equals(ProductStatus.DELETED)) {
            log.error("id-You can not delete already deleted product");
            throw new ClientException("id-You can not delete already deleted product", ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED);
        }

        if (productRepository.hasActiveConnectionToService(id)) {
            log.error("id-You can’t delete the product because it is connected to service;");
            throw new OperationNotAllowedException("id-You can’t delete the product because it is connected to service;");
        }

        if (productRepository.hasActiveConnectionToProduct(id)) {
            log.error("id-You can’t delete the product because it is connected to product;");
            throw new OperationNotAllowedException("id-You can’t delete the product because it is connected to product;");
        }

        if (productRepository.hasActiveConnectionToProductContract(id)) {
            log.error("id-You can’t delete the product because it is connected to Product Contract;");
            throw new OperationNotAllowedException("id-You can’t delete the product because it is connected to Product Contract;");
        }

        product.setProductStatus(ProductStatus.DELETED);
        productRepository.save(product);
        return id;
    }

    /**
     * Generates a preview of a product with the given ID and version.
     *
     * @param id      The ID of the product.
     * @param version The version of the product. If null, the latest version will be used.
     * @return The product detail response containing the preview.
     * @throws DomainEntityNotFoundException If the product or its details cannot be found.
     */
    public ProductDetailResponse preview(Long id, Long version) {
        Product product = productRepository
                .findByIdAndProductStatusIn(id, getProductStatuses())
                .orElseThrow(() -> new DomainEntityNotFoundException("id-product do not exists with this id"));

        validatePreviewPermissions(StringUtils.isNotBlank(product.getCustomerIdentifier()));

        ProductDetails details;
        if (version == null) {
            details = productDetailsRepository
                    .findLatestDetails(id, List.of(ProductDetailStatus.ACTIVE, ProductDetailStatus.INACTIVE), Sort.by(Sort.Direction.DESC, "version"))
                    .orElseThrow(() -> new DomainEntityNotFoundException("id-There is no details for given Product"));

        } else {
            details = productDetailsRepository
                    .findByProductIdAndVersionAndStatus(id, version, List.of(ProductDetailStatus.ACTIVE, ProductDetailStatus.INACTIVE))
                    .orElseThrow(() -> new DomainEntityNotFoundException("id-There is no details for given version id"));
        }

        ProductDetailResponse productDetailResponse = productMapper.mapToResponse(product, details);
        productDetailResponse.setTemplateResponses(productTemplateRepository.findForContract(details.getId(), LocalDate.now()));
        productDetailResponse.setCollectionChannels(productDetailCollectionChannelsRepository.getByDetailId(productDetailResponse.getDetailsId()));

        List<ProductVersion> productVersions = productDetailsRepository
                .findAllByProductIdAndProductDetailStatusIn(
                        id,
                        List.of(ProductDetailStatus.ACTIVE, ProductDetailStatus.INACTIVE)
                );
        productDetailResponse.setProductVersions(productVersions);

        Boolean globalGridOperator = details.getGlobalGridOperators();
        if (globalGridOperator == null || !globalGridOperator) {
            productDetailResponse.setGridOperatorResponses(productMapper.createGridOperatorResponse(details, List.of(NomenclatureItemStatus.ACTIVE, NomenclatureItemStatus.INACTIVE)));
        }

        Boolean globalSegment = details.getGlobalSegment();
        if (globalSegment == null || !globalSegment) {
            productDetailResponse.setSegments(details
                    .getSegments()
                    .stream()
                    .filter(productSegments -> productSegments.getProductSubObjectStatus().equals(ProductSubObjectStatus.ACTIVE))
                    .map(ProductSegments::getSegment)
                    .filter(segment -> List.of(NomenclatureItemStatus.ACTIVE, NomenclatureItemStatus.INACTIVE).contains(segment.getStatus()))
                    .map(SegmentResponse::new)
                    .toList());
        }

        Boolean globalSalesArea = details.getGlobalSalesArea();
        if (globalSalesArea == null || !globalSalesArea) {
            productDetailResponse.setSalesAreas(details
                    .getSalesAreas()
                    .stream()
                    .filter(productSalesArea -> productSalesArea.getProductSubObjectStatus().equals(ProductSubObjectStatus.ACTIVE))
                    .map(ProductSalesArea::getSalesArea)
                    .filter(salesArea -> List.of(NomenclatureItemStatus.ACTIVE, NomenclatureItemStatus.INACTIVE).contains(salesArea.getStatus()))
                    .map(SalesAreaResponse::new)
                    .toList());

        }

        Boolean globalSalesChannel = details.getGlobalSalesChannel();
        if (globalSalesChannel == null || !globalSalesChannel) {
            productDetailResponse.setSalesChannels(details
                    .getSalesChannels()
                    .stream()
                    .filter(productSalesChannel -> productSalesChannel.getProductSubObjectStatus().equals(ProductSubObjectStatus.ACTIVE))
                    .map(ProductSalesChannel::getSalesChannel)
                    .filter(salesChannel -> List.of(NomenclatureItemStatus.ACTIVE, NomenclatureItemStatus.INACTIVE).contains(salesChannel.getStatus()))
                    .map(SalesChannelResponse::new)
                    .toList());
        }
        productDetailResponse.setLocked(checkForBoundObjectsForPreview(details));
        return productDetailResponse;
    }

    /**
     * Retrieves a preview of a product's description based on its ID and version.
     * If the version is not specified, the latest available version is used.
     *
     * @param id      the ID of the product to retrieve the description for
     * @param version the specific version of the product details to preview (optional)
     * @return {@link ProductDescriptionResponse} containing the short and full descriptions of the product
     * @throws DomainEntityNotFoundException if the product or its details are not found
     */
    public ProductDescriptionResponse previewDescription(Long id, Long version) {
        Product product = productRepository
                .findByIdAndProductStatusIn(id, getProductStatuses())
                .orElseThrow(() -> new DomainEntityNotFoundException("id-product do not exists with this id"));

        validatePreviewPermissions(StringUtils.isNotBlank(product.getCustomerIdentifier()));

        ProductDetails details;
        if (version == null) {
            details = productDetailsRepository
                    .findLatestDetails(id, List.of(ProductDetailStatus.ACTIVE, ProductDetailStatus.INACTIVE), Sort.by(Sort.Direction.DESC, "version"))
                    .orElseThrow(() -> new DomainEntityNotFoundException("id-There is no details for given Product"));

        } else {
            details = productDetailsRepository
                    .findByProductIdAndVersionAndStatus(id, version, List.of(ProductDetailStatus.ACTIVE, ProductDetailStatus.INACTIVE))
                    .orElseThrow(() -> new DomainEntityNotFoundException("id-There is no details for given version id"));
        }

        return ProductDescriptionResponse
                .builder()
                .shortDescription(
                        details.getShortDescription()
                )
                .fullDescription(
                        details.getFullDescription()
                )
                .build();
    }

    /**
     * Copies a product by id and version, if present - otherwise copies the latest version
     *
     * @param id      id of the product to be copied
     * @param version version of the product to be copied
     * @return {@link ProductDetailCopyResponse}
     */
    @Transactional
    public ProductDetailCopyResponse copy(Long id, Long version) {
        Product product = productRepository.findByIdAndProductStatusIn(id, getProductStatuses())
                .orElseThrow(() -> new ClientException("id-product do not exists with this id", ErrorCode.DOMAIN_ENTITY_NOT_FOUND));

        validateCopyPermissions(StringUtils.isNotBlank(product.getCustomerIdentifier()));

        ProductDetails referenceProductDetails;
        if (version == null) {
            referenceProductDetails = productDetailsRepository
                    .findLatestDetails(id, List.of(ProductDetailStatus.ACTIVE, ProductDetailStatus.INACTIVE), Sort.by(Sort.Direction.DESC, "version"))
                    .orElseThrow(() -> new ClientException("id-There is no details for given Product", ErrorCode.DOMAIN_ENTITY_NOT_FOUND));

        } else {
            referenceProductDetails = productDetailsRepository
                    .findByProductIdAndVersionAndProductDetailStatusIn(id, version, List.of(ProductDetailStatus.ACTIVE, ProductDetailStatus.INACTIVE))
                    .orElseThrow(() -> new ClientException("id-There is no details for given version id", ErrorCode.DOMAIN_ENTITY_NOT_FOUND));
        }

        ProductDetailCopyResponse productDetailResponse = productMapper.mapToCopyResponse(product, referenceProductDetails);
        productDetailResponse.setCollectionChannels(productDetailCollectionChannelsRepository.getByDetailId(referenceProductDetails.getId()));

        Boolean globalGridOperator = referenceProductDetails.getGlobalGridOperators();
        if (BooleanUtils.isNotTrue(globalGridOperator)) {
            productDetailResponse.setGridOperatorResponses(productMapper.createGridOperatorResponse(referenceProductDetails, List.of(NomenclatureItemStatus.ACTIVE)));
        } else {
            productDetailResponse.setGlobalGridOperator(true);
        }

        Boolean globalSegment = referenceProductDetails.getGlobalSegment();
        if (globalSegment == null || !globalSegment) {
            productDetailResponse.setSegments(referenceProductDetails
                    .getSegments()
                    .stream()
                    .filter(x -> x.getProductSubObjectStatus().equals(ProductSubObjectStatus.ACTIVE))
                    .map(ProductSegments::getSegment)
                    .filter(x -> Objects.equals(NomenclatureItemStatus.ACTIVE, x.getStatus()))
                    .map(SegmentResponse::new).toList());
        }

        Boolean globalSalesArea = referenceProductDetails.getGlobalSalesArea();
        if (globalSalesArea == null || !globalSalesArea) {
            productDetailResponse.setSalesAreas(referenceProductDetails
                    .getSalesAreas()
                    .stream()
                    .filter(x -> x.getProductSubObjectStatus().equals(ProductSubObjectStatus.ACTIVE))
                    .map(ProductSalesArea::getSalesArea)
                    .filter(x -> Objects.equals(NomenclatureItemStatus.ACTIVE, x.getStatus()))
                    .map(SalesAreaResponse::new).toList());

        }

        Boolean globalSalesChannel = referenceProductDetails.getGlobalSalesChannel();
        if (globalSalesChannel == null || !globalSalesChannel) {
            productDetailResponse.setSalesChannels(referenceProductDetails
                    .getSalesChannels()
                    .stream()
                    .filter(x -> x.getProductSubObjectStatus().equals(ProductSubObjectStatus.ACTIVE))
                    .map(ProductSalesChannel::getSalesChannel)
                    .map(SalesChannelResponse::new)
                    .filter(x -> Objects.equals(NomenclatureItemStatus.ACTIVE, x.getStatus()))
                    .toList());

        }

        Terms terms = referenceProductDetails.getTerms();
        if (terms != null && terms.getStatus().equals(TermStatus.ACTIVE)) {
            Terms clonedTerm = termsService.copyTerms(terms);
            if (clonedTerm != null) {
                productDetailResponse.setTerms(new TermsShortResponse(clonedTerm));
            }
        }

        List<ProductContractTerms> productContractTerms = productContractTermRepository
                .findAllByProductDetailsIdAndStatusInOrderByCreateDate(referenceProductDetails.getId(), List.of(ProductSubObjectStatus.ACTIVE));
        productDetailResponse.setProductContractTerms(productContractTerms.stream()
                .map(ProductContractTermsResponse::new)
                .toList());

        productDetailResponse.setPenalties(productMapper.copyPenalties(referenceProductDetails.getPenalties()));

        productDetailResponse.setTerminations(productMapper.copyTerminations(referenceProductDetails.getTerminations()));

        productDetailResponse.setRelatedEntities(productMapper.createLinkedEntitiesShortResponse(referenceProductDetails.getLinkedProducts(), referenceProductDetails.getLinkedServices()));

        productDetailResponse.setInterimAdvancePayments(productMapper.copyInterimAdvancePayments(referenceProductDetails.getInterimAndAdvancePayments()));
        productDetailResponse.setTemplateResponses(productTemplateRepository.findForCopy(referenceProductDetails.getId(), LocalDate.now()));
        productDetailResponse.setPriceComponents(productMapper.copyPriceComponents(referenceProductDetails.getPriceComponents()));

        if (!referenceProductDetails.getProductAdditionalParams().isEmpty()) {
            productDetailResponse.setProductAdditionalParams(productMapper.mapAdditionalParams(referenceProductDetails));
        }

        return productDetailResponse;
    }


    public Page<ProductListResponse> list(ProductsListRequest request) {
        validateListingPermissions(request.getIndividualProduct());
        String excludeOldVersion = String.valueOf(Boolean.TRUE.equals(request.getExcludePastVersion()));
        Sort.Order order = new Sort.Order(checkColumnDirection(request), checkSortField(request));

        List<ProductStatus> standardProductStatusesByPermission = getStandardProductStatusesByPermission();
        List<ProductStatus> individualProductStatusesByPermission = getIndividualProductStatusesByPermission();

        return productRepository.filter(
                EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                getSearchBy(request),
                standardProductStatusesByPermission,
                individualProductStatusesByPermission,
                CollectionUtils.isNotEmpty(request.getStatus()) ? request.getStatus() : null,
                CollectionUtils.isNotEmpty(request.getFilterProductGroups()) ? request.getFilterProductGroups() : null,
                CollectionUtils.isNotEmpty(request.getFilterProductTypes()) ? request.getFilterProductTypes() : null,
                CollectionUtils.isNotEmpty(request.getFilterContractTerms()) ? request.getFilterContractTerms() : null,
                CollectionUtils.isNotEmpty(request.getFilterSalesChannels()) ? request.getFilterSalesChannels() : null,
                CollectionUtils.isNotEmpty(request.getFilterSegments()) ? request.getFilterSegments() : null,
                getPurposeOfConsumption(request),
                request.getGlobalSalesChannel(),
                request.getGlobalSegment(),
                getIndividualProduct(request.getIndividualProduct()).name(),
                excludeOldVersion,
                PageRequest.of(
                        request.getPage(),
                        request.getSize(), Sort.by(order)
                )
        );
    }

    private ProductListIndividualProduct getIndividualProduct(ProductListIndividualProduct individualProduct) {
        return Objects.requireNonNullElse(individualProduct, ProductListIndividualProduct.ALL);
    }

    @Override
    public CopyDomainWithVersion getGroupType() {
        return CopyDomainWithVersion.PRODUCTS;
    }

    @Override
    public Page<CopyDomainWithVersionBaseResponse> findGroups(CopyDomainWithVersionBaseRequest request) {
        log.debug("Fetching products for copy group with request: {}", request);
        return productRepository.findByCopyGroupBaseRequest(
                EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                getFilter(request),
                PageRequest.of(
                        request.getPage(),
                        request.getSize(),
                        Sort.by(Sort.Direction.DESC, "id")
                )
        );
    }

    private String getFilter(CopyDomainWithVersionBaseRequest request) {
        if (request.getFilter() != null &&
            request.getFilter().equals(CopyDomainWithVersionBasedRequestFilter.INDIVIDUAL_PRODUCT)) {
            return request.getFilter().getValue();
        } else return null;
    }

    @Override
    public List<CopyDomainWithVersionMiddleResponse> findGroupVersions(Long groupId) {
        return productDetailsRepository.findByCopyGroupBaseRequest(groupId);
    }

    /**
     * Returns a list of unique combined names of product contract terms for listing purposes
     *
     * @param page page number
     * @param size page size
     * @return page of unique combined names of product contract terms
     */
    public Page<ContractTermNameResponse> getContractTermNames(int page, int size, String prompt) {
        log.debug("Getting page of unique combined names of product contract terms");
        return productContractTermRepository.findDistinctNameByStatusIn(
                List.of(ProductSubObjectStatus.ACTIVE),
                EPBStringUtils.fromPromptToQueryParameter(prompt),
                PageRequest.of(page, size)
        );
    }

    public Page<AvailableProductRelatedEntitiesResponse> findAvailableProductsAndServices(AvailableProductRelatedEntitiesRequest request) {
        return productRepository.findAvailableProductsAndServices(
                EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                request.getExcludedId(), // ID of the product which is being edited
                request.getExcludedItemId(), // ID of the service/product which is selected in dropdown
                request.getExcludedItemType(), // Type of the service/product which is selected in dropdown
                PageRequest.of(request.getPage(), request.getSize())
        );
    }

    public Page<AvailableProductRelatedEntitiesResponse> findAvailableTerms(AvailableProductRelatedEntitiesRequest request) {
        return termsRepository.findAvailableTermsForProduct(EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()), PageRequest.of(request.getPage(), request.getSize()));
    }

    public Page<AvailableProductRelatedGroupEntityResponse> findAvailableGroupOfTerms(AvailableProductRelatedEntitiesRequest request) {
        return termsGroupsRepository.findAvailableTermGroupsForProduct(
                EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                PageRequest.of(request.getPage(), request.getSize())
        );
    }

    public Page<AvailableProductRelatedEntitiesResponse> findAvailableAdvancedPayments(AvailableProductRelatedEntitiesRequest request) {
        return interimAdvancePaymentRepository.findAvailableAdvancePaymentsForProduct(request.getPrompt(), PageRequest.of(request.getPage(), request.getSize()));
    }

    public Page<AvailableProductRelatedGroupEntityResponse> findAvailableAdvancedPaymentGroups
            (AvailableProductRelatedEntitiesRequest request) {
        return interimAdvancePaymentGroupRepository.findAvailableAdvancePaymentGroupsForProduct(
                EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                PageRequest.of(request.getPage(), request.getSize())
        );
    }

    public Page<AvailableProductRelatedEntitiesResponse> findAvailablePriceComponents(AvailableProductRelatedEntitiesRequest request) {
        return priceComponentRepository.findAvailablePriceComponentsForProduct(EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()), PageRequest.of(request.getPage(), request.getSize()));
    }

    public Page<AvailableProductRelatedGroupEntityResponse> findAvailablePriceComponentGroups(AvailableProductRelatedEntitiesRequest request) {
        return priceComponentGroupRepository.findAvailablePriceComponentGroupsForProduct(
                EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                PageRequest.of(request.getPage(), request.getSize())
        );
    }

    public Page<AvailableProductRelatedEntitiesResponse> findAvailableTerminations(AvailableProductRelatedEntitiesRequest request) {
        return terminationRepository.findAvailableTerminationsForProduct(EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()), PageRequest.of(request.getPage(), request.getSize()));
    }

    public Page<AvailableProductRelatedGroupEntityResponse> findAvailableTerminationGroups(AvailableProductRelatedEntitiesRequest request) {
        return terminationGroupRepository.findAvailableTerminationGroupsForProduct(
                EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                PageRequest.of(request.getPage(), request.getSize())
        );
    }

    public Page<AvailableProductRelatedEntitiesResponse> findAvailablePenalties(AvailableProductRelatedEntitiesRequest request) {
        return penaltyRepository.findAvailablePenaltiesForProduct(EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()), PageRequest.of(request.getPage(), request.getSize()));
    }

    public Page<AvailableProductRelatedGroupEntityResponse> findAvailablePenaltyGroups(AvailableProductRelatedEntitiesRequest request) {
        return penaltyGroupRepository.findAvailablePenaltyGroupsForProduct(
                EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                PageRequest.of(request.getPage(), request.getSize())
        );
    }


    private List<ProductStatus> getProductStatuses() {
        if (permissionService.permissionContextContainsPermissions(PermissionContextEnum.PRODUCTS, List.of(PermissionEnum.PRODUCT_VIEW_DELETED))) {
            return List.of(ProductStatus.ACTIVE, ProductStatus.DELETED);
        }
        return List.of(ProductStatus.ACTIVE);
    }

    private void createProductGridOperatorsAndAssignGridOperators(ProductDetails productDetails,
                                                                  List<GridOperator> gridOperators) {
        log.debug("Assigning product grid operators to product details");
        productDetails.setGridOperator(gridOperators
                .stream()
                .map(gridOperator ->
                        new ProductGridOperator(
                                null,
                                productDetails,
                                gridOperator,
                                ProductSubObjectStatus.ACTIVE
                        ))
                .collect(Collectors.toList())
        );
    }

    private void createProductSalesChannelsAndAssignSalesChannel(ProductDetails productDetails,
                                                                 List<SalesChannel> salesChannels) {
        log.debug("Assigning sales channels to product details");
        productDetails
                .setSalesChannels(salesChannels
                        .stream()
                        .map(salesChannel ->
                                new ProductSalesChannel(
                                        null,
                                        productDetails,
                                        salesChannel,
                                        ProductSubObjectStatus.ACTIVE
                                ))
                        .collect(Collectors.toList()));
    }

    private void createProductSalesAreasAndAssignSalesAreas(ProductDetails productDetails,
                                                            List<SalesArea> salesAreas) {
        log.debug("Assigning sales areas to product details");
        productDetails
                .setSalesAreas(salesAreas
                        .stream()
                        .map(salesArea ->
                                new ProductSalesArea(
                                        null,
                                        productDetails,
                                        salesArea,
                                        ProductSubObjectStatus.ACTIVE
                                ))
                        .collect(Collectors.toList()));
    }

    private void createProductSegmentsAndAssignSegments(ProductDetails productDetails,
                                                        List<Segment> segments) {
        log.debug("Assigning segments to product details");
        productDetails
                .setSegments(segments
                        .stream()
                        .map(segment ->
                                new ProductSegments(
                                        null,
                                        productDetails,
                                        segment,
                                        ProductSubObjectStatus.ACTIVE)
                        ).collect(Collectors.toList()));
    }

    private void createLinkedProductFiles(ProductDetails productDetails,
                                          List<ProductFile> productFiles) {
        log.debug("Assigning product files to product details");
        if (CollectionUtils.isNotEmpty(productFiles)) {
            for (ProductFile existingProductFile : productFiles) {
                existingProductFile.setProductDetailId(productDetails.getId());

                productFileRepository.save(existingProductFile);
            }
        }
    }

    private void archiveFiles(ProductDetails productDetails) {
        List<ProductFile> productFiles = productFileRepository.findActiveProductDetailFiles(productDetails.getId());
        if (CollectionUtils.isNotEmpty(productFiles)) {
            for (ProductFile productFile : productFiles) {
                try {
                    productFile.setNeedArchive(true);
                    productFile.setArchivationConstraints(EDMSArchivationConstraints.DOCUMENT_TYPE_PRODUCT_FILE);
                    productFile.setAttributes(
                            List.of(
                                    new Attribute(attributeProperties.getDocumentTypeGuid(), EDMSArchivationConstraints.DOCUMENT_TYPE_PRODUCT_FILE),
                                    new Attribute(attributeProperties.getDocumentNumberGuid(), productDetails.getName()),
                                    new Attribute(attributeProperties.getDocumentDateGuid(), LocalDateTime.now()),
                                    new Attribute(attributeProperties.getCustomerIdentifierGuid(), ""),
                                    new Attribute(attributeProperties.getCustomerNumberGuid(), ""),
                                    new Attribute(attributeProperties.getSignedGuid(), false)
                            )
                    );

                    fileArchivationService.archive(productFile);
                } catch (Exception e) {
                    log.error("Cannot archive file", e);
                }
            }
        }
    }

    /**
     * Assigns the specified collection channels to the given product details.
     *
     * @param collectionChannelIds The IDs of the collection channels to assign.
     * @param productDetailId      The ID of the product details to assign the collection channels to.
     * @param errorMessages        A list to store any error messages encountered during the assignment process.
     */
    private void createConnectedCollectionChannels(List<Long> collectionChannelIds, Long
            productDetailId, List<String> errorMessages) {
        if (CollectionUtils.isNotEmpty(collectionChannelIds)) {
            log.debug("Assigning collection channels to product details");
            List<ProductDetailCollectionChannels> productDetailCollectionChannelsList = new ArrayList<>();
            for (Long collectionChannelId : collectionChannelIds) {
                Optional<CollectionChannel> collectionChannel = collectionChannelRepository.findByIdAndStatuses(collectionChannelId, List.of(EntityStatus.ACTIVE));
                if (collectionChannel.isEmpty()) {
                    errorMessages.add("collectionChannelIds-Active Collection Channel with id: %s not found;".formatted(collectionChannelId));
                } else {
                    ProductDetailCollectionChannels productDetailCollectionChannels = new ProductDetailCollectionChannels();
                    productDetailCollectionChannels.setCollectionChannelId(collectionChannelId);
                    productDetailCollectionChannels.setProductDetailsId(productDetailId);
                    productDetailCollectionChannels.setStatus(EntityStatus.ACTIVE);
                    productDetailCollectionChannelsList.add(productDetailCollectionChannels);
                }
            }
            productDetailCollectionChannelsRepository.saveAll(productDetailCollectionChannelsList);
        }
    }

    /**
     * Updates the collection channels connected to a product detail.
     *
     * @param collectionChannelIds The list of collection channel IDs to connect to the product detail.
     * @param productDetailId      The ID of the product detail to update.
     * @param errorMessages        A list to store any error messages that occur during the update.
     */
    private void updateConnectedCollectionChannels(List<Long> collectionChannelIds, Long
            productDetailId, List<String> errorMessages) {
        List<Long> oldList = productDetailCollectionChannelsRepository.getCollectionChannelIdsByProductDetailId(productDetailId);
        List<Long> addedElementsFromList = EPBListUtils.getAddedElementsFromList(Objects.requireNonNullElse(oldList, new ArrayList<>()), Objects.requireNonNullElse(collectionChannelIds, new ArrayList<>()));
        if (CollectionUtils.isNotEmpty(addedElementsFromList)) {
            createConnectedCollectionChannels(addedElementsFromList, productDetailId, errorMessages);
        }
        List<Long> deletedElementsFromList = EPBListUtils.getDeletedElementsFromList(Objects.requireNonNullElse(oldList, new ArrayList<>()), Objects.requireNonNullElse(collectionChannelIds, new ArrayList<>()));
        if (CollectionUtils.isNotEmpty(deletedElementsFromList)) {
            productDetailCollectionChannelsRepository.deleteByCollectionChannelIdInAndProductDetailsId(deletedElementsFromList, productDetailId);
        }
    }

    private void createProductAdditionalParams(ProductDetails
                                                       productDetails, List<ProductsAdditionalParamsRequest> additionalParamsRequests) {
        if (additionalParamsRequests == null) {
            additionalParamsRequests = new ArrayList<>();
        }
        List<ProductAdditionalParams> productAdditionalParamsList = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            Long orderId = (long) i;
            ProductAdditionalParams productAdditionalParams = new ProductAdditionalParams(
                    null,
                    orderId,
                    productDetails.getId(),
                    null,
                    null);

            additionalParamsRequests
                    .stream()
                    .filter(adp -> adp.orderingId().equals(orderId))
                    .findFirst()
                    .ifPresent(adPar -> {
                        productAdditionalParams.setLabel(adPar.label());
                        productAdditionalParams.setValue(adPar.value());
                    });
            productAdditionalParamsList.add(productAdditionalParams);
        }
        productAdditionalParamsRepository.saveAll(productAdditionalParamsList);

    }

    private String getPurposeOfConsumption(ProductsListRequest request) {
        Set<PurposeOfConsumption> filterConsumptionTypes = request.getFilterConsumptionTypes();
        String purposeOfConsumption;
        purposeOfConsumption = "{";
        if (CollectionUtils.isNotEmpty(filterConsumptionTypes)) {
            purposeOfConsumption += filterConsumptionTypes.stream()
                    .map(PurposeOfConsumption::name)
                    .collect(Collectors.joining(","));
            purposeOfConsumption += "}";
        } else
            purposeOfConsumption = null;
        return purposeOfConsumption;
    }


    private List<ProductStatus> getStandardProductStatusesByPermission() {
        List<ProductStatus> statuses = new ArrayList<>();
        if (permissionService.permissionContextContainsPermissions(PermissionContextEnum.PRODUCTS, List.of(PermissionEnum.PRODUCT_VIEW_BASIC))) {
            statuses.add(ProductStatus.ACTIVE);
        }

        if (permissionService.permissionContextContainsPermissions(PermissionContextEnum.PRODUCTS, List.of(PermissionEnum.PRODUCT_VIEW_DELETED))) {
            statuses.add(ProductStatus.DELETED);
        }

        return statuses;
    }

    private List<ProductStatus> getIndividualProductStatusesByPermission() {
        List<ProductStatus> statuses = new ArrayList<>();
        if (permissionService.permissionContextContainsPermissions(PermissionContextEnum.PRODUCTS, List.of(PermissionEnum.INDIVIDUAL_PRODUCT_VIEW_BASIC))) {
            statuses.add(ProductStatus.ACTIVE);
        }

        if (permissionService.permissionContextContainsPermissions(PermissionContextEnum.PRODUCTS, List.of(PermissionEnum.INDIVIDUAL_PRODUCT_VIEW_DELETED))) {
            statuses.add(ProductStatus.DELETED);
        }

        return statuses;
    }

    private String getSearchBy(ProductsListRequest request) {
        ProductParameterFilterField searchBy = request.getSearchBy();
        return Objects.requireNonNullElse(searchBy, ProductParameterFilterField.ALL).getValue();
    }

    private Sort.Direction checkColumnDirection(ProductsListRequest request) {
        if (request.getSortDirection() == null) {
            return Sort.Direction.ASC;
        }
        return request.getSortDirection();
    }

    private String checkSortField(ProductsListRequest request) {
        if (request.getSortBy() == null) {
            return ProductListColumns.ID.getValue();
        }
        return request.getSortBy().getValue();
    }

    private void validateProductNameUniqueness(String name, List<String> exceptionMessages) {
        log.debug("Validating product name uniqueness");
        List<ProductDetails> productDetails = productDetailsRepository.findAllByProductNameAndStatusesForUniqueness(name, List.of(ProductStatus.ACTIVE));
        if (!productDetails.isEmpty()) {
            log.error("Product with name [{}] already exists, cannot create new one", name);
            exceptionMessages.add(String.format("basicSettings.name-Product with same name: [%s] already exists;", name));
        }
    }

    private void validatePriceComponentFields(BaseProductRequest request, List<String> exceptionMessages) {
        List<PriceComponent> priceComponentContext = new ArrayList<>();
        priceComponentContext
                .addAll(
                        priceComponentRepository
                                .findAllById(CollectionUtils.emptyIfNull(request.getPriceComponentIds()))
                );
        priceComponentContext
                .addAll(
                        priceComponentRepository
                                .findPriceComponentByPriceComponentGroupIds(CollectionUtils.emptyIfNull(request.getPriceComponentGroupIds()))
                );

        if (StringUtils.isBlank(request.getIncomeAccountNumber())) {
            List<String> erroredPriceComponents = new ArrayList<>();
            for (PriceComponent priceComponent : priceComponentContext) {
                if (StringUtils.isEmpty(priceComponent.getIncomeAccountNumber())) {
                    erroredPriceComponents.add("%s (%s)".formatted(priceComponent.getName(), priceComponent.getId()));
                }
            }
            if (!erroredPriceComponents.isEmpty()) {
                exceptionMessages.add("Income account number is mandatory in product as there are price components without income account number: %s;".formatted(String.join(", ", erroredPriceComponents)));
            }
        }

        if (StringUtils.isBlank(request.getCostCenterControllingOrder())) {
            List<String> erroredPriceComponents = new ArrayList<>();
            for (PriceComponent priceComponent : priceComponentContext) {
                if (StringUtils.isEmpty(priceComponent.getCostCenterControllingOrder())) {
                    erroredPriceComponents.add("%s (%s)".formatted(priceComponent.getName(), priceComponent.getId()));
                }
            }
            if (!erroredPriceComponents.isEmpty()) {
                exceptionMessages.add("Cost center and controlling order is mandatory in product as there are price components without income account number: %s;".formatted(String.join(", ", erroredPriceComponents)));
            }
        }
    }

    private void updatePaymentGuaranteeCurrencies(BaseProductRequest request, ProductDetails productDetails,
                                                  boolean onUpdate, List<String> exceptionMessages) {
        Long cashDepositCurrencyId = request.getCashDepositCurrencyId();
        if (cashDepositCurrencyId != null) {
            if (onUpdate) {
                Optional<Currency> currencyOptional = currencyRepository
                        .findByIdAndStatus(cashDepositCurrencyId, List.of(NomenclatureItemStatus.ACTIVE, NomenclatureItemStatus.INACTIVE));
                if (currencyOptional.isEmpty()) {
                    exceptionMessages.add("basicSettings.cashDepositCurrencyId-Currency with presented ID: %s not found;".formatted(cashDepositCurrencyId));
                } else {
                    Currency currency = currencyOptional.get();
                    if (currency.getStatus().equals(NomenclatureItemStatus.INACTIVE)) {
                        Currency cashDepositCurrency = productDetails.getCashDepositCurrency();
                        if (!cashDepositCurrency.getId().equals(currency.getId())) {
                            exceptionMessages.add("basicSettings.cashDepositCurrencyId-Active currency with presented ID: %s not found;".formatted(cashDepositCurrencyId));
                        } else {
                            productDetails.setCashDepositCurrency(currencyOptional.get());
                        }
                    } else {
                        productDetails.setCashDepositCurrency(currencyOptional.get());
                    }
                }
            } else {
                Optional<Currency> currencyOptional = currencyRepository
                        .findByIdAndStatus(cashDepositCurrencyId, List.of(NomenclatureItemStatus.ACTIVE));
                if (currencyOptional.isEmpty()) {
                    exceptionMessages.add("basicSettings.cashDepositCurrencyId-Currency with presented ID: %s not found;".formatted(cashDepositCurrencyId));
                } else {
                    productDetails.setCashDepositCurrency(currencyOptional.get());
                }
            }
        } else {
            productDetails.setCashDepositCurrency(null);
        }

        Long bankGuaranteeCurrencyId = request.getBankGuaranteeCurrencyId();
        if (bankGuaranteeCurrencyId != null) {
            if (onUpdate) {
                Optional<Currency> currencyOptional = currencyRepository
                        .findByIdAndStatus(bankGuaranteeCurrencyId, List.of(NomenclatureItemStatus.ACTIVE, NomenclatureItemStatus.INACTIVE));
                if (currencyOptional.isEmpty()) {
                    exceptionMessages.add("basicSettings.bankGuaranteeCurrencyId-Currency with presented ID: %s not found;".formatted(bankGuaranteeCurrencyId));
                } else {
                    Currency currency = currencyOptional.get();
                    if (currency.getStatus().equals(NomenclatureItemStatus.INACTIVE)) {
                        Currency bankGuaranteeCurrency = productDetails.getBankGuaranteeCurrency();
                        if (!bankGuaranteeCurrency.getId().equals(currency.getId())) {
                            exceptionMessages.add("basicSettings.bankGuaranteeCurrencyId-Active currency with presented ID: %s not found;".formatted(bankGuaranteeCurrencyId));
                        } else {
                            productDetails.setBankGuaranteeCurrency(currencyOptional.get());
                        }
                    } else {
                        productDetails.setBankGuaranteeCurrency(currencyOptional.get());
                    }
                }
            } else {
                Optional<Currency> currencyOptional = currencyRepository
                        .findByIdAndStatus(bankGuaranteeCurrencyId, List.of(NomenclatureItemStatus.ACTIVE));
                if (currencyOptional.isEmpty()) {
                    exceptionMessages.add("basicSettings.bankGuaranteeCurrencyId-Currency with presented ID: %s not found;".formatted(bankGuaranteeCurrencyId));
                } else {
                    productDetails.setBankGuaranteeCurrency(currencyOptional.get());
                }
            }
        } else {
            productDetails.setBankGuaranteeCurrency(null);
        }
    }

    private void checkIndividualProductUpdate(ProductDetails productDetails, ProductEditRequest
            request, List<String> exceptionMessages) {
        Product product = productDetails.getProduct();
        String requestCustomerIdentifier = request.getCustomerIdentifier();
        String productCustomerIdentifier = product.getCustomerIdentifier();


        if (StringUtils.isBlank(productCustomerIdentifier)) {
            if (StringUtils.isNotBlank(requestCustomerIdentifier)) {
                exceptionMessages.add("basicSettings.customerIdentifier-You cannot switch between individual and standard product;");
            }
        } else {
            if (!permissionService.permissionContextContainsPermissions(PermissionContextEnum.PRODUCTS, List.of(PermissionEnum.PRODUCT_EDIT_LOCKED))) {
                if (productRepository.hasActiveConnectionToProductContract(product.getId())) {
                    exceptionMessages.add("basicSettings.customerIdentifier-You cannot change customer identifier of individual product because it is connected to Product Contract;");
                }
            }
            if (StringUtils.isBlank(requestCustomerIdentifier)) {
                exceptionMessages.add("basicSettings.customerIdentifier-You cannot switch between individual and standard product;");
            }
        }

        if (BooleanUtils.isTrue(request.getIsIndividual())) {
            if (!request.getName().equals(productDetails.getName())) {
                exceptionMessages.add("basicSettings.name-You cannot change name of individual product;");
            }

            if (!request.getNameTransliterated().equals(productDetails.getNameTransliterated())) {
                exceptionMessages.add("basicSettings.nameTransliterated-You cannot change name transliterated of individual product;");
            }

            if (!request.getUpdateExistingVersion()) {
                exceptionMessages.add("basicSettings.updateExistingVersion-You cannot update individual product as new version;");
            }
        }
    }

    private List<GridOperator> fetchGridOperators(ProductCreateRequest request, List<String> exceptionMessages) {
        List<Long> gridOperatorIds = request.getGridOperatorIds();
        log.debug("Searching requested grid operators with id: [{}]", gridOperatorIds);

        List<Long> ids = gridOperatorIds.stream().toList();
        List<GridOperator> existingGridOperators = gridOperatorRepository.findByIdInAndStatusIn(ids, List.of(NomenclatureItemStatus.ACTIVE));
        List<Long> existingGridOperatorIds = existingGridOperators.stream().map(GridOperator::getId).toList();

        List<Long> nonMatchingGridOperatorIds = ids.stream().filter(id -> !existingGridOperatorIds.contains(id)).toList();
        if (CollectionUtils.isNotEmpty(nonMatchingGridOperatorIds)) {
            nonMatchingGridOperatorIds.forEach(id -> {
                log.error("Grid operator with presented id not found [{}]", id);
                exceptionMessages.add(String.format("basicSettings.gridOperatorIds-Grid Operator with presented id [%s] not found;", id));
            });
        }
        return existingGridOperators;
    }

    private List<SalesChannel> fetchSalesChannels(ProductCreateRequest request, List<String> exceptionMessages) {
        if (BooleanUtils.isFalse(request.getGlobalSalesChannel())) {
            log.debug("Searching requested sales channels with id: [{}]", request.getSalesChannelIds().toString());
            return searchSaleChannelsAndReturnList(request.getSalesChannelIds(), exceptionMessages);
        }
        return new ArrayList<>();
    }

    private List<SalesArea> fetchSalesAreas(ProductCreateRequest request, List<String> exceptionMessages) {
        if (BooleanUtils.isFalse(request.getGlobalSalesArea())) {
            log.debug("Searching requested sales areas with id: [{}]", request.getSalesAreasIds().toString());
            return searchSalesAreasAndReturnList(request.getSalesAreasIds(), exceptionMessages);
        }
        return new ArrayList<>();
    }

    private List<Segment> fetchSegments(ProductCreateRequest request, List<String> exceptionMessages) {
        if (BooleanUtils.isFalse(request.getGlobalSegment())) {
            log.debug("Searching requested segments with id: [{}]", request.getSegmentIds().toString());
            return searchSegmentsAndReturnList(request.getSegmentIds(), exceptionMessages);
        }
        return new ArrayList<>();
    }

    private List<GridOperator> gridOperatorsFetchOption(ProductCreateRequest
                                                                request, List<String> exceptionMessages) {
        if (BooleanUtils.isNotTrue(request.getGlobalGridOperator())) {
            log.debug("Searching requested grid operators with id: [{}]", request.getGridOperatorIds().toString());
            return fetchGridOperators(request, exceptionMessages);
        }
        return new ArrayList<>();
    }

    private List<SalesChannel> searchSaleChannelsAndReturnList
            (List<Long> idsSet, List<String> exceptionMessages) {
        List<Long> ids = idsSet.stream().toList();
        List<SalesChannel> existingSalesChannels = salesChannelRepository.findByIdInAndStatusIn(ids, List.of(NomenclatureItemStatus.ACTIVE));
        List<Long> existingSalesChannelIds = existingSalesChannels.stream().map(SalesChannel::getId).toList();

        List<Long> nonMatchingSalesChannelIds = ids.stream().filter(id -> !existingSalesChannelIds.contains(id)).toList();
        if (CollectionUtils.isNotEmpty(nonMatchingSalesChannelIds)) {
            nonMatchingSalesChannelIds.forEach(id -> {
                log.error("Sales Channel with presented id not found [{}]", id);
                exceptionMessages.add(String.format("basicSettings.salesChannelIds-Sales Channel with presented id [%s] not found;", id));
            });
        }
        return existingSalesChannels;
    }

    private List<SalesArea> searchSalesAreasAndReturnList(List<Long> idsSet, List<String> exceptionMessages) {
        List<Long> ids = idsSet.stream().toList();
        List<SalesArea> existingSalesAreas = salesAreaRepository.findByIdInAndStatusIn(ids.stream().toList(), List.of(NomenclatureItemStatus.ACTIVE));
        List<Long> existingSalesAreaIds = existingSalesAreas.stream().map(SalesArea::getId).toList();

        List<Long> nonMatchingSalesAreaIds = ids.stream().filter(id -> !existingSalesAreaIds.contains(id)).toList();
        if (CollectionUtils.isNotEmpty(nonMatchingSalesAreaIds)) {
            nonMatchingSalesAreaIds.forEach(id -> {

                log.error("Sales Area with presented id not found [{}]", id);
                exceptionMessages.add(String.format("basicSettings.salesAreasIds-Sales Area with presented id [%s] not found;", id));
            });
        }
        return existingSalesAreas;
    }

    private List<Segment> searchSegmentsAndReturnList(List<Long> idsSet, List<String> exceptionMessages) {
        List<Long> ids = idsSet.stream().toList();
        List<Segment> existingSegments = segmentRepository.findByIdInAndStatusIn(ids, List.of(NomenclatureItemStatus.ACTIVE));
        List<Long> existingSegmentIds = existingSegments.stream().map(Segment::getId).toList();

        List<Long> nonMatchingSegmentIds = ids.stream().filter(id -> !existingSegmentIds.contains(id)).toList();
        if (CollectionUtils.isNotEmpty(nonMatchingSegmentIds)) {
            nonMatchingSegmentIds.forEach(id -> {
                log.error("Segment with presented id not found [{}]", id);
                exceptionMessages.add(String.format("basicSettings.segmentIds-Segment with presented id [%s] not found;", id));
            });
        }
        return existingSegments;
    }

    private List<ProductFile> searchProductFilesAndReturnList(List<Long> idsSet,
                                                              List<String> exceptionMessages) {
        log.debug("Searching product files with ids: [{}]", idsSet);
        if (CollectionUtils.isNotEmpty(idsSet)) {
            List<Long> ids = idsSet.stream().toList();
            List<ProductFile> existingProductFiles = productFileRepository.findByIdInAndStatusIn(ids, List.of(EntityStatus.ACTIVE));
            List<Long> existingProductFileIds = existingProductFiles.stream().map(ProductFile::getId).toList();

            List<Long> nonMatchingProductFileIds = ids.stream().filter(id -> !existingProductFileIds.contains(id)).toList();
            if (CollectionUtils.isNotEmpty(nonMatchingProductFileIds)) {
                nonMatchingProductFileIds.forEach(id -> {
                    log.error("Product File with presented id not found [{}]", id);
                    exceptionMessages.add(String.format("productFileIds[%s]-Product File with presented id [%s] not found;", ids.indexOf(id), id));
                });
            }

            return existingProductFiles;
        }
        return new ArrayList<>();
    }

    public void updateGridOperators(ProductDetails updatedProductDetails,
                                    ProductDetails currentProductDetails,
                                    List<Long> gridOperatorIds,
                                    List<String> exceptionMessages) {
        log.debug("Updating grid operators");
        List<Long> ids = gridOperatorIds == null ? new ArrayList<>() : gridOperatorIds.stream().toList();
        List<GridOperator> gridOperators = gridOperatorRepository.findByIdInAndStatusIn(ids, List.of(NomenclatureItemStatus.ACTIVE, NomenclatureItemStatus.INACTIVE));
        List<Long> existingGridOperatorIds = gridOperators.stream().map(GridOperator::getId).toList();

        List<Long> nonMatchingGridOperatorIds = ids.stream().filter(id -> !existingGridOperatorIds.contains(id)).toList();
        if (CollectionUtils.isNotEmpty(nonMatchingGridOperatorIds)) {
            nonMatchingGridOperatorIds.forEach(id -> {
                log.error("Grid operator with presented id not found [{}]", id);
                exceptionMessages.add(String.format("basicSettings.gridOperatorIds-Grid Operator with presented id [%s] not found;", id));
            });
        }

        if (!exceptionMessages.isEmpty()) {
            return;
        }

        List<ProductGridOperator> assignedProductGridOperators =
                updatedProductDetails
                        .getGridOperator()
                        .stream()
                        .filter(productGridOperator -> productGridOperator.getProductSubObjectStatus().equals(ProductSubObjectStatus.ACTIVE))
                        .toList();

        assignedProductGridOperators.forEach(operator -> {
            if (!gridOperators.contains(operator.getGridOperator())) {
                operator.setProductSubObjectStatus(ProductSubObjectStatus.DELETED);
            }
        });

        List<Long> currentGridOperatorIds = currentProductDetails
                .getGridOperator()
                .stream()
                .map(ProductGridOperator::getGridOperator)
                .map(GridOperator::getId)
                .toList();

        gridOperators.stream()
                .filter(gridOperator -> gridOperator.getStatus().equals(NomenclatureItemStatus.INACTIVE))
                .filter(gridOperator -> !currentGridOperatorIds.contains(gridOperator.getId()))
                .forEach(gridOperator -> exceptionMessages.add("basicSettings.gridOperatorIds-You can't assign INACTIVE entity to product;"));

        if (!exceptionMessages.isEmpty()) {
            return;
        }

        List<ProductGridOperator> newGridOperators = new ArrayList<>();
        gridOperators.stream()
                .filter(gridOperator -> assignedProductGridOperators.stream()
                        .noneMatch(operator -> operator.getGridOperator().getId().equals(gridOperator.getId())))
                .forEach(gridOperator -> newGridOperators.add(new ProductGridOperator(
                        null,
                        updatedProductDetails,
                        gridOperator,
                        ProductSubObjectStatus.ACTIVE
                )));
        productGridOperatorRepository.saveAll(newGridOperators);
    }

    public void updateSalesChannels(ProductDetails productDetails, ProductDetails
            currentProductDetails, List<Long> salesChannelIds, List<String> exceptionMessages) {
        log.debug("Updating sales channels");
        List<Long> ids = salesChannelIds == null ? new ArrayList<>() : salesChannelIds.stream().toList();
        List<SalesChannel> salesChannels = salesChannelRepository.findByIdInAndStatusIn(ids, List.of(NomenclatureItemStatus.ACTIVE, NomenclatureItemStatus.INACTIVE));
        List<Long> existingSalesChannelIds = salesChannels.stream().map(SalesChannel::getId).toList();

        List<Long> nonMatchingSalesChannelIds = ids.stream().filter(id -> !existingSalesChannelIds.contains(id)).toList();
        if (CollectionUtils.isNotEmpty(nonMatchingSalesChannelIds)) {
            nonMatchingSalesChannelIds.forEach(id -> {
                log.error("Sales Channel with presented id not found [{}]", id);
                exceptionMessages.add(String.format("basicSettings.salesChannelIds-Sales Channel with presented id [%s] not found;", id));
            });
        }

        if (!exceptionMessages.isEmpty()) {
            return;
        }

        List<ProductSalesChannel> assignedProductSalesChannels =
                productDetails
                        .getSalesChannels()
                        .stream()
                        .filter(productSalesChannel -> productSalesChannel.getProductSubObjectStatus().equals(ProductSubObjectStatus.ACTIVE))
                        .toList();

        assignedProductSalesChannels.forEach(salesChannel -> {
            if (!salesChannels.contains(salesChannel.getSalesChannel())) {
                salesChannel.setProductSubObjectStatus(ProductSubObjectStatus.DELETED);
            }
        });

        List<Long> currentSalesChannelIds = currentProductDetails.getSalesChannels().stream().map(ProductSalesChannel::getSalesChannel).map(SalesChannel::getId).toList();

        salesChannels.stream()
                .filter(salesChannel -> salesChannel.getStatus().equals(NomenclatureItemStatus.INACTIVE))
                .filter(salesChannel ->
                        !currentSalesChannelIds.contains(salesChannel.getId()))
                .forEach(salesChannel -> exceptionMessages.add("basicSettings.salesChannelIds-You can't assign INACTIVE entity to product;"));

        if (!exceptionMessages.isEmpty()) {
            return;
        }

        List<ProductSalesChannel> newProductSalesChannels = new ArrayList<>();
        salesChannels.stream()
                .filter(salesChannel -> assignedProductSalesChannels.stream()
                        .noneMatch(productSalesChannel -> productSalesChannel.getSalesChannel().getId().equals(salesChannel.getId())))
                .forEach(salesChannel -> newProductSalesChannels.add(new ProductSalesChannel(
                        null,
                        productDetails,
                        salesChannel,
                        ProductSubObjectStatus.ACTIVE
                )));
        productSalesChannelRepository.saveAll(newProductSalesChannels);
    }

    public void updateSalesAreas(ProductDetails productDetails, ProductDetails
            currentProductDetails, List<Long> salesAreaIds, List<String> exceptionMessages) {
        log.debug("Updating sales areas");
        List<Long> ids = salesAreaIds == null ? new ArrayList<>() : salesAreaIds.stream().toList();
        List<SalesArea> salesAreas = salesAreaRepository.findByIdInAndStatusIn(ids, List.of(NomenclatureItemStatus.ACTIVE, NomenclatureItemStatus.INACTIVE));
        List<Long> existingSalesAreaIds = salesAreas.stream().map(SalesArea::getId).toList();

        List<Long> nonMatchingSalesAreaIds = ids.stream().filter(id -> !existingSalesAreaIds.contains(id)).toList();
        if (CollectionUtils.isNotEmpty(nonMatchingSalesAreaIds)) {
            nonMatchingSalesAreaIds.forEach(id -> {
                log.error("Sales Area with presented id not found [{}]", id);
                exceptionMessages.add(String.format("basicSettings.salesAreasIds-Sales Area with presented id [%s] not found;", id));
            });
        }

        if (!exceptionMessages.isEmpty()) {
            return;
        }

        List<ProductSalesArea> assignedSalesAreas =
                productDetails
                        .getSalesAreas()
                        .stream()
                        .filter(productSalesArea -> productSalesArea.getProductSubObjectStatus().equals(ProductSubObjectStatus.ACTIVE))
                        .toList();

        assignedSalesAreas.forEach(salesArea -> {
            if (!salesAreas.contains(salesArea.getSalesArea())) {
                salesArea.setProductSubObjectStatus(ProductSubObjectStatus.DELETED);
            }
        });

        List<Long> currentSalesAreaIds = currentProductDetails.getSalesAreas().stream().map(ProductSalesArea::getSalesArea).map(SalesArea::getId).toList();

        salesAreas.stream()
                .filter(salesArea -> salesArea.getStatus().equals(NomenclatureItemStatus.INACTIVE))
                .filter(salesArea ->
                        !currentSalesAreaIds.contains(salesArea.getId()))
                .forEach(salesArea -> exceptionMessages.add("basicSettings.salesAreasIds-You can't assign INACTIVE entity to product;"));

        if (!exceptionMessages.isEmpty()) {
            return;
        }

        List<ProductSalesArea> newProductSalesAreas = new ArrayList<>();
        salesAreas.stream()
                .filter(salesArea -> assignedSalesAreas.stream()
                        .noneMatch(productSalesArea -> productSalesArea.getSalesArea().getId().equals(salesArea.getId())))
                .forEach(salesArea -> newProductSalesAreas.add(new ProductSalesArea(
                        null,
                        productDetails,
                        salesArea,
                        ProductSubObjectStatus.ACTIVE
                )));
        productSalesAreaRepository.saveAll(newProductSalesAreas);
    }

    public void updateSegments(ProductDetails productDetails, ProductDetails
            currentProductDetails, List<Long> segmentIds, List<String> exceptionMessages) {
        log.debug("Updating segments");
        List<Long> ids = segmentIds == null ? new ArrayList<>() : segmentIds.stream().toList();
        List<Segment> segments = segmentRepository.findByIdInAndStatusIn(ids, List.of(NomenclatureItemStatus.ACTIVE, NomenclatureItemStatus.INACTIVE));
        List<Long> existingSegmentIds = segments.stream().map(Segment::getId).toList();

        List<Long> nonMatchingSegmentIds = ids.stream().filter(id -> !existingSegmentIds.contains(id)).toList();
        if (CollectionUtils.isNotEmpty(nonMatchingSegmentIds)) {
            nonMatchingSegmentIds.forEach(id -> {
                log.error("Segment with presented id not found [{}]", id);
                exceptionMessages.add(String.format("basicSettings.segmentIds-Segment with presented id [%s] not found;", id));
            });
        }

        if (!exceptionMessages.isEmpty()) {
            return;
        }

        List<ProductSegments> assignedSegments =
                productDetails
                        .getSegments()
                        .stream()
                        .filter(productSegments -> productSegments.getProductSubObjectStatus().equals(ProductSubObjectStatus.ACTIVE))
                        .toList();

        assignedSegments.forEach(productSegments -> {
            if (!segments.contains(productSegments.getSegment())) {
                productSegments.setProductSubObjectStatus(ProductSubObjectStatus.DELETED);
            }
        });

        List<Long> currentSegmentIds = currentProductDetails.getSegments().stream().map(ProductSegments::getSegment).map(Segment::getId).toList();

        segments.stream()
                .filter(segment -> segment.getStatus().equals(NomenclatureItemStatus.INACTIVE))
                .filter(segment ->
                        !currentSegmentIds.contains(segment.getId()))
                .forEach(segment -> exceptionMessages.add("basicSettings.segmentIds-You can't assign INACTIVE entity to product;"));

        if (!exceptionMessages.isEmpty()) {
            return;
        }

        List<ProductSegments> newProductSegments = new ArrayList<>();
        segments.stream()
                .filter(segment -> assignedSegments.stream()
                        .noneMatch(productSalesArea -> productSalesArea.getSegment().getId().equals(segment.getId())))
                .forEach(segment -> newProductSegments.add(new ProductSegments(
                        null,
                        productDetails,
                        segment,
                        ProductSubObjectStatus.ACTIVE
                )));
        productSegmentRepository.saveAll(newProductSegments);
    }

    public void updateLinkedAdditionalParams(BaseProductRequest request, ProductDetails
            productDetails, List<String> exceptionMessages) {
        if (CollectionUtils.isNotEmpty(exceptionMessages)) {
            return;
        }
        List<ProductsAdditionalParamsRequest> productAdditionalParamsFromReq = request.getProductAdditionalParams();
        if (productAdditionalParamsFromReq == null) {
            productAdditionalParamsFromReq = new ArrayList<>();
        }

        List<ProductAdditionalParams> productAdditionalParamsFromDb =
                productAdditionalParamsRepository.findProductAdditionalParamsByProductDetailId(productDetails.getId());

        for (int i = 0; i < 12; i++) {
            Long orderId = (long) i;
            Optional<ProductAdditionalParams> paramFromDbOptional = productAdditionalParamsFromDb
                    .stream()
                    .filter(db -> db.getOrderingId().equals(orderId))
                    .findFirst();

            if (paramFromDbOptional.isEmpty()) {
                return;
            }
            ProductAdditionalParams paramFromDb = paramFromDbOptional.get();
            Optional<ProductsAdditionalParamsRequest> productsAdditionalParamsRequestOptional = productAdditionalParamsFromReq
                    .stream()
                    .filter(adp -> adp.orderingId().equals(orderId))
                    .findFirst();
            if (productsAdditionalParamsRequestOptional.isPresent()) {
                ProductsAdditionalParamsRequest productsAdditionalParamsRequest = productsAdditionalParamsRequestOptional.get();
                paramFromDb.setLabel(productsAdditionalParamsRequest.label());
                paramFromDb.setValue(productsAdditionalParamsRequest.value());
            } else {
                paramFromDb.setLabel(null);
                paramFromDb.setValue(null);
            }
            productAdditionalParamsRepository.save(paramFromDb);
        }

    }

    private ProductGroups validateProductGroup(ProductCreateRequest request, List<String> exceptionMessages) {
        if (BooleanUtils.isTrue(request.getIsIndividual())) {
            if (request.getProductGroupId() == null) {
                return null;
            }
        }

        Optional<ProductGroups> productGroupsOptional = productGroupsRepository
                .findByIdAndStatus(request.getProductGroupId(),
                        List.of(NomenclatureItemStatus.ACTIVE));
        if (productGroupsOptional.isPresent()) {
            return productGroupsOptional.get();
        } else {
            log.error("Product group with presented id [{}] not found", request.getProductGroupId());
            exceptionMessages.add(String.format("basicSettings.productGroupId-Product Group with presented ID [%s] not found;", request.getProductGroupId()));
        }

        return null;
    }

    private ProductTypes validateProductType(ProductCreateRequest request, List<String> exceptionMessages) {
        Optional<ProductTypes> productTypeOptional = productTypeRepository
                .findByIdAndStatus(request.getProductTypeId(),
                        List.of(NomenclatureItemStatus.ACTIVE));
        if (productTypeOptional.isPresent()) {
            return productTypeOptional.get();
        } else {
            log.error("Product type with presented id [{}] not found", request.getProductTypeId());
            exceptionMessages.add(String.format("basicSettings.productTypeId-Product Type with presented ID [%s] not found;", request.getProductTypeId()));
        }
        return null;
    }

    private VatRate validateVatRate(ProductCreateRequest request, List<String> exceptionMessages) {
        if (!request.getGlobalVatRate()) {
            Optional<VatRate> vatRateOptional = vatRateRepository
                    .findByIdAndStatus(request.getVatRateId(),
                            List.of(NomenclatureItemStatus.ACTIVE));
            if (vatRateOptional.isPresent()) {
                return vatRateOptional.get();
            } else {
                log.debug("Vat rate with presented id [{}] not found", request.getVatRateId());
                exceptionMessages.add(String.format("basicSettings.vatRateId-Vat Rate with presented ID [%s] not found;", request.getVatRateId()));
            }
        }
        return null;
    }

    private ElectricityPriceType validateElectricityPriceType(ProductCreateRequest
                                                                      request, List<String> exceptionMessages) {
        Optional<ElectricityPriceType> electricityPriceTypeOptional = electricityPriceTypeRepository
                .findByIdAndStatuses(request.getElectricityPriceTypeId(),
                        List.of(NomenclatureItemStatus.ACTIVE));
        if (electricityPriceTypeOptional.isPresent()) {
            return electricityPriceTypeOptional.get();
        } else {
            log.debug("Electricity price type with presented id [{}] not found", request.getElectricityPriceTypeId());
            exceptionMessages.add(String.format("priceSettings.electricityPriceTypeId-Electricity Price Type with presented ID [%s] not found;", request.getElectricityPriceTypeId()));
        }
        return null;
    }

    private Currency validateCurrency(ProductCreateRequest request, List<String> exceptionMessages) {
        if (request.getCurrencyId() != null) {
            Optional<Currency> currencyOptional = currencyRepository
                    .findByIdAndStatus(request.getCurrencyId(),
                            List.of(NomenclatureItemStatus.ACTIVE));
            if (currencyOptional.isPresent()) {
                return currencyOptional.get();
            } else {
                log.debug("Currency with presented id [{}] not found", request.getCurrencyId());
                exceptionMessages.add(String.format("priceSettings.currencyId-Currency with presented ID [%s] not found;", request.getCurrencyId()));
            }
        }
        return null;
    }

    private Terms validateTerms(ProductCreateRequest request, List<String> exceptionMessages) {
        if (request.getTermId() != null) {
            Optional<Terms> termsOptional = termsRepository
                    .findByIdAndStatusIn(request.getTermId(),
                            List.of(TermStatus.ACTIVE));
            if (termsOptional.isPresent()) {
                Terms existingTerm = termsOptional.get();
                List<Long> allAvailableTermIdsForProduct = termsRepository.findAllAvailableTermIdsForProduct();
                if (!allAvailableTermIdsForProduct.contains(existingTerm.getId())) {
                    exceptionMessages.add(String.format("basicSettings.termId-Term with id [%s] is not available for this product;", existingTerm.getId()));
                }
                return existingTerm;
            } else {
                log.debug("Terms with presented id [{}] not found", request.getTermId());
                exceptionMessages.add(String.format("basicSettings.termId-Term with presented ID [%s] not found;", request.getTermId()));
            }
        }
        return null;
    }

    private TermsGroups validateTermsGroup(BaseProductRequest request, List<String> exceptionMessages) {
        if (request.getTermGroupId() != null) {
            Optional<TermsGroups> termsGroupsOptional = termsGroupsRepository
                    .findByIdAndStatusIn(request.getTermGroupId(),
                            List.of(TermGroupStatus.ACTIVE));
            if (termsGroupsOptional.isPresent()) {
                return termsGroupsOptional.get();
            } else {
                log.debug("Terms Group with presented id [{}] not found", request.getTermGroupId());
                exceptionMessages.add(String.format("basicSettings.termGroupId-Term Group with presented ID [%s] not found;", request.getTermGroupId()));
            }
        }
        return null;
    }

    private ProductDetails updateProductDetailsInstance(ProductEditRequest request, ProductDetails productDetails) {
        if (request.getUpdateExistingVersion()) {
            return productDetails;
        } else {
            long lastDetailVersion = productDetailsRepository.findLastDetailVersion(productDetails.getProduct().getId());
            ProductDetails updatedProductDetailsInstance = productMapper.newInstance(++lastDetailVersion, productDetails);
            productDetailsRepository.saveAndFlush(updatedProductDetailsInstance);
            return updatedProductDetailsInstance;
        }
    }

    private ProductGroups validateProductGroupOnUpdate(ProductEditRequest request, ProductDetails
            productDetails, List<String> exceptionMessages) {
        if (BooleanUtils.isTrue(request.getIsIndividual())) {
            if (request.getProductGroupId() == null) {
                return null;
            }
        }

        Optional<ProductGroups> productGroupsOptional = productGroupsRepository
                .findByIdAndStatus(request.getProductGroupId(),
                        List.of(NomenclatureItemStatus.ACTIVE, NomenclatureItemStatus.INACTIVE));
        if (productGroupsOptional.isPresent()) {
            ProductGroups requestedProductGroup = productGroupsOptional.get();
            if (requestedProductGroup.getStatus().equals(NomenclatureItemStatus.INACTIVE)) {
                if (!Objects.equals(productDetails.getProductGroups().getId(), requestedProductGroup.getId())) {
                    exceptionMessages.add("basicSettings.productGroupId-You can't assign INACTIVE entity to product;");
                } else {
                    return requestedProductGroup;
                }
            } else {
                return requestedProductGroup;
            }
        } else {
            log.error("Product group with presented id [{}] not found", request.getProductGroupId());
            exceptionMessages.add(String.format("basicSettings.productGroupId-Product Group with presented ID [%s] not found;", request.getProductGroupId()));
        }
        return null;
    }

    private ProductTypes validateProductTypeOnUpdate(ProductEditRequest request, ProductDetails
            productDetails, List<String> exceptionMessages) {
        Optional<ProductTypes> productTypeOptional = productTypeRepository
                .findByIdAndStatus(request.getProductTypeId(),
                        List.of(NomenclatureItemStatus.ACTIVE, NomenclatureItemStatus.INACTIVE));
        if (productTypeOptional.isPresent()) {
            ProductTypes requestedProductType = productTypeOptional.get();
            if (requestedProductType.getStatus().equals(NomenclatureItemStatus.INACTIVE)) {
                if (!Objects.equals(productDetails.getProductType().getId(), requestedProductType.getId())) {
                    exceptionMessages.add("basicSettings.productTypeId-You can't assign INACTIVE entity to product;");
                } else {
                    return requestedProductType;
                }
            } else {
                return requestedProductType;
            }
        } else {
            log.error("Product type with presented id [{}] not found", request.getProductTypeId());
            exceptionMessages.add(String.format("basicSettings.productTypeId-Product Type with presented ID [%s] not found;", request.getProductTypeId()));
        }
        return null;
    }

    private VatRate validateVatRateOnUpdate(ProductEditRequest request, ProductDetails
            productDetails, List<String> exceptionMessages) {
        if (!request.getGlobalVatRate()) {
            Optional<VatRate> vatRateOptional = vatRateRepository
                    .findByIdAndStatus(request.getVatRateId(),
                            List.of(NomenclatureItemStatus.ACTIVE, NomenclatureItemStatus.INACTIVE));
            if (vatRateOptional.isPresent()) {
                VatRate requestedVatRate = vatRateOptional.get();
                if (requestedVatRate.getStatus().equals(NomenclatureItemStatus.INACTIVE)) {
                    if (!Objects.equals(productDetails.getVatRate().getId(), requestedVatRate.getId())) {
                        exceptionMessages.add("basicSettings.vatRateId-You can't assign INACTIVE entity to product;");
                    } else {
                        return requestedVatRate;
                    }
                } else {
                    return requestedVatRate;
                }
            } else {
                log.debug("Vat rate with presented id [{}] not found", request.getVatRateId());
                exceptionMessages.add(String.format("basicSettings.vatRateId-Vat Rate with presented ID [%s] not found;", request.getVatRateId()));
            }
        }
        return null;
    }

    private ElectricityPriceType validateElectricityPriceTypeOnUpdate(ProductEditRequest request, ProductDetails
            productDetails, List<String> exceptionMessages) {
        Optional<ElectricityPriceType> electricityPriceTypeOptional = electricityPriceTypeRepository
                .findByIdAndStatuses(request.getElectricityPriceTypeId(),
                        List.of(NomenclatureItemStatus.ACTIVE, NomenclatureItemStatus.INACTIVE));
        if (electricityPriceTypeOptional.isPresent()) {
            ElectricityPriceType requestedElectricityPriceType = electricityPriceTypeOptional.get();
            if (requestedElectricityPriceType.getStatus().equals(NomenclatureItemStatus.INACTIVE)) {
                if (!Objects.equals(productDetails.getElectricityPriceType().getId(), requestedElectricityPriceType.getId())) {
                    exceptionMessages.add("priceSettings.electricityPriceTypeId-You can't assign INACTIVE entity to product;");
                } else {
                    return requestedElectricityPriceType;
                }
            } else {
                return requestedElectricityPriceType;
            }
        } else {
            log.debug("Electricity price type with presented id [{}] not found", request.getElectricityPriceTypeId());
            exceptionMessages.add(String.format("priceSettings.electricityPriceTypeId-Electricity Price Type with presented ID [%s] not found;", request.getElectricityPriceTypeId()));
        }
        return null;
    }

    private Currency validateCurrencyOnUpdate(ProductEditRequest request, ProductDetails
            productDetails, List<String> exceptionMessages) {
        if (request.getCurrencyId() != null) {
            Optional<Currency> currencyOptional = currencyRepository
                    .findByIdAndStatus(request.getCurrencyId(),
                            List.of(NomenclatureItemStatus.ACTIVE, NomenclatureItemStatus.INACTIVE));
            if (currencyOptional.isPresent()) {
                Currency requestedCurrency = currencyOptional.get();
                if (requestedCurrency.getStatus().equals(NomenclatureItemStatus.INACTIVE)) {
                    if (!Objects.equals(productDetails.getCurrency().getId(), requestedCurrency.getId())) {
                        exceptionMessages.add("priceSettings.currencyId-You can't assign INACTIVE entity to product;");
                    } else {
                        return requestedCurrency;
                    }
                } else {
                    return requestedCurrency;
                }
            } else {
                log.debug("Currency with presented id [{}] not found", request.getCurrencyId());
                exceptionMessages.add(String.format("priceSettings.currencyId-Currency with presented ID [%s] not found;", request.getCurrencyId()));
            }
        }
        return null;
    }

    private Terms validateTermsOnUpdate(ProductEditRequest request, ProductDetails
            productDetails, List<String> exceptionMessages) {
        Terms terms = null;
        if (request.getTermId() != null) {
            List<Long> allAvailableTermIdsForProduct = termsRepository.findAllAvailableTermIdsForProduct();

            // if terms was not provided in source version, or it is different from source version's terms, then its availability should be checked
            if (productDetails.getTerms() == null || !request.getTermId().equals(productDetails.getTerms().getId())) {
                if (allAvailableTermIdsForProduct.contains(request.getTermId())) {
                    Optional<Terms> termsOptional = termsRepository.findByIdAndStatusIn(request.getTermId(), List.of(TermStatus.ACTIVE));
                    if (termsOptional.isEmpty()) {
                        log.error("basicSettings.termId-Term with presented ID [%s] not found;".formatted(request.getTermId()));
                        exceptionMessages.add(String.format("basicSettings.termId-Term with presented ID [%s] not found;", request.getTermId()));
                        return null;
                    }

                    terms = termsOptional.get();
                } else {
                    log.error("Term with id [{}] is not available for this product", request.getTermId());
                    exceptionMessages.add(String.format("basicSettings.termId-Term with id [%s] is not available for this product;", request.getTermId()));
                }
            } else {
                // it means that the source version had terms and the provided terms is the same in request
                if (request.getUpdateExistingVersion()) {
                    // term should be the same if not changed when updating
                    terms = productDetails.getTerms();
                } else {
                    // if terms is the same as in the source version, then it should be cloned for new version
                    terms = termsService.cloneTerms(productDetails.getTerms().getId());
                }
            }
        }

        return terms;
    }

    private ProductForBalancing validateConsumerBalancingProductName(BaseProductRequest
                                                                             baseProductRequest, List<String> exceptionMessages) {
        Long balancingProductNameId = baseProductRequest.getConsumerBalancingProductNameId();

        if (Objects.nonNull(balancingProductNameId)) {
            Optional<ProductForBalancing> balancingProductOptional = productForBalancingRepository
                    .findByIdAndStatusIn(balancingProductNameId, List.of(EntityStatus.ACTIVE));

            if (balancingProductOptional.isEmpty()) {
                exceptionMessages.add("consumerBalancingProductNameId-Balancing Product Name with presented id: [%s] not found;".formatted(balancingProductNameId));
            } else {
                return balancingProductOptional.get();
            }
        }

        return null;
    }

    private ProductForBalancing validateGeneratorBalancingProductName(BaseProductRequest
                                                                              baseProductRequest, List<String> exceptionMessages) {
        Long balancingProductNameId = baseProductRequest.getGeneratorBalancingProductNameId();

        if (Objects.nonNull(balancingProductNameId)) {
            Optional<ProductForBalancing> balancingProductOptional = productForBalancingRepository
                    .findByIdAndStatusIn(balancingProductNameId, List.of(EntityStatus.ACTIVE));

            if (balancingProductOptional.isEmpty()) {
                exceptionMessages.add("generatorBalancingProductNameId-Balancing Product Name with presented id: [%s] not found;".formatted(balancingProductNameId));
            } else {
                return balancingProductOptional.get();
            }
        }

        return null;
    }

    private ProductDetails createProductDetailsInstance(ProductCreateRequest request,
                                                        ProductGroups productGroups,
                                                        ProductTypes productTypes,
                                                        VatRate vatRate,
                                                        ElectricityPriceType electricityPriceType,
                                                        Currency currency,
                                                        Terms terms,
                                                        TermsGroups termsGroups,
                                                        ProductForBalancing productForConsumerBalancing,
                                                        ProductForBalancing productForGeneratorBalancing) {
        log.debug("Creating new Product entity");
        Product savedProduct = productRepository.saveAndFlush(new Product(null, null, ProductStatus.ACTIVE, request.getCustomerIdentifier()));
        log.debug("Product entity created [{}]", savedProduct);

        log.debug("Creating new product details entity");
        ProductDetails productDetails = productDetailsRepository
                .saveAndFlush(
                        productMapper
                                .mapProductEntityToProductDetailsEntity(
                                        savedProduct,
                                        request,
                                        1L, // hardcoded initial version on create
                                        productGroups,
                                        productTypes,
                                        vatRate,
                                        electricityPriceType,
                                        currency,
                                        terms,
                                        termsGroups,
                                        productForConsumerBalancing,
                                        productForGeneratorBalancing));
        log.debug("Product details entity created [{}]", productDetails);

        log.debug("Updating product entity with product details id");
        savedProduct.setLastProductDetail(productDetails.getId());
        productRepository.save(savedProduct);

        return productDetails;
    }

    private void checkForProductNameUniqueness(long productDetailId, String requestProductName, String
            productDetailsName, List<String> exceptionMessages) {
        if (!requestProductName.equals(productDetailsName)) {
            List<ProductDetails> productDetailsOptional = productDetailsRepository.findAllByName(requestProductName);
            if (productDetailsOptional
                    .stream()
                    .anyMatch(productDetails ->
                            productDetailId != productDetails.getId())) {
                exceptionMessages.add(String.format("basicSettings.name-Product with same name [%s] already exists;", requestProductName));
            }
        }
    }

    private void checkForProductStatus(ProductDetails productDetails) {
        if (productDetails.getProduct().getProductStatus().equals(ProductStatus.DELETED)) {
            throw new OperationNotAllowedException("id-Cannot edit DELETED product;");
        }
    }

    private void validateCreatePermissions(Boolean isIndividual) {
        if (BooleanUtils.isTrue(isIndividual)) {
            if (!permissionService.permissionContextContainsPermissions(PermissionContextEnum.PRODUCTS, List.of(PermissionEnum.INDIVIDUAL_PRODUCT_CREATE))) {
                log.error("You do not have permission to create an individual product.");
                throw new ClientException("You do not have permission to create an individual product.", ErrorCode.ACCESS_DENIED);
            }
        } else {
            if (!permissionService.permissionContextContainsPermissions(PermissionContextEnum.PRODUCTS, List.of(PermissionEnum.PRODUCT_CREATE))) {
                log.error("You do not have permission to create a non-individual product.");
                throw new ClientException("You do not have permission to create a non-individual product.", ErrorCode.ACCESS_DENIED);
            }
        }
    }

    private void validateEditPermissions(Boolean isIndividual) {
        if (isIndividual) {
            if (!permissionService.permissionContextContainsPermissions(PermissionContextEnum.PRODUCTS, List.of(PermissionEnum.INDIVIDUAL_PRODUCT_EDIT_BASIC, PermissionEnum.INDIVIDUAL_PRODUCT_EDIT_LOCKED))) {
                log.error("You do not have permission to edit individual product.");
                throw new ClientException("You do not have permission to edit individual product.", ErrorCode.ACCESS_DENIED);
            }
        } else {
            if (!permissionService.permissionContextContainsPermissions(PermissionContextEnum.PRODUCTS, List.of(PermissionEnum.PRODUCT_EDIT_BASIC, PermissionEnum.PRODUCT_EDIT_LOCKED))) {
                log.error("You do not have permission to edit standard product.");
                throw new ClientException("You do not have permission to edit standard product.", ErrorCode.ACCESS_DENIED);
            }
        }
    }

    private void validateDeletePermission(Boolean isIndividual) {
        if (isIndividual) {
            if (!permissionService.permissionContextContainsPermissions(PermissionContextEnum.PRODUCTS, List.of(PermissionEnum.INDIVIDUAL_PRODUCT_DELETE))) {
                log.error("You do not have permission to delete individual product.");
                throw new ClientException("You do not have permission to delete individual product.", ErrorCode.ACCESS_DENIED);
            }
        } else {
            if (!permissionService.permissionContextContainsPermissions(PermissionContextEnum.PRODUCTS, List.of(PermissionEnum.PRODUCT_DELETE))) {
                log.error("You do not have permission to delete standard product.");
                throw new ClientException("You do not have permission to delete standard product.", ErrorCode.ACCESS_DENIED);
            }
        }
    }

    private void validatePreviewPermissions(Boolean isIndividual) {
        if (isIndividual) {
            if (!permissionService.permissionContextContainsPermissions(PermissionContextEnum.PRODUCTS, List.of(PermissionEnum.INDIVIDUAL_PRODUCT_VIEW_BASIC, PermissionEnum.INDIVIDUAL_PRODUCT_VIEW_DELETED))) {
                log.error("You do not have permission to preview individual product.");
                throw new ClientException("You do not have permission to preview individual product.", ErrorCode.ACCESS_DENIED);
            }
        } else {
            if (!permissionService.permissionContextContainsPermissions(PermissionContextEnum.PRODUCTS, List.of(PermissionEnum.PRODUCT_VIEW_BASIC, PermissionEnum.PRODUCT_VIEW_DELETED))) {
                log.error("You do not have permission to preview standard product.");
                throw new ClientException("You do not have permission to preview standard product.", ErrorCode.ACCESS_DENIED);
            }
        }
    }

    private void validateCopyPermissions(Boolean isIndividual) {
        if (isIndividual) {
            if (!permissionService.permissionContextContainsPermissions(PermissionContextEnum.PRODUCTS, List.of(PermissionEnum.INDIVIDUAL_PRODUCT_CREATE, PermissionEnum.INDIVIDUAL_PRODUCT_EDIT_BASIC, PermissionEnum.INDIVIDUAL_PRODUCT_EDIT_LOCKED))) {
                log.error("You do not have permission to copy individual product.");
                throw new ClientException("You do not have permission to copy individual product.", ErrorCode.ACCESS_DENIED);
            }
        } else {
            if (!permissionService.permissionContextContainsPermissions(PermissionContextEnum.PRODUCTS, List.of(PermissionEnum.PRODUCT_CREATE, PermissionEnum.PRODUCT_EDIT_BASIC, PermissionEnum.PRODUCT_EDIT_LOCKED))) {
                log.error("You do not have permission to copy standard product.");
                throw new ClientException("You do not have permission to copy standard product.", ErrorCode.ACCESS_DENIED);
            }
        }
    }

    private void validateListingPermissions(ProductListIndividualProduct individualProduct) {
        if (individualProduct == ProductListIndividualProduct.NO) {
            if (!permissionService.permissionContextContainsPermissions(PermissionContextEnum.PRODUCTS, List.of(PermissionEnum.PRODUCT_VIEW_BASIC, PermissionEnum.PRODUCT_VIEW_DELETED))) {
                log.error("You do not have permission to list standard product.");
                throw new ClientException("You do not have permission to list standard product.", ErrorCode.ACCESS_DENIED);
            }
        } else if (individualProduct == ProductListIndividualProduct.YES) {
            if (!permissionService.permissionContextContainsPermissions(PermissionContextEnum.PRODUCTS, List.of(PermissionEnum.INDIVIDUAL_PRODUCT_VIEW_BASIC, PermissionEnum.INDIVIDUAL_PRODUCT_VIEW_DELETED))) {
                log.error("You do not have permission to list individual product.");
                throw new ClientException("You do not have permission to list individual product.", ErrorCode.ACCESS_DENIED);
            }
        } else {
            if (!permissionService.permissionContextContainsPermissions(PermissionContextEnum.PRODUCTS, List.of(PermissionEnum.PRODUCT_VIEW_BASIC, PermissionEnum.PRODUCT_VIEW_DELETED, PermissionEnum.INDIVIDUAL_PRODUCT_VIEW_BASIC, PermissionEnum.INDIVIDUAL_PRODUCT_VIEW_DELETED))) {
                log.error("You do not have permission to list product.");
                throw new ClientException("You do not have permission to list product.", ErrorCode.ACCESS_DENIED);
            }
        }
    }

    @Transactional
    public ProductDetails cloneForContract(Long id, Long version, ProductParameterBaseRequest request) {
        Product product = productRepository.findByIdAndProductStatusIn(id, getProductStatuses())
                .orElseThrow(() -> new DomainEntityNotFoundException(String.format("Product with id[%s] not found", id)));

//        validateCopyPermissions(StringUtils.isNotBlank(product.getCustomerIdentifier()));

        ProductDetails originalDetails;
        if (version == null) {
            originalDetails = productDetailsRepository
                    .findLatestDetails(id, List.of(ProductDetailStatus.ACTIVE, ProductDetailStatus.INACTIVE), Sort.by(Sort.Direction.DESC, "version"))
                    .orElseThrow(() -> new DomainEntityNotFoundException("id-There is no details for given Product"));

        } else {
            originalDetails = productDetailsRepository
                    .findByProductIdAndVersionAndProductDetailStatusIn(id, version, List.of(ProductDetailStatus.ACTIVE, ProductDetailStatus.INACTIVE))
                    .orElseThrow(() -> new DomainEntityNotFoundException(String.format("Product Details with productId[%s] and version[%s] not found", id, version)));
        }
        ProductDetails clonedProductDetails = cloneBasicInformation(product, originalDetails);
        List<ProductContractTerms> originalProductTerms = productContractTermRepository.findAllByProductDetailsIdAndStatusInOrderByCreateDate(originalDetails.getId(), List.of(ProductSubObjectStatus.ACTIVE));

        for (ProductContractTerms originalProductTerm : originalProductTerms) {
            if (originalProductTerm.getStatus().equals(ProductSubObjectStatus.ACTIVE)) {
                ProductContractTerms productContractTerms = new ProductContractTerms();
                productContractTerms.setProductDetailsId(clonedProductDetails.getId());
                productContractTerms.setName(originalProductTerm.getName());
                productContractTerms.setType(originalProductTerm.getType());
                productContractTerms.setPeriodType(originalProductTerm.getPeriodType());
                productContractTerms.setAutomaticRenewal(originalProductTerm.getAutomaticRenewal());
                productContractTerms.setNumberOfRenewals(originalProductTerm.getNumberOfRenewals());
                productContractTerms.setValue(originalProductTerm.getValue());
                productContractTerms.setPerpetuityCause(originalProductTerm.getPerpetuityCause());
                productContractTerms.setStatus(ProductSubObjectStatus.ACTIVE);
                productContractTerms.setRenewalPeriodType(originalProductTerm.getRenewalPeriodType());
                productContractTerms.setRenewalPeriodValue(originalProductTerm.getRenewalPeriodValue());
                ProductContractTerms savedClone = productContractTermRepository.save(productContractTerms);
                if (originalProductTerm.getId().equals(request.getProductContractTermId())) {
                    request.setProductContractTermId(savedClone.getId());
                }
            }
        }
        if (originalDetails.getTerms() != null) {
            clonedProductDetails.setTerms(termsService.cloneTermsForProductContract(originalDetails.getTerms().getId(), request));
        }
        clonedProductDetails.setInterimAndAdvancePayments(productMapper.createProductIapCloneForProductContract(originalDetails.getInterimAndAdvancePayments(), clonedProductDetails, request));

        List<ProductPriceComponents> productPriceComponents = new ArrayList<>();
        List<ProductPriceComponents> priceComponents = originalDetails.getPriceComponents();
        if (priceComponents != null) {
            for (ProductPriceComponents priceComponent : priceComponents) {
                if (priceComponent.getProductSubObjectStatus().equals(ProductSubObjectStatus.ACTIVE)) {
                    ProductPriceComponents clonedProductPc = new ProductPriceComponents();
                    clonedProductPc.setProductDetails(clonedProductDetails);
                    clonedProductPc.setProductSubObjectStatus(ProductSubObjectStatus.ACTIVE);
                    Map<Long, PriceComponentFormulaVariable> oldVariableMap = priceComponent.getPriceComponent().getFormulaVariables().stream().collect(Collectors.toMap(PriceComponentFormulaVariable::getId, j -> j));
                    PriceComponent priceComponentClone = priceComponentService.clonePriceComponent(priceComponent.getPriceComponent().getId());
                    List<PriceComponentFormulaVariable> priceComponentFormulaVariables = priceComponentFormulaVariableRepository.findAllByPriceComponentIdOrderByIdAsc(priceComponentClone.getId());
                    Map<PriceComponentMathVariableName, PriceComponentFormulaVariable> newVariableMap = priceComponentFormulaVariables.stream().filter(x -> !x.getVariable().equals(PriceComponentMathVariableName.PRICE_PROFILE)).collect(Collectors.toMap(PriceComponentFormulaVariable::getVariable, j -> j));
                    List<PriceComponentContractFormula> contractFormulas = request.getContractFormulas();
                    for (PriceComponentContractFormula contractFormula : contractFormulas) {
                        if (oldVariableMap.containsKey(contractFormula.getFormulaVariableId())) {
                            PriceComponentFormulaVariable priceComponentFormulaVariable = oldVariableMap.get(contractFormula.getFormulaVariableId());
                            PriceComponentFormulaVariable variable = newVariableMap.get(priceComponentFormulaVariable.getVariable());
                            if (variable != null) {
                                contractFormula.setFormulaVariableId(variable.getId());
                            }
                        }
                    }
                    clonedProductPc.setPriceComponent(priceComponentClone);
                    productPriceComponents.add(clonedProductPc);
                }
            }
        }
        productPriceComponentRepository.saveAll(productPriceComponents);
        productDetailsRepository.save(clonedProductDetails);
        return clonedProductDetails;
    }

    @Transactional
    public ProductDetails clone(Long id, Long version) {
        Product product = productRepository.findByIdAndProductStatusIn(id, getProductStatuses())
                .orElseThrow(() -> new DomainEntityNotFoundException(String.format("Product with id[%s] not found", id)));

//        validateCopyPermissions(StringUtils.isNotBlank(product.getCustomerIdentifier()));

        ProductDetails originalDetails;
        if (version == null) {
            originalDetails = productDetailsRepository
                    .findLatestDetails(id, List.of(ProductDetailStatus.ACTIVE, ProductDetailStatus.INACTIVE), Sort.by(Sort.Direction.DESC, "version"))
                    .orElseThrow(() -> new DomainEntityNotFoundException("id-There is no details for given Product"));

        } else {
            originalDetails = productDetailsRepository
                    .findByProductIdAndVersionAndProductDetailStatusIn(id, version, List.of(ProductDetailStatus.ACTIVE, ProductDetailStatus.INACTIVE))
                    .orElseThrow(() -> new DomainEntityNotFoundException(String.format("Product Details with productId[%s] and version[%s] not found", id, version)));
        }
        ProductDetails clonedProductDetails = cloneBasicInformation(product, originalDetails);

        List<ProductContractTerms> originalProductTerms = productContractTermRepository.findAllByProductDetailsIdAndStatusInOrderByCreateDate(originalDetails.getId(), List.of(ProductSubObjectStatus.ACTIVE));
        List<ProductContractTerms> clonedTerms = new ArrayList<>();
        for (ProductContractTerms originalProductTerm : originalProductTerms) {
            if (originalProductTerm.getStatus().equals(ProductSubObjectStatus.ACTIVE)) {
                ProductContractTerms productContractTerms = new ProductContractTerms();
                productContractTerms.setProductDetailsId(clonedProductDetails.getId());
                productContractTerms.setName(originalProductTerm.getName());
                productContractTerms.setType(originalProductTerm.getType());
                productContractTerms.setPeriodType(originalProductTerm.getPeriodType());
                productContractTerms.setAutomaticRenewal(originalProductTerm.getAutomaticRenewal());
                productContractTerms.setNumberOfRenewals(originalProductTerm.getNumberOfRenewals());
                productContractTerms.setRenewalPeriodValue(originalProductTerm.getRenewalPeriodValue());
                productContractTerms.setRenewalPeriodValue(originalProductTerm.getRenewalPeriodValue());
                productContractTerms.setValue(originalProductTerm.getValue());
                productContractTerms.setPerpetuityCause(originalProductTerm.getPerpetuityCause());
                productContractTerms.setStatus(ProductSubObjectStatus.ACTIVE);
                clonedTerms.add(productContractTerms);
            }
        }
        if (originalDetails.getTerms() != null) {
            clonedProductDetails.setTerms(termsService.cloneTerms(originalDetails.getTerms().getId()));
        }
        clonedProductDetails.setInterimAndAdvancePayments(productMapper.createProductIapClone(originalDetails.getInterimAndAdvancePayments(), clonedProductDetails));
        clonedProductDetails.setPriceComponents(productMapper.createPriceComponentClones(originalDetails.getPriceComponents(), clonedProductDetails));

        productContractTermRepository.saveAll(clonedTerms);
        return clonedProductDetails;
    }

    private ProductDetails cloneBasicInformation(Product product, ProductDetails originalDetails) {


        Product clonedProduct = new Product();
        ProductDetails clonedProductDetails = new ProductDetails();

        Boolean globalGridOperators = originalDetails.getGlobalGridOperators();
        if (BooleanUtils.isNotTrue(globalGridOperators)) {
            clonedProductDetails.setGridOperator(productMapper.createGridOperatorClones(originalDetails.getGridOperator(), clonedProductDetails));
        } else {
            clonedProductDetails.setGlobalGridOperators(true);
        }

        Boolean globalSegment = originalDetails.getGlobalSegment();
        if (globalSegment == null || !globalSegment) {
            clonedProductDetails.setSegments(originalDetails
                    .getSegments()
                    .stream()
                    .filter(x -> x.getProductSubObjectStatus().equals(ProductSubObjectStatus.ACTIVE))
                    .map(x -> productMapper.cloneSegment(x, clonedProductDetails))
                    .toList());
        }

        Boolean globalSalesArea = originalDetails.getGlobalSalesArea();
        if (globalSalesArea == null || !globalSalesArea) {
            clonedProductDetails.setSalesAreas(originalDetails
                    .getSalesAreas()
                    .stream()
                    .filter(x -> x.getProductSubObjectStatus().equals(ProductSubObjectStatus.ACTIVE))
                    .map(x -> productMapper.cloneSalesArea(x, clonedProductDetails))
                    .toList());

        }

        Boolean globalSalesChannel = originalDetails.getGlobalSalesChannel();
        if (globalSalesChannel == null || !globalSalesChannel) {
            clonedProductDetails.setSalesChannels(originalDetails
                    .getSalesChannels()
                    .stream()
                    .filter(x -> x.getProductSubObjectStatus().equals(ProductSubObjectStatus.ACTIVE))
                    .map(x -> productMapper.cloneSaleChannel(x, clonedProductDetails))
                    .toList());

        }
        clonedProduct.setProductStatus(ProductStatus.ACTIVE);
        clonedProduct.setCustomerIdentifier(product.getCustomerIdentifier());
        productRepository.saveAndFlush(clonedProduct);
        productMapper.createProductClone(originalDetails, clonedProduct, clonedProductDetails);

        clonedProduct.setLastProductDetail(clonedProductDetails.getId());
        productRepository.save(clonedProduct);
        return clonedProductDetails;
    }


    /**
     * Fetches the product versions with the purpose of showing them in the product contracts listing filter.
     *
     * @param prompt search prompt
     * @return list of product versions optionally filtered by prompt
     */
    public Page<ProductVersionShortResponse> listProductVersionsForProductContracts(String prompt, int page,
                                                                                    int size) {
        log.debug("Fetching product versions for product contracts listing filter");

        return productDetailsRepository.getProductVersionsForProductContractsListing(
                EPBStringUtils.fromPromptToQueryParameter(prompt),
                PageRequest.of(page, size)
        );
    }

    public Page<ProductForBalancingShortResponse> getBalancingProductNames(BalancingNamesRequest request) {
        return productForBalancingRepository
                .findAllActiveBalancingProducts(EPBStringUtils.fromPromptToQueryParameter(request.prompt()), PageRequest.of(request.page(), request.size()))
                .map(ProductForBalancingShortResponse::new);
    }

    public boolean validateProductRelatedContractsUpdate(ProductEditRequest request) {
        return productRelatedContractUpdateService
                .validateProductForUpdateRelatedContracts(request);
    }

    public List<String> validateProductRelatedContractsUpdateTest(ProductEditRequest request) {
        return productRelatedContractUpdateService
                .validateProductForUpdateRelatedContractsTest(request);
    }

    private void updateRelatedProductContracts(ProductDetails currentProductDetails,
                                               List<Long> productDetailsWithRelatedContractThatShouldBeUpdated,
                                               List<String> exceptionMessages) {
        if (CollectionUtils.isNotEmpty(productDetailsWithRelatedContractThatShouldBeUpdated)) {
            if (!permissionService.permissionContextContainsPermissions(PermissionContextEnum.PRODUCTS, List.of(PermissionEnum.AUTOMATIC_RELATED_CONTRACT_UPDATE_FOR_PRODUCT))) {
                exceptionMessages.add("productDetailIdsForUpdatingProductContracts-You have not access to automatic related contract update for Product");
            } else {
                if (validateProductDetailsForUpdatingProductContracts(
                        currentProductDetails.getProduct().getId(),
                        productDetailsWithRelatedContractThatShouldBeUpdated,
                        exceptionMessages)
                ) {
                    productRelatedContractUpdateService
                            .updateProductContracts(
                                    currentProductDetails,
                                    productDetailsWithRelatedContractThatShouldBeUpdated,
                                    exceptionMessages
                            );
                }
            }
        }
    }

    private boolean validateProductDetailsForUpdatingProductContracts(Long productId,
                                                                      List<Long> productDetailIds,
                                                                      List<String> exceptionMessagesContext) {
        List<String> tempContext = new ArrayList<>();

        productDetailsRepository
                .findAllById(productDetailIds)
                .stream()
                .filter(pd -> !pd.getProduct().getId().equals(productId))
                .forEach(pd -> tempContext.add("Product Detail With id: [%s] is not assigned to Product with id: [%s]".formatted(pd.getId(), productId)));

        exceptionMessagesContext.addAll(tempContext);
        return tempContext.isEmpty();
    }


    public void saveTemplates(Set<TemplateSubObjectRequest> templateRequests,
                              Long productDetailId,
                              List<String> errorMessages) {
        if (CollectionUtils.isEmpty(templateRequests)) {
            return;
        }
        Map<ProductServiceTemplateType, List<Long>> requestMap = new HashMap<>();

        for (TemplateSubObjectRequest templateRequest : templateRequests) {
            if (!requestMap.containsKey(templateRequest.getTemplateType())) {
                List<Long> value = new ArrayList<>();
                value.add(templateRequest.getTemplateId());
                requestMap.put(templateRequest.getTemplateType(), value);
            } else {
                requestMap.get(templateRequest.getTemplateType()).add(templateRequest.getTemplateId());
            }
        }

        List<ProductTemplate> productContractTemplates = new ArrayList<>();
        createNewProductTemplates(productDetailId, errorMessages, requestMap, productContractTemplates);
        if (!errorMessages.isEmpty()) {
            return;
        }
        productTemplateRepository.saveAll(productContractTemplates);
    }

    private void createNewProductTemplates(Long productDetailId,
                                           List<String> errorMessages,
                                           Map<ProductServiceTemplateType, List<Long>> requestMap,
                                           List<ProductTemplate> productContractTemplates) {
        AtomicInteger i = new AtomicInteger(0);
        requestMap.forEach((key, value) -> {
            if (key.equals(ProductServiceTemplateType.INVOICE_TEMPLATE)) {
                Long templateId = value.get(0);
                if (contractTemplateRepository.existsByIdAndTemplatePurposeAndTemplateType(templateId, ContractTemplatePurposes.INVOICE, ContractTemplateType.DOCUMENT, LocalDate.now())) {
                    productContractTemplates.add(new ProductTemplate(templateId, productDetailId, key));
                    return;
                }
                errorMessages.add("templateIds[%s]-Template with id %s was not found or has wrong purpose;".formatted(i.getAndIncrement(), templateId));
                return;
            } else if (key.equals(ProductServiceTemplateType.EMAIL_TEMPLATE)) {
                Long templateId = value.get(0);
                if (contractTemplateRepository.existsByIdAndTemplatePurposeAndTemplateType(templateId, ContractTemplatePurposes.INVOICE, ContractTemplateType.EMAIL, LocalDate.now())) {
                    productContractTemplates.add(new ProductTemplate(templateId, productDetailId, key));
                    return;
                }
                errorMessages.add("templateIds[%s]-Template with id %s was not found or has wrong purpose;".formatted(i.getAndIncrement(), templateId));

                return;
            }
            Set<Long> allIdByIdAndLanguages = contractTemplateRepository.findAllIdByIdAndLanguages(value, key.getPurpose(ContractTemplatePurposes.PRODUCT), key.getLanguage(), key.getTemplateTypes(), ContractTemplateStatus.ACTIVE, LocalDate.now());
            for (Long l : value) {
                if (!allIdByIdAndLanguages.contains(l)) {
                    errorMessages.add("templateIds[%s]-Template with id %s was not found or has wrong purpose;".formatted(i.getAndIncrement(), l));
                    continue;
                }
                productContractTemplates.add(new ProductTemplate(l, productDetailId, key));
            }
        });
    }

    public void updateTemplates(Set<TemplateSubObjectRequest> templateIds,
                                Long productDetailId,
                                List<String> errorMessages) {
        if (CollectionUtils.isEmpty(templateIds)) {
            return;
        }
        Map<ProductServiceTemplateType, List<Long>> templatesToCreate = new HashMap<>();
        Map<Pair<Long, ProductServiceTemplateType>, ProductTemplate> templateMap = productTemplateRepository.findByProductDetailId(productDetailId).stream().collect(Collectors.toMap(x -> Pair.of(x.getTemplateId(), x.getType()), j -> j));
        List<ProductTemplate> productContractTemplates = new ArrayList<>();
        for (TemplateSubObjectRequest templateRequest : templateIds) {
            ProductTemplate remove = templateMap.remove(Pair.of(templateRequest.getTemplateId(), templateRequest.getTemplateType()));
            if (remove == null) {
                if (!templatesToCreate.containsKey(templateRequest.getTemplateType())) {
                    List<Long> value = new ArrayList<>();
                    value.add(templateRequest.getTemplateId());
                    templatesToCreate.put(templateRequest.getTemplateType(), value);
                } else {
                    templatesToCreate.get(templateRequest.getTemplateType()).add(templateRequest.getTemplateId());
                }
            }
        }
        createNewProductTemplates(productDetailId, errorMessages, templatesToCreate, productContractTemplates);
        Collection<ProductTemplate> values = templateMap.values();
        values.forEach(x -> {
            x.setStatus(EntityStatus.DELETED);
            productContractTemplates.add(x);
        });
        if (!errorMessages.isEmpty()) {
            return;
        }
        productTemplateRepository.saveAll(productContractTemplates);
    }


    private void updateProductFilesForExistingVersion(ProductEditRequest request, ProductDetails currentVersion, List<String> exceptionMessages) {
        List<Long> requestedFileIds = CollectionUtils.emptyIfNull(request.getProductFileIds()).stream().toList();
        List<ProductFile> requestedFiles = searchProductFilesAndReturnList(requestedFileIds, exceptionMessages);

        if (CollectionUtils.isEmpty(exceptionMessages)) {
            List<ProductFile> currentAssignedFiles = productFileRepository.findActiveProductDetailFiles(currentVersion.getId());
            List<Long> currentAssignedFileIds = currentAssignedFiles.stream().map(ProductFile::getId).toList();

            List<ProductFile> outdatedProductFiles = currentAssignedFiles
                    .stream()
                    .filter(pf -> !requestedFileIds.contains(pf.getId()))
                    .toList();

            outdatedProductFiles.forEach(pf -> pf.setStatus(EntityStatus.DELETED));

            requestedFiles
                    .stream()
                    .filter(pf -> !currentAssignedFileIds.contains(pf.getId()))
                    .forEach(pf -> pf.setProductDetailId(currentVersion.getId()));
        }
    }

    private void updateProductFilesForNewVersion(ProductEditRequest request, ProductDetails newVersion, List<String> exceptionMessages) {
        List<Long> requestedFileIds = CollectionUtils.emptyIfNull(request.getProductFileIds()).stream().toList();
        List<ProductFile> requestedFiles = searchProductFilesAndReturnList(requestedFileIds, exceptionMessages);

        List<ProductFile> newFilesContext = new ArrayList<>();
        if (CollectionUtils.isEmpty(exceptionMessages)) {
            for (ProductFile requestedFile : requestedFiles) {
                if (Objects.isNull(requestedFile.getProductDetailId())) {
                    requestedFile.setProductDetailId(newVersion.getId());
                } else {
                    if (Objects.isNull(requestedFile.getLocalFileUrl())) {
                        try {
                            ByteArrayResource archivedFile = archivationService.downloadArchivedFile(requestedFile.getDocumentId(), requestedFile.getFileId());

                            FileWithStatusesResponse localFile = productFileService.uploadProductFile(new ByteMultiPartFile(requestedFile.getName(), archivedFile.getContentAsByteArray(), requestedFile.getFileType()), requestedFile.getFileStatuses());

                            ProductFile productFile = productFileRepository
                                    .findById(localFile.getId())
                                    .orElseThrow(() -> new DomainEntityNotFoundException("Uploaded file with id: [%s] not found;".formatted(localFile.getId())));

                            productFile.setProductDetailId(newVersion.getId());

                            newFilesContext.add(productFile);
                        } catch (Exception e) {
                            log.error("Exception handled while trying to archive product file: %s".formatted(requestedFile.getName()), e);
                            exceptionMessages.add("Exception handled while trying to archive product file: %s".formatted(requestedFile.getName()));
                        }
                    } else {
                        newFilesContext.add(
                                ProductFile
                                        .builder()
                                        .name(requestedFile.getName())
                                        .status(EntityStatus.ACTIVE)
                                        .productDetailId(newVersion.getId())
                                        .fileType(requestedFile.getFileType())
                                        .fileStatuses(requestedFile.getFileStatuses())
                                        .localFileUrl(requestedFile.getLocalFileUrl())
                                        .build()
                        );
                    }
                }
            }
        }

        if (CollectionUtils.isEmpty(exceptionMessages)) {
            productFileRepository.saveAll(newFilesContext);
        }
    }

    private void checkForBoundObjects(ProductDetails productDetails) {
        List<ProductDetails> bound = productDetailsRepository.checkForBoundObjects(productDetails.getId()); //TODO SQL NEEDED
        if (CollectionUtils.isNotEmpty(bound)) {
            if (!checkIfHasLockedPermission()) {
                throw new ClientException("You can't edit Product because it is connected to the PRODUCT_CONTRACT;", ErrorCode.CONFLICT);
            }
        }
    }

    private boolean checkForBoundObjectsForPreview(ProductDetails productDetails) {
        List<ProductDetails> bound = productDetailsRepository.checkForBoundObjects(productDetails.getId()); //TODO SQL NEEDED
        return CollectionUtils.isNotEmpty(bound);
    }

    private boolean checkIfHasLockedPermission() {
        List<String> customerContext = permissionService.getPermissionsFromContext(PermissionContextEnum.PRODUCTS);
        return customerContext.contains(PermissionEnum.PRODUCT_EDIT_LOCKED.getId());
    }
}
