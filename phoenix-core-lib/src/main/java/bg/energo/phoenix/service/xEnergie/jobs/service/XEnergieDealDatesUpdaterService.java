package bg.energo.phoenix.service.xEnergie.jobs.service;

import bg.energo.phoenix.model.customAnotations.aspects.ExecutionTimeLogger;
import bg.energo.phoenix.model.entity.contract.billing.ContractPods;
import bg.energo.phoenix.model.entity.contract.product.ProductContract;
import bg.energo.phoenix.model.entity.contract.product.ProductContractDetails;
import bg.energo.phoenix.model.entity.customer.Customer;
import bg.energo.phoenix.model.entity.product.price.priceComponent.PriceComponentFormulaVariable;
import bg.energo.phoenix.model.entity.product.product.ProductDetails;
import bg.energo.phoenix.model.entity.product.product.ProductForBalancing;
import bg.energo.phoenix.model.enums.customer.CustomerStatus;
import bg.energo.phoenix.model.request.communication.xEnergie.Parametric;
import bg.energo.phoenix.model.response.communication.xEnergie.XEnergieDealInformation;
import bg.energo.phoenix.process.model.entity.Process;
import bg.energo.phoenix.repository.contract.billing.ContractPodRepository;
import bg.energo.phoenix.repository.contract.product.ProductContractDetailsRepository;
import bg.energo.phoenix.repository.contract.product.ProductContractRepository;
import bg.energo.phoenix.repository.customer.CustomerRepository;
import bg.energo.phoenix.repository.product.price.priceComponent.PriceComponentRepository;
import bg.energo.phoenix.repository.product.price.priceComponentGroup.PriceComponentGroupRepository;
import bg.energo.phoenix.repository.product.product.ProductDetailsRepository;
import bg.energo.phoenix.service.xEnergie.XEnergieCommunicationService;
import bg.energo.phoenix.service.xEnergie.XEnergieRepository;
import bg.energo.phoenix.service.xEnergie.jobs.enums.XEnergieJobType;
import bg.energo.phoenix.service.xEnergie.jobs.model.xEnergieDealDatesUpdate.XEnergieGeneratorDealDatesUpdateModel;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
@Profile({"dev","test"})
@ConditionalOnExpression("${app.cfg.schedulers.enabled:true}")
public class XEnergieDealDatesUpdaterService extends AbstractXEnergieService {
    public XEnergieDealDatesUpdaterService(CustomerRepository customerRepository,
                                           ProductContractDetailsRepository productContractDetailsRepository,
                                           ContractPodRepository contractPodRepository,
                                           ProductContractRepository productContractRepository,
                                           XEnergieRepository xEnergieRepository,
                                           XEnergieCommunicationService xEnergieCommunicationService,
                                           ProductDetailsRepository productDetailsRepository,
                                           PriceComponentRepository priceComponentRepository,
                                           PriceComponentGroupRepository priceComponentGroupRepository,
                                           XEnergieSchedulerErrorHandler xEnergieSchedulerErrorHandler,
                                           XEnergiePointOfDeliveryExcelGenerationService xEnergiePointOfDeliveryExcelGenerationService) {
        super(xEnergieSchedulerErrorHandler);
        this.customerRepository = customerRepository;
        this.productContractDetailsRepository = productContractDetailsRepository;
        this.contractPodRepository = contractPodRepository;
        this.productContractRepository = productContractRepository;
        this.xEnergieRepository = xEnergieRepository;
        this.xEnergieCommunicationService = xEnergieCommunicationService;
        this.productDetailsRepository = productDetailsRepository;
        this.priceComponentRepository = priceComponentRepository;
        this.priceComponentGroupRepository = priceComponentGroupRepository;
        this.xEnergiePointOfDeliveryExcelGenerationService = xEnergiePointOfDeliveryExcelGenerationService;
    }

    private final CustomerRepository customerRepository;
    private final ProductContractDetailsRepository productContractDetailsRepository;
    private final ContractPodRepository contractPodRepository;
    private final ProductContractRepository productContractRepository;
    private final XEnergieRepository xEnergieRepository;
    private final XEnergieCommunicationService xEnergieCommunicationService;
    private final ProductDetailsRepository productDetailsRepository;
    private final PriceComponentRepository priceComponentRepository;
    private final PriceComponentGroupRepository priceComponentGroupRepository;
    private final XEnergiePointOfDeliveryExcelGenerationService xEnergiePointOfDeliveryExcelGenerationService;

