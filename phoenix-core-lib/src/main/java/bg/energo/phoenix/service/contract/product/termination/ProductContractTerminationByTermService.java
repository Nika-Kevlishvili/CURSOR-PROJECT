package bg.energo.phoenix.service.contract.product.termination;

import bg.energo.phoenix.model.entity.contract.product.ProductContract;
import bg.energo.phoenix.model.entity.contract.product.ProductContractDetails;
import bg.energo.phoenix.model.entity.product.product.ProductDetails;
import bg.energo.phoenix.model.entity.product.term.terms.Terms;
import bg.energo.phoenix.model.entity.product.term.termsGroups.TermGroupDetails;
import bg.energo.phoenix.model.entity.product.term.termsGroups.TermsGroupTerms;
import bg.energo.phoenix.model.entity.product.term.termsGroups.TermsGroups;
import bg.energo.phoenix.model.enums.contract.products.ContractDetailsStatus;
import bg.energo.phoenix.model.enums.contract.products.ContractDetailsSubStatus;
import bg.energo.phoenix.model.enums.product.term.terms.ContractDeliveryActivationType;
import bg.energo.phoenix.model.enums.product.term.terms.TermStatus;
import bg.energo.phoenix.model.enums.product.term.termsGroup.TermGroupStatus;
import bg.energo.phoenix.model.response.contract.productContract.terminations.ProductContractTerminationGenericModel;
import bg.energo.phoenix.model.response.contract.productContract.terminations.ProductContractTerminationProperties;
import bg.energo.phoenix.repository.contract.billing.ContractPodRepository;
import bg.energo.phoenix.repository.contract.product.ProductContractDetailsRepository;
import bg.energo.phoenix.repository.contract.product.ProductContractRepository;
import bg.energo.phoenix.repository.product.product.ProductDetailsRepository;
import bg.energo.phoenix.repository.product.term.terms.TermsRepository;
import bg.energo.phoenix.repository.product.term.termsGroups.TermsGroupDetailsRepository;
import bg.energo.phoenix.repository.product.term.termsGroups.TermsGroupTermsRepository;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
@Order(4)
@RequiredArgsConstructor
public class ProductContractTerminationByTermService implements ProductContractTerminator {
    private final TermsRepository termsRepository;
    private final ProductContractRepository productContractRepository;
    private final ProductContractDetailsRepository productContractDetailsRepository;
    private final ProductDetailsRepository productDetailsRepository;
    private final TermsGroupDetailsRepository termsGroupDetailsRepository;
    private final TermsGroupTermsRepository termsGroupTermsRepository;
    private final CustomerStatusChangeEventPublisher customerStatusChangeEventPublisher;
    private final ContractPodRepository contractPodRepository;
    private final EmailCommunicationService emailCommunicationService;
    private final ContractTerminationEmailSenderService contractTerminationEmailSenderService;
    @Value("${product-contract.termination-by-term.batch-size}")
    private Integer batchSize;

    @Value("${product-contract.termination-by-term.number-of-threads}")
    private Integer numberOfThreads;

