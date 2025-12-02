package bg.energo.phoenix.service.contract.product.termination;

import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.OperationNotAllowedException;
import bg.energo.phoenix.model.entity.contract.product.ProductContract;
import bg.energo.phoenix.model.entity.contract.product.ProductContractDetails;
import bg.energo.phoenix.model.enums.contract.products.ContractDetailsStatus;
import bg.energo.phoenix.model.enums.contract.products.ContractDetailsSubStatus;
import bg.energo.phoenix.model.response.contract.productContract.terminations.ProductContractTerminationGenericModel;
import bg.energo.phoenix.model.response.contract.productContract.terminations.ProductContractTerminationProperties;
import bg.energo.phoenix.model.response.contract.productContract.terminations.ProductContractTerminationWithActionsResponse;
import bg.energo.phoenix.repository.contract.product.ProductContractDetailsRepository;
import bg.energo.phoenix.repository.contract.product.ProductContractRepository;
import bg.energo.phoenix.service.crm.emailCommunication.EmailCommunicationService;
import bg.energo.phoenix.service.customer.statusChangeEvent.CustomerStatusChangeEventPublisher;
import bg.energo.phoenix.service.product.termination.terminations.ContractTerminationEmailSenderService;
import bg.energo.phoenix.util.contract.action.ActionTypeProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@Order(1)
@RequiredArgsConstructor
public class ProductContractTerminationWithActionService implements ProductContractTerminator {

    private final ProductContractRepository productContractRepository;
    private final ActionTypeProperties actionTypeProperties;
    private final ProductContractDetailsRepository productContractDetailsRepository;
    private final CustomerStatusChangeEventPublisher customerStatusChangeEventPublisher;
    private final EmailCommunicationService emailCommunicationService;
    private final ContractTerminationEmailSenderService contractTerminationEmailSenderService;
    @Value("${product-contract.termination-with-action.number-of-threads}")
    private Integer numberOfThreads;

    @Value("${product-contract.termination-with-action.batch-size}")
    private Integer batchSize;

    @Value("${product-contract.termination-with-action.query-batch-size}")
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
                .getEligibleProductContractsForTerminationWithAction(PageRequest.of(page, size))

