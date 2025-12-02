package bg.energo.phoenix.service.contract.billing;

import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.IllegalArgumentsProvidedException;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.contract.billing.ContractBillingGroup;
import bg.energo.phoenix.model.entity.customer.CustomerDetails;
import bg.energo.phoenix.model.entity.customer.communication.CustomerCommContactPurposes;
import bg.energo.phoenix.model.entity.customer.communication.CustomerCommunications;
import bg.energo.phoenix.model.entity.nomenclature.customer.Bank;
import bg.energo.phoenix.model.enums.contract.billing.BillingGroupSendingInvoice;
import bg.energo.phoenix.model.enums.contract.products.ProductContractStatus;
import bg.energo.phoenix.model.enums.customer.CustomerStatus;
import bg.energo.phoenix.model.enums.customer.Status;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.request.contract.billing.BillingGroupRequest;
import bg.energo.phoenix.model.request.receivable.payment.ContractBillingGroupForPaymentListingRequest;
import bg.energo.phoenix.model.response.contract.biling.*;
import bg.energo.phoenix.model.response.nomenclature.customer.BankResponse;
import bg.energo.phoenix.model.response.receivable.payment.ContractBillingGroupsForPaymentResponse;
import bg.energo.phoenix.repository.contract.billing.ContractBillingGroupRepository;
import bg.energo.phoenix.repository.contract.billing.ContractPodRepository;
import bg.energo.phoenix.repository.contract.product.ProductContractRepository;
import bg.energo.phoenix.repository.customer.CustomerDetailsRepository;
import bg.energo.phoenix.repository.customer.communicationData.CustomerCommContactPurposesRepository;
import bg.energo.phoenix.repository.customer.communicationData.CustomerCommunicationsRepository;
import bg.energo.phoenix.repository.nomenclature.customer.BankRepository;
import bg.energo.phoenix.util.contract.CommunicationContactPurposeProperties;
import bg.energo.phoenix.util.epb.EPBChainedExceptionTriggerUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@RequiredArgsConstructor
@Service
@Slf4j
public class BillingGroupService {

    private final ContractBillingGroupRepository billingGroupRepository;
    private final ProductContractRepository productContractRepository;
    private final BankRepository bankRepository;
    private final CustomerDetailsRepository customerDetailsRepository;
    private final CustomerCommunicationsRepository customerCommunicationsRepository;
    private final CustomerCommContactPurposesRepository contactPurposesRepository;
    private final CommunicationContactPurposeProperties communicationContactPurposeProperties;
    private final ContractPodRepository contractPodRepository;


    /**
     * method will create initial billing group.
     */
    @Transactional
    public BillingGroupShortResponse createInitial(Long contractId) {

        BillingGroupRequest request = new BillingGroupRequest();
        request.setContractId(contractId);
        request.setSendingInvoice(BillingGroupSendingInvoice.EMAIL);
        return create(request);
    }

    @Transactional
    public BillingGroupShortResponse create(BillingGroupRequest request) {
        List<String> messages = new ArrayList<>();
        validateBillingGroupCreateRequest(request, messages);

        List<BillingGroupNumberWrapper> number = billingGroupRepository.findAllBillingGroupNumberAvailableInPod(request.getContractId());


        String availableNumber = validateGroupNumber(request, messages, number);
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(messages, log);

        ContractBillingGroup contractBillingGroup = new ContractBillingGroup();
        contractBillingGroup.setGroupNumber(availableNumber);
        contractBillingGroup.setContractId(request.getContractId());
        fillContractBilling(request, contractBillingGroup);
        contractBillingGroup.setStatus(EntityStatus.ACTIVE);
        return new BillingGroupShortResponse(billingGroupRepository.save(contractBillingGroup));
    }

