package bg.energo.phoenix.service.billing.billingRun;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.billing.billingRun.BillingRun;
import bg.energo.phoenix.model.entity.contract.order.goods.GoodsOrder;
import bg.energo.phoenix.model.entity.contract.order.service.ServiceOrder;
import bg.energo.phoenix.model.entity.contract.product.ProductContractDetails;
import bg.energo.phoenix.model.entity.contract.service.ServiceContractDetails;
import bg.energo.phoenix.model.entity.customer.communication.CustomerCommContactPurposes;
import bg.energo.phoenix.model.entity.customer.communication.CustomerCommunications;
import bg.energo.phoenix.model.enums.billing.billings.ContractOrderType;
import bg.energo.phoenix.model.enums.contract.products.ProductContractStatus;
import bg.energo.phoenix.model.enums.customer.Status;
import bg.energo.phoenix.repository.contract.order.goods.GoodsOrderRepository;
import bg.energo.phoenix.repository.contract.order.service.ServiceOrderRepository;
import bg.energo.phoenix.repository.contract.product.ProductContractDetailsRepository;
import bg.energo.phoenix.repository.contract.product.ProductContractRepository;
import bg.energo.phoenix.repository.contract.service.ServiceContractDetailsRepository;
import bg.energo.phoenix.repository.contract.service.ServiceContractsRepository;
import bg.energo.phoenix.repository.customer.communicationData.CustomerCommContactPurposesRepository;
import bg.energo.phoenix.repository.customer.communicationData.CustomerCommunicationContactsRepository;
import bg.energo.phoenix.repository.customer.communicationData.CustomerCommunicationsRepository;
import bg.energo.phoenix.util.contract.CommunicationContactPurposeProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static bg.energo.phoenix.util.communication.CommunicationDataUtils.checkCommunicationEmailAndNumber;
@Service
@RequiredArgsConstructor
public class BillingRunCommonService {
    private final GoodsOrderRepository goodsOrderRepository;

    private final ServiceOrderRepository serviceOrderRepository;

    private final CustomerCommunicationsRepository communicationsRepository;
    private final CustomerCommContactPurposesRepository commContactPurposesRepository;

    private final CommunicationContactPurposeProperties communicationContactPurposeProperties;
    private final ServiceContractDetailsRepository serviceContractDetailsRepository;
    private final ProductContractDetailsRepository productContractDetailsRepository;
    private final CustomerCommunicationContactsRepository communicationContactsRepository;
    private final ProductContractRepository productContractRepository;
    private final ServiceContractsRepository serviceContractsRepository;