                .map(ProductContractTerminationGenericModel::new)
                .getContent();
    }


    @Override
    @Transactional
    public void terminate(ProductContractTerminationGenericModel model) {
        ProductContractTerminationWithActionsResponse actionsResponse = model.getActionsResponse();
        if (actionsResponse == null) {
            return;
        }

        ProductContract productContract = productContractRepository
                .findById(actionsResponse.getContractId())
                .orElse(null);

        if (productContract == null) {
            return;
        }

        switch (actionsResponse.getAutoTerminationFrom()) {
            case EVENT_DATE -> tryTerminationWhenTypeEventDate(
                    productContract,
                    actionsResponse
            );
            case FIRST_DAY_OF_MONTH_FOLLOWING_EVENT_DATE -> tryTerminationWhenTypeFirstDayOfFollowingMonth(
                    productContract,
                    actionsResponse
            );
        }

        productContractRepository.saveAndFlush(productContract);
        List<Long> customerIdsToChangeStatusWithContractId = productContractDetailsRepository.getCustomerIdsToChangeStatusWithContractId(productContract.getId());
        customerStatusChangeEventPublisher.publishCustomerStatusChangeEvent(customerIdsToChangeStatusWithContractId);

        Optional<ProductContractDetails> respectiveProductContractDetailOptional = productContractDetailsRepository.findRespectiveProductContractDetailsByProductContractId(LocalDate.now(), productContract.getId());
        if (respectiveProductContractDetailOptional.isPresent() && Boolean.TRUE.equals(actionsResponse.getNoticeDue())) {
            ProductContractDetails respectiveProductContract = respectiveProductContractDetailOptional.get();
            contractTerminationEmailSenderService.createAndSendTerminationEmail(
                    respectiveProductContract.getId(),
                    true,
                    respectiveProductContract.getCustomerDetailId(),
                    respectiveProductContract.getCustomerCommunicationIdForContract(),
                    model.getTerminationId());
        }
    }


    /**
     * Processes the termination when the type is event date. In this case, the termination date is the same as the action execution date.
     * If the action execution date is in the future, the termination process is stopped.
     *
     * @param productContract the contract which is the candidate for termination
     * @param actionsResponse object containing the necessary information for deciding the termination date
     */
    private void tryTerminationWhenTypeEventDate(ProductContract productContract, ProductContractTerminationWithActionsResponse actionsResponse) {
        if (actionsResponse.getActionExecutionDate().isAfter(LocalDate.now())) {
            log.info("Product contract [ID {}] - Termination date is in the future. Stopping the termination process.", productContract.getId());
            return;
        }

        productContract.setTerminationDate(actionsResponse.getActionExecutionDate());
        productContract.setContractStatus(ContractDetailsStatus.TERMINATED);
        setContractSubStatus(productContract, actionsResponse);
    }


    /**
     * Processes the termination when the type is first day of following month.
     * In this case, the termination date is the first day of the month following the action execution date.
     * If the termination date is in the future, the termination process is stopped.
     *
     * @param productContract the contract which is the candidate for termination
     * @param actionsResponse object containing the necessary information for deciding the termination date
     */
    private void tryTerminationWhenTypeFirstDayOfFollowingMonth(ProductContract productContract, ProductContractTerminationWithActionsResponse actionsResponse) {
        LocalDate nextMonthDate = actionsResponse.getActionExecutionDate().plusMonths(1);
        LocalDate terminationDate = LocalDate.of(nextMonthDate.getYear(), nextMonthDate.getMonth(), 1);

        if (terminationDate.isAfter(LocalDate.now())) {
            log.info("Product contract [ID {}] - Termination date is in the future. Stopping the termination process.", productContract.getId());
            return;
        }

        productContract.setTerminationDate(terminationDate);
        productContract.setContractStatus(ContractDetailsStatus.TERMINATED);
        setContractSubStatus(productContract, actionsResponse);
    }


    /**
     * Sets the contract sub status based on the action penalty payer and action type.
     *
     * @param actionsResponse object containing the necessary information for deciding the termination process
     */
    private void setContractSubStatus(ProductContract productContract, ProductContractTerminationWithActionsResponse actionsResponse) {
        boolean isTypeContractTerminationWithNotice = actionsResponse.getActionTypeId().equals(actionTypeProperties.getContractTerminationWithNoticeId());
        boolean isTypeContractTerminationWithoutNotice = actionsResponse.getActionTypeId().equals(actionTypeProperties.getContractTerminationWithoutNoticeId());

        switch (actionsResponse.getActionPenaltyPayer()) {
            case CUSTOMER -> {
                if (isTypeContractTerminationWithNotice) {
                    productContract.setSubStatus(ContractDetailsSubStatus.FROM_CUSTOMER_WITH_NOTICE);
                } else if (isTypeContractTerminationWithoutNotice) {
                    productContract.setSubStatus(ContractDetailsSubStatus.FROM_CUSTOMER_WITHOUT_NOTICE);
                }
            }
            case EPRES -> {
                if (isTypeContractTerminationWithNotice) {
                    productContract.setSubStatus(ContractDetailsSubStatus.FROM_EPRES_WITH_NOTICE);
                } else if (isTypeContractTerminationWithoutNotice) {
                    productContract.setSubStatus(ContractDetailsSubStatus.FROM_EPRES_WITHOUT_NOTICE);
                }
            }
        }
    }


    // TODO: 11/8/23 This method is for testing purposes only. Remove it when the termination process is ready for release.
    @Transactional
    public String terminateForTesting(Long contractId, int sizeToFilter, boolean persist) {
        List<ProductContractTerminationGenericModel> contractsEligibleForActionTermination = getContractData(sizeToFilter, 0);

        if (CollectionUtils.isEmpty(contractsEligibleForActionTermination)) {
            throw new DomainEntityNotFoundException("No contracts eligible for termination with action found.");
        }

        List<ProductContractTerminationGenericModel> filteredContracts = contractsEligibleForActionTermination
                .stream()
                .filter(model -> model.getActionsResponse() != null && model.getActionsResponse().getContractId().equals(contractId))
                .toList();

        if (filteredContracts.isEmpty()) {
            throw new OperationNotAllowedException("The provided contract ID %s is not eligible for termination with action.".formatted(contractId));
        }

        ProductContractTerminationWithActionsResponse actionsResponse = filteredContracts.get(0).getActionsResponse();

        ProductContract productContract = productContractRepository
                .findById(actionsResponse.getContractId())
                .orElse(null);

        if (productContract == null) {
            throw new IllegalArgumentException("The provided contract ID %s is not valid.".formatted(contractId));
        }

        switch (actionsResponse.getAutoTerminationFrom()) {
            case EVENT_DATE -> tryTerminationWhenTypeEventDate(
                    productContract,
                    actionsResponse
            );
            case FIRST_DAY_OF_MONTH_FOLLOWING_EVENT_DATE -> tryTerminationWhenTypeFirstDayOfFollowingMonth(
                    productContract,
                    actionsResponse
            );
        }

        if (persist) {
            productContractRepository.save(productContract);

            Optional<ProductContractDetails> respectiveProductContractDetailOptional = productContractDetailsRepository.findRespectiveProductContractDetailsByProductContractId(LocalDate.now(), productContract.getId());
            if (respectiveProductContractDetailOptional.isPresent() && Boolean.TRUE.equals(actionsResponse.getNoticeDue())) {
                ProductContractDetails respectiveProductContract = respectiveProductContractDetailOptional.get();
                contractTerminationEmailSenderService.createAndSendTerminationEmail(
                        respectiveProductContract.getId(),
                        true,
                        respectiveProductContract.getCustomerDetailId(),
                        respectiveProductContract.getCustomerCommunicationIdForContract(),
                        actionsResponse.getTerminationId());
            }
            return "The contract with ID %s was terminated successfully.".formatted(contractId);
        }

        String resultMessage = (
                "If you had persisted the contract with ID %s, it would have been terminated with the following properties: " +
                        "Termination date - %s, Contract status - %s, Contract sub status - %s."
        ).formatted(contractId, productContract.getTerminationDate(), productContract.getContractStatus(), productContract.getSubStatus());

        throw new OperationNotAllowedException(resultMessage);
    }
}