    @Transactional
    public BillingGroupShortResponse update(Long id, BillingGroupRequest request) {
        List<String> messages = new ArrayList<>();
        ContractBillingGroup contractBillingGroup = billingGroupRepository.findByIdAndStatusIn(id, List.of(EntityStatus.ACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Billing group not found!;"));

        validateBillingGroupUpdateRequest(request, contractBillingGroup, messages);

        List<BillingGroupNumberWrapper> number = billingGroupRepository.findAllBillingGroupNumberAvailableInPod(request.getContractId());


        if (!Objects.equals(request.getGroupNumber(), contractBillingGroup.getGroupNumber())) {
            contractBillingGroup.setGroupNumber(validateGroupNumber(request, messages, number));
        }
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(messages, log);
        fillContractBilling(request, contractBillingGroup);
        return new BillingGroupShortResponse(billingGroupRepository.save(contractBillingGroup));
    }


    public BillingGroupResponse view(Long id) {
        ContractBillingGroup contractBillingGroup = billingGroupRepository.findByIdAndStatusIn(id, List.of(EntityStatus.ACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Billing Group not found!;"));

        BillingGroupResponse billingGroupResponse = new BillingGroupResponse(contractBillingGroup);
        if (contractBillingGroup.getBankId() != null) {
            Bank bank = bankRepository.findByIdAndStatus(contractBillingGroup.getBankId(), List.of(NomenclatureItemStatus.ACTIVE, NomenclatureItemStatus.INACTIVE))
                    .orElseThrow(() -> new DomainEntityNotFoundException("bankId-Invalid data in database bank not found!;"));
            billingGroupResponse.setBankResponse(new BankResponse(bank));
        }
        if (contractBillingGroup.getBillingCustomerCommunicationId() != null) {
            CustomerCommunications customerCommunications = customerCommunicationsRepository.findByIdAndStatuses(contractBillingGroup.getBillingCustomerCommunicationId(), List.of(Status.ACTIVE))
                    .orElseThrow(() -> new DomainEntityNotFoundException("id-Invalid data customer communicatin not found!;"));
            billingGroupResponse.setCommunicationName(customerCommunications.getContactTypeName());
        }
        if (contractBillingGroup.getAlternativeRecipientCustomerDetailId() != null) {
            CustomerDetails customerDetails = customerDetailsRepository.findById(contractBillingGroup.getAlternativeRecipientCustomerDetailId())
                    .orElseThrow(() -> new DomainEntityNotFoundException("id-Invalid data customer not found!;"));
            billingGroupResponse.setAlternativeCustomerName(createUserName(customerDetails));
            billingGroupResponse.setCustomerId(customerDetails.getCustomerId());
            billingGroupResponse.setCustomerVersionId(customerDetails.getVersionId());
        }
        return billingGroupResponse;
    }

    @Transactional
    public void delete(Long id) {
        ContractBillingGroup billingGroup = billingGroupRepository.findByIdAndStatusIn(id, List.of(EntityStatus.ACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Billing group do not exist!"));

        boolean podsExist = contractPodRepository.existsByBillingGroupIdAndStatusIn(billingGroup.getId(), List.of(EntityStatus.ACTIVE));
        if (podsExist) {
            throw new IllegalArgumentsProvidedException("id-You can’t delete billing group because it contains active PODs;");
        }
        billingGroup.setStatus(EntityStatus.DELETED);
        billingGroupRepository.save(billingGroup);
    }

    public List<BillingGroupListingResponse> findContractBillingGroups(Long contractId) {
        return billingGroupRepository.findAllByContractId(contractId, List.of(EntityStatus.ACTIVE));
    }

    public List<ContractBillingGroupResponse> findByCustomerId(Long customerId) {
        return billingGroupRepository.findAllByCustomerId(customerId);
    }

    public Page<ContractBillingGroupsForPaymentResponse> findAllByCustomerIdForPayment(ContractBillingGroupForPaymentListingRequest contractBillingGroupForPaymentListingRequest) {
        return billingGroupRepository.findAllByCustomerIdForPayment(
                contractBillingGroupForPaymentListingRequest.getCustomerId(),
                contractBillingGroupForPaymentListingRequest.getPrompt(),
                PageRequest.of(
                        contractBillingGroupForPaymentListingRequest.getPage(),
                        contractBillingGroupForPaymentListingRequest.getSize()
                )
        ).map(ContractBillingGroupsForPaymentResponse::new);
    }

    public BillingGroupListingResponse getDefaultForContract(Long contractId) {
        List<BillingGroupListingResponse> billingGroupsToCheck = billingGroupRepository.findAllByContractId(contractId, List.of(EntityStatus.ACTIVE));
        if (billingGroupsToCheck == null || billingGroupsToCheck.isEmpty())
            return null;
//        return billingGroupsToCheck.stream()
//                .min(Comparator.comparing(BillingGroupListingResponse::getGroupNumber)
//                        .thenComparing(BillingGroupListingResponse::getId))
//                .orElse(null);
        return billingGroupsToCheck.stream()
                .min(Comparator.comparing(BillingGroupListingResponse::getId))
                .orElse(null);
    }

    public Optional<ContractBillingGroup> findByContractIdGroupNumber(Long contractId, String billingGroupNumber) {
        return billingGroupRepository.findByContractIdAndGroupNumberAndStatusIn(contractId, billingGroupNumber, List.of(EntityStatus.ACTIVE));
    }

    public Optional<ContractBillingGroup> findByContractIdAndId(Long contractId, Long billingGroupId) {
        return billingGroupRepository.findByContractIdAndIdAndStatusIn(contractId, billingGroupId, List.of(EntityStatus.ACTIVE));
    }

    private String createUserName(CustomerDetails details) {

        StringBuilder stringBuilder = new StringBuilder();
        if (details.getName() != null) {
            stringBuilder.append(details.getName());
        }
        if (details.getMiddleName() != null) {
            stringBuilder.append(" ");
            stringBuilder.append(details.getMiddleName());
        }
        if (details.getLastName() != null) {
            stringBuilder.append(" ");
            stringBuilder.append(details.getLastName());
        }

        return stringBuilder.toString();
    }

    /**
     * will validate update request;
     */
    private void validateBillingGroupUpdateRequest(BillingGroupRequest request, ContractBillingGroup contractBillingGroup, List<String> messages) {

        if (!Objects.equals(request.getContractId(), contractBillingGroup.getContractId()) && request.getContractId() != null) {
            messages.add("contractId-You can not change contract!;");
        }
        if (!Objects.equals(request.getBillingCustomerCommunicationId(), contractBillingGroup.getBillingCustomerCommunicationId())) {
            validateCommunicationId(request, messages);
        }
        if (!Objects.equals(request.getBankId(), contractBillingGroup.getBankId())) {
            validateBank(request, messages);
        }
        if (!Objects.equals(request.getAlternativeRecipientCustomerDetailId(), contractBillingGroup.getAlternativeRecipientCustomerDetailId())) {
            validateCustomer(request, messages);
        }
    }

    /**
     * Method will fill entity with request information after validations are passed.
     */
    private void fillContractBilling(BillingGroupRequest request, ContractBillingGroup contractBillingGroup) {

        contractBillingGroup.setIban(request.getIban());
        contractBillingGroup.setBankId(request.getBankId());
        contractBillingGroup.setBillingCustomerCommunicationId(request.getBillingCustomerCommunicationId());
        contractBillingGroup.setAlternativeRecipientCustomerDetailId(request.getAlternativeRecipientCustomerDetailId());
        contractBillingGroup.setContractId(request.getContractId());
        contractBillingGroup.setDirectDebit(request.isDirectDebit());
        contractBillingGroup.setSendingInvoice(request.getSendingInvoice());
        contractBillingGroup.setSeparateInvoiceForEachPod(request.isSeparateInvoiceForEachPod());
    }

    /**
     * will decide which method to use for number validation and return result in 'XXXX' format.
     */
    private String validateGroupNumber(BillingGroupRequest request, List<String> messages, List<BillingGroupNumberWrapper> number) {
        String availableNumber;
        if (request.getGroupNumber() == null) {
            Integer lowestAvailableNumber = findLowestAvailableNumber(number.stream().distinct().toList());
            if (lowestAvailableNumber > 9999) {
                messages.add("groupNumber-Group number can not exceed 9999!;");
            }
            availableNumber = String.valueOf(lowestAvailableNumber);
        } else {
            if (findNumberExists(number, Integer.parseInt(request.getGroupNumber()))) {
                messages.add("groupNumber-Customer already has this group number!;");
            }
            availableNumber = request.getGroupNumber();
        }
        return "0".repeat(4 - availableNumber.length()) + availableNumber;
    }


    /**
     * will validate create request fields.
     */
    private void validateBillingGroupCreateRequest(BillingGroupRequest request, List<String> messages) {
        if (!productContractRepository.existsByIdAndStatusIn(request.getContractId(), List.of(ProductContractStatus.ACTIVE))) {
            messages.add("contractId-Contract do not exists");
        }
        validateBank(request, messages);
        validateCustomer(request, messages);
        validateCommunicationId(request, messages);
    }

    /**
     * general field validation.
     */
    private void validateBank(BillingGroupRequest request, List<String> messages) {
        if (request.getBankId() != null && !bankRepository.existsByIdAndStatusIn(request.getBankId(), List.of(NomenclatureItemStatus.ACTIVE))) {
            messages.add("bankId-Bank do not exist!;");
        }
    }

    /**
     * general field validation.
     */
    private void validateCustomer(BillingGroupRequest request, List<String> messages) {
        if (request.getAlternativeRecipientCustomerDetailId() != null && !customerDetailsRepository.existsByDetailIdAndCustomerStatus(request.getAlternativeRecipientCustomerDetailId(), List.of(CustomerStatus.ACTIVE))) {
            messages.add("alternativeRecipientCustomerDetailId-Customer do not exist!;");
        }
    }

    /**
     * general field validation.
     */
    private void validateCommunicationId(BillingGroupRequest request, List<String> messages) {
        if (request.getBillingCustomerCommunicationId() != null && !customerDetailsRepository.existsByDetailIdAndCustomerStatus(request.getAlternativeRecipientCustomerDetailId(), List.of(CustomerStatus.ACTIVE))) {
            messages.add("alternativeRecipientCustomerDetailId-Customer do not exist!;");
        }


        boolean communicationsExists = customerCommunicationsRepository.existsByIdAndStatusIn(request.getBillingCustomerCommunicationId(), List.of(Status.ACTIVE));
        if (request.getBillingCustomerCommunicationId() != null && !communicationsExists) {
            messages.add("billingCustomerCommunicationId-Customer do not exist!;");
        } else if (communicationsExists) {
            boolean billingExist = false;
            List<CustomerCommContactPurposes> commContactPurposes = contactPurposesRepository.findByCustomerCommId(request.getBillingCustomerCommunicationId(), List.of(Status.ACTIVE));
            for (CustomerCommContactPurposes commContactPurpose : commContactPurposes) {
                if (communicationContactPurposeProperties.getBillingCommunicationId().equals(commContactPurpose.getContactPurposeId())) {
                    billingExist = true;
                    break;
                }
            }
            if (!billingExist) {
                messages.add("billingCustomerCommunicationId-Communication data is not for billing!;");
            }
        }
    }

    /**
     * Given sorted used groupNumbers method will find lowest using binary search.
     */
    private Integer findLowestAvailableNumber(List<BillingGroupNumberWrapper> number) {
        int finalValue;

        int start = 0;
        int end = number.size() - 1;
        while (true) {
            if (start > end) {
                finalValue = end + 1;
                break;
            }
            if (start != number.get(start).getNumber()) {
                finalValue = start;
                break;
            }
            int mid = (start + end) / 2;
            if (number.get(mid).getNumber() == mid) {
                start = mid + 1;
                continue;
            }
            end = mid;
        }
        return finalValue;
    }

    /**
     * Given sorted used groupNumbers method will find given number using binary search.
     */
    private boolean findNumberExists(List<BillingGroupNumberWrapper> number, int x) {
        int start = 0;
        int end = number.size() - 1;
        while (start <= end) {
            int middle = start + (end - start) / 2;

            if (number.get(middle).getNumber() == x)
                return true;

            if (number.get(middle).getNumber() < x)
                start = middle + 1;
            else
                end = middle - 1;
        }

        return false;
    }


}