    @Value("${product-contract.termination-by-term.query-batch-size}")
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
                .getEligibleProductContractsForTerminationByTerms( PageRequest.of(page, size))
                .map(ProductContractTerminationGenericModel::new).getContent();
    }

    @Override
    public void terminate(ProductContractTerminationGenericModel model) {
        try {
            ProductContract productContract = productContractRepository
                    .findById(model.getId())
                    .orElseThrow(() -> new RuntimeException("123"));

            Optional<ProductContractDetails> productContractDetailsOptional = productContractDetailsRepository
                    .findLatestProductContractDetails(model.getId());
            if (productContractDetailsOptional.isEmpty()) {
                return;
            }

            ProductContractDetails productContractDetails = productContractDetailsOptional.get();

            ProductDetails productDetails = productDetailsRepository
                    .findById(productContractDetails.getProductDetailId())
                    .orElseThrow(() -> new RuntimeException("Termination cannot be executed, Product Details not found for product contract"));

            if (contractPodRepository.existsAnyActiveFutureOrPresentPointOfDeliveryByProductContractId(productContract.getId())) {
                throw new RuntimeException("Termination cannot be executed, because contract has active point of delivery");
            }

            Terms terms = productDetails.getTerms();
            if (Objects.nonNull(terms)) {
                executeTermination(productContract, terms);
            } else {
                TermsGroups termsGroups = productDetails.getTermsGroups();
                if (Objects.isNull(termsGroups)) {
                    return;
                }

                TermGroupDetails termGroupDetails = termsGroupDetailsRepository
                        .findFirstByGroupIdAndStartDateLessThanEqualOrderByStartDateDesc(termsGroups.getId(), LocalDateTime.now())
                        .orElseThrow(() -> new RuntimeException("Termination cannot be executed, Terms group details not found for terms group"));

                TermsGroupTerms termsGroupTerms = termsGroupTermsRepository
                        .findByTermGroupDetailIdAndTermGroupStatusIn(termGroupDetails.getId(), List.of(TermGroupStatus.ACTIVE))
                        .orElseThrow(() -> new RuntimeException("Terms group terms not found for terms details"));

                Terms term = termsRepository
                        .findByIdAndStatusIn(termsGroupTerms.getTermId(), List.of(TermStatus.ACTIVE))
                        .orElseThrow(() -> new RuntimeException("Terms not found for terms group terms"));

                executeTermination(productContract, term);
            }
        } catch (Exception e) {
            log.error("Termination process failed for Product Contract with id: %s".formatted(model.getId()), e);
        }
    }

    private void executeTermination(ProductContract productContract, Terms termsProxy) {
        Terms terms = termsRepository
                .findById(termsProxy.getId())
                .orElseThrow(() -> new RuntimeException("Terms not found"));
        if (!terms.getContractDeliveryActivationAutoTermination()) {
            return;
        }

        LocalDate calculatedTerminationDate = calculateTerminationDate(productContract, terms);

        if (Objects.nonNull(calculatedTerminationDate)) {
            LocalDate now = LocalDate.now();
            if (!calculatedTerminationDate.isBefore(now)) {
                return;
            }

            productContract.setTerminationDate(now.minusDays(1));
            productContract.setContractStatus(ContractDetailsStatus.TERMINATED);
            productContract.setSubStatus(ContractDetailsSubStatus.CONTRACT_IS_NOT_ACTIVE);

            productContractRepository.saveAndFlush(productContract);
            List<Long> customerIdsToChangeStatusWithContractId = productContractDetailsRepository.getCustomerIdsToChangeStatusWithContractId(productContract.getId());
            customerStatusChangeEventPublisher.publishCustomerStatusChangeEvent(customerIdsToChangeStatusWithContractId);

        }
    }

    private LocalDate calculateTerminationDate(ProductContract productContract, Terms terms) {
        LocalDate calculatedTerminationDate;

        LocalDate entryIntoForceDate = productContract.getEntryIntoForceDate();
        Integer contractDeliveryActivationValue = terms.getContractDeliveryActivationValue();
        ContractDeliveryActivationType contractDeliveryActivationType = terms.getContractDeliveryActivationType();
        switch (contractDeliveryActivationType) {
            case DAY -> calculatedTerminationDate = entryIntoForceDate.plusDays(contractDeliveryActivationValue);
            case WEEK -> calculatedTerminationDate = entryIntoForceDate.plusWeeks(contractDeliveryActivationValue);
            case MONTH -> calculatedTerminationDate = entryIntoForceDate.plusMonths(contractDeliveryActivationValue);
            default -> {
                log.error("Illegal argument provided for Contract Delivery Activation Type: [%s]".formatted(contractDeliveryActivationType));
                throw new IllegalArgumentException("Illegal argument provided for Contract Delivery Activation Type: [%s]".formatted(contractDeliveryActivationType));
            }
        }

        return calculatedTerminationDate;
    }
}
