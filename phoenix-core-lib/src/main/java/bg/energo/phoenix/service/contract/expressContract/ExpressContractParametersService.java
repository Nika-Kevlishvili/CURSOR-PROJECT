package bg.energo.phoenix.service.contract.expressContract;

import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.model.entity.contract.InterestRate.InterestRate;
import bg.energo.phoenix.model.entity.contract.product.ProductContract;
import bg.energo.phoenix.model.entity.contract.product.ProductContractDetails;
import bg.energo.phoenix.model.entity.contract.product.ProductContractVersionTypes;
import bg.energo.phoenix.model.entity.contract.service.ServiceContractDetails;
import bg.energo.phoenix.model.entity.contract.service.ServiceContracts;
import bg.energo.phoenix.model.entity.nomenclature.contract.Campaign;
import bg.energo.phoenix.model.entity.nomenclature.contract.ContractVersionType;
import bg.energo.phoenix.model.entity.nomenclature.customer.Bank;
import bg.energo.phoenix.model.entity.product.product.Product;
import bg.energo.phoenix.model.entity.product.product.ProductAdditionalParams;
import bg.energo.phoenix.model.entity.product.product.ProductDetails;
import bg.energo.phoenix.model.entity.product.service.EPService;
import bg.energo.phoenix.model.entity.product.service.ServiceAdditionalParams;
import bg.energo.phoenix.model.entity.product.service.ServiceDetails;
import bg.energo.phoenix.model.entity.product.term.terms.Terms;
import bg.energo.phoenix.model.enums.contract.InterestRate.InterestRateStatus;
import bg.energo.phoenix.model.enums.contract.express.ProductContractVersionStatus;
import bg.energo.phoenix.model.enums.contract.products.ContractDetailType;
import bg.energo.phoenix.model.enums.contract.products.ContractSubObjectStatus;
import bg.energo.phoenix.model.enums.contract.service.ContractVersionStatus;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.product.product.ProductStatus;
import bg.energo.phoenix.model.enums.product.service.ServiceStatus;
import bg.energo.phoenix.model.enums.product.term.terms.ContractEntryIntoForce;
import bg.energo.phoenix.model.enums.product.term.terms.StartOfContractInitialTerm;
import bg.energo.phoenix.model.request.contract.express.ExpressContractBankingDetails;
import bg.energo.phoenix.model.request.contract.express.ExpressContractParameters;
import bg.energo.phoenix.model.response.contract.express.CustomerExpressContractDto;
import bg.energo.phoenix.repository.contract.product.ProductContractRepository;
import bg.energo.phoenix.repository.contract.product.ProductContractVersionTypeRepository;
import bg.energo.phoenix.repository.contract.service.ServiceContractsRepository;
import bg.energo.phoenix.repository.interestRate.InterestRateRepository;
import bg.energo.phoenix.repository.nomenclature.contract.CampaignRepository;
import bg.energo.phoenix.repository.nomenclature.contract.ContractVersionTypesRepository;
import bg.energo.phoenix.repository.nomenclature.customer.BankRepository;
import bg.energo.phoenix.repository.product.product.ProductDetailsRepository;
import bg.energo.phoenix.repository.product.product.ProductRepository;
import bg.energo.phoenix.repository.product.service.ServiceAdditionalParamsRepository;
import bg.energo.phoenix.repository.product.service.ServiceDetailsRepository;
import bg.energo.phoenix.repository.product.service.ServiceRepository;
import bg.energo.phoenix.security.PermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExpressContractParametersService {
    private final ServiceDetailsRepository serviceDetailsRepository;
    private final ServiceRepository serviceRepository;


    private final BankRepository bankRepository;
    private final ProductRepository productRepository;
    private final ProductDetailsRepository productDetailsRepository;
    private final ProductContractRepository productContractRepository;
    private final InterestRateRepository interestRateRepository;
    private final ProductContractVersionTypeRepository contractVersionTypeRepository;
    private final ContractVersionTypesRepository versionTypesRepository;

    private final ServiceContractsRepository serviceContractsRepository;

    private final CampaignRepository campaignRepository;
    private final ServiceAdditionalParamsRepository serviceAdditionalParamsRepository;
    private final PermissionService permissionService;


    @Transactional(propagation = Propagation.MANDATORY)
    public ProductContractDetails createProductContractDetail(ExpressContractParameters request,
                                                              ProductContract productContract,
                                                              Long customerDetailId,
                                                              List<String> messages) {
        ProductContractDetails productContractDetails = new ProductContractDetails();
        productContractDetails.setContractId(productContract.getId());
        productContractDetails.setCustomerDetailId(customerDetailId);
        productContractDetails.setVersionId(1);
        productContractDetails.setVersionStatus(ProductContractVersionStatus.SIGNED);
        productContractDetails.setCampaignId(getCampaign(request.getCampaignId()));
        //Todo change with risk integration
        productContractDetails.setRiskAssessment("approved");
        productContractDetails.setRiskAssessmentAdditionalCondition("none;");
        fillProductContractDetails(productContractDetails, request);
        if (productContract.getSigningDate() != null && productContract.getSigningDate().isBefore(LocalDate.now())) {
            productContractDetails.setStartDate(productContract.getSigningDate());
        } else {
            productContractDetails.setStartDate(LocalDate.now());
        }
        Optional<Product> productOptional = productRepository.findByIdAndProductStatusIn(
                request.getProductId(),
                List.of(ProductStatus.ACTIVE)
        );
        if (productOptional.isEmpty()) {
            messages.add("expressContractParameters.productId-Product not found!;");
        }
        productOptional.ifPresent(product -> individualProductCheck(product, messages));
        Optional<ProductDetails> productDetailsOptional = productDetailsRepository.findByProductIdAndVersion(
                request.getProductId(),
                request.getProductVersionId()
        );
        if (productDetailsOptional.isEmpty()) {
            messages.add("expressContractParameters.productVersionId-Product version not found!;");
        }
        if (productDetailsOptional.isPresent()) {
            ProductDetails productDetails = productDetailsOptional.get();
            checkProductDetailsAdditionalParams(productDetails, messages);
            productContractDetails.setProductDetailId(productDetails.getId());
            fillTermsProduct(productDetails, productContract);
            if (productDetailsRepository.canCreateExpressContractForProductDetail(productDetails.getId()) <= 0) {
                messages.add("expressContractParameters.productVersionId-Product version is not suitable for express contract!;");
            }
            if (!Objects.equals(Boolean.TRUE, productDetails.getGlobalSalesChannel())) {
                checkProductSaleChannels(productDetails.getId(), messages);
            }
        }
        ExpressContractBankingDetails bankingDetails = request.getBankingDetails();
        Long bankId = bankingDetails.getBankId();
        if (bankId != null) {
            Optional<Bank> bankOptional = bankRepository.findByIdAndStatus(bankId, List.of(NomenclatureItemStatus.ACTIVE));
            if (bankOptional.isEmpty()) {
                messages.add("expressContractParameters.bankingDetails.bankId-Bank not found!;");
            } else {
                productContractDetails.setBankId(bankingDetails.getBankId());
                productContractDetails.setDirectDebit(bankingDetails.getDirectDebit());
                productContractDetails.setIban(bankingDetails.getIban());
            }
        }
        setInterestRate(productContractDetails);
        return productContractDetails;
    }

    private void checkProductSaleChannels(Long id, List<String> messages) {
        if (!productRepository.checkSegments(id, permissionService.getLoggedInUserId())) {
            messages.add("basicParameters.productId-You do not have permission to create contract with this product!;");
        }
    }

    private void checkServiceSaleChannels(Long id, List<String> messages) {
        if (!serviceRepository.checkSegments(id, permissionService.getLoggedInUserId())) {
            messages.add("expressContractParameters.productId-You do not have permission to create contract with this Service!;");
        }
    }

    private void fillTermsProduct(ProductDetails productDetails, ProductContract productContract) {
        Terms term = productDetails.getTerms();
        if (term != null) {
            LocalDate signingDate = productContract.getSigningDate();
            if (term.getContractEntryIntoForces() != null && term.getContractEntryIntoForces().contains(ContractEntryIntoForce.SIGNING)) {
                productContract.setEntryIntoForceDate(signingDate);
            }
            if (term.getStartsOfContractInitialTerms() != null) {
                if (term.getStartsOfContractInitialTerms().contains(StartOfContractInitialTerm.SIGNING)) {
                    productContract.setInitialTermDate(signingDate);
                } else if (term.getStartsOfContractInitialTerms().contains(StartOfContractInitialTerm.FIRST_DAY_MONTH_SIGNING) && signingDate != null) {
                    int maxDay = YearMonth.now().lengthOfMonth();
                    Integer contractDay = term.getFirstDayOfTheMonthOfInitialContractTerm();
                    int day = contractDay > maxDay ? maxDay : contractDay;
                    LocalDate calculatedDate = null;
                    if (day >= signingDate.getDayOfMonth()) {
                        LocalDate localDate = signingDate.plusMonths(1);

                        calculatedDate = LocalDate.of(localDate.getYear(), localDate.getMonth(), 1);
                    } else {
                        LocalDate localDate = signingDate.plusMonths(2);
                        calculatedDate = LocalDate.of(localDate.getYear(), localDate.getMonth(), 1);
                    }
                    productContract.setInitialTermDate(calculatedDate);
                }

            }
        }
    }

    private void checkProductDetailsAdditionalParams(ProductDetails productDetails, List<String> messages) {
        for (ProductAdditionalParams params : productDetails.getProductAdditionalParams()) {
            if (params.getValue() == null && params.getLabel() != null) {
                messages.add("product must have filled additional param values;");
                break;
            }
        }
    }

    private Long getCampaign(Long campaignId) {
        Optional<Campaign> campaignOptional = campaignRepository.findByIdAndStatusIn(campaignId, List.of(NomenclatureItemStatus.ACTIVE));
        return campaignOptional.map(Campaign::getId).orElse(null);
    }

    private void setInterestRate(ProductContractDetails productContractDetails) {
        InterestRate interestRate = interestRateRepository.findByIsDefaultAndStatus(true, InterestRateStatus.ACTIVE)
                .orElseThrow(() ->
                        new DomainEntityNotFoundException("Interest rate do not exist!;")
                );
        productContractDetails.setApplicableInterestRate(interestRate.getId());
    }

    private void setInterestRateForServiceContract(ServiceContractDetails serviceContractDetails) {
        InterestRate interestRate = interestRateRepository.findByIsDefaultAndStatus(true, InterestRateStatus.ACTIVE)
                .orElseThrow(() ->
                        new DomainEntityNotFoundException("Interest rate do not exist!;")
                );
        serviceContractDetails.setApplicableInterestRate(interestRate.getId());
    }

    public void setVersionType(ProductContractDetails productContractDetails) {
        ContractVersionType versionType = versionTypesRepository.findByIsDefaultTrue()
                .orElseThrow(() ->
                        new DomainEntityNotFoundException("Contract can't be created because 'Contract version type' doesn't have default value;")
                );
        ProductContractVersionTypes productContractVersionTypes = new ProductContractVersionTypes();
        productContractVersionTypes.setStatus(ContractSubObjectStatus.ACTIVE);
        productContractVersionTypes.setContractVersionTypeId(versionType.getId());
        productContractVersionTypes.setContractDetailId(productContractDetails.getId());
        contractVersionTypeRepository.save(productContractVersionTypes);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public ServiceContractDetails createForService(ExpressContractParameters request, ServiceContracts serviceContracts, CustomerExpressContractDto customerDetailId, List<String> errorMessages) {
        ServiceContractDetails serviceContractDetails = new ServiceContractDetails();
        serviceContractDetails.setContractId(serviceContracts.getId());
        serviceContractDetails.setCustomerDetailId(customerDetailId.getCustomerDetailId());
        serviceContractDetails.setVersionId(1L);
        serviceContractDetails.setContractVersionStatus(ContractVersionStatus.READY);
        if (serviceContracts.getSigningDate() != null && serviceContracts.getSigningDate().isBefore(LocalDate.now())) {
            serviceContractDetails.setStartDate(serviceContracts.getSigningDate());
        } else {
            serviceContractDetails.setStartDate(LocalDate.now());
        }
        Optional<EPService> serviceOptional = serviceRepository.findByIdAndStatusIn(request.getProductId(), List.of(ServiceStatus.ACTIVE));
        if (serviceOptional.isEmpty()) {
            errorMessages.add("expressContractParameters.productId-Service not found!;");
        }
        if (serviceOptional.isPresent()) {
            individualServiceCheck(serviceOptional.get(), serviceContracts.getId(), errorMessages);
        }
        Optional<ServiceDetails> serviceDetailsOptional = serviceDetailsRepository.findByServiceIdAndVersion(request.getProductId(), request.getProductVersionId());
        if (serviceDetailsOptional.isEmpty()) {
            errorMessages.add("expressContractParameters.productVersionId-Service version not found!;");
        }
        if (serviceDetailsOptional.isPresent()) {
            ServiceDetails serviceDetails = serviceDetailsOptional.get();
            checkServiceDetailsAdditionalParams(serviceDetails, errorMessages);
            serviceContractDetails.setServiceDetailId(serviceDetails.getId());
            fillTermsService(serviceDetails, serviceContracts);
            if (serviceDetailsRepository.canCreateExpressContractForServiceDetails(serviceDetails.getId(), customerDetailId.getCustomerDetailId(), "%%") <= 0) {
                errorMessages.add("expressContractParameters.productVersionId-Service version is not suitable for express contract!;"); //TODO UNCOMMENT
            }
            if (!Objects.equals(Boolean.TRUE, serviceDetails.getGlobalSalesChannel())) {
                checkServiceSaleChannels(serviceDetails.getId(), errorMessages);
            }
        }
        ExpressContractBankingDetails bankingDetails = request.getBankingDetails();
        if (bankingDetails != null) {
            if (bankingDetails.getBankId() != null) {
                Optional<Bank> bankOptional = bankRepository.findByIdAndStatus(bankingDetails.getBankId(), List.of(NomenclatureItemStatus.ACTIVE));
                if (bankOptional.isEmpty()) {
                    errorMessages.add("expressContractParameters.bankingDetails.bankId-Bank not found!;");
                } else {
                    serviceContractDetails.setBankId(bankingDetails.getBankId());
                    serviceContractDetails.setIban(bankingDetails.getIban());
                }
            }
            serviceContractDetails.setDirectDebit(bankingDetails.getDirectDebit());
        }
        setInterestRateForServiceContract(serviceContractDetails);
        return serviceContractDetails;
    }

    private void fillTermsService(ServiceDetails serviceDetails, ServiceContracts serviceContract) {
        Terms term = serviceDetails.getTerms();
        if (term != null) {
            LocalDate signingDate = serviceContract.getSigningDate();
            if (term.getContractEntryIntoForces() != null && term.getContractEntryIntoForces().contains(ContractEntryIntoForce.SIGNING)) {
                serviceContract.setEntryIntoForceDate(signingDate);
            }
            if (term.getStartsOfContractInitialTerms() != null) {
                if (term.getStartsOfContractInitialTerms().contains(StartOfContractInitialTerm.SIGNING)) {
                    serviceContract.setContractInitialTermStartDate(signingDate);
                } else if (term.getStartsOfContractInitialTerms().contains(StartOfContractInitialTerm.FIRST_DAY_MONTH_SIGNING) && signingDate != null) {
                    int maxDay = YearMonth.now().lengthOfMonth();
                    Integer contractDay = term.getFirstDayOfTheMonthOfInitialContractTerm();
                    int day = contractDay > maxDay ? maxDay : contractDay;
                    LocalDate calculatedDate = null;
                    if (day >= signingDate.getDayOfMonth()) {
                        LocalDate localDate = signingDate.plusMonths(1);

                        calculatedDate = LocalDate.of(localDate.getYear(), localDate.getMonth(), 1);
                    } else {
                        LocalDate localDate = signingDate.plusMonths(2);
                        calculatedDate = LocalDate.of(localDate.getYear(), localDate.getMonth(), 1);
                    }
                    serviceContract.setContractInitialTermStartDate(calculatedDate);
                }

            }

        }
    }

    private void checkServiceDetailsAdditionalParams(ServiceDetails serviceDetails, List<String> errorMessages) {
        for (ServiceAdditionalParams additionalParams : serviceAdditionalParamsRepository
                .findServiceAdditionalParamsByServiceDetailId(serviceDetails.getId())) {
            if (additionalParams.getValue() == null && additionalParams.getLabel() != null) {
                errorMessages.add("service must have filled additional params values;");
                break;
            }
        }
    }

    private void fillProductContractDetails(ProductContractDetails productContractDetails, ExpressContractParameters request) {
        productContractDetails.setType(ContractDetailType.CONTRACT);
        productContractDetails.setPublicProcurementLaw(request.isProcurementLaw());
        productContractDetails.setEstimatedTotalConsumptionUnderContractKwh(request.getEstimatedTotalConsumption());

    }

    private void individualProductCheck(Product product, List<String> messages) {
        if (product.getCustomerIdentifier() == null) {
            return;
        }
        if (productContractRepository.existsByProductId(product.getId())) {
            messages.add("basicParameters.productId-Individual Product with id %s is Already used!;".formatted(product.getId()));
        }
    }

    private void individualServiceCheck(EPService service, Long serviceContractDetailsId, List<String> errorMessages) {
        if (service.getCustomerIdentifier() == null) {
            return;
        }
        if (serviceContractsRepository.existsByServiceIdAndContractIdNotEquals(service.getId(), serviceContractDetailsId)) {
            errorMessages.add("basicParameters.productId-Individual Service with id %s is Already used!;".formatted(service.getId()));
        }
    }
}
