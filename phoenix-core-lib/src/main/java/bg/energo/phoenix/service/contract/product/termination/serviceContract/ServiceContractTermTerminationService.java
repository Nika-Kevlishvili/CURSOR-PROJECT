package bg.energo.phoenix.service.contract.product.termination.serviceContract;


import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.contract.service.ServiceContractDetails;
import bg.energo.phoenix.model.entity.contract.service.ServiceContracts;
import bg.energo.phoenix.model.entity.contract.service.ServiceTermRenewalCount;
import bg.energo.phoenix.model.entity.product.termination.terminations.Termination;
import bg.energo.phoenix.model.enums.contract.service.ServiceContractDetailStatus;
import bg.energo.phoenix.model.enums.contract.service.ServiceContractDetailsSubStatus;
import bg.energo.phoenix.model.enums.product.termination.terminations.AutoTerminationFrom;
import bg.energo.phoenix.model.response.contract.productContract.terminations.serviceContract.ServiceContractTerminationGenericModel;
import bg.energo.phoenix.model.response.contract.productContract.terminations.serviceContract.ServiceContractTerminationProperties;
import bg.energo.phoenix.model.response.contract.productContract.terminations.serviceContract.ServiceContractTerminationWithContractTermsResponse;
import bg.energo.phoenix.repository.contract.service.ServiceContractDetailsRepository;
import bg.energo.phoenix.repository.contract.service.ServiceContractsRepository;
import bg.energo.phoenix.repository.contract.service.ServiceTermRenewalCountRepository;
import bg.energo.phoenix.repository.product.termination.terminations.TerminationRepository;
import bg.energo.phoenix.service.customer.statusChangeEvent.CustomerStatusChangeEventPublisher;
import bg.energo.phoenix.service.product.termination.terminations.ContractTerminationEmailSenderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ServiceContractTermTerminationService implements ServiceContractTerminator {

    private final ServiceContractsRepository serviceContractsRepository;
    private final ServiceContractDetailsRepository serviceContractDetailsRepository;
    private final TerminationRepository terminationRepository;
    private final ServiceTermRenewalCountRepository serviceTermRenewalCountRepository;
    private final CustomerStatusChangeEventPublisher customerStatusChangeEventPublisher;
    private final ContractTerminationEmailSenderService contractTerminationEmailSenderService;

    @Override
    public ServiceContractTerminationProperties getProperties() {
        //TODO: fetch values from properties file
        return ServiceContractTerminationProperties
                .builder()
                .numberOfThreads(5)
                .batchSize(10)
                .queryBatchSize(10)
                .build();
    }

    @Override
    public List<ServiceContractTerminationGenericModel> getContractData(Integer size, List<Long> contractIdsToExclude) {
        return serviceContractsRepository.getServiceContractsForTermDeactivation(size, contractIdsToExclude).stream().map(ServiceContractTerminationGenericModel::new).toList();
    }

    @Override
    public void terminate(ServiceContractTerminationGenericModel model) {
        ServiceContractTerminationWithContractTermsResponse terminationDto = model.getTermsResponses();
        Long contractId = terminationDto.getContractId();
        Long detailId = terminationDto.getDetailId();
        Optional<ServiceContracts> serviceContractOptional = serviceContractsRepository.findByIdAndStatusIn(contractId, List.of(EntityStatus.ACTIVE));
        if (serviceContractOptional.isEmpty()) {
            return;
        }
        ServiceContracts serviceContract = serviceContractOptional.get();
        Optional<ServiceContractDetails> contractDetailsOptional = serviceContractDetailsRepository.findById(detailId);
        if (contractDetailsOptional.isEmpty()) {
            return;
        }
        ServiceContractDetails serviceContractDetails = contractDetailsOptional.get();

        if (serviceContract.getContractTermEndDate() == null || !serviceContract.getContractTermEndDate().isBefore(LocalDate.now())) {
            return;
        }
        if (terminationDto.getAutomaticRenewal() == null || terminationDto.getAutomaticRenewal().equals(Boolean.FALSE)) {

            checkServiceContractPerpetuity(serviceContract, serviceContractDetails, terminationDto);
        } else {
            doServiceContractRenewals(serviceContract, serviceContractDetails, terminationDto);
        }
    }

    private void doServiceContractRenewals(ServiceContracts serviceContracts, ServiceContractDetails serviceContractDetails, ServiceContractTerminationWithContractTermsResponse terminationDto) {
        if (terminationDto.getNumberOfRenewals() == null || terminationDto.getNumberOfRenewals() < 0) {
            renewServiceContract(serviceContracts, terminationDto);
        } else {
            checkIfRenewalPeriodReached(serviceContracts, serviceContractDetails, terminationDto);
        }
    }

    private void checkIfRenewalPeriodReached(ServiceContracts serviceContracts, ServiceContractDetails serviceContractDetails, ServiceContractTerminationWithContractTermsResponse terminationDto) {
        Integer numberOfRenewals = terminationDto.getNumberOfRenewals();
        Optional<ServiceTermRenewalCount> renewalCountOptional = serviceTermRenewalCountRepository.findByTermIdAndContractDetailId(terminationDto.getTermId(), serviceContractDetails.getId());
        if (renewalCountOptional.isEmpty()) {
            serviceTermRenewalCountRepository.save(new ServiceTermRenewalCount(null, terminationDto.getTermId(), serviceContractDetails.getId(), 1));
            renewServiceContract(serviceContracts, terminationDto);
            return;
        } else {
            ServiceTermRenewalCount serviceTermRenewalCount = renewalCountOptional.get();
            if (serviceTermRenewalCount.getRenewalCount() < numberOfRenewals) {
                renewServiceContract(serviceContracts, terminationDto);
                serviceTermRenewalCount.setRenewalCount(serviceTermRenewalCount.getRenewalCount() + 1);
                serviceTermRenewalCountRepository.save(serviceTermRenewalCount);
                return;
            }
        }
        checkServiceContractPerpetuity(serviceContracts, serviceContractDetails, terminationDto);
    }

    private void renewServiceContract(ServiceContracts serviceContract, ServiceContractTerminationWithContractTermsResponse terminationDto) {
        serviceContract.setContractTermEndDate(serviceContract.getContractTermEndDate().plus(terminationDto.getRenewalValue(), terminationDto.getRenewalPeriodType().getUnit()));
        serviceContractsRepository.save(serviceContract);
    }

    private void checkServiceContractPerpetuity(ServiceContracts serviceContract, ServiceContractDetails serviceContractDetails, ServiceContractTerminationWithContractTermsResponse terminationDto) {
        if (terminationDto.getPerpetuityCause() == null || terminationDto.getPerpetuityCause().equals(Boolean.FALSE)) {
            checkServiceContractTerminationObject(serviceContract, serviceContractDetails, terminationDto);
        } else {
            serviceContract.setPerpetuityDate(serviceContract.getContractTermEndDate().plusDays(1));
            serviceContract.setContractTermEndDate(LocalDate.of(2090, 12, 31));
            serviceContract.setContractStatus(ServiceContractDetailStatus.ACTIVE_IN_PERPETUITY);
            serviceContract.setSubStatus(ServiceContractDetailsSubStatus.DELIVERY);
            serviceContractsRepository.saveAndFlush(serviceContract);
            List<Long> customerIdsToChangeStatusWithContractId = serviceContractDetailsRepository.getCustomerIdsToChangeStatusWithContractId(serviceContract.getId());
            customerStatusChangeEventPublisher.publishCustomerStatusChangeEvent(customerIdsToChangeStatusWithContractId);
        }
    }

    private void checkServiceContractTerminationObject(ServiceContracts serviceContract, ServiceContractDetails serviceContractDetails, ServiceContractTerminationWithContractTermsResponse terminationDto) {
        if (terminationDto.getTerminationId() == null) {
            return;
        }
        Optional<Termination> terminations = terminationRepository.findById(terminationDto.getTerminationId());
        if (terminations.isEmpty()) {
            return;
        }
        Termination termination = terminations.get();
        if (termination.getAutoTerminationFrom().equals(AutoTerminationFrom.EVENT_DATE)) {
            serviceContract.setTerminationDate(serviceContract.getContractTermEndDate());
            serviceContract.setContractStatus(ServiceContractDetailStatus.TERMINATED);
            serviceContract.setSubStatus(ServiceContractDetailsSubStatus.EXPIRED);
        } else if (termination.getAutoTerminationFrom().equals(AutoTerminationFrom.FIRST_DAY_OF_MONTH_FOLLOWING_EVENT_DATE)) {
            LocalDate contractTermEndDate = serviceContract.getContractTermEndDate();
            LocalDate nextMonthDate = contractTermEndDate.plusMonths(1);
            LocalDate terminationDate = LocalDate.of(nextMonthDate.getYear(), nextMonthDate.getMonth(), 1);
            if (!terminationDate.isBefore(LocalDate.now())) {
                return;
            }
            serviceContract.setTerminationDate(serviceContract.getContractTermEndDate());
            serviceContract.setContractStatus(ServiceContractDetailStatus.TERMINATED);
            serviceContract.setSubStatus(ServiceContractDetailsSubStatus.EXPIRED);
        }
        serviceContractsRepository.saveAndFlush(serviceContract);
        List<Long> customerIdsToChangeStatusWithContractId = serviceContractDetailsRepository.getCustomerIdsToChangeStatusWithContractId(serviceContract.getId());
        customerStatusChangeEventPublisher.publishCustomerStatusChangeEvent(customerIdsToChangeStatusWithContractId);

        Optional<ServiceContractDetails> respectiveServiceContractDetailOptional = serviceContractDetailsRepository.findRespectiveServiceContractDetailsByServiceContractId(LocalDate.now(), serviceContract.getId());
        if (respectiveServiceContractDetailOptional.isPresent() && Boolean.TRUE.equals(termination.getNoticeDue())) {
            ServiceContractDetails respectiveServiceContract = respectiveServiceContractDetailOptional.get();
            contractTerminationEmailSenderService.createAndSendTerminationEmail(
                    respectiveServiceContract.getId(),
                    false,
                    respectiveServiceContract.getCustomerDetailId(),
                    respectiveServiceContract.getCustomerCommunicationIdForContract(),
                    termination.getId()
            );
        }
    }
}
