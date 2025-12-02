package bg.energo.phoenix.service.contract.product.termination;

import bg.energo.phoenix.model.entity.contract.product.ProductContract;
import bg.energo.phoenix.model.entity.contract.product.ProductContractDetails;
import bg.energo.phoenix.model.entity.contract.product.ProductTermRenewalCount;
import bg.energo.phoenix.model.entity.product.termination.terminations.Termination;
import bg.energo.phoenix.model.enums.contract.products.ContractDetailsStatus;
import bg.energo.phoenix.model.enums.contract.products.ContractDetailsSubStatus;
import bg.energo.phoenix.model.enums.contract.products.ProductContractStatus;
import bg.energo.phoenix.model.enums.product.termination.terminations.AutoTerminationFrom;
import bg.energo.phoenix.model.response.contract.productContract.terminations.ProductContractTerminationGenericModel;
import bg.energo.phoenix.model.response.contract.productContract.terminations.ProductContractTerminationProperties;
import bg.energo.phoenix.model.response.contract.productContract.terminations.ProductContractTerminationWithContractTermsResponse;
import bg.energo.phoenix.repository.contract.product.ProductContractDetailsRepository;
import bg.energo.phoenix.repository.contract.product.ProductContractRepository;
import bg.energo.phoenix.repository.contract.product.ProductTermRenewalCountRepository;
import bg.energo.phoenix.repository.product.termination.terminations.TerminationRepository;
import bg.energo.phoenix.service.crm.emailCommunication.EmailCommunicationService;
import bg.energo.phoenix.service.customer.statusChangeEvent.CustomerStatusChangeEventPublisher;
import bg.energo.phoenix.service.product.termination.terminations.ContractTerminationEmailSenderService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@Order(2)
@RequiredArgsConstructor
public class ProductContractTermTerminationService implements ProductContractTerminator {

    private final ProductContractRepository productContractRepository;
    private final ProductContractDetailsRepository productContractDetailsRepository;
    private final TerminationRepository terminationRepository;
    private final ProductTermRenewalCountRepository termRenewalCountRepository;
    private final CustomerStatusChangeEventPublisher customerStatusChangeEventPublisher;
    private final EmailCommunicationService emailCommunicationService;
    private final ContractTerminationEmailSenderService contractTerminationEmailSenderService;

    @Value("${product-contract.termination-with-expiration-term.number-of-threads}")
    private Integer numberOfThreads;

    @Value("${product-contract.termination-with-expiration-term.batch-size}")
    private Integer batchSize;

