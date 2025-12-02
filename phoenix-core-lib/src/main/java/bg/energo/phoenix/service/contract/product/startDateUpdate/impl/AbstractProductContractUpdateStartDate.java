package bg.energo.phoenix.service.contract.product.startDateUpdate.impl;

import bg.energo.phoenix.model.entity.contract.billing.ContractPods;
import bg.energo.phoenix.model.entity.contract.product.ProductContractDetails;
import bg.energo.phoenix.model.entity.customer.Customer;
import bg.energo.phoenix.model.entity.customer.CustomerDetails;
import bg.energo.phoenix.model.entity.pod.pod.PointOfDeliveryDetails;
import bg.energo.phoenix.model.enums.communication.xEnergie.XEnergieRepositoryCreateCustomerResponse;
import bg.energo.phoenix.model.enums.contract.products.DealNumberCheckResult;
import bg.energo.phoenix.model.enums.customer.CustomerStatus;
import bg.energo.phoenix.model.process.ProductContractPodExportData;
import bg.energo.phoenix.model.response.communication.xEnergie.AdditionalInformationForPointOfDeliveries;
import bg.energo.phoenix.repository.contract.billing.ContractPodRepository;
import bg.energo.phoenix.repository.contract.product.ProductContractDetailsRepository;
import bg.energo.phoenix.repository.customer.CustomerDetailsRepository;
import bg.energo.phoenix.repository.customer.CustomerRepository;
import bg.energo.phoenix.repository.pod.pod.PointOfDeliveryDetailsRepository;
import bg.energo.phoenix.repository.pod.pod.PointOfDeliveryRepository;
import bg.energo.phoenix.repository.product.product.ProductDetailsRepository;
import bg.energo.phoenix.service.contract.product.ProductContractPodExportService;
import bg.energo.phoenix.service.contract.product.startDateUpdate.ProductContractStartDateUpdateRoute;
import bg.energo.phoenix.service.xEnergie.XEnergieCommunicationService;
import bg.energo.phoenix.service.xEnergie.XEnergieRepository;
import bg.energo.phoenix.util.epb.EPBChainedExceptionTriggerUtil;
import bg.energo.phoenix.util.epb.EPBFinalFields;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
@RequiredArgsConstructor
public abstract class AbstractProductContractUpdateStartDate implements ProductContractStartDateUpdateRoute {
    @Value("${product-contract.start-date-updater.max-threads}")
    private Integer maxThreadsCount;

    @Value("${product-contract.start-date-updater.batch-size}")
    private Integer batchSize;

    protected final ProductContractDetailsRepository productContractDetailsRepository;
    protected final ContractPodRepository contractPodRepository;
    protected final XEnergieCommunicationService xEnergieCommunicationService;
    protected final XEnergieRepository xEnergieRepository;
    protected final ProductDetailsRepository productDetailsRepository;
    protected final PointOfDeliveryRepository pointOfDeliveryRepository;
    protected final PointOfDeliveryDetailsRepository pointOfDeliveryDetailsRepository;
    protected final ProductContractPodExportService exportService;
    protected final CustomerDetailsRepository customerDetailsRepository;
    protected final CustomerRepository customerRepository;

    protected void createCustomerInXEnergieIfRequired(ProductContractDetails productContractDetails, List<String> exceptionMessages, Logger log) {
        Optional<Customer> customerOptional = customerRepository
                .findByCustomerDetailIdAndStatusIn(productContractDetails.getCustomerDetailId(), List.of(CustomerStatus.ACTIVE));
        if (customerOptional.isEmpty()) {
            EPBChainedExceptionTriggerUtil.addExceptionAndTrigger("Customer not found for POD", exceptionMessages, log);
            return;
        }

        Customer customer = customerOptional.get();

        try {
            boolean customerExists = xEnergieRepository
                    .isCustomerExists(customer.getIdentifier(), customer.getCustomerNumber().toString());
            if (!customerExists) {
                Optional<CustomerDetails> customerDetailsOptional = customerDetailsRepository
                        .findFirstByCustomerId(customer.getId(), Sort.by(Sort.Direction.DESC, "createDate"));
                if (customerDetailsOptional.isEmpty()) {
                    EPBChainedExceptionTriggerUtil.addExceptionAndTrigger("Customer Details not found for POD", exceptionMessages, log);
                    return;
                }

                XEnergieRepositoryCreateCustomerResponse xEnergieResponse = xEnergieRepository
                        .createCustomer(customerDetailsOptional.get().getName(), customer.getCustomerNumber().toString(), customer.getIdentifier());

                switch (xEnergieResponse) {
                    case CUSTOMER_CREATED, CUSTOMER_EXISTS -> {
                        // ok
                    }
                    default ->
                            EPBChainedExceptionTriggerUtil.addExceptionAndTrigger("Database exception handled while trying to create customer: [%s]".formatted(xEnergieResponse), exceptionMessages, log);
                }
            }
        } catch (Exception e) {
            EPBChainedExceptionTriggerUtil.addExceptionAndTrigger("Exception handled while trying to create customer", exceptionMessages, log);
        }
    }

    protected boolean isPodActivationDateLessThanRequestedStartDate(ContractPods pod, LocalDate requestedStartDate) {
        return pod.getActivationDate().isBefore(requestedStartDate);
    }

