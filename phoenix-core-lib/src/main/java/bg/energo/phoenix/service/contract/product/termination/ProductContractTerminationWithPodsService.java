package bg.energo.phoenix.service.contract.product.termination;

import bg.energo.phoenix.model.entity.contract.product.ProductContract;
import bg.energo.phoenix.model.entity.contract.product.ProductContractDetails;
import bg.energo.phoenix.model.enums.contract.products.ContractDetailsStatus;
import bg.energo.phoenix.model.enums.contract.products.ContractDetailsSubStatus;
import bg.energo.phoenix.model.enums.product.termination.terminations.AutoTerminationFrom;
import bg.energo.phoenix.model.response.contract.productContract.terminations.ProductContractTerminationGenericModel;
import bg.energo.phoenix.model.response.contract.productContract.terminations.ProductContractTerminationProperties;
import bg.energo.phoenix.model.response.contract.productContract.terminations.ProductContractTerminationWithPodsResponse;
import bg.energo.phoenix.repository.contract.product.ProductContractDetailsRepository;
import bg.energo.phoenix.repository.contract.product.ProductContractRepository;
import bg.energo.phoenix.service.crm.emailCommunication.EmailCommunicationService;
import bg.energo.phoenix.service.customer.statusChangeEvent.CustomerStatusChangeEventPublisher;
import bg.energo.phoenix.service.product.termination.terminations.ContractTerminationEmailSenderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@Order(3)
@RequiredArgsConstructor
public class ProductContractTerminationWithPodsService implements ProductContractTerminator {
    private final ProductContractRepository productContractRepository;
    private final ProductContractDetailsRepository productContractDetailsRepository;
    private final CustomerStatusChangeEventPublisher customerStatusChangeEventPublisher;
    private final EmailCommunicationService emailCommunicationService;
    private final ContractTerminationEmailSenderService contractTerminationEmailSenderService;

    @Value("${product-contract.termination-with-pod.number-of-threads}")
    private Integer numberOfThreads;

    @Value("${product-contract.termination-with-pod.batch-size}")
    private Integer batchSize;

    @Value("${product-contract.termination-with-pod.query-batch-size}")
    private Integer queryBatchSize;

    @Override
    public ProductContractTerminationProperties getProperties() {
        return ProductContractTerminationProperties
                .builder()
                .numberOfThreads(numberOfThreads)
                .batchSize(batchSize)
                .queryBatchSize(queryBatchSize)
                .build();
    }

    @Override
    public List<ProductContractTerminationGenericModel> getContractData(Integer size, Integer page) {
        return productContractRepository
                .getProductContractsToTerminationWithPodsDeactivation(PageRequest.of(page,size))
                .map(ProductContractTerminationGenericModel::new)
                .getContent();
    }

    @Override
    public void terminate(ProductContractTerminationGenericModel model) {
        ProductContractTerminationWithPodsResponse terminationModel = model.getPodsResponse();
        if (terminationModel == null)
            return;
        ProductContract productContract = productContractRepository.findById(terminationModel.getId()).orElse(null);
        if (productContract != null) {
            if (terminationModel.getAutoTerminationFrom() == AutoTerminationFrom.EVENT_DATE) {
                productContract.setTerminationDate(terminationModel.getDeactivationDate()); // this is the latest pod deactivation date
                productContract.setContractStatus(ContractDetailsStatus.TERMINATED);
                productContract.setSubStatus(ContractDetailsSubStatus.ALL_PODS_ARE_DEACTIVATED);
                log.info("Termination date when EVENT_DATE is selected: " + terminationModel.getDeactivationDate());
            } else if (terminationModel.getAutoTerminationFrom() == AutoTerminationFrom.FIRST_DAY_OF_MONTH_FOLLOWING_EVENT_DATE) {
                LocalDate podTerminationDate = terminationModel.getDeactivationDate();
                LocalDate nextMonthDate = podTerminationDate.plusMonths(1);
                LocalDate terminationDate = LocalDate.of(nextMonthDate.getYear(), nextMonthDate.getMonth(), 1);
                log.info("Termination date when FIRST_DAY_OF_MONTH_FOLLOWING_EVENT_DATE is selected: " + terminationDate);
                if (terminationDate.isAfter(LocalDate.now())) {
                    return;
                }
                productContract.setTerminationDate(terminationDate);
                productContract.setContractStatus(ContractDetailsStatus.TERMINATED);
                productContract.setSubStatus(ContractDetailsSubStatus.ALL_PODS_ARE_DEACTIVATED);
            }
            productContractRepository.saveAndFlush(productContract);
            List<Long> customerIdsToChangeStatusWithContractId = productContractDetailsRepository.getCustomerIdsToChangeStatusWithContractId(productContract.getId());
            customerStatusChangeEventPublisher.publishCustomerStatusChangeEvent(customerIdsToChangeStatusWithContractId);

            Optional<ProductContractDetails> respectiveProductContractDetailOptional = productContractDetailsRepository.findRespectiveProductContractDetailsByProductContractId(LocalDate.now(), productContract.getId());
            if (respectiveProductContractDetailOptional.isPresent() && Boolean.TRUE.equals(terminationModel.getNoticeDue())) {
                ProductContractDetails respectiveProductContract = respectiveProductContractDetailOptional.get();
                contractTerminationEmailSenderService.createAndSendTerminationEmail(
                        respectiveProductContract.getId(),
                        true,
                        respectiveProductContract.getCustomerDetailId(),
                        respectiveProductContract.getCustomerCommunicationIdForContract(),
                        model.getTerminationId()
                );
            }
        }
    }
}