package bg.energo.phoenix.service.contract.product;

import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.OperationNotAllowedException;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.contract.product.*;
import bg.energo.phoenix.model.entity.customer.Customer;
import bg.energo.phoenix.model.entity.customer.CustomerDetails;
import bg.energo.phoenix.model.entity.customer.CustomerSegment;
import bg.energo.phoenix.model.entity.customer.UnwantedCustomer;
import bg.energo.phoenix.model.entity.customer.communication.CustomerCommContactPurposes;
import bg.energo.phoenix.model.entity.customer.communication.CustomerCommunications;
import bg.energo.phoenix.model.entity.documents.Document;
import bg.energo.phoenix.model.entity.nomenclature.contract.ContractVersionType;
import bg.energo.phoenix.model.entity.product.product.Product;
import bg.energo.phoenix.model.entity.product.product.ProductContractProductListingMiddleResponse;
import bg.energo.phoenix.model.entity.product.product.ProductDetails;
import bg.energo.phoenix.model.entity.product.product.ProductSegments;
import bg.energo.phoenix.model.enums.contract.products.ContractDetailType;
import bg.energo.phoenix.model.enums.contract.products.ContractSubObjectStatus;
import bg.energo.phoenix.model.enums.customer.CustomerDetailStatus;
import bg.energo.phoenix.model.enums.customer.CustomerStatus;
import bg.energo.phoenix.model.enums.customer.CustomerType;
import bg.energo.phoenix.model.enums.customer.Status;
import bg.energo.phoenix.model.enums.customer.unwantedCustomer.UnwantedCustomerStatus;
import bg.energo.phoenix.model.enums.documents.DocumentSigners;
import bg.energo.phoenix.model.enums.documents.DocumentStatus;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.product.product.ProductStatus;
import bg.energo.phoenix.model.enums.product.product.ProductSubObjectStatus;
import bg.energo.phoenix.model.request.communication.edms.Attribute;
import bg.energo.phoenix.model.request.contract.product.ProductContractBasicParametersCreateRequest;
import bg.energo.phoenix.model.request.contract.product.ProductContractCreateRequest;
import bg.energo.phoenix.model.request.contract.product.ProductContractUpdateRequest;
import bg.energo.phoenix.model.request.contract.product.ProductParameterBaseRequest;
import bg.energo.phoenix.model.request.contract.relatedEntities.RelatedEntityType;
import bg.energo.phoenix.model.request.product.product.ProductContractProductListingRequest;
import bg.energo.phoenix.model.response.contract.ContractFileResponse;
import bg.energo.phoenix.model.response.contract.productContract.BasicParametersResponse;
import bg.energo.phoenix.model.response.customer.CustomerCommunicationDataResponse;
import bg.energo.phoenix.model.response.nomenclature.contract.ContractVersionTypesResponse;
import bg.energo.phoenix.model.response.nomenclature.product.currency.CurrencyResponse;
import bg.energo.phoenix.model.response.proxy.ProxyResponse;
import bg.energo.phoenix.model.response.shared.FileWithStatusesResponse;
import bg.energo.phoenix.repository.contract.billing.ContractPodRepository;
import bg.energo.phoenix.repository.contract.product.*;
import bg.energo.phoenix.repository.customer.AccountManagerRepository;
import bg.energo.phoenix.repository.customer.CustomerDetailsRepository;
import bg.energo.phoenix.repository.customer.CustomerRepository;
import bg.energo.phoenix.repository.customer.CustomerSegmentRepository;
import bg.energo.phoenix.repository.customer.communicationData.CustomerCommContactPurposesRepository;
import bg.energo.phoenix.repository.customer.communicationData.CustomerCommunicationContactsRepository;
import bg.energo.phoenix.repository.customer.communicationData.CustomerCommunicationsRepository;
import bg.energo.phoenix.repository.documents.DocumentsRepository;
import bg.energo.phoenix.repository.nomenclature.contract.ContractVersionTypesRepository;
import bg.energo.phoenix.repository.nomenclature.product.CurrencyRepository;
import bg.energo.phoenix.repository.product.product.ProductDetailsRepository;
import bg.energo.phoenix.repository.product.product.ProductRepository;
import bg.energo.phoenix.repository.product.product.ProductSegmentRepository;
import bg.energo.phoenix.repository.template.ContractTemplateRepository;
import bg.energo.phoenix.security.PermissionService;
import bg.energo.phoenix.service.archivation.edms.FileArchivationService;
import bg.energo.phoenix.service.archivation.edms.config.EDMSAttributeProperties;
import bg.energo.phoenix.service.contract.proxy.ProxyService;
import bg.energo.phoenix.service.contract.relatedEntities.RelatedContractsAndOrdersService;
import bg.energo.phoenix.service.customer.UnwantedCustomerService;
import bg.energo.phoenix.service.document.enums.FileFormat;
import bg.energo.phoenix.service.product.product.ProductService;
import bg.energo.phoenix.service.xEnergie.XEnergieRepository;
import bg.energo.phoenix.util.archivation.EDMSArchivationConstraints;
import bg.energo.phoenix.util.contract.CommunicationContactPurposeProperties;
import bg.energo.phoenix.util.contract.product.ProductContractStatusChainUtil;
import bg.energo.phoenix.util.versionDates.CalculateVersionDates;
import bg.energo.phoenix.util.versionDates.VersionWithDatesModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static bg.energo.phoenix.model.enums.contract.express.ProductContractVersionStatus.READY;
import static bg.energo.phoenix.model.enums.contract.express.ProductContractVersionStatus.SIGNED;
import static bg.energo.phoenix.permissions.PermissionContextEnum.PRODUCT_CONTRACTS;
import static bg.energo.phoenix.permissions.PermissionEnum.*;
import static bg.energo.phoenix.util.communication.CommunicationDataUtils.checkCommunicationEmailAndNumber;

