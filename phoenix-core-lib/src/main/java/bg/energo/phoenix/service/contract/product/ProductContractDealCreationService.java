package bg.energo.phoenix.service.contract.product;

import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.contract.billing.ContractPods;
import bg.energo.phoenix.model.entity.contract.product.ProductContract;
import bg.energo.phoenix.model.entity.contract.product.ProductContractDetails;
import bg.energo.phoenix.model.entity.customer.Customer;
import bg.energo.phoenix.model.entity.customer.CustomerDetails;
import bg.energo.phoenix.model.entity.product.price.priceComponent.PriceComponentFormulaVariable;
import bg.energo.phoenix.model.entity.product.product.ProductDetails;
import bg.energo.phoenix.model.entity.product.product.ProductForBalancing;
import bg.energo.phoenix.model.enums.communication.xEnergie.XEnergieRepositoryCreateCustomerResponse;
import bg.energo.phoenix.model.enums.contract.express.ProductContractVersionStatus;
import bg.energo.phoenix.model.enums.customer.CustomerStatus;
import bg.energo.phoenix.model.request.communication.xEnergie.Parametric;
import bg.energo.phoenix.model.response.communication.xEnergie.ETRMDealDataResponse;
import bg.energo.phoenix.repository.contract.billing.ContractPodRepository;
import bg.energo.phoenix.repository.contract.product.ProductContractDetailsRepository;
import bg.energo.phoenix.repository.contract.product.ProductContractRepository;
import bg.energo.phoenix.repository.customer.CustomerDetailsRepository;
import bg.energo.phoenix.repository.customer.CustomerRepository;
import bg.energo.phoenix.repository.product.price.priceComponent.PriceComponentRepository;
import bg.energo.phoenix.repository.product.price.priceComponentGroup.PriceComponentGroupRepository;
import bg.energo.phoenix.repository.product.product.ProductDetailsRepository;
import bg.energo.phoenix.service.contract.product.dealCreationEvent.ProductContractDealCreationEvent;
import bg.energo.phoenix.service.contract.product.dealCreationEvent.ProductContractDealCreationEventPublisher;
import bg.energo.phoenix.service.xEnergie.XEnergieCommunicationService;
import bg.energo.phoenix.service.xEnergie.XEnergieRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductContractDealCreationService {
    private final ProductContractDetailsRepository productContractDetailsRepository;
    private final ContractPodRepository contractPodRepository;
    private final ProductDetailsRepository productDetailsRepository;
    private final CustomerDetailsRepository customerDetailsRepository;
    private final CustomerRepository customerRepository;
    private final XEnergieRepository xEnergieRepository;
    private final XEnergieCommunicationService xEnergieCommunicationService;
    private final ProductContractRepository productContractRepository;
    private final PriceComponentRepository priceComponentRepository;
    private final PriceComponentGroupRepository priceComponentGroupRepository;
    private final ProductContractDealCreationEventPublisher eventPublisher;
    private final Object lock = new Object();

    @Async
    @Transactional
    @EventListener(ProductContractDealCreationEvent.class)
    public void handleEvent(ProductContractDealCreationEvent event) {
        synchronized (lock) {
            ProductContractDetails productContractDetails = event.getProductContractDetails();
            if (productContractDetails.getVersionStatus() != ProductContractVersionStatus.SIGNED) {
                return;
            }

            ProductContract productContract = productContractRepository
                    .findById(productContractDetails.getContractId())
                    .orElseThrow(() -> new DomainEntityNotFoundException("Deal creation failed, Product Contract with id: [%s] not found;".formatted(productContractDetails.getContractId())));

            boolean isDealAlreadyCreatedForThisContract = Objects.nonNull(productContractDetails.getDealNumber());
            if (isDealAlreadyCreatedForThisContract) {
            /*
              Deal already created for this contract, future logics not required
             */
            } else {
                Optional<ProductContractDetails> previousVersionOptional = productContractDetailsRepository
                        .findPreviousProductContractDetailsDependingOnStartDate(
                                productContractDetails.getContractId(),
                                productContractDetails.getStartDate(),
                                PageRequest.of(0, 1)
                        );
                boolean isUpdatedProductContractFirstVersion = previousVersionOptional.isEmpty();
                if (isUpdatedProductContractFirstVersion) {
                    createDealForProductContract(productContractDetails, productContract);
                } else {
                    ProductContractDetails previousVersion = previousVersionOptional.get();
                    boolean hasPreviousVersionActiveDeal = Objects.nonNull(previousVersion.getDealNumber());
                    if (hasPreviousVersionActiveDeal) {
                        boolean isCustomerSame = Objects.equals(previousVersion.getCustomerDetailId(), productContractDetails.getCustomerDetailId());
                        boolean isProductSame = Objects.equals(previousVersion.getProductDetailId(), productContractDetails.getProductDetailId());

                        if (isCustomerSame && isProductSame) {
                            List<ContractPods> currentActiveConsumers = contractPodRepository
                                    .findAllConsumersByContractDetailIdAndStatusIn(productContractDetails.getId(), List.of(EntityStatus.ACTIVE));

                            boolean atLeastOnePointOfDeliveryIsActivated =
                                    currentActiveConsumers
                                            .stream()
                                            .anyMatch(cp -> Objects.nonNull(cp.getActivationDate()));
                            if (atLeastOnePointOfDeliveryIsActivated) {
                                productContractDetails.setDealNumber(previousVersion.getDealNumber());

                                productContractDetailsRepository.save(productContractDetails);
                            }
                        } else {
                            createDealForProductContract(productContractDetails, productContract);
                        }
                    } else {
                        createDealForProductContract(productContractDetails, productContract);
                    }
                }
            }

            createDealForProductContractPointOfDelivery(productContract, productContractDetails);

            productContractDetailsRepository
                    .findNextProductContractDetailsDependingOnStartDate(
                            productContract.getId(),
                            productContractDetails.getStartDate(),
                            PageRequest.of(0, 1)
                    )
                    .ifPresent(
                            contractDetails ->
                                    eventPublisher.publishProductContractDealCreationEvent(new ProductContractDealCreationEvent(contractDetails))
                    );
        }
    }

    /**
     * Creates a deal for the given product contract and product contract details.
     *
     * @param productContract        The product contract.
     * @param productContractDetails The product contract details.
     */
    private void createDealForProductContractPointOfDelivery(ProductContract productContract,
                                                             ProductContractDetails productContractDetails) {
        ProductDetails productDetails = validateProductDetails(productContractDetails);
        if (productContractDetails.getVersionStatus() != ProductContractVersionStatus.SIGNED) {
            return;
        }

        if (Objects.nonNull(productDetails.getProductBalancingIdForGenerator())) {
            List<ContractPods> currentActiveGenerators = contractPodRepository
                    .findAllActivatedGeneratorsByContractDetailIdAndStatusInWithoutDeal(productContractDetails.getId(), List.of(EntityStatus.ACTIVE));
            for (ContractPods generator : currentActiveGenerators) {
                Optional<ProductContractDetails> previousProductContractDetailsOptional = productContractDetailsRepository
                        .findPreviousProductContractDetailsDependingOnStartDate(
                                productContractDetails.getContractId(),
                                productContractDetails.getStartDate(),
                                PageRequest.of(0, 1)
                        );
                if (previousProductContractDetailsOptional.isPresent()) {
                    ProductContractDetails previousVersion = previousProductContractDetailsOptional.get();

                    Optional<ContractPods> generatorInPreviousVersionOptional = contractPodRepository
                            .findGeneratorProductContractPointOfDeliveryByPointOfDeliveryDetailId(previousVersion.getId(), generator.getPodDetailId());
                    if (generatorInPreviousVersionOptional.isPresent()) {
                        ContractPods generatorInPreviousVersion = generatorInPreviousVersionOptional.get();

                        if (Objects.nonNull(generatorInPreviousVersion.getDealNumber())) {
                            boolean isCustomerSame = Objects.equals(previousVersion.getCustomerDetailId(), productContractDetails.getCustomerDetailId());
                            boolean isProductSame = Objects.equals(previousVersion.getProductDetailId(), productContractDetails.getProductDetailId());

                            if (isCustomerSame && isProductSame) {
                                generator.setDealNumber(generatorInPreviousVersion.getDealNumber());
                            } else {
                                createGeneratorDeal(productContract, productContractDetails, productDetails, generator);
                            }
                        } else {
                            createGeneratorDeal(productContract, productContractDetails, productDetails, generator);
                        }
                    } else {
                        createGeneratorDeal(productContract, productContractDetails, productDetails, generator);
                    }
                } else {
                    createGeneratorDeal(productContract, productContractDetails, productDetails, generator);
                }
            }
        }
    }

    /**
     * Creates a generator deal based on the given parameters.
     *
     * @param productContract        The product contract associated with the deal.
     * @param productContractDetails The details of the product contract.
     * @param productDetails         The details of the product.
     * @param generator              The contract pods for the generator.
     */
    private void createGeneratorDeal(ProductContract productContract,
                                     ProductContractDetails productContractDetails,
                                     ProductDetails productDetails,
                                     ContractPods generator) {
        List<PriceComponentFormulaVariable> priceComponentFormulaVariables = new ArrayList<>();

        priceComponentFormulaVariables.addAll(priceComponentRepository
                .findGeneratorPriceComponentFormulaVariablesByProductDetailId(productDetails.getId()));

        priceComponentFormulaVariables.addAll(priceComponentGroupRepository
                .findGeneratorPriceComponentGroupPriceComponentFormulaVariablesByProductDetailId(productDetails.getId()));

        priceComponentFormulaVariables.removeIf(
                priceComponentFormulaVariable ->
                        Objects.isNull(priceComponentFormulaVariable.getProfileForBalancing())
        );

        CustomerDetails customerDetails = validateCustomerDetails(productContractDetails);
        Customer customer = validateCustomer(customerDetails);
        createCustomerIfRequired(customerDetails, customer);

        try {
            String customerNumber = String.valueOf(customer.getCustomerNumber());
            String contractNumber = String.valueOf(productContract.getContractNumber());
            String productBalancingName = productDetails.getProductBalancingIdForGenerator().getName();

            LocalDateTime dealStartDate = generator.getActivationDate().minusDays(1).atTime(23, 0);

            LocalDateTime dealEndDate = LocalDate.of(2030, 12, 30).atTime(23, 0);
            if (Objects.isNull(generator.getDeactivationDate())) {
                if (!Objects.isNull(productContract.getContractTermEndDate())) {
                    dealEndDate = productContract.getContractTermEndDate().minusDays(1).atTime(23, 0);
                }
            } else {
                dealEndDate = generator.getDeactivationDate().minusDays(1).atTime(23, 0);
            }

            List<Parametric.Formula> formulaList = priceComponentFormulaVariables
                    .stream()
                    .map(x -> Parametric.Formula
                            .builder()
                            .name(x.getProfileForBalancing().getName())
                            .formula(x.getValue().setScale(2, RoundingMode.DOWN).toString())
                            .build())
                    .distinct()
                    .toList();

            ETRMDealDataResponse deal = xEnergieCommunicationService.createDeal(
                    LocalDateTime.now(),
                    dealStartDate,
                    dealEndDate,
                    customerNumber,
                    customer.getIdentifier(),
                    contractNumber,
                    generator.getActivationDate(),
                    productBalancingName,
                    formulaList
            );

            String dealNumber = deal.getDeal().getNumber().getValue().getV();
            generator.setDealNumber(dealNumber);
        } catch (Exception e) {
            throw new RuntimeException("Unexpected exception handled while trying to create deal in xEnergie");
        }
    }

    /**
     * Creates a deal for a product contract.
     *
     * @param productContractDetails The details of the product contract.
     * @param productContract        The product contract.
     */
    private void createDealForProductContract(ProductContractDetails productContractDetails,
                                              ProductContract productContract) {
        List<ContractPods> currentActiveConsumers = contractPodRepository
                .findAllConsumersByContractDetailIdAndStatusIn(productContractDetails.getId(), List.of(EntityStatus.ACTIVE));

        boolean atLeastOnePointOfDeliveryIsActivated =
                currentActiveConsumers
                        .stream()
                        .anyMatch(cp -> Objects.nonNull(cp.getActivationDate()));
        ProductDetails productDetails = validateProductDetails(productContractDetails);

        ProductForBalancing productForBalancing = productDetails.getProductBalancingIdForConsumer();
        if (atLeastOnePointOfDeliveryIsActivated && Objects.nonNull(productForBalancing)) {
            CustomerDetails customerDetails = validateCustomerDetails(productContractDetails);
            Customer customer = validateCustomer(customerDetails);
            createCustomerIfRequired(customerDetails, customer);

            LocalDate calculatedMinActivationDate = calculateMinActivationDate(currentActiveConsumers);
            LocalDate calculatedMaxDate = calculateMaxDateOrReturnDefault(productContract, currentActiveConsumers);

            prepareXEnergieCommunicationAndCreateDealForConsumer(
                    productContractDetails,
                    productContract,
                    calculatedMinActivationDate,
                    calculatedMaxDate,
                    customer,
                    productForBalancing,
                    productDetails
            );

            List<ContractPods> podsThatHaveActivationDate = contractPodRepository
                    .getPodsThatHaveActivationDate(productContractDetails.getId(), EntityStatus.ACTIVE);
            podsThatHaveActivationDate.forEach(cp -> cp.setCustomModifyDate(LocalDateTime.now()));

            contractPodRepository.saveAll(podsThatHaveActivationDate);
        }
    }

    /**
     * Prepares communication with xEnergie and creates a deal for a consumer.
     *
     * @param productContractDetails The product contract details.
     * @param productContract        The product contract.
     * @param minActivationDate      The minimum activation date.
     * @param calculatedMaxDate      The calculated maximum date.
     * @param customer               The customer.
     * @param productForBalancing    The product for balancing.
     * @param productDetails         The product details.
     */
    private void prepareXEnergieCommunicationAndCreateDealForConsumer(ProductContractDetails productContractDetails,
                                                                      ProductContract productContract,
                                                                      LocalDate minActivationDate,
                                                                      LocalDate calculatedMaxDate,
                                                                      Customer customer,
                                                                      ProductForBalancing productForBalancing,
                                                                      ProductDetails productDetails) {
        try {
            List<PriceComponentFormulaVariable> priceComponentFormulaVariables = new ArrayList<>();

            priceComponentFormulaVariables.addAll(priceComponentRepository
                    .findConsumerPriceComponentFormulaVariablesByProductDetailId(productDetails.getId()));

            priceComponentFormulaVariables.addAll(priceComponentGroupRepository
                    .findConsumerPriceComponentGroupPriceComponentFormulaVariablesByProductDetailId(productDetails.getId()));

            priceComponentFormulaVariables.removeIf(
                    priceComponentFormulaVariable ->
                            Objects.isNull(priceComponentFormulaVariable.getProfileForBalancing())
            );

            ETRMDealDataResponse deal = xEnergieCommunicationService.createDeal(
                    LocalDateTime.now(),
                    minActivationDate.minusDays(1).atTime(23, 0),
                    calculatedMaxDate.minusDays(1).atTime(23, 0),
                    String.valueOf(customer.getCustomerNumber()),
                    customer.getIdentifier(),
                    String.valueOf(productContract.getContractNumber()),
                    productContractDetails.getCreateDate().toLocalDate(),
                    productForBalancing.getName(),
                    priceComponentFormulaVariables
                            .stream()
                            .map(x -> Parametric.Formula
                                    .builder()
                                    .name(x.getProfileForBalancing().getName())
                                    .formula(x.getValue().setScale(2, RoundingMode.DOWN).toString())
                                    .build())
                            .distinct()
                            .toList()
            );

            String dealNumber = deal.getDeal().getNumber().getValue().getV();
            productContractDetails.setDealNumber(dealNumber);

            productContractDetailsRepository.save(productContractDetails);
        } catch (Exception e) {
            throw new RuntimeException("Unexpected exception handled while trying to create deal in xEnergie");
        }
    }

    /**
     * Calculates the maximum deactivation date from the given list of active pods or returns the default date.
     *
     * @param productContract   the product contract object
     * @param currentActivePods the list of current active pods
     * @return the maximum deactivation date from the list of active pods if all pods have deactivation dates,
     * otherwise returns the contract term end date if it is not null, otherwise returns the default date
     */
    private LocalDate calculateMaxDateOrReturnDefault(ProductContract productContract,
                                                      List<ContractPods> currentActivePods) {
        LocalDate maxDeactivationDate = LocalDate.of(2030, 12, 31);

        boolean hasAllPointOfDeliveriesOwnDeactivationDate =
                currentActivePods
                        .stream()
                        .allMatch(cp -> Objects.nonNull(cp.getActivationDate()) && Objects.nonNull(cp.getDeactivationDate()));
        if (hasAllPointOfDeliveriesOwnDeactivationDate) {
            Optional<LocalDate> maxDeactivationDateOptional = currentActivePods
                    .stream()
                    .max(Comparator.comparing(ContractPods::getDeactivationDate))
                    .map(ContractPods::getDeactivationDate);

            maxDeactivationDate = maxDeactivationDateOptional.orElse(LocalDate.of(2030, 12, 31));
        } else {
            LocalDate contractTermEndDate = productContract.getContractTermEndDate();
            if (contractTermEndDate != null) {
                maxDeactivationDate = contractTermEndDate;
            }
        }

        if (maxDeactivationDate.isAfter(LocalDate.of(2030, Month.DECEMBER, 31))) {
            return LocalDate.of(2030, Month.DECEMBER, 31);
        } else {
            return maxDeactivationDate;
        }
    }

    /**
     * Calculates the minimum activation date based on the given list of current active pods.
     *
     * @param currentActivePods the list of current active pods
     * @return the minimum activation date
     * @throws RuntimeException if the minimum activation date is not found in the point of deliveries
     */
    private LocalDate calculateMinActivationDate(List<ContractPods> currentActivePods) {
        Optional<LocalDate> minActivationDateOptional = currentActivePods
                .stream()
                .filter(cp -> Objects.nonNull(cp.getActivationDate()))
                .min(Comparator.comparing(ContractPods::getActivationDate))
                .map(ContractPods::getActivationDate);

        return minActivationDateOptional
                .orElseThrow(
                        () -> new RuntimeException("Deal creation failed, min activation date not found in point of deliveries")
                );
    }

    /**
     * Creates a customer if required.
     *
     * @param customerDetails the customer details
     * @param customer        the customer object
     * @throws RuntimeException if an error occurs during customer creation or communication with the xEnergie database
     */
    private void createCustomerIfRequired(CustomerDetails customerDetails, Customer customer) {
        XEnergieRepositoryCreateCustomerResponse xEnergieRepositoryCreateCustomerResponse;
        try {
            xEnergieRepositoryCreateCustomerResponse = xEnergieRepository
                    .createCustomer(customerDetails.getName(), String.valueOf(customer.getCustomerNumber()), customer.getIdentifier());
        } catch (Exception e) {
            throw new RuntimeException("Deal creation failed, Customer assigned to Product contract not found;");
        }

        switch (xEnergieRepositoryCreateCustomerResponse) {
            case CUSTOMER_CREATED, CUSTOMER_EXISTS -> {
            }
            default -> throw new RuntimeException(
                    "Deal creation failed, exception handled while communicating with xEnergie database: [%s];"
                            .formatted(xEnergieRepositoryCreateCustomerResponse)
            );
        }
    }

    /**
     * Validates the customer details.
     *
     * @param customerDetails The customer details to be validated.
     * @return The validated customer.
     * @throws DomainEntityNotFoundException if the customer assigned to the product contract is not found.
     */
    private Customer validateCustomer(CustomerDetails customerDetails) {
        return customerRepository
                .findByIdAndStatuses(customerDetails.getCustomerId(), List.of(CustomerStatus.ACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("Deal creation failed, Customer assigned to Product contract not found;"));
    }

    /**
     * Validates the customer details associated with the given product contract.
     *
     * @param productContractDetails The product contract details to validate.
     * @return The validated customer details.
     * @throws DomainEntityNotFoundException If the customer details are not found.
     */
    private CustomerDetails validateCustomerDetails(ProductContractDetails productContractDetails) {
        return customerDetailsRepository
                .findById(productContractDetails.getCustomerDetailId())
                .orElseThrow(() -> new DomainEntityNotFoundException("Deal creation failed, Customer assigned to Product contract not found;"));
    }

    /**
     * Validates the product details based on the given product contract details.
     *
     * @param productContractDetails the product contract details to validate
     * @return the validated product details
     * @throws DomainEntityNotFoundException if the product assigned to the product contract is not found
     */
    private ProductDetails validateProductDetails(ProductContractDetails productContractDetails) {
        return productDetailsRepository
                .findById(productContractDetails.getProductDetailId())
                .orElseThrow(() -> new DomainEntityNotFoundException("Deal creation failed, Product assigned to Product contract not found;"));
    }
}
