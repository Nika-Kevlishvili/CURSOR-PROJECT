package bg.energo.phoenix.service.billing.billingRun.manualInterimAndAdvancePayment;

import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.billing.billingRun.BillingRun;
import bg.energo.phoenix.model.entity.billing.billingRun.BillingRunBillingGroup;
import bg.energo.phoenix.model.entity.customer.Customer;
import bg.energo.phoenix.model.entity.customer.CustomerDetails;
import bg.energo.phoenix.model.entity.nomenclature.product.Currency;
import bg.energo.phoenix.model.enums.contract.ContractType;
import bg.energo.phoenix.model.enums.contract.InterestRate.InterestRateStatus;
import bg.energo.phoenix.model.enums.contract.products.ProductContractStatus;
import bg.energo.phoenix.model.enums.customer.CustomerStatus;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.response.billing.billingRun.ManualInterimAndAdvancePaymentParametersResponse;
import bg.energo.phoenix.model.response.billing.billingRun.manualInvoice.ContractOrderShortResponse;
import bg.energo.phoenix.model.response.contract.InterestRate.InterestRateShortResponse;
import bg.energo.phoenix.model.response.contract.biling.BillingGroupListingResponse;
import bg.energo.phoenix.model.response.customer.CustomerDetailsShortResponse;
import bg.energo.phoenix.model.response.nomenclature.customer.BankResponse;
import bg.energo.phoenix.model.response.nomenclature.product.VatRateResponse;
import bg.energo.phoenix.model.response.shared.ShortResponse;
import bg.energo.phoenix.repository.billing.billingRun.BillingRunBillingGroupRepository;
import bg.energo.phoenix.repository.contract.billing.ContractBillingGroupRepository;
import bg.energo.phoenix.repository.contract.product.ProductContractRepository;
import bg.energo.phoenix.repository.contract.service.ServiceContractsRepository;
import bg.energo.phoenix.repository.customer.CustomerDetailsRepository;
import bg.energo.phoenix.repository.customer.CustomerRepository;
import bg.energo.phoenix.repository.interestRate.InterestRateRepository;
import bg.energo.phoenix.repository.nomenclature.customer.BankRepository;
import bg.energo.phoenix.repository.nomenclature.product.CurrencyRepository;
import bg.energo.phoenix.repository.nomenclature.product.VatRateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class ManualInterimAndAdvancePaymentMapperService {

    private final CustomerDetailsRepository customerDetailsRepository;
    private final CustomerRepository customerRepository;
    private final InterestRateRepository interestRateRepository;
    private final VatRateRepository vatRateRepository;
    private final BankRepository bankRepository;
    private final ProductContractRepository productContractRepository;
    private final ServiceContractsRepository serviceContractsRepository;
    private final BillingRunBillingGroupRepository billingRunBillingGroupRepository;
    private final ContractBillingGroupRepository billingGroupRepository;
    private final CurrencyRepository currencyRepository;

    public ManualInterimAndAdvancePaymentParametersResponse mapInterimAndAdvancePaymentParameters(BillingRun billingRun) {
        ManualInterimAndAdvancePaymentParametersResponse manualInterimAndAdvancePaymentParametersResponse = new ManualInterimAndAdvancePaymentParametersResponse();
        manualInterimAndAdvancePaymentParametersResponse.setNumberOfIncomeAccount(billingRun.getNumberOfIncomeAccount());
        manualInterimAndAdvancePaymentParametersResponse.setNumberOfIncomeAccountManual(billingRun.getNumberOfIncomeAccount() != null);
        manualInterimAndAdvancePaymentParametersResponse.setCostCenterControllingOrder(billingRun.getCostCenterControllingOrder());
        manualInterimAndAdvancePaymentParametersResponse.setCostCenterControllingOrderManual(billingRun.getCostCenterControllingOrder() != null);
        manualInterimAndAdvancePaymentParametersResponse.setAmountExcludingVat(billingRun.getAmountExcludingVat());
        manualInterimAndAdvancePaymentParametersResponse.setIssuingForTheMonthToCurrent(billingRun.getIssuingForTheMonthToCurrent());
        manualInterimAndAdvancePaymentParametersResponse.setIssuedSeparateInvoices(billingRun.getIssuedSeparateInvoices());
        manualInterimAndAdvancePaymentParametersResponse.setCurrencyResponse(getCurrencyResponse(billingRun.getCurrencyId()));
        manualInterimAndAdvancePaymentParametersResponse.setDeductionFrom(billingRun.getDeductionFrom());

        if (billingRun.getVatRateId() != null || (billingRun.getGlobalVatRate() != null && billingRun.getGlobalVatRate().equals(Boolean.TRUE))) {
            manualInterimAndAdvancePaymentParametersResponse.setVatRateManual(true);
            
            if (billingRun.getVatRateId() != null) {
                manualInterimAndAdvancePaymentParametersResponse.setVatRate(getVatRate(billingRun.getVatRateId()));
            }
        }
        
        manualInterimAndAdvancePaymentParametersResponse.setGlobalVatRate(Boolean.TRUE.equals(billingRun.getGlobalVatRate()));
        
        if (billingRun.getInterestRateId() != null) {
            manualInterimAndAdvancePaymentParametersResponse.setApplicableInterestRateManual(true);
            manualInterimAndAdvancePaymentParametersResponse.setApplicableInterestRate(getInterestRate(billingRun.getInterestRateId()));
        }
        
        manualInterimAndAdvancePaymentParametersResponse.setDirectDebit(billingRun.getDirectDebit());
        manualInterimAndAdvancePaymentParametersResponse.setDirectDebitManual(billingRun.getDirectDebit() != null);
        
        if (billingRun.getBankId() != null) {
            manualInterimAndAdvancePaymentParametersResponse.setBank(getBank(billingRun.getBankId()));
        }
        
        manualInterimAndAdvancePaymentParametersResponse.setIban(billingRun.getIban());
        manualInterimAndAdvancePaymentParametersResponse.setCustomerDetails(getCustomerDetailsResponse(billingRun.getCustomerDetailId()));
        manualInterimAndAdvancePaymentParametersResponse.setBasisForIssuing(billingRun.getBasisForIssuing());
        manualInterimAndAdvancePaymentParametersResponse.setPrefixType(billingRun.getPrefixType());
        mapContractOrderParameters(manualInterimAndAdvancePaymentParametersResponse, billingRun);
        
        if (Objects.equals(manualInterimAndAdvancePaymentParametersResponse.getContractType(), ContractType.PRODUCT_CONTRACT)) {
            manualInterimAndAdvancePaymentParametersResponse.setBillingGroupResponses(getBillingGroupResponse(billingRun.getId(), billingRun.getProductContractId()));
        }
        
        return manualInterimAndAdvancePaymentParametersResponse;
    }

    private CustomerDetailsShortResponse getCustomerDetailsResponse(Long customerDetailId) {
        Customer customer = getCustomer(customerDetailId, List.of(CustomerStatus.ACTIVE));
        CustomerDetails customerDetails = getCustomerDetails(customerDetailId);
        return new CustomerDetailsShortResponse(customer.getId(), customerDetails.getId(), getCustomerName(customer, customerDetails), customer.getCustomerType(),customerDetails.getBusinessActivity());
    }

    private CustomerDetails getCustomerDetails(Long customerDetailId) {
        return customerDetailsRepository.findById(customerDetailId)
                .orElseThrow(() -> new DomainEntityNotFoundException("CustomerDetails not found with id:%s".formatted(customerDetailId)));
    }

    private Customer getCustomer(Long customerDetailId, List<CustomerStatus> statuses) {
        return customerRepository.findByCustomerDetailIdAndStatusIn(customerDetailId, statuses)
                .orElseThrow(() -> new DomainEntityNotFoundException("Customer not found with customerDetailId: %s;".formatted(customerDetailId)));
    }

    private String getCustomerName(Customer customer, CustomerDetails customerDetails) {
        String legalFormName = customerDetailsRepository.getLegalFormName(customerDetails.getId());
        return String.format("%s (%s%s%s%s)", customer.getIdentifier(), customerDetails.getName(),
                customerDetails.getMiddleName() != null ? " " + customerDetails.getMiddleName() : "",
                customerDetails.getLastName() != null ? " " + customerDetails.getLastName() : "",
                StringUtils.isNotEmpty(legalFormName) ? " " + legalFormName : "");
    }


    private InterestRateShortResponse getInterestRate(Long interestRateId) {
        return new InterestRateShortResponse(
                interestRateRepository.findByIdAndStatusIn(interestRateId, List.of(InterestRateStatus.ACTIVE))
                        .orElseThrow(() ->
                                new DomainEntityNotFoundException("interest rate with given id: %s not found".formatted(interestRateId))
                        )
        );
    }

    private VatRateResponse getVatRate(Long vatRateId) {
        return new VatRateResponse(
                vatRateRepository.findByIdAndStatus(vatRateId, List.of(NomenclatureItemStatus.ACTIVE, NomenclatureItemStatus.INACTIVE,NomenclatureItemStatus.DELETED))
                        .orElseThrow(() ->
                                new DomainEntityNotFoundException("vat rate with given id: %s not found".formatted(vatRateId)))
        );
    }

    private BankResponse getBank(Long bankId) {
        return new BankResponse(
                bankRepository.findByIdAndStatus(bankId, List.of(NomenclatureItemStatus.ACTIVE, NomenclatureItemStatus.INACTIVE,NomenclatureItemStatus.DELETED))
                        .orElseThrow(() -> new DomainEntityNotFoundException("bank with given id: %s not found;".formatted(bankId)))
        );
    }

    private void mapContractOrderParameters(ManualInterimAndAdvancePaymentParametersResponse manualInterimAndAdvancePaymentParametersResponse, BillingRun billingRun) {
        if (billingRun.getServiceContractId() != null) {
            manualInterimAndAdvancePaymentParametersResponse.setContractType(ContractType.SERVICE_CONTRACT);
        } else if (billingRun.getProductContractId() != null) {
            manualInterimAndAdvancePaymentParametersResponse.setContractType(ContractType.PRODUCT_CONTRACT);
        }

        ContractOrderShortResponse shortResponse;
        if (manualInterimAndAdvancePaymentParametersResponse.getContractType() != null) {
            switch (manualInterimAndAdvancePaymentParametersResponse.getContractType()) {
                case SERVICE_CONTRACT ->
                        shortResponse = serviceContractsRepository.findByIdAndStatus(billingRun.getServiceContractId(), EntityStatus.ACTIVE)
                                .orElseThrow(() -> new DomainEntityNotFoundException("Service contract not found by given id: %s".formatted(billingRun.getServiceContractId())));
                case PRODUCT_CONTRACT ->
                        shortResponse = productContractRepository.findByIdAndStatus(billingRun.getProductContractId(), ProductContractStatus.ACTIVE)
                                .orElseThrow(() -> new DomainEntityNotFoundException("Product contract not found by given id: %s".formatted(billingRun.getProductContractId())));
                default -> shortResponse = null;
            }
            manualInterimAndAdvancePaymentParametersResponse.setContract(shortResponse);
        }
    }

    private Set<BillingGroupListingResponse> getBillingGroupResponse(Long id, Long productContractId) {
        List<BillingRunBillingGroup> billingRunBillingGroups = billingRunBillingGroupRepository.findByBillingRunId(id, EntityStatus.ACTIVE);

        if (billingRunBillingGroups.isEmpty())
            throw new DomainEntityNotFoundException("Billing run billing group not found by billing run id: %s".formatted(id));

        Set<BillingGroupListingResponse> billingGroupListingResponses = billingGroupRepository.findAllIdAndContractIdAndStatus(billingRunBillingGroups.stream().map(BillingRunBillingGroup::getBillingGroupId).toList(), productContractId, EntityStatus.ACTIVE);

        if (billingGroupListingResponses.isEmpty())
            throw new DomainEntityNotFoundException("Contract billing group not found for given contract with id: %s".formatted(productContractId));

        return billingGroupListingResponses;
    }

    private ShortResponse getCurrencyResponse(Long valueCurrencyId) {
        Currency currency = currencyRepository.findByIdAndStatus(valueCurrencyId, List.of(NomenclatureItemStatus.ACTIVE, NomenclatureItemStatus.INACTIVE,NomenclatureItemStatus.DELETED))
                .orElseThrow(() -> new DomainEntityNotFoundException("currency not found with given id: %s".formatted(valueCurrencyId)));
        return new ShortResponse(currency.getId(), currency.getName());
    }
}