@SuppressWarnings("DuplicatedCode")
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductContractBasicParametersService {
    private final ContractPodRepository contractPodRepository;
    private final ProductRepository productRepository;
    private final ProductDetailsRepository productDetailsRepository;
    private final CustomerRepository customerRepository;
    private final CustomerDetailsRepository customerDetailsRepository;
    private final CustomerSegmentRepository customerSegmentRepository;
    private final ProductSegmentRepository productSegmentRepository;
    private final CustomerCommunicationsRepository communicationsRepository;
    private final CustomerCommContactPurposesRepository commContactPurposesRepository;
    private final CommunicationContactPurposeProperties communicationContactPurposeProperties;
    private final ProductContractRepository productContractRepository;
    private final ProductContractFilesService productContractFilesService;
    private final ProductContractDocumentService productContractDocumentService;
    private final ProxyService proxyService;
    private final UnwantedCustomerService unwantedCustomerService;
    private final ProductContractVersionTypeRepository productContractVersionTypeRepository;
    private final ProductService productService;
    private final ProductContractDetailsRepository productContractDetailsRepository;
    private final RelatedContractsAndOrdersService relatedContractsAndOrdersService;
    private final CustomerCommunicationContactsRepository communicationContactsRepository;
    private final ContractVersionTypesRepository contractVersionTypesRepository;
    private final CurrencyRepository currencyRepository;
    private final ProductContractFileRepository productContractFileRepository;
    private final ProductContractDocumentRepository productContractDocumentRepository;
    private final ProductContractResignedContractsRepository productContractResignedContractsRepository;
    private final XEnergieRepository xEnergieRepository;
    private final PermissionService permissionService;
    private final AccountManagerRepository accountManagerRepository;
    private final FileArchivationService fileArchivationService;
    private final EDMSAttributeProperties attributeProperties;
    private final DocumentsRepository documentsRepository;
    private final ContractTemplateRepository templateRepository;
    private final ProductContractBillingLockValidationService productContractBillingLockValidationService;
    private final ProductContractDraftValidationService productContractDraftValidationService;

    @Transactional
    public void create(ProductContractCreateRequest createRequest, ProductContract productContract, List<String> messages, ProductContractDetails contractDetails) {
        ProductContractBasicParametersCreateRequest request = createRequest.getBasicParameters();

        createContractDetails(contractDetails, request, messages);
        contractDetails.setContractId(productContract.getId());
        Optional<Product> productOptional = productRepository.findByIdAndProductStatusIn(request.getProductId(), List.of(ProductStatus.ACTIVE));
        if (productOptional.isEmpty()) {
            messages.add("basicParameters.productId-Product not found!;");
        }
        productOptional.ifPresent(product -> individualProductCheck(product, messages));
        Optional<ProductDetails> productDetailsOptional = productDetailsRepository.findByProductIdAndVersion(request.getProductId(), request.getProductVersionId());
        if (productDetailsOptional.isEmpty()) {
            messages.add("basicParameters.productVersionId-Product version not found!;");
        }
        Optional<Customer> customerOptional = customerRepository.findByIdAndStatuses(request.getCustomerId(), List.of(CustomerStatus.ACTIVE));
        if (customerOptional.isEmpty()) {
            messages.add("basicParameters.customerId-Customer can not be found;");
        }
        if (customerOptional.isPresent()) {
            Customer customer = customerOptional.get();
            UnwantedCustomer unwantedCustomer = unwantedCustomerService.checkUnwantedCustomer(customer.getIdentifier());
            if (unwantedCustomer != null) {
                if (Boolean.TRUE.equals(unwantedCustomer.getCreateContractRestriction()) && unwantedCustomer.getStatus().equals(UnwantedCustomerStatus.ACTIVE)) {
                    messages.add("basicParameters.customerId-Customer is unwanted and restricted to create contract;");
                }
            }

            validateManuallyAddedDealNumber(null, createRequest.getAdditionalParameters().getDealNumber(), customer.getIdentifier(), messages);
        }

        Optional<CustomerDetails> customerDetailsOptional = customerDetailsRepository.findByCustomerIdAndVersionId(request.getCustomerId(), request.getCustomerVersionId());
        if (customerDetailsOptional.isEmpty()) {
            messages.add("basicParameters.customerVersionId-Customer version not found");
        } else {
            CustomerDetails customerDetails = customerDetailsOptional.get();
            if (!List.of(CustomerDetailStatus.NEW, CustomerDetailStatus.ACTIVE, CustomerDetailStatus.LOST).contains(customerDetails.getStatus())) {
                messages.add("basicParameters.customerVersionId-Can not conclude contract because customer has wrong status!;");
            }
        }

        ProductDetails productDetails = null;
        CustomerDetails customerDetails = null;
        if (customerDetailsOptional.isPresent() && productDetailsOptional.isPresent()) {
            productDetails = productDetailsOptional.get();
            customerDetails = customerDetailsOptional.get();
            if (productDetails.getProduct().getCustomerIdentifier() == null && !Objects.equals(Boolean.TRUE, productDetails.getGlobalSegment())) {
                checkSegments(productDetails, customerDetails, messages);
            }
            List<CustomerDetailStatus> forbiddenStatuses = List.of(CustomerDetailStatus.POTENTIAL);
            if (forbiddenStatuses.contains(customerDetails.getStatus())) {
                messages.add("basicParameters.customerId[0]-Customer can not be of status [%s]".formatted(forbiddenStatuses));
            }
            if (productDetails.getProduct().getCustomerIdentifier() == null && (productDetails.getAvailableForSale() == null || productDetails.getAvailableForSale().equals(Boolean.FALSE))) {
                messages.add("basicParameters.productVersionId-Product version is not for sale!;");
            } else if (productDetails.getProduct().getCustomerIdentifier() == null && (!((productDetails.getAvailableFrom() == null || productDetails.getAvailableFrom().isBefore(LocalDateTime.now())) && (productDetails.getAvailableTo() == null || productDetails.getAvailableTo().isAfter(LocalDateTime.now()))))) {
                messages.add("basicParameters.productVersionId-Product version is not for sale today!;");
            }
            if (!Objects.equals(Boolean.TRUE, productDetails.getGlobalSalesChannel()) && productOptional.isPresent() && StringUtils.isEmpty(productOptional.get().getCustomerIdentifier())) {
                checkProductSaleChannels(productDetails.getId(), messages);
            }
        }

        if (customerDetails != null) {
            contractDetails.setCustomerDetailId(customerDetails.getId());
        }

        if (productDetails != null) {
            contractDetails.setProductDetailId(productDetails.getId());
        }
        if (productContract.getSigningDate() != null && productContract.getSigningDate().isBefore(LocalDate.now())) {
            contractDetails.setStartDate(productContract.getSigningDate());
        } else {
            contractDetails.setStartDate(LocalDate.now());
        }

        createContractCommunicationData(request.getCommunicationDataContractId(), contractDetails, messages);
        createBillingCommunicationData(request.getCommunicationDataBillingId(), contractDetails, messages);

        archiveFiles(contractDetails);
        archiveDocuments(contractDetails);
    }


    private void checkProductSaleChannels(Long id, List<String> messages) {
        if (!productRepository.checkSegments(id, permissionService.getLoggedInUserId())) {
            messages.add("basicParameters.productId-You do not have permission to create contract with this product!;");
        }
    }

    private void individualProductCheck(Product product, List<String> messages) {
        if (product.getCustomerIdentifier() == null) {
            return;
        }
        if (productContractRepository.existsByProductId(product.getId())) {
            messages.add("basicParameters.productId-Individual Product with id %s is Already used!;".formatted(product.getId()));
        }
    }

    private void individualProductCheck(Product product, Long productContractDetailId, List<String> messages) {
        if (product.getCustomerIdentifier() == null) {
            return;
        }
        if (productContractRepository.existsByProductIdAndContractIdNotEquals(product.getId(), productContractDetailId)) {
            messages.add("basicParameters.productId-Individual Product with id %s is Already used!;".formatted(product.getId()));
        }
    }

    public Page<ProductContractProductListingMiddleResponse> getProducts(ProductContractProductListingRequest request) {

        return productRepository.searchForContract(
                request.getCustomerDetailId(),
                permissionService.getLoggedInUserId(),
                request.getPrompt(),
                request.getProductContractId(),
                PageRequest.of(request.getPage(), request.getSize(), Sort.by(Sort.Direction.ASC, "customer_identifier", "pd.name")
                ));
    }


    private void createContractCommunicationData(Long contractId, ProductContractDetails details, List<String> messages) {
        Optional<CustomerCommunications> contractCommunicationsOptional = communicationsRepository.findByIdAndStatuses(contractId, List.of(Status.ACTIVE));
        if (contractCommunicationsOptional.isEmpty()) {
            messages.add("basicParameters.communicationDataContractId-Communication data for contract not found!;");
            return;
        }
        CustomerCommunications contractCommunications = contractCommunicationsOptional.get();
        List<CustomerCommContactPurposes> contractPurposes = commContactPurposesRepository.findByCustomerCommId(contractCommunications.getId(), List.of(Status.ACTIVE));
        Long contractCommunicationId = communicationContactPurposeProperties.getContractCommunicationId();
        boolean contains = false;
        for (CustomerCommContactPurposes contractPurpose : contractPurposes) {
            if (contractPurpose.getContactPurposeId().equals(contractCommunicationId)) {
                contains = true;
                details.setCustomerCommunicationIdForContract(contractCommunications.getId());
            }
        }
        if (!contains) {
            messages.add("basicParameters.communicationDataContractId-Contract communications is invalid;");
            return;
        }
        checkForEmailAndNumber(contractId, messages, "basicParameters.communicationDataContractId-Contract communication should have Email and Mobile number contact types!;");
    }

    private void checkForEmailAndNumber(Long contractId, List<String> messages, String message) {
        checkCommunicationEmailAndNumber(contractId, messages, message, communicationContactsRepository);
    }

    private void createBillingCommunicationData(Long billingId, ProductContractDetails details, List<String> messages) {
        Optional<CustomerCommunications> billingCommunicationsOptional = communicationsRepository.findByIdAndStatuses(billingId, List.of(Status.ACTIVE));
        if (billingCommunicationsOptional.isEmpty()) {
            messages.add("basicParameters.communicationDataContractId-Communication data for contract not found!;");
            return;
        }
        CustomerCommunications billingCommunications = billingCommunicationsOptional.get();
        List<CustomerCommContactPurposes> contractPurposes = commContactPurposesRepository.findByCustomerCommId(billingCommunications.getId(), List.of(Status.ACTIVE));
        Long billingCommunicationId = communicationContactPurposeProperties.getBillingCommunicationId();
        boolean contains = false;
        for (CustomerCommContactPurposes contractPurpose : contractPurposes) {
            if (contractPurpose.getContactPurposeId().equals(billingCommunicationId)) {
                contains = true;
                details.setCustomerCommunicationIdForBilling(billingCommunications.getId());
            }
        }
        if (!contains) {
            messages.add("basicParameters.communicationDataBillingId-Billing communications is invalid;");
        }
        checkForEmailAndNumber(billingId, messages, "basicParameters.communicationDataBillingId-Billing communication should have Email and Mobile number contact types!;");

    }

    private void checkSegments(ProductDetails productDetails, CustomerDetails customerDetails, List<String> messages) {
        List<CustomerSegment> customerSegments = customerSegmentRepository.findAllByCustomerDetailIdAndStatus(customerDetails.getId(), Status.ACTIVE);
        List<ProductSegments> productSegments = productSegmentRepository.findAllByProductDetailsIdAndProductSubObjectStatus(productDetails.getId(), ProductSubObjectStatus.ACTIVE);
        Set<Long> customerSegmentIds = new HashSet<>();
        boolean contains = false;
        for (CustomerSegment customerSegment : customerSegments) {
            customerSegmentIds.add(customerSegment.getSegment().getId());
        }
        for (ProductSegments productSegment : productSegments) {
            if (customerSegmentIds.contains(productSegment.getSegment().getId())) {
                contains = true;
            }
        }
        if (!contains) {
            messages.add("customerId-Customer and product segments do not overlap;");
        }
    }


    private ProductContractDetails createContractDetails(ProductContractDetails productContractDetails, ProductContractBasicParametersCreateRequest request, List<String> messages) {
        productContractDetails.setType(request.getType());
        productContractDetails.setVersionId(1);
        if (request.isHasUntilVolume()) {
            productContractDetails.setContractTermUntilTheVolumeValue(request.getUntilVolume());
        }
        productContractDetails.setContractTermUntilTheVolume(request.isHasUntilVolume());
        if (request.isHasUntilAmount()) {
            if (!currencyRepository.existsByIdAndStatusIn(request.getUntilAmountCurrencyId(), List.of(NomenclatureItemStatus.ACTIVE))) {
                messages.add("basicParameters.untilAmountCurrencyId-Until amount currency do not exist!;");
            }
            productContractDetails.setCurrencyId(request.getUntilAmountCurrencyId());
            productContractDetails.setContractTermUntilTheAmountValue(request.getUntilAmount());
        }
        productContractDetails.setContractTermUntilTheAmount(request.isHasUntilAmount());
        productContractDetails.setVersionStatus(request.getVersionStatus());
        productContractDetails.setPublicProcurementLaw(request.isProcurementLaw());
        return productContractDetails;
    }

    private ProductContractDetails updateContractDetails(ProductContractDetails productContractDetails, ProductContractDetails sourceDetails, ProductContractBasicParametersCreateRequest request, Integer versionId, List<String> messages) {
        productContractDetails.setType(request.getType());
        productContractDetails.setVersionId(versionId);
        productContractDetails.setContractTermUntilTheVolumeValue(request.getUntilVolume());
        productContractDetails.setContractTermUntilTheVolume(request.isHasUntilVolume());
        productContractDetails.setEntryIntoForceDate(request.getEntryInForceDate());
        productContractDetails.setInitialTermDate(request.getStartOfInitialTerm());
        if (request.isHasUntilAmount()) {
            if (!Objects.equals(sourceDetails.getCurrencyId(), request.getUntilAmountCurrencyId()) && !currencyRepository.existsByIdAndStatusIn(request.getUntilAmountCurrencyId(), List.of(NomenclatureItemStatus.ACTIVE))) {
                messages.add("basicParameters.untilAmountCurrencyId-Until amount currency do not exist!;");
            }
        } else {
            sourceDetails.setCurrencyId(null);
        }
        productContractDetails.setCurrencyId(request.getUntilAmountCurrencyId());
        productContractDetails.setContractTermUntilTheAmountValue(request.getUntilAmount());
        productContractDetails.setContractTermUntilTheAmount(request.isHasUntilAmount());

        if (Objects.equals(productContractDetails.getId(), sourceDetails.getId()) && !ProductContractStatusChainUtil.versionStatusCanBeChanged(productContractDetails.getVersionStatus(), request.getVersionStatus())) {
            messages.add("basicParameters.versionStatus-Version status can not be changed to provided status!;");
        }
        productContractDetails.setVersionStatus(request.getVersionStatus());
        productContractDetails.setPublicProcurementLaw(request.isProcurementLaw());
        return productContractDetails;
    }

    public BasicParametersResponse getBasicParameterResponse(ProductContract contract, ProductContractDetails details) {
        BasicParametersResponse basicParametersResponse = mapToBasicParameters(contract, details);
        List<ProxyResponse> proxy = proxyService.preview(details.getId());
        basicParametersResponse.setProxy(proxy);
        setCustomerData(details, basicParametersResponse);
        setProductData(details, basicParametersResponse);
        basicParametersResponse.setRelatedEntities(relatedContractsAndOrdersService.getRelatedEntities(contract.getId(), RelatedEntityType.PRODUCT_CONTRACT));
        setVersionStatusData(details, basicParametersResponse);
        setFiles(basicParametersResponse, details);
        setDocuments(basicParametersResponse, details);
        setResignedContractsFrom(basicParametersResponse, contract);
        setResignedContractsTo(basicParametersResponse, contract);
        return basicParametersResponse;
    }

    private void setResignedContractsFrom(BasicParametersResponse basicParametersResponse, ProductContract contract) {
        basicParametersResponse.setResignedFrom(productContractResignedContractsRepository.findResignedContractsFrom(contract.getId()));
    }

    private void setResignedContractsTo(BasicParametersResponse basicParametersResponse, ProductContract contract) {
        basicParametersResponse.setResignedTo(productContractResignedContractsRepository.findResignedContractsTo(contract.getResignedTo()));
    }

    private void setDocuments(BasicParametersResponse basicParametersResponse, ProductContractDetails details) {
        basicParametersResponse
                .setDocuments(
                        productContractDocumentRepository
                                .findAllByContractDetailIdAndStatusIn(details.getId(), List.of(EntityStatus.ACTIVE))
                                .stream()
                                .map(d -> new FileWithStatusesResponse(d, accountManagerRepository.findByUserName(d.getSystemUserId())
                                        .map(manager -> " (".concat(manager.getDisplayName()).concat(")")).orElse("")))
                                .toList()
                );
    }

    private void setFiles(BasicParametersResponse basicParametersResponse, ProductContractDetails details) {
        List<ContractFileResponse> allFilesList = new ArrayList<>(productContractFileRepository
                .findAllByContractDetailIdAndStatusIn(details.getId(), List.of(EntityStatus.ACTIVE))
                .stream()
                .map(f -> new ContractFileResponse(f, accountManagerRepository.findByUserName(f.getSystemUserId())
                        .map(manager -> " (".concat(manager.getDisplayName()).concat(")")).orElse("")))
                .toList());
        List<ContractFileResponse> signableDocs = documentsRepository.findSignedDocumentsForProductContractDetail(details.getId())
                .stream()
                .flatMap(x -> {
                    Stream.Builder<Pair<Document, ContractFileResponse>> builder = Stream.builder();
                    if (x.getDocumentStatus().equals(DocumentStatus.SIGNED)) {
                        boolean isNotSigned = !x.getFileFormat().equals(FileFormat.PDF) || (ListUtils.emptyIfNull(x.getSigners()).contains(DocumentSigners.NO));
                        builder.add(Pair.of(x, new ContractFileResponse(DocumentStatus.SIGNED, x, isNotSigned)));
                        if (!(x.getSigners().contains(DocumentSigners.NO) || x.getSigners().isEmpty())) {
                            builder.add(Pair.of(x, new ContractFileResponse(DocumentStatus.UNSIGNED, x, false)));
                        }
                    } else {
                        builder.add(Pair.of(x, new ContractFileResponse(DocumentStatus.UNSIGNED, x, false)));
                    }
                    return builder.build();
                })
                .peek(x -> x.getSecond().updateFileInfo(x.getFirst(), accountManagerRepository.findByUserName(x.getFirst().getSystemUserId())
                        .map(manager -> " (".concat(manager.getDisplayName()).concat(")")).orElse("")))
                .map(Pair::getSecond)
                .toList();
        allFilesList
                .addAll(signableDocs);
        allFilesList
                .addAll(documentsRepository.findActionDocumentsForProductContractDetail(details.getId()).stream()
                        .map(f -> new ContractFileResponse(f, accountManagerRepository.findByUserName(f.getSystemUserId())
                                .map(manager -> " (".concat(manager.getDisplayName()).concat(")")).orElse("")))
                        .toList());
        basicParametersResponse.setFiles(
                allFilesList
        );
    }

    private void setVersionStatusData(ProductContractDetails details, BasicParametersResponse basicParametersResponse) {
        List<ContractVersionType> versionTypesForContract = productContractVersionTypeRepository.findVersionTypesForContract(details.getId());
        basicParametersResponse.setVersionTypesResponse(versionTypesForContract.stream().map(ContractVersionTypesResponse::new).toList());
        basicParametersResponse.setVersionStatus(details.getVersionStatus());
    }

    private void setProductData(ProductContractDetails details, BasicParametersResponse basicParametersResponse) {
        ProductDetails productDetails = productDetailsRepository.findById(details.getProductDetailId())
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Attached product do not exist!;"));

        basicParametersResponse.setProductId(productDetails.getProduct().getId());
        if (details.getAgreementSuffix() != null) {
            basicParametersResponse.setContractSuffix(details.getType().equals(ContractDetailType.CONTRACT) ? null : "#" + details.getAgreementSuffix());
        }
        basicParametersResponse.setProductName("%s (version %s)".formatted(productDetails.getName(), productDetails.getVersion()));
        basicParametersResponse.setProductDetailId(productDetails.getId());
        basicParametersResponse.setProductVersionId(productDetails.getVersion());
    }

    private void setCustomerData(ProductContractDetails details, BasicParametersResponse basicParametersResponse) {
        CustomerDetails customerDetails = customerDetailsRepository.findById(details.getCustomerDetailId())
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Attached customer do not exists!;"));
        basicParametersResponse.setCustomerId(customerDetails.getCustomerId());
        basicParametersResponse.setCustomerType(getCustomerType(customerDetails.getCustomerId()));
        basicParametersResponse.setCustomerDetailId(customerDetails.getId());
        basicParametersResponse.setCustomerVersionId(customerDetails.getVersionId());
        basicParametersResponse.setBusinessActivity(customerDetails.getBusinessActivity());
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(customerDetails.getName());
        stringBuilder.append(" ");
        if (customerDetails.getMiddleName() != null) {
            stringBuilder.append(customerDetails.getMiddleName());
            stringBuilder.append(" ");
        }
        if (customerDetails.getLastName() != null) {
            stringBuilder.append(customerDetails.getLastName());
            stringBuilder.append(" ");
        }
        String legalFormName = customerDetailsRepository.getLegalFormName(customerDetails.getId());
        if (StringUtils.isNotEmpty(legalFormName)) {
            stringBuilder.append(legalFormName);
        }

        Optional<CustomerCommunications> billingComsOptional = communicationsRepository.findByIdAndStatuses(details.getCustomerCommunicationIdForBilling(), List.of(Status.ACTIVE));
        Optional<CustomerCommunications> contractComsOptional = communicationsRepository.findByIdAndStatuses(details.getCustomerCommunicationIdForContract(), List.of(Status.ACTIVE));

        if (billingComsOptional.isPresent()) {
            CustomerCommunications billingComs = billingComsOptional.get();
            basicParametersResponse.setBillingCommunicationData(new CustomerCommunicationDataResponse(billingComs.getId(), billingComs.getContactTypeName(), billingComs.getCreateDate()));
            basicParametersResponse.getBillingCommunicationData().setConcatPurposes(customerRepository.getConcatPurposeFromCustomerCommunicationData(billingComs.getId()));
        }

        if (contractComsOptional.isPresent()) {
            CustomerCommunications contractComs = contractComsOptional.get();
            basicParametersResponse.setContractCommunicationData(new CustomerCommunicationDataResponse(contractComs.getId(), contractComs.getContactTypeName(), contractComs.getCreateDate()));
            basicParametersResponse.getContractCommunicationData().setConcatPurposes(customerRepository.getConcatPurposeFromCustomerCommunicationData(contractComs.getId()));
        }

        Customer customer = customerRepository.findById(customerDetails.getCustomerId())
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Customer not found for given product!"));
        basicParametersResponse.setCustomerName("%s (%s)".formatted(customer.getIdentifier(), stringBuilder.toString()));
    }

    private CustomerType getCustomerType(Long customerId) {
        Optional<Customer> customerOptional = customerRepository.findById(customerId);
        return customerOptional.map(Customer::getCustomerType).orElse(null);
    }

    private BasicParametersResponse mapToBasicParameters(ProductContract contract, ProductContractDetails details) {
        BasicParametersResponse response = new BasicParametersResponse();
        response.setId(details.getContractId());
        response.setVersionId(details.getVersionId());
        response.setContractNumber(contract.getContractNumber());
        response.setCreationDate(contract.getCreateDate().toLocalDate());
        response.setStatus(contract.getContractStatus());
        response.setSubStatus(contract.getSubStatus());
        response.setStatusModifyDate(contract.getStatusModifyDate());
        response.setType(details.getType());
        response.setHasUntilAmount(details.getContractTermUntilTheAmount());
        response.setUntilAmount(details.getContractTermUntilTheAmountValue());
        response.setHasUntilVolume(details.getContractTermUntilTheVolume());
        response.setUntilVolume(details.getContractTermUntilTheVolumeValue());
        response.setProcurementLaw(details.getPublicProcurementLaw());
        if (details.getCurrencyId() != null) {
            currencyRepository.findByIdAndStatus(details.getCurrencyId(), List.of(NomenclatureItemStatus.ACTIVE, NomenclatureItemStatus.INACTIVE))
                    .ifPresent(x -> response.setUntilAmountCurrency(new CurrencyResponse(x)));
        }
        response.setUntilVolume(details.getContractTermUntilTheVolumeValue());
        response.setSigningDate(contract.getSigningDate());
        response.setEntryInForceDate(contract.getEntryIntoForceDate());
        response.setTerminationDate(contract.getTerminationDate());
        response.setContractTermEndDate(contract.getContractTermEndDate());
        response.setActivationDate(contract.getActivationDate());
        response.setStartOfInitialTerm(contract.getInitialTermDate());
        response.setPerpetuityDate(contract.getPerpetuityDate());
        response.setProcurementLaw(details.getPublicProcurementLaw());
        return response;
    }

    public void createBasicParameterSubObjects(ProductContractBasicParametersCreateRequest request, ProductContractDetails contractDetails, List<String> messages) {
        assignProductDetailVersionTypes(contractDetails, request, messages);
        productContractFilesService.assignProductContractFilesToProductContract(contractDetails, Optional.ofNullable(request.getFiles()).orElse(new ArrayList<>()), messages);
        productContractDocumentService.assignProductContractDocumentsToProductContract(contractDetails, Optional.ofNullable(request.getDocuments()).orElse(new ArrayList<>()), messages);
        relatedContractsAndOrdersService.createEntityRelations(
                contractDetails.getContractId(),
                RelatedEntityType.PRODUCT_CONTRACT,
                request.getRelatedEntities(),
                messages
        );
    }

    public void archiveFiles(ProductContractDetails contractDetails) {
        Optional<ProductContract> productContractOptional = productContractRepository
                .findById(contractDetails.getContractId());

        Optional<CustomerDetails> customerDetailsOptional = customerDetailsRepository
                .findById(contractDetails.getCustomerDetailId());

        Optional<Customer> customerOptional = Optional.empty();
        if (customerDetailsOptional.isPresent()) {
            customerOptional = customerRepository.findById(customerDetailsOptional.get().getCustomerId());
        }

        if (productContractOptional.isPresent()) {
            List<ProductContractFile> productContractFiles = productContractFileRepository.findAllByContractDetailIdAndStatusIn(contractDetails.getId(), List.of(EntityStatus.ACTIVE));
            if (CollectionUtils.isNotEmpty(productContractFiles)) {
                for (ProductContractFile productContractFile : productContractFiles) {
                    try {
                        productContractFile.setNeedArchive(true);
                        productContractFile.setArchivationConstraints(EDMSArchivationConstraints.DOCUMENT_TYPE_PRODUCT_CONTRACT_FILE);
                        productContractFile.setAttributes(
                                List.of(
                                        new Attribute(attributeProperties.getDocumentTypeGuid(), EDMSArchivationConstraints.DOCUMENT_TYPE_PRODUCT_CONTRACT_FILE),
                                        new Attribute(attributeProperties.getDocumentNumberGuid(), productContractOptional.map(ProductContract::getContractNumber).orElse("")),
                                        new Attribute(attributeProperties.getDocumentDateGuid(), LocalDateTime.now()),
                                        new Attribute(attributeProperties.getCustomerIdentifierGuid(), customerOptional.map(Customer::getIdentifier).orElse("")),
                                        new Attribute(attributeProperties.getCustomerNumberGuid(), customerOptional.isPresent() ? customerOptional.get().getCustomerNumber() : ""),
                                        new Attribute(attributeProperties.getSignedGuid(), false)
                                )
                        );

                        fileArchivationService.archive(productContractFile);
                    } catch (Exception e) {
                        log.error("Cannot archive file: ", e);
                    }
                }
            }
        }
    }

    public void archiveDocuments(ProductContractDetails contractDetails) {
        Optional<ProductContract> productContractOptional = productContractRepository
                .findById(contractDetails.getContractId());

        Optional<CustomerDetails> customerDetailsOptional = customerDetailsRepository
                .findById(contractDetails.getCustomerDetailId());

        Optional<Customer> customerOptional = Optional.empty();
        if (customerDetailsOptional.isPresent()) {
            customerOptional = customerRepository.findById(customerDetailsOptional.get().getCustomerId());
        }

        if (productContractOptional.isPresent()) {
            List<ProductContractDocument> productContractDocuments = productContractDocumentRepository.findAllByContractDetailIdAndStatusIn(contractDetails.getId(), List.of(EntityStatus.ACTIVE));
            if (CollectionUtils.isNotEmpty(productContractDocuments)) {
                for (ProductContractDocument productContractDocument : productContractDocuments) {
                    try {
                        productContractDocument.setNeedArchive(true);
                        productContractDocument.setArchivationConstraints(EDMSArchivationConstraints.DOCUMENT_TYPE_PRODUCT_CONTRACT_DOCUMENT);
                        productContractDocument.setAttributes(
                                List.of(
                                        new Attribute(attributeProperties.getDocumentTypeGuid(), EDMSArchivationConstraints.DOCUMENT_TYPE_PRODUCT_CONTRACT_DOCUMENT),
                                        new Attribute(attributeProperties.getDocumentNumberGuid(), productContractOptional.map(ProductContract::getContractNumber).orElse("")),
                                        new Attribute(attributeProperties.getDocumentDateGuid(), LocalDateTime.now()),
                                        new Attribute(attributeProperties.getCustomerIdentifierGuid(), customerOptional.map(Customer::getIdentifier).orElse("")),
                                        new Attribute(attributeProperties.getCustomerNumberGuid(), customerOptional.isPresent() ? customerOptional.get().getCustomerNumber() : ""),
                                        new Attribute(attributeProperties.getSignedGuid(), false)
                                )
                        );

                        fileArchivationService.archive(productContractDocument);
                    } catch (Exception e) {
                        log.error("Cannot archive file: ", e);
                    }
                }
            }
        }
    }

    private void assignProductDetailVersionTypes(ProductContractDetails details,
                                                 ProductContractBasicParametersCreateRequest request,
                                                 List<String> errorMessages) {
        List<ProductContractVersionTypes> versionTypes = new ArrayList<>();
        for (Long versionTypeId : request.getVersionTypeIds()) {
            if (!contractVersionTypesRepository.existsByIdAndStatusIn(versionTypeId, List.of(NomenclatureItemStatus.ACTIVE))) {
                errorMessages.add("basicParameters.versionTypeIds-Invalid Version type with id[%s] provided!;".formatted(versionTypeId));
                continue;
            }
            ProductContractVersionTypes productContractVersionTypes = new ProductContractVersionTypes();
            productContractVersionTypes.setContractDetailId(details.getId());
            productContractVersionTypes.setStatus(ContractSubObjectStatus.ACTIVE);
            productContractVersionTypes.setContractVersionTypeId(versionTypeId);
            versionTypes.add(productContractVersionTypes);
        }

        if (errorMessages.isEmpty()) {
            productContractVersionTypeRepository.saveAll(versionTypes);
        }


    }

    @Transactional
    public ProductContractDetails edit(ProductContract productContract,
                                       ProductContractUpdateRequest updateRequest,
                                       ProductContractDetails productContractDetails,
                                       List<String> messages) {

        boolean hasAdditionalAgreementPermission = permissionService.permissionContextContainsPermissions(
                PRODUCT_CONTRACTS,
                List.of(PRODUCT_CONTRACT_EDIT_ADDITIONAL_AGREEMENTS)
        );
        boolean hasEditLockedPermission = permissionService.permissionContextContainsPermissions(
                PRODUCT_CONTRACTS,
                List.of(PRODUCT_CONTRACT_EDIT_LOCKED)
        );
        boolean hasEditPermission = permissionService.permissionContextContainsPermissions(
                PRODUCT_CONTRACTS,
                List.of(PRODUCT_CONTRACT_EDIT)
        );
        if (hasAdditionalAgreementPermission && !hasEditLockedPermission) {
            if (!hasEditPermission && !updateRequest.isSavingAsNewVersion()) {
                throw new OperationNotAllowedException("Is not allowed change current version");
            }
            if (!hasEditPermission) {
                updateRequest.getBasicParameters().setVersionStatus(READY);
            }
            if (!hasEditPermission) {
                updateRequest.getBasicParameters().setType(ContractDetailType.ADDITIONAL_AGREEMENT);
            }
            productContractDraftValidationService.draftContractValidationForAdditionalAgreementUser(
                    productContract.getId(),
                    updateRequest,
                    productContractDetails,
                    messages
            );
        }
        if (updateRequest.getBasicParameters().getVersionStatus() == SIGNED) {
            productContractBillingLockValidationService.LockedContractValidationOnEditContract(
                    productContract.getId(),
                    updateRequest,
                    productContractDetails,
                    messages);
        }

        Long productContractId = productContract.getId();

        ProductContractBasicParametersCreateRequest basicParameters = updateRequest.getBasicParameters();
        ProductContractDetails contractDetailsUpdating;
        ProductDetails productDetails;
        boolean updatingNewVersion = updateRequest.isSavingAsNewVersion();
        int nextVersionId;
        if (updatingNewVersion) {
            nextVersionId = productContractDetailsRepository.findMaxVersionId(productContractId) + 1;
            contractDetailsUpdating = new ProductContractDetails();
            if (productDetailsRepository.versionWithStartDateExists(productContractId, updateRequest.getStartDate())) {
                messages.add("startDate-Contract version already has provided start date!;");
            }
            Optional<ProductContractDetails> firstVersion = productContractDetailsRepository.findByContractIdAndVersionId(productContractId, 1);
            if (firstVersion.isPresent()) {
                LocalDate firstVersionStartDate = firstVersion.get().getStartDate();
                if (updateRequest.getStartDate().isBefore(firstVersionStartDate)) {
                    messages.add("startDate-Start date must be after the start date of the first version;");
                }
            }

            contractDetailsUpdating.setContractId(productContractId);
            productDetails = getProductForUpdate(productContractDetails, basicParameters, updateRequest.getProductParameters());
            if (!Objects.equals(Boolean.TRUE, productDetails.getGlobalSalesChannel()) && !productDetails.getId().equals(productContractDetails.getProductDetailId())) {
                checkProductSaleChannels(productDetails.getId(), messages);
            }
        } else {
            contractDetailsUpdating = productContractDetails;
            nextVersionId = contractDetailsUpdating.getVersionId();
            if (productContractDetails.getVersionId() == 1 && (updateRequest.getBasicParameters().getSigningDate() == null || updateRequest.getBasicParameters().getSigningDate().equals(LocalDate.now()))
                    && !productContractDetails.getStartDate().equals(updateRequest.getStartDate())) {
                messages.add("startDate-Start date must not be changed for this version;");
            }
            productDetails = productDetailsRepository.findByProductIdAndVersion(basicParameters.getProductId(), basicParameters.getProductVersionId())
                    .orElseThrow(() -> new DomainEntityNotFoundException("id-product details do not exists on selected contract!"));
            Product product = productDetails.getProduct();
            if (!product.getProductStatus().equals(ProductStatus.ACTIVE)) {
                throw new DomainEntityNotFoundException("id-Product do not exists on selected contract!");
            }
            if (!Objects.equals(Boolean.TRUE, productDetails.getGlobalSalesChannel()) && !productDetails.getId().equals(productContractDetails.getProductDetailId())) {
                checkProductSaleChannels(productDetails.getId(), messages);
            }
            individualProductCheck(product, productContractDetails.getId(), messages);
        }

        updateStartAndEndDates(updateRequest, productContractId, contractDetailsUpdating, nextVersionId);

        updateContractDetails(contractDetailsUpdating, productContractDetails, basicParameters, nextVersionId, messages);
        Optional<CustomerDetails> customerDetailsOptional = customerDetailsRepository.findByCustomerIdAndVersionId(basicParameters.getCustomerId(), basicParameters.getCustomerVersionId());
        if (customerDetailsOptional.isEmpty()) {
            messages.add("customerVersionId-Customer version not found");
        }
        if (customerDetailsOptional.isPresent()) {
            CustomerDetails customerDetails = customerDetailsOptional.get();
            if (!List.of(CustomerDetailStatus.NEW, CustomerDetailStatus.ACTIVE, CustomerDetailStatus.LOST).contains(customerDetails.getStatus())) {
                messages.add("basicParameters.customerVersionId-Can not conclude contract because customer has wrong status!;");
            }
            Optional<Customer> customerOptional = customerRepository.findByIdAndStatuses(basicParameters.getCustomerId(), List.of(CustomerStatus.ACTIVE));
            if (customerOptional.isEmpty()) {
                messages.add("customerId-Customer can not be found;");
            } else {
                if (!Objects.equals(productContractDetails.getCustomerDetailId(), customerDetails.getId())) {
                    Customer customer = customerOptional.get();
                    UnwantedCustomer unwantedCustomer = unwantedCustomerService.checkUnwantedCustomer(customer.getIdentifier());
                    if (unwantedCustomer != null) {
                        if (Boolean.TRUE.equals(unwantedCustomer.getCreateContractRestriction()) && unwantedCustomer.getStatus().equals(UnwantedCustomerStatus.ACTIVE)) {
                            messages.add("customerId-Customer is unwanted and restricted to create contract;");
                        }
                    }
                    if (productDetails.getProduct().getCustomerIdentifier() == null && !Objects.equals(Boolean.TRUE, productDetails.getGlobalSegment())) {
                        checkSegments(productDetails, customerDetails, messages);
                    }
                }

                validateManuallyAddedDealNumber(productContractId, updateRequest.getAdditionalParameters().getDealNumber(), customerOptional.get().getIdentifier(), messages);

                contractDetailsUpdating.setCustomerDetailId(
                        updateRequest.getBasicParameters().getCustomerNewDetailsId() != null ?
                                updateRequest.getBasicParameters().getCustomerNewDetailsId() :
                                customerDetails.getId());
            }
        }
        contractDetailsUpdating.setProductDetailId(productDetails.getId());
        createBillingCommunicationData(basicParameters.getCommunicationDataBillingId(), contractDetailsUpdating, messages);
        createContractCommunicationData(basicParameters.getCommunicationDataContractId(), contractDetailsUpdating, messages);

        relatedContractsAndOrdersService.updateEntityRelations(
                productContractId,
                RelatedEntityType.PRODUCT_CONTRACT,
                updateRequest.getBasicParameters().getRelatedEntities(),
                messages
        );

        archiveFiles(contractDetailsUpdating);
        archiveDocuments(contractDetailsUpdating);
        return contractDetailsUpdating;
    }

    public void updateProductContractDocuments(ProductContractUpdateRequest updateRequest,
                                               ProductContractDetails initialProductContractDetails,
                                               ProductContractDetails contractDetailsUpdating,
                                               List<String> messages) {
        productContractDocumentService.updateProductContractDocumentsOnProductContract(
                initialProductContractDetails,
                contractDetailsUpdating,
                updateRequest,
                messages
        );
    }

    public void updateProductContractFiles(ProductContractUpdateRequest updateRequest,
                                           ProductContractDetails initialProductContractDetails,
                                           ProductContractDetails contractDetailsUpdating,
                                           List<String> messages) {
        productContractFilesService.updateProductContractFilesOnProductContract(
                initialProductContractDetails,
                contractDetailsUpdating,
                updateRequest,
                messages
        );
    }

    private void updateStartAndEndDates(ProductContractUpdateRequest updateRequest, Long productContractId, ProductContractDetails contractDetailsUpdating, int nextVersionId) {
        if (updateRequest.getBasicParameters().getVersionStatus() != SIGNED) {
            contractDetailsUpdating.setStartDate(updateRequest.getStartDate());
            contractDetailsUpdating.setVersionId(nextVersionId);
            return;
        }
        List<ProductContractDetails> productContractVersions = productContractDetailsRepository.
                findProductContractDetailsByContractIdAndVersionStatus(
                        productContractId,
                        SIGNED
                );
        List<VersionWithDatesModel> versionWithDatesModels = productContractVersions
                .stream()
                .map(VersionWithDatesModel::new)
                .collect(Collectors.toList());

        contractDetailsUpdating.setStartDate(updateRequest.getStartDate());
        contractDetailsUpdating.setVersionId(nextVersionId);
        List<VersionWithDatesModel> updatedVersionWithDatesModels = CalculateVersionDates
                .calculateVersionEndDates(
                        versionWithDatesModels,
                        updateRequest.getStartDate(),
                        nextVersionId
                );

        productContractVersions.add(contractDetailsUpdating);
        productContractVersions
                .forEach(pcv -> updatedVersionWithDatesModels.stream()
                        .filter(v -> Objects.equals(v.getVersionId(), pcv.getVersionId()))
                        .findFirst()
                        .ifPresent(model -> {
                            pcv.setEndDate(model.getEndDate());
                            pcv.setStartDate(model.getStartDate());
                        })
                );
    }

    private ProductDetails getProductForUpdate(ProductContractDetails details, ProductContractBasicParametersCreateRequest basicParameters, ProductParameterBaseRequest baseRequest) {
        ProductDetails productDetails = productDetailsRepository.findByProductIdAndVersion(basicParameters.getProductId(), basicParameters.getProductVersionId())
                .orElseThrow(() -> new DomainEntityNotFoundException("id-product details do not exists on selected contract!"));
        Product product = productDetails.getProduct();
        if (!product.getProductStatus().equals(ProductStatus.ACTIVE)) {
            throw new DomainEntityNotFoundException("id-Product do not exists on selected contract!");
        }
        if (product.getCustomerIdentifier() != null && !Objects.equals(productDetails.getId(), details.getProductDetailId())) {
            productContractRepository.existsByProductId(product.getId());
        } else if (product.getCustomerIdentifier() != null) {
            return productService.cloneForContract(productDetails.getProduct().getId(), productDetails.getVersion(), baseRequest);
        }
        return productDetails;
    }

    public void updateVersionTypes(ProductContractDetails sourceDetails, ProductContractDetails detailsUpdating, ProductContractBasicParametersCreateRequest createRequest, List<String> errorMessages) {
        Map<Long, ProductContractVersionTypes> productContractVersionTypesMap = productContractVersionTypeRepository.findProductContractVersionTypeForContractDetail(sourceDetails.getId())
                .stream().collect(Collectors.toMap(ProductContractVersionTypes::getContractVersionTypeId, j -> j));
        List<ProductContractVersionTypes> versionTypesToSave = new ArrayList<>();
        boolean isNewVersion = !detailsUpdating.getId().equals(sourceDetails.getId());
        for (Long versionTypeId : createRequest.getVersionTypeIds()) {
            ProductContractVersionTypes types = productContractVersionTypesMap.remove(versionTypeId);
            if (types == null) {
                if (!contractVersionTypesRepository.existsByIdAndStatusIn(versionTypeId, List.of(NomenclatureItemStatus.ACTIVE))) {
                    errorMessages.add("basicParameters.versionTypeIds-Invalid Version type with id[%s] provided!;".formatted(versionTypeId));
                    continue;
                }
                ProductContractVersionTypes productContractVersionTypes = new ProductContractVersionTypes();
                productContractVersionTypes.setContractDetailId(detailsUpdating.getId());
                productContractVersionTypes.setStatus(ContractSubObjectStatus.ACTIVE);
                productContractVersionTypes.setContractVersionTypeId(versionTypeId);
                versionTypesToSave.add(productContractVersionTypes);
            } else if (isNewVersion) {
                ProductContractVersionTypes productContractVersionTypes = new ProductContractVersionTypes();
                productContractVersionTypes.setContractDetailId(detailsUpdating.getId());
                productContractVersionTypes.setStatus(ContractSubObjectStatus.ACTIVE);
                productContractVersionTypes.setContractVersionTypeId(versionTypeId);
                versionTypesToSave.add(productContractVersionTypes);
            }
        }
        Collection<ProductContractVersionTypes> values = productContractVersionTypesMap.values();
        if (!isNewVersion) {
            for (ProductContractVersionTypes value : values) {
                value.setStatus(ContractSubObjectStatus.DELETED);
                versionTypesToSave.add(value);
            }
        }

        productContractVersionTypeRepository.saveAll(versionTypesToSave);

    }

    private void validateManuallyAddedDealNumber(Long contractId, String dealNumber, String customerIdentifier, List<String> exceptionMessages) {
        if (StringUtils.isEmpty(dealNumber)) {
            return;
        }

        try {
            if (!xEnergieRepository.isDealExistsForCustomer(dealNumber, customerIdentifier)) {
                exceptionMessages.add("additionalParameters.dealNumber-Deal number does not exist for this customer");
            }
        } catch (Exception e) {
            exceptionMessages.add("additionalParameters.dealNumber-Deal number validation failed from xEnergie database side");
        }

        List<ProductContract> productContractsWithSameDealNumber = productContractDetailsRepository
                .findProductContractDetailsWithPresentedDealNumberExcludingContractId(contractId, dealNumber);

        List<String> generatorProductContractPointOfDeliveriesWithDealNumber = contractPodRepository
                .findGeneratorProductContractPointOfDeliveriesWithDealNumber(dealNumber, List.of(EntityStatus.ACTIVE));

        if (CollectionUtils.isNotEmpty(productContractsWithSameDealNumber)) {
            productContractsWithSameDealNumber
                    .forEach(pc -> exceptionMessages.add("additionalParameters.dealNumber-Deal number is already added in another contract [%s]".formatted(pc.getContractNumber())));
        }

        if (CollectionUtils.isNotEmpty(generatorProductContractPointOfDeliveriesWithDealNumber)) {
            generatorProductContractPointOfDeliveriesWithDealNumber
                    .forEach(generator -> exceptionMessages.add("additionalParameters.dealNumber-Deal number is already added in another contract [%s]".formatted(generator)));
        }
    }
}