    protected boolean existsGapBetweenPreviousAndUpdatedVersion(ContractPods previousVersionPod, ContractPods updatedVersionPod) {
        if (Objects.isNull(updatedVersionPod.getActivationDate())) {
            return false;
        }

        if (Objects.isNull(previousVersionPod.getDeactivationDate())) {
            return false;
        }

        return previousVersionPod.getDeactivationDate().until(updatedVersionPod.getActivationDate()).getDays() > 1;
    }

    protected boolean isPodDeactivationDateLessThanRequestedStartDate(ContractPods pod, LocalDate requestedStartDate) {
        return pod.getDeactivationDate().isBefore(requestedStartDate);
    }

    protected boolean isPodDeactivationDateMoreThanRequestedStartDate(ContractPods pod, LocalDate requestedStartDate) {
        return pod.getDeactivationDate().isAfter(requestedStartDate);
    }

    protected DealNumberCheckResult checkDealNumber(ProductContractDetails updatedVersion, ProductContractDetails previousVersion) {
        if (StringUtils.isNotBlank(updatedVersion.getDealNumber()) && StringUtils.isNotBlank(previousVersion.getDealNumber())) {
            return DealNumberCheckResult.BOTH_VERSIONS;
        } else if (StringUtils.isNotBlank(updatedVersion.getDealNumber())) {
            return DealNumberCheckResult.UPDATED_VERSION;
        } else {
            return DealNumberCheckResult.PREVIOUS_VERSION;
        }
    }

    protected boolean isDealNumbersSame(ProductContractDetails updatedVersion, ProductContractDetails previousVersion) {
        return StringUtils.equals(updatedVersion.getDealNumber(), previousVersion.getDealNumber());
    }

    protected void addNewUncommittedActionToList(List<Runnable> uncommittedActions, Runnable action) {
        uncommittedActions.add(action);
    }

    private boolean isPodPrefixed(String podIdentifier) {
        return podIdentifier.startsWith(EPBFinalFields.POD_PREFIX);
    }

    protected PointOfDeliveryDetails fetchPointOfDeliveryDetailsOrElseThrowException(ContractPods pod, List<String> exceptionMessages) {
        Optional<PointOfDeliveryDetails> previousPodDetailsOptional = pointOfDeliveryDetailsRepository.findByPodId(pod.getId());
        if (previousPodDetailsOptional.isEmpty()) {
            EPBChainedExceptionTriggerUtil.addExceptionAndTrigger("Point of delivery details not found", exceptionMessages, log);
        }
        return null;
    }

    protected AdditionalInformationForPointOfDeliveries fetchAdditionalInformation(String identifier, List<String> exceptionMessages) {
        try {
            return xEnergieRepository.retrieveAdditionalInformationForPointOfDelivery(identifier);
        } catch (Exception e) {
            EPBChainedExceptionTriggerUtil.addExceptionAndTrigger("Additional Information not found for pod", exceptionMessages, log);
            return null;
        }
    }

    protected Optional<String> getBalancingProductName(ProductContractDetails productContractDetails, List<String> exceptionMessages) {
        return Optional.empty(); // TODO: 12.10.23
    }

    protected void commit(List<ContractPods> uncommittedContractPods, List<Runnable> uncommittedActions, List<ProductContractPodExportData> uncommittedExportData, List<String> exceptionMessages) {
        if (CollectionUtils.isEmpty(exceptionMessages)) {
            contractPodRepository.saveAll(uncommittedContractPods);
        }

        if (CollectionUtils.isNotEmpty(uncommittedActions)) {
            try {
                int nThreads = uncommittedActions.size() / batchSize + 1;
                ExecutorService threadPool = Executors.newFixedThreadPool(Math.min(nThreads, maxThreadsCount));

                for (List<Runnable> partition : ListUtils.partition(uncommittedActions, batchSize)) {
                    processPartitionExecution(threadPool, partition);
                }
            } catch (Exception e) {
                log.error("Exception handled while trying to commit xEnergie actions", e);
            }
        }

        if (CollectionUtils.isNotEmpty(uncommittedExportData)) {
            try {
                exportService.process(uncommittedExportData);
            } catch (Exception e) {
                // TODO: 24.10.23 audit in database
                log.error("Exception handled while trying to commit pod export", e);
            }
        }
    }

    private void processPartitionExecution(ExecutorService threadPool, List<Runnable> partition) throws InterruptedException {
        threadPool
                .invokeAll(partition
                        .stream()
                        .map(runnable ->
                                (Callable<Boolean>) () -> {
                                    try {
                                        runnable.run();
                                    } catch (Exception e) {
                                        // TODO: 24.10.23 audit in database
                                        log.error("Exception handled while trying to commit xEnergie action", e);
                                    }
                                    return true;
                                })
                        .toList()
                );
    }

    protected String fetchIdentifierOrThrowException(List<String> exceptionMessages, ContractPods pod) {
        Optional<String> identifierOptional = pointOfDeliveryRepository.getIdentifierByProductContractPodDetailId(pod.getPodDetailId(), pod.getContractDetailId());
        if (identifierOptional.isEmpty()) {
            EPBChainedExceptionTriggerUtil.addExceptionAndTrigger("Point of delivery not found by Product Contract POD detail id", exceptionMessages, log);
            return null;
        }
        return identifierOptional.get();
    }
}
