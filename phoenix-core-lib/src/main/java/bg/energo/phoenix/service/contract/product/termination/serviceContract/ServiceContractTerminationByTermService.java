package bg.energo.phoenix.service.contract.product.termination.serviceContract;


import bg.energo.phoenix.model.entity.contract.service.ServiceContractDetails;
import bg.energo.phoenix.model.entity.contract.service.ServiceContracts;
import bg.energo.phoenix.model.entity.product.service.ServiceDetails;
import bg.energo.phoenix.model.entity.product.term.terms.Terms;
import bg.energo.phoenix.model.entity.product.term.termsGroups.TermGroupDetails;
import bg.energo.phoenix.model.entity.product.term.termsGroups.TermsGroupTerms;
import bg.energo.phoenix.model.entity.product.term.termsGroups.TermsGroups;
import bg.energo.phoenix.model.enums.contract.service.ServiceContractDetailStatus;
import bg.energo.phoenix.model.enums.contract.service.ServiceContractDetailsSubStatus;
import bg.energo.phoenix.model.enums.product.term.terms.ContractDeliveryActivationType;
import bg.energo.phoenix.model.enums.product.term.terms.TermStatus;
import bg.energo.phoenix.model.enums.product.term.termsGroup.TermGroupStatus;
import bg.energo.phoenix.model.response.contract.productContract.terminations.serviceContract.ServiceContractTerminationGenericModel;
import bg.energo.phoenix.model.response.contract.productContract.terminations.serviceContract.ServiceContractTerminationProperties;
import bg.energo.phoenix.repository.contract.service.ServiceContractDetailsRepository;
import bg.energo.phoenix.repository.contract.service.ServiceContractsRepository;
import bg.energo.phoenix.repository.product.service.ServiceDetailsRepository;
import bg.energo.phoenix.repository.product.term.terms.TermsRepository;
import bg.energo.phoenix.repository.product.term.termsGroups.TermsGroupDetailsRepository;
import bg.energo.phoenix.repository.product.term.termsGroups.TermsGroupTermsRepository;
import bg.energo.phoenix.service.customer.statusChangeEvent.CustomerStatusChangeEventPublisher;
import bg.energo.phoenix.service.product.termination.terminations.ContractTerminationEmailSenderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceContractTerminationByTermService implements ServiceContractTerminator {
    private final TermsRepository termsRepository;
    private final ServiceContractsRepository serviceContractsRepository;
    private final ServiceContractDetailsRepository serviceContractDetailsRepository;
    private final ServiceDetailsRepository serviceDetailsRepository;
    private final TermsGroupDetailsRepository termsGroupDetailsRepository;
    private final TermsGroupTermsRepository termsGroupTermsRepository;
    private final CustomerStatusChangeEventPublisher customerStatusChangeEventPublisher;

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
    public List<ServiceContractTerminationGenericModel> getContractData(Integer size, List<Long> contractIds) {
        return serviceContractsRepository
                .getEligibleProductContractsForTerminationByTerms(contractIds, PageRequest.of(0, size))
                .stream()
                .map(ServiceContractTerminationGenericModel::new)
                .toList();
    }

    @Override
    public void terminate(ServiceContractTerminationGenericModel model) {
        try {
            ServiceContracts serviceContract = serviceContractsRepository
                    .findById(model.getId())
                    .orElseThrow(() -> new RuntimeException("Service Contract not found"));

            Optional<ServiceContractDetails> serviceContractDetailsOptional = serviceContractDetailsRepository
                    .findLatestServiceContractDetails(model.getId());
            if (serviceContractDetailsOptional.isEmpty()) {
                return;
            }

            ServiceContractDetails serviceContractDetails = serviceContractDetailsOptional.get();

            ServiceDetails serviceDetails = serviceDetailsRepository
                    .findById(serviceContractDetails.getServiceDetailId())
                    .orElseThrow(() -> new RuntimeException("Service Details not found for service contract"));

            Terms terms = serviceDetails.getTerms();
            if (Objects.nonNull(terms)) {
                executeServiceContractTermination(serviceContract, terms);
            } else {
                TermsGroups termsGroups = serviceDetails.getTermsGroups();
                if (Objects.isNull(termsGroups)) {
                    return;
                }

                TermGroupDetails termGroupDetails = termsGroupDetailsRepository
                        .findFirstByGroupIdAndStartDateLessThanEqualOrderByStartDateDesc(termsGroups.getId(), LocalDateTime.now())
                        .orElseThrow(() -> new RuntimeException("Terms group details not found for terms group"));

                TermsGroupTerms termsGroupTerms = termsGroupTermsRepository
                        .findByTermGroupDetailIdAndTermGroupStatusIn(termGroupDetails.getId(), List.of(TermGroupStatus.ACTIVE))
                        .orElseThrow(() -> new RuntimeException("Terms group terms not found for terms details"));

                Terms term = termsRepository
                        .findByIdAndStatusIn(termsGroupTerms.getTermId(), List.of(TermStatus.ACTIVE))
                        .orElseThrow(() -> new RuntimeException("Terms not found for terms group terms"));

                executeServiceContractTermination(serviceContract, term);
            }
        } catch (Exception e) {
            log.error("Termination process failed for Service Contract with id: %s".formatted(model.getId()), e);
        }
    }

    private void executeServiceContractTermination(ServiceContracts serviceContract, Terms termsProxy) {
        Terms terms = termsRepository
                .findById(termsProxy.getId())
                .orElseThrow(() -> new RuntimeException("Terms not found"));
        if (!terms.getContractDeliveryActivationAutoTermination()) {
            return;
        }

        LocalDate calculatedTerminationDate = calculateServiceContractTerminationDate(serviceContract, terms);

        if (Objects.nonNull(calculatedTerminationDate)) {
            LocalDate now = LocalDate.now();
            if (calculatedTerminationDate.isAfter(now)) {
                return;
            }

            serviceContract.setTerminationDate(now.minusDays(1));
            serviceContract.setContractStatus(ServiceContractDetailStatus.TERMINATED);
            serviceContract.setSubStatus(ServiceContractDetailsSubStatus.CONTRACT_IS_NOT_ACTIVE);

            serviceContractsRepository.saveAndFlush(serviceContract);
            List<Long> customerIdsToChangeStatusWithContractId = serviceContractDetailsRepository.getCustomerIdsToChangeStatusWithContractId(serviceContract.getId());
            customerStatusChangeEventPublisher.publishCustomerStatusChangeEvent(customerIdsToChangeStatusWithContractId);

        }
    }

    private LocalDate calculateServiceContractTerminationDate(ServiceContracts serviceContract, Terms terms) {
        LocalDate calculatedTerminationDate;


        LocalDate entryIntoForceDate = serviceContract.getEntryIntoForceDate();
        ContractDeliveryActivationType contractDeliveryActivationType = terms.getContractDeliveryActivationType();
        Integer contractDeliveryActivationValue = terms.getContractDeliveryActivationValue();
        switch (contractDeliveryActivationType) {
            case MONTH -> calculatedTerminationDate = entryIntoForceDate.plusMonths(contractDeliveryActivationValue);
            case WEEK -> calculatedTerminationDate = entryIntoForceDate.plusWeeks(contractDeliveryActivationValue);
            case DAY -> calculatedTerminationDate = entryIntoForceDate.plusDays(contractDeliveryActivationValue);
            default ->
                    throw new IllegalArgumentException("Illegal argument provided for Contract Delivery Activation Type: [%s]".formatted(contractDeliveryActivationType));
        }

        return calculatedTerminationDate;
    }
}