    @Value("${product-contract.termination-with-expiration-term.query-batch-size}")
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
                .getProductContractsForTermDeactivation(PageRequest.of(page, size))
                .map(ProductContractTerminationGenericModel::new).toList();
    }


    @Override
    @Transactional
    public void terminate(ProductContractTerminationGenericModel model) {
        ProductContractTerminationWithContractTermsResponse terminationDto = model.getTermsResponses();
        Long contractId = terminationDto.getContractId();
        Long detailId = terminationDto.getDetailId();
        Optional<ProductContract> productContractOptional = productContractRepository.findByIdAndStatusIn(contractId, List.of(ProductContractStatus.ACTIVE));
        if (productContractOptional.isEmpty()) {
            return;
        }
        ProductContract productContract = productContractOptional.get();
        Optional<ProductContractDetails> contractDetailsOptional = productContractDetailsRepository.findById(detailId);
        if (contractDetailsOptional.isEmpty()) {
            return;
        }
        ProductContractDetails productContractDetails = contractDetailsOptional.get();

        if (productContract.getContractTermEndDate() == null || !productContract.getContractTermEndDate().isBefore(LocalDate.now())) {
            return;
        }
        if (terminationDto.getAutomaticRenewal() == null || terminationDto.getAutomaticRenewal().equals(Boolean.FALSE)) {
            checkPerpetuity(productContract, productContractDetails, terminationDto);
        } else {
            doRenewals(productContract, productContractDetails, terminationDto);
        }


    }

    private void doRenewals(ProductContract productContract, ProductContractDetails productContractDetails, ProductContractTerminationWithContractTermsResponse terminationDto) {
        if (terminationDto.getNumberOfRenewals() == null || terminationDto.getNumberOfRenewals() < 0) {
            renewContract(productContract, terminationDto);
        } else {
            checkIfRenewalPeriodReached(productContract, productContractDetails, terminationDto);
        }
    }

    private void checkIfRenewalPeriodReached(ProductContract productContract, ProductContractDetails productContractDetails, ProductContractTerminationWithContractTermsResponse terminationDto) {
        Integer numberOfRenewals = terminationDto.getNumberOfRenewals();
        Optional<ProductTermRenewalCount> renewalCountOptional = termRenewalCountRepository.findByTermIdAndContractDetailId(terminationDto.getTermId(), productContractDetails.getId());
        if (renewalCountOptional.isEmpty()) {
            termRenewalCountRepository.save(new ProductTermRenewalCount(null, terminationDto.getTermId(), productContractDetails.getId(), 1));
            renewContract(productContract, terminationDto);
            return;
        } else {
            ProductTermRenewalCount productTermRenewalCount = renewalCountOptional.get();
            if (numberOfRenewals > productTermRenewalCount.getRenewalCount()) {
                renewContract(productContract, terminationDto);
                productTermRenewalCount.setRenewalCount(productTermRenewalCount.getRenewalCount() + 1);
                termRenewalCountRepository.save(productTermRenewalCount);
                return;
            }
        }
        checkPerpetuity(productContract, productContractDetails, terminationDto);
    }

    private void renewContract(ProductContract productContract, ProductContractTerminationWithContractTermsResponse terminationDto) {
        productContract.setContractTermEndDate(productContract.getContractTermEndDate().plus(terminationDto.getRenewalValue(), terminationDto.getRenewalPeriodType().getUnit()));
        productContractRepository.saveAndFlush(productContract);
    }

    private void checkPerpetuity(ProductContract productContract, ProductContractDetails productContractDetails, ProductContractTerminationWithContractTermsResponse terminationDto) {
        if (terminationDto.getPerpetuityCause() == null || terminationDto.getPerpetuityCause().equals(Boolean.FALSE)) {
            checkTerminationObject(productContract, productContractDetails, terminationDto);
        } else {
            productContract.setPerpetuityDate(productContract.getContractTermEndDate().plusDays(1));
            productContract.setContractTermEndDate(LocalDate.of(2090, 12, 31));
            productContract.setContractStatus(ContractDetailsStatus.ACTIVE_IN_PERPETUITY);
            productContract.setSubStatus(ContractDetailsSubStatus.DELIVERY);
            productContractRepository.saveAndFlush(productContract);
            List<Long> customerIdsToChangeStatusWithContractId = productContractDetailsRepository.getCustomerIdsToChangeStatusWithContractId(productContract.getId());
            customerStatusChangeEventPublisher.publishCustomerStatusChangeEvent(customerIdsToChangeStatusWithContractId);
        }
    }

    private void checkTerminationObject(ProductContract productContract, ProductContractDetails productContractDetails, ProductContractTerminationWithContractTermsResponse terminationDto) {
        if (terminationDto.getTerminationId() == null) {
            return;
        }
        Optional<Termination> terminations = terminationRepository.findById(terminationDto.getTerminationId());
        if (terminations.isEmpty()) {
            return;
        }
        Termination termination = terminations.get();
        if (termination.getAutoTerminationFrom().equals(AutoTerminationFrom.EVENT_DATE)) {
            productContract.setTerminationDate(productContract.getContractTermEndDate());
            productContract.setContractStatus(ContractDetailsStatus.TERMINATED);
            productContract.setSubStatus(ContractDetailsSubStatus.EXPIRED);
        } else if (termination.getAutoTerminationFrom().equals(AutoTerminationFrom.FIRST_DAY_OF_MONTH_FOLLOWING_EVENT_DATE)) {
            LocalDate contractTermEndDate = productContract.getContractTermEndDate();
            LocalDate nextMonthDate = contractTermEndDate.plusMonths(1);
            LocalDate terminationDate = LocalDate.of(nextMonthDate.getYear(), nextMonthDate.getMonth(), 1);
            if (!terminationDate.isBefore(LocalDate.now())) {
                return;
            }
            productContract.setTerminationDate(productContract.getContractTermEndDate());
            productContract.setContractStatus(ContractDetailsStatus.TERMINATED);
            productContract.setSubStatus(ContractDetailsSubStatus.EXPIRED);
        }
        productContractRepository.saveAndFlush(productContract);
        List<Long> customerIdsToChangeStatusWithContractId = productContractDetailsRepository.getCustomerIdsToChangeStatusWithContractId(productContract.getId());
        customerStatusChangeEventPublisher.publishCustomerStatusChangeEvent(customerIdsToChangeStatusWithContractId);

        Optional<ProductContractDetails> respectiveProductContractDetailOptional = productContractDetailsRepository.findRespectiveProductContractDetailsByProductContractId(LocalDate.now(), productContract.getId());
        if (respectiveProductContractDetailOptional.isPresent() && Boolean.TRUE.equals(termination.getNoticeDue())) {
            ProductContractDetails respectiveProductContract = respectiveProductContractDetailOptional.get();
            contractTerminationEmailSenderService.createAndSendTerminationEmail(respectiveProductContract.getId(),
                    true,
                    respectiveProductContract.getCustomerDetailId(),
                    respectiveProductContract.getCustomerCommunicationIdForContract(),
                    termination.getId());

        }
    }

}