    @Override
    protected XEnergieJobType getJobType() {
        return XEnergieJobType.X_ENERGIE_DEAL_DATES_UPDATER;
    }

    @Override
    protected AbstractXEnergieService getNextJobInChain() {
        return xEnergiePointOfDeliveryExcelGenerationService;
    }

    @Transactional
    @ExecutionTimeLogger
    public void execute(Process process) {
        ExecutorService executorService = Executors.newFixedThreadPool(getProperties().numberOfThreads());

        try {
            Integer queryBatchSize = getProperties().queryBatchSize();
            List<Callable<Boolean>> callableQueue = new ArrayList<>();

            LocalDateTime yesterdayStart = LocalDateTime.now().minusDays(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
            LocalDateTime yesterdayEnd = yesterdayStart.plusDays(1).minusSeconds(1);

            Long countModifiedProductContractDealNumbersByConsumerPointOfDeliveries = contractPodRepository
                    .countDealsByModifyDatePastOneDay(yesterdayStart, yesterdayEnd);
            Long fetchedDataSizeForConsumers = 0L;
            int updatedDealsOffsetForConsumers = 0;

            while (countModifiedProductContractDealNumbersByConsumerPointOfDeliveries > fetchedDataSizeForConsumers) {
                List<String> modifiedProductContractDealNumbersByConsumerPointOfDeliveries =
                        contractPodRepository
                                .findDealsByConsumerPointOfDeliveriesAndModifyDatePastOneDay(
                                        yesterdayStart,
                                        yesterdayEnd,
                                        PageRequest.of(updatedDealsOffsetForConsumers, queryBatchSize)
                                );

                for (String modifiedDeal : modifiedProductContractDealNumbersByConsumerPointOfDeliveries) {
                    try {
                        callableQueue.add(() -> processConsumerPointOfDeliveriesDealDatesUpdate(process, modifiedDeal));
                    } catch (Exception e) {
                        handleException(process, e.getMessage());
                    }
                }

                fetchedDataSizeForConsumers += queryBatchSize;
                updatedDealsOffsetForConsumers++;
            }

            Long countDealsByGeneratorPointOfDeliveriesAndModifyDatePastOneDay = contractPodRepository
                    .countDealsByGeneratorPointOfDeliveriesAndModifyDatePastOneDay(yesterdayStart, yesterdayEnd);
            Long fetchedDataSizeForGenerators = 0L;
            int updatedDealsOffsetForGenerators = 0;

            while (countDealsByGeneratorPointOfDeliveriesAndModifyDatePastOneDay > fetchedDataSizeForGenerators) {
                List<XEnergieGeneratorDealDatesUpdateModel> updatedGenerators = contractPodRepository
                        .findDealsByGeneratorPointOfDeliveriesAndModifyDatePastOneDay(
                                yesterdayStart,
                                yesterdayEnd,
                                PageRequest.of(updatedDealsOffsetForGenerators, queryBatchSize)
                        );

                for (XEnergieGeneratorDealDatesUpdateModel model : updatedGenerators) {
                    callableQueue.add(() -> processGeneratorPointOfDeliveriesDealDateUpdate(process, model));
                }

                fetchedDataSizeForGenerators += queryBatchSize;
                updatedDealsOffsetForGenerators++;
            }

            executorService.invokeAll(callableQueue);
        } catch (Exception e) {
            handleException(process, e.getMessage());
        } finally {
            executeNextJobInChain(process);
        }
    }

    private Boolean processGeneratorPointOfDeliveriesDealDateUpdate(Process process, XEnergieGeneratorDealDatesUpdateModel model) {
        ProductContract productContract = model.productContract();
        ContractPods productContractPointOfDelivery = model.contractPods();
        ProductContractDetails productContractDetails = model.productContractDetails();

        String pointOfDeliveryDealNumber = productContractPointOfDelivery.getDealNumber();

        XEnergieDealInformation xEnergieDealInformation;
        try {
            xEnergieDealInformation = xEnergieRepository
                    .retrieveDealInformation(pointOfDeliveryDealNumber);
        } catch (Exception e) {
            handleException(process, "Cannot retrieve additional information for deal: [%s]".formatted(pointOfDeliveryDealNumber));
            return true;
        }

        List<ContractPods> allGeneratorsByDealNumberInContract = contractPodRepository
                .findAllGeneratorsByDealNumberInContract(productContract.getId(), pointOfDeliveryDealNumber);

        LocalDate earliestActivationDate = calculateEarliestActivationDate(allGeneratorsByDealNumberInContract);
        LocalDate maxDeactivationDate = calculateMaxDeactivationDate(allGeneratorsByDealNumberInContract, productContract);

        return updateDealActivationDatesIfRequired(process, productContract, productContractDetails, pointOfDeliveryDealNumber, xEnergieDealInformation, earliestActivationDate, maxDeactivationDate);
    }

    private Boolean updateDealActivationDatesIfRequired(Process process,
                                                        ProductContract productContract,
                                                        ProductContractDetails productContractDetails,
                                                        String pointOfDeliveryDealNumber,
                                                        XEnergieDealInformation xEnergieDealInformation,
                                                        LocalDate activationDate,
                                                        LocalDate deactivationDate) {
        boolean dealDatesModified = isDealDatesModified(
                xEnergieDealInformation,
                activationDate,
                deactivationDate
        );
        if (dealDatesModified) {
            Optional<Customer> customerOptional = getCustomer(productContractDetails.getCustomerDetailId());
            if (customerOptional.isPresent()) {
                Customer customer = customerOptional.get();
                Optional<ProductDetails> productDetailsOptional = productDetailsRepository
                        .findById(productContractDetails.getProductDetailId());
                if (productDetailsOptional.isEmpty()) {
                    handleException(process, "Product assigned to Product contract not found;");
                    return true;
                }

                ProductDetails productDetails = productDetailsOptional.get();
                ProductForBalancing productForBalancing = productDetails.getProductBalancingIdForGenerator();
                if (productForBalancing != null) {
                    String customerNumberAsString = String.valueOf(customer.getCustomerNumber() != null ? customer.getCustomerNumber() : "");
                    String customerIdentifier = customer.getIdentifier();

                    List<PriceComponentFormulaVariable> priceComponentFormulaVariables = new ArrayList<>();

                    priceComponentFormulaVariables.addAll(priceComponentRepository
                            .findConsumerPriceComponentFormulaVariablesByProductDetailId(productDetails.getId()));

                    priceComponentFormulaVariables.addAll(priceComponentGroupRepository
                            .findConsumerPriceComponentGroupPriceComponentFormulaVariablesByProductDetailId(productDetails.getId()));

                    priceComponentFormulaVariables.removeIf(
                            priceComponentFormulaVariable ->
                                    Objects.isNull(priceComponentFormulaVariable.getProfileForBalancing())
                    );

                    executeDealUpdateCall(
                            process,
                            pointOfDeliveryDealNumber,
                            activationDate,
                            deactivationDate,
                            customerNumberAsString,
                            customerIdentifier,
                            productContract,
                            productContractDetails,
                            productForBalancing,
                            priceComponentFormulaVariables
                    );
                } else {
                    handleException(process, "Can't find balancing product for productId: %s;".formatted(productDetails.getProduct().getId()));
                }
            } else {
                handleException(process, "Can't find customer with details id: %s;".formatted(productContractDetails.getCustomerDetailId()));
            }
        }

        return true;
    }

    private Boolean processConsumerPointOfDeliveriesDealDatesUpdate(Process process, String modifiedDeal) {
        Optional<ProductContractDetails> latestProductContractDetailsOptional = getLatestProductContractDetails(modifiedDeal);
        if (latestProductContractDetailsOptional.isEmpty()) {
            handleException(process, "Cannot find latest version Product Contract Details by deal number: [%s]".formatted(modifiedDeal));
            return true;
        }
        ProductContractDetails productContractDetails = latestProductContractDetailsOptional.get();

        List<ContractPods> contractPodsByDealNumber = contractPodRepository
                .findContractPodsByProductContractDealNumber(modifiedDeal);

        if (CollectionUtils.isEmpty(contractPodsByDealNumber)) {
            return true;
        }

        Optional<ProductContract> productContractOptional = getProductContract(productContractDetails.getContractId());
        if (productContractOptional.isEmpty()) {
            handleException(process, "Cannot find latest version Product Contract by deal number: [%s]".formatted(modifiedDeal));
            return true;
        }
        ProductContract productContract = productContractOptional.get();

        LocalDate earliestActivationDate = calculateEarliestActivationDate(contractPodsByDealNumber);
        LocalDate maxDeactivationDate = calculateMaxDeactivationDate(contractPodsByDealNumber, productContract);

        XEnergieDealInformation xEnergieDealInformation;
        try {
            xEnergieDealInformation = xEnergieRepository
                    .retrieveDealInformation(modifiedDeal);
        } catch (Exception e) {
            handleException(process, "Cannot retrieve additional information for deal: [%s]".formatted(modifiedDeal));
            return true;
        }

        return updateDealActivationDatesIfRequired(process, productContract, productContractDetails, modifiedDeal, xEnergieDealInformation, earliestActivationDate, maxDeactivationDate);
    }

    private LocalDate calculateMaxDeactivationDate(List<ContractPods> contractPodsByDealNumber, ProductContract productContract) {
        LocalDate maxDeactivationDate = LocalDate.of(2030, Month.DECEMBER, 31);

        boolean anyContractPodIsInPerpetuity =
                contractPodsByDealNumber
                        .stream()
                        .filter(contractPods -> contractPods.getActivationDate() != null)
                        .anyMatch(contractPods -> contractPods.getDeactivationDate() == null);

        if (anyContractPodIsInPerpetuity) {
            if (Objects.nonNull(productContract.getContractTermEndDate())) {
                maxDeactivationDate = productContract.getContractTermEndDate();
            }
        } else {
            Optional<ContractPods> maxDeactivationDateContractPodOptional = contractPodsByDealNumber
                    .stream()
                    .filter(contractPods -> Objects.nonNull(contractPods.getDeactivationDate()))
                    .max(Comparator.comparing(ContractPods::getDeactivationDate));

            if (maxDeactivationDateContractPodOptional.isPresent()) {
                maxDeactivationDate = maxDeactivationDateContractPodOptional.get().getDeactivationDate();
            }
        }

        if (maxDeactivationDate.isAfter(LocalDate.of(2030, Month.DECEMBER, 31))) {
            return LocalDate.of(2030, Month.DECEMBER, 31);
        } else {
            return maxDeactivationDate;
        }
    }

    private LocalDate calculateEarliestActivationDate(List<ContractPods> contractPodsByDealNumber) {
        LocalDate earliestActivationDate = LocalDate.of(2030, Month.DECEMBER, 30);
        Optional<ContractPods> earliestActivationDateContractPodOptional = contractPodsByDealNumber
                .stream()
                .filter(contractPods -> Objects.nonNull(contractPods.getActivationDate()))
                .min(Comparator.comparing(ContractPods::getActivationDate));
        if (earliestActivationDateContractPodOptional.isPresent()) {
            ContractPods earliestActivationDateContractPod = earliestActivationDateContractPodOptional.get();
            earliestActivationDate = earliestActivationDateContractPod.getActivationDate();
        }
        return earliestActivationDate;
    }

    private void executeDealUpdateCall(Process process, String modifiedDeal, LocalDate earliestActivationDate, LocalDate maxDeactivationDate, String customerNumberAsString, String customerIdentifier, ProductContract productContract, ProductContractDetails productContractDetails, ProductForBalancing productForBalancing, List<PriceComponentFormulaVariable> priceComponentFormulaVariables) {
        try {
            xEnergieCommunicationService.updateDeal(
                    modifiedDeal,
                    LocalDateTime.now(),
                    earliestActivationDate.minusDays(1).atTime(23, 0),
                    maxDeactivationDate.minusDays(1).atTime(23, 0),
                    customerNumberAsString,
                    customerIdentifier,
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
        } catch (Exception e) {
            handleException(process, "Unexpected exception handled while trying to update deal: [%s] from xEnergie side;".formatted(modifiedDeal));
        }
    }

    public List<ContractPods> fetchNonSynchronizedData() {
        LocalDateTime yesterdayStart = LocalDateTime.now().minusDays(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime yesterdayEnd = yesterdayStart.plusDays(1).minusSeconds(1);

        return contractPodRepository.findDealsByModifyDatePastOneDayTestControllerOnly(yesterdayStart, yesterdayEnd);
    }

    private Optional<ProductContractDetails> getLatestProductContractDetails(String dealNumber) {
        return productContractDetailsRepository
                .findFirstByDealNumberOrderByStartDateDesc(dealNumber);
    }

    private boolean isDealDatesModified(XEnergieDealInformation xEnergieDealInformation, LocalDate earliestActivationDate, LocalDate maxDeactivationDate) {
        return !xEnergieDealInformation.getDateFrom().equals(earliestActivationDate) || !xEnergieDealInformation.getDateTo().equals(maxDeactivationDate);
    }

    private Optional<ProductContract> getProductContract(Long contractId) {
        return productContractRepository
                .findById(contractId);
    }

    private Optional<Customer> getCustomer(Long customerDetailId) {
        return customerRepository
                .findByCustomerDetailIdAndStatusIn(customerDetailId, List.of(CustomerStatus.ACTIVE));
    }
}