    public void checkInvoiceCommunicationData(Long invoiceCommunicationDataId, Long contractOrderId, Long customerDetailsId, ContractOrderType contractOrderType, BillingRun billingRun, List<String> errorMassages, String messagePrefix) {
        if (contractOrderId == null || contractOrderType==null) {
            Optional<CustomerCommunications> contractCommunicationsOptional = communicationsRepository.findByIdAndStatuses(invoiceCommunicationDataId, List.of(Status.ACTIVE));
            if (contractCommunicationsOptional.isEmpty()) {
                errorMassages.add("%s.invoiceCommunicationDataId-Communication data for contract not found!;".formatted(messagePrefix));
                return;
            }
            CustomerCommunications customerCommunications = contractCommunicationsOptional.get();
            if (!customerCommunications.getCustomerDetailsId().equals(customerDetailsId)) {
                errorMassages.add("%s.invoiceCommunicationDataId-Communication data for customer details with id %s not found;".formatted(messagePrefix,customerDetailsId));
                return;
            }
            List<CustomerCommContactPurposes> contactPurposes = commContactPurposesRepository.findByCustomerCommId(customerCommunications.getId(), List.of(Status.ACTIVE));
            Long contractCommunicationId = communicationContactPurposeProperties.getBillingCommunicationId();
            boolean contains = false;
            for (CustomerCommContactPurposes contactPurpose : contactPurposes) {
                if (contactPurpose.getContactPurposeId().equals(contractCommunicationId)) {
                    contains = true;
                    billingRun.setCustomerCommunicationId(customerCommunications.getId());
                    break;
                }
            }
            if (!contains) {
                errorMassages.add("%s.invoiceCommunicationDataId-communications data is invalid;".formatted(messagePrefix));
            }
        } else {
            switch (contractOrderType) {
                case GOODS_ORDER -> {
                    Optional<GoodsOrder> goodsOrder = goodsOrderRepository.findByIdAndStatusIn(contractOrderId, List.of(EntityStatus.ACTIVE));
                    if (goodsOrder.isPresent()) {
                        if (!Objects.equals(goodsOrder.get().getCustomerCommunicationIdForBilling(), invoiceCommunicationDataId)) {
                            errorMassages.add("%s.invoiceCommunicationDataId-communications data is invalid;".formatted(messagePrefix));
                        } else {
                            billingRun.setCustomerCommunicationId(invoiceCommunicationDataId);
                        }
                    } else {
                        errorMassages.add("%s.contractOrderId-[contractOrderId] goods order not found".formatted(messagePrefix));
                    }
                }
                case SERVICE_ORDER -> {
                    Optional<ServiceOrder> serviceOrder = serviceOrderRepository.findByIdAndStatusIn(contractOrderId, List.of(EntityStatus.ACTIVE));
                    if (serviceOrder.isPresent()) {
                        if (!Objects.equals(serviceOrder.get().getCustomerCommunicationIdForBilling(), invoiceCommunicationDataId)) {
                            errorMassages.add("%s.invoiceCommunicationDataId-communications data is invalid;".formatted(messagePrefix));
                        } else {
                            billingRun.setCustomerCommunicationId(invoiceCommunicationDataId);
                        }
                    } else {
                        errorMassages.add("%s.contractOrderId-[contractOrderId] service order not found".formatted(messagePrefix));
                    }
                }
                case PRODUCT_CONTRACT -> {
                    if (productContractRepository.existsByIdAndStatusIn(contractOrderId, List.of(ProductContractStatus.ACTIVE))) {
                        Optional<ProductContractDetails> productContractDetailsOptional = productContractDetailsRepository.findLatestDetailByContractIdAndByCustomerDetailsId(contractOrderId, customerDetailsId);
                        if (productContractDetailsOptional.isPresent()) {
                            if (!Objects.equals(productContractDetailsOptional.get().getCustomerCommunicationIdForBilling(), invoiceCommunicationDataId)) {
                                errorMassages.add("%s.invoiceCommunicationDataId-communications data is invalid;".formatted(messagePrefix));
                            } else {
                                billingRun.setCustomerCommunicationId(invoiceCommunicationDataId);
                            }
                        } else {
                            errorMassages.add("%s.contractOrderId-[contractOrderId] product contract details not found by given contract id %s, and customer details id %s;".formatted(messagePrefix, contractOrderId, customerDetailsId));
                        }
                    } else {
                        errorMassages.add("%s.contractOrderId-[contractOrderId] product contract not found".formatted(messagePrefix));
                    }
                }
                case SERVICE_CONTRACT -> {
                    if (serviceContractsRepository.existsByIdAndStatusIn(contractOrderId, List.of(EntityStatus.ACTIVE))) {
                        Optional<ServiceContractDetails> serviceContractDetailsOptional = serviceContractDetailsRepository.findByContractIdAndLatestDetailByCustomerDetailsId(contractOrderId, customerDetailsId);
                        if (serviceContractDetailsOptional.isPresent()) {
                            if (!Objects.equals(serviceContractDetailsOptional.get().getCustomerCommunicationIdForBilling(), invoiceCommunicationDataId)) {
                                errorMassages.add("%s.invoiceCommunicationDataId-communications data is invalid;".formatted(messagePrefix));
                            } else {
                                billingRun.setCustomerCommunicationId(invoiceCommunicationDataId);
                            }
                        } else {
                            errorMassages.add("%s.contractOrderId-[contractOrderId] service contract details not found by given contract id %s, and customer details id %s;".formatted(messagePrefix,contractOrderId, customerDetailsId));
                        }
                    } else {
                        errorMassages.add("%s.contractOrderId-[contractOrderId] service contract not found".formatted(messagePrefix));
                    }
                }
            }
        }
        checkForEmailAndNumber(invoiceCommunicationDataId, errorMassages, "%s.invoiceCommunicationDataId- communication must have Email and Mobile number contact types;".formatted(messagePrefix));
    }

    private void checkForEmailAndNumber(Long communicationDataId, List<String> messages, String message) {
        checkCommunicationEmailAndNumber(communicationDataId, messages, message, communicationContactsRepository);
    }
}
