package bg.energo.phoenix.service.contract.product.termination.serviceContract;

import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.OperationNotAllowedException;
import bg.energo.phoenix.model.entity.contract.product.ProductContractDetails;
import bg.energo.phoenix.model.entity.contract.service.ServiceContractDetails;
import bg.energo.phoenix.model.entity.contract.service.ServiceContracts;
import bg.energo.phoenix.model.enums.contract.service.ServiceContractDetailStatus;
import bg.energo.phoenix.model.enums.contract.service.ServiceContractDetailsSubStatus;
import bg.energo.phoenix.model.response.contract.productContract.terminations.serviceContract.ServiceContractTerminationGenericModel;
import bg.energo.phoenix.model.response.contract.productContract.terminations.serviceContract.ServiceContractTerminationProperties;
import bg.energo.phoenix.model.response.contract.productContract.terminations.serviceContract.ServiceContractTerminationWithActionsResponse;
import bg.energo.phoenix.repository.contract.service.ServiceContractDetailsRepository;
import bg.energo.phoenix.repository.contract.service.ServiceContractsRepository;
import bg.energo.phoenix.service.customer.statusChangeEvent.CustomerStatusChangeEventPublisher;
import bg.energo.phoenix.service.product.termination.terminations.ContractTerminationEmailSenderService;
import bg.energo.phoenix.util.contract.action.ActionTypeProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceContractTerminationWithActionService implements ServiceContractTerminator {

    private final ServiceContractsRepository serviceContractsRepository;
    private final ActionTypeProperties actionTypeProperties;
    private final ServiceContractDetailsRepository serviceContractDetailsRepository;
    private final CustomerStatusChangeEventPublisher customerStatusChangeEventPublisher;
    private final ContractTerminationEmailSenderService contractTerminationEmailSenderService;

    @Value("${service-contract.termination-with-action.number-of-threads}")
    private Integer numberOfThreads;

    @Value("${service-contract.termination-with-action.batch-size}")
    private Integer batchSize;

    @Value("${service-contract.termination-with-action.query-batch-size}")
    private Integer queryBatchSize;


    @Override
    public ServiceContractTerminationProperties getProperties() {
        return ServiceContractTerminationProperties
                .builder()
                .numberOfThreads(numberOfThreads)
                .batchSize(batchSize)
                .queryBatchSize(queryBatchSize)
                .build();
    }


    @Override
    public List<ServiceContractTerminationGenericModel> getContractData(Integer size, List<Long> contractIdsToExclude) {
        return serviceContractsRepository
                .getEligibleServiceContractsForTerminationWithAction(size, contractIdsToExclude)
                .stream()
                .map(ServiceContractTerminationGenericModel::new)
                .toList();
    }


    @Override
    @Transactional
    public void terminate(ServiceContractTerminationGenericModel model) {
        ServiceContractTerminationWithActionsResponse actionsResponse = model.getActionsResponse();
        if (actionsResponse == null) {
            return;
        }

        ServiceContracts serviceContract = serviceContractsRepository
                .findById(actionsResponse.getContractId())
                .orElse(null);

        if (serviceContract == null) {
            return;
        }

        switch (actionsResponse.getAutoTerminationFrom()) {
            case EVENT_DATE -> tryTerminationWhenTypeEventDate(
                    serviceContract,
                    actionsResponse
            );
            case FIRST_DAY_OF_MONTH_FOLLOWING_EVENT_DATE -> tryTerminationWhenTypeFirstDayOfFollowingMonth(
                    serviceContract,
                    actionsResponse
            );
        }

        serviceContractsRepository.saveAndFlush(serviceContract);
        List<Long> customerIdsToChangeStatusWithContractId = serviceContractDetailsRepository.getCustomerIdsToChangeStatusWithContractId(serviceContract.getId());
        customerStatusChangeEventPublisher.publishCustomerStatusChangeEvent(customerIdsToChangeStatusWithContractId);

        Optional<ServiceContractDetails> respectiveServiceContractDetailOptional = serviceContractDetailsRepository.findRespectiveServiceContractDetailsByServiceContractId(LocalDate.now(), serviceContract.getId());
        if (respectiveServiceContractDetailOptional.isPresent() && Boolean.TRUE.equals(actionsResponse.getNoticeDue())) {
            ServiceContractDetails respectiveServiceContract = respectiveServiceContractDetailOptional.get();
            contractTerminationEmailSenderService.createAndSendTerminationEmail(
                    respectiveServiceContract.getId(),
                    false,
                    respectiveServiceContract.getCustomerDetailId(),
                    respectiveServiceContract.getCustomerCommunicationIdForContract(),
                    actionsResponse.getTerminationId()
            );
        }
    }


    /**
     * Processes the termination when the type is event date. In this case, the termination date is the same as the action execution date.
     * If the action execution date is in the future, the termination process is stopped.
     *
     * @param serviceContract the contract which is the candidate for termination
     * @param actionsResponse object containing the necessary information for deciding the termination date
     */
    private void tryTerminationWhenTypeEventDate(ServiceContracts serviceContract, ServiceContractTerminationWithActionsResponse actionsResponse) {
        if (actionsResponse.getActionExecutionDate().isAfter(LocalDate.now())) {
            log.info("Service contract [ID {}] - Termination date is in the future. Stopping the termination process.", serviceContract.getId());
            return;
        }

        serviceContract.setTerminationDate(actionsResponse.getActionExecutionDate());
        serviceContract.setContractStatus(ServiceContractDetailStatus.TERMINATED);
        setContractSubStatus(serviceContract, actionsResponse);
    }


    /**
     * Processes the termination when the type is first day of following month.
     * In this case, the termination date is the first day of the month following the action execution date.
     * If the termination date is in the future, the termination process is stopped.
     *
     * @param serviceContract the contract which is the candidate for termination
     * @param actionsResponse object containing the necessary information for deciding the termination date
     */
    private void tryTerminationWhenTypeFirstDayOfFollowingMonth(ServiceContracts serviceContract, ServiceContractTerminationWithActionsResponse actionsResponse) {
        LocalDate nextMonthDate = actionsResponse.getActionExecutionDate().plusMonths(1);
        LocalDate terminationDate = LocalDate.of(nextMonthDate.getYear(), nextMonthDate.getMonth(), 1);

        if (terminationDate.isAfter(LocalDate.now())) {
            log.info("Service contract [ID {}] - Termination date is in the future. Stopping the termination process.", serviceContract.getId());
            return;
        }

        serviceContract.setTerminationDate(terminationDate);
        serviceContract.setContractStatus(ServiceContractDetailStatus.TERMINATED);
        setContractSubStatus(serviceContract, actionsResponse);
    }


    /**
     * Sets the contract sub status based on the action type.
     * If the action type is not found, the sub status is set to null.
     *
     * @param serviceContract the contract which is the candidate for termination
     * @param actionsResponse object containing the necessary information for deciding the termination date
     */
    private void setContractSubStatus(ServiceContracts serviceContract, ServiceContractTerminationWithActionsResponse actionsResponse) {
        boolean isTypeContractTerminationWithNotice = actionsResponse.getActionTypeId().equals(actionTypeProperties.getContractTerminationWithNoticeId());
        boolean isTypeContractTerminationWithoutNotice = actionsResponse.getActionTypeId().equals(actionTypeProperties.getContractTerminationWithoutNoticeId());

        switch (actionsResponse.getActionPenaltyPayer()) {
            case CUSTOMER -> {
                if (isTypeContractTerminationWithNotice) {
                    serviceContract.setSubStatus(ServiceContractDetailsSubStatus.FROM_CUSTOMER_WITH_NOTICE);
                } else if (isTypeContractTerminationWithoutNotice) {
                    serviceContract.setSubStatus(ServiceContractDetailsSubStatus.FROM_CUSTOMER_WITHOUT_NOTICE);
                }
            }
            case EPRES -> {
                if (isTypeContractTerminationWithNotice) {
                    serviceContract.setSubStatus(ServiceContractDetailsSubStatus.FROM_EPRES_WITH_NOTICE);
                } else if (isTypeContractTerminationWithoutNotice) {
                    serviceContract.setSubStatus(ServiceContractDetailsSubStatus.FROM_EPRES_WITHOUT_NOTICE);
                }
            }
        }
    }


    // TODO: 11/23/23 This method is for testing purposes only. Remove it when the termination process is ready for release.
    @Transactional
    public String terminateForTesting(Long contractId, int sizeToFilter, boolean persist) {
        List<ServiceContractTerminationGenericModel> contractEligibleForActionTermination = getContractData(sizeToFilter, List.of());

        if (CollectionUtils.isEmpty(contractEligibleForActionTermination)) {
            throw new DomainEntityNotFoundException("No contracts found for termination with action.");
        }

        List<ServiceContractTerminationGenericModel> filteredContracts = contractEligibleForActionTermination
                .stream()
                .filter(model -> model.getActionsResponse() != null && model.getActionsResponse().getContractId().equals(contractId))
                .toList();

        if (filteredContracts.isEmpty()) {
            throw new OperationNotAllowedException("The provided contract ID %s is not eligible for termination with action.".formatted(contractId));
        }

        ServiceContractTerminationWithActionsResponse actionsResponse = filteredContracts.get(0).getActionsResponse();

        ServiceContracts serviceContract = serviceContractsRepository
                .findById(actionsResponse.getContractId())
                .orElse(null);

        if (serviceContract == null) {
            throw new IllegalArgumentException("The provided contract ID %s is not found.".formatted(contractId));
        }

        switch (actionsResponse.getAutoTerminationFrom()) {
            case EVENT_DATE -> tryTerminationWhenTypeEventDate(
                    serviceContract,
                    actionsResponse
            );
            case FIRST_DAY_OF_MONTH_FOLLOWING_EVENT_DATE -> tryTerminationWhenTypeFirstDayOfFollowingMonth(
                    serviceContract,
                    actionsResponse
            );
        }

        if (persist) {
            serviceContractsRepository.save(serviceContract);
            Optional<ServiceContractDetails> respectiveServiceContractDetailOptional = serviceContractDetailsRepository.findRespectiveServiceContractDetailsByServiceContractId(LocalDate.now(), serviceContract.getId());
            if (respectiveServiceContractDetailOptional.isPresent() && Boolean.TRUE.equals(actionsResponse.getNoticeDue())) {
                ServiceContractDetails respectiveServiceContract = respectiveServiceContractDetailOptional.get();
                contractTerminationEmailSenderService.createAndSendTerminationEmail(
                        respectiveServiceContract.getId(),
                        false,
                        respectiveServiceContract.getCustomerDetailId(),
                        respectiveServiceContract.getCustomerCommunicationIdForContract(),
                        actionsResponse.getTerminationId()
                );
            }
            return "The contract with ID %s has been terminated successfully.".formatted(contractId);
        }

        String resultMessage = (
                "If you had persisted the contract with ID %s, it would have been terminated with the following properties: " +
                        "Termination date - %s, Contract status - %s, Contract sub status - %s."
        ).formatted(contractId, serviceContract.getTerminationDate(), serviceContract.getContractStatus(), serviceContract.getSubStatus());

        throw new OperationNotAllowedException(resultMessage);
    }

}
