package bg.energo.phoenix.service.billing.billingRun.manualInvoice;

import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.billing.billingRun.BillingDetailedData;
import bg.energo.phoenix.model.entity.billing.billingRun.BillingRun;
import bg.energo.phoenix.model.entity.billing.billingRun.BillingRunBillingGroup;
import bg.energo.phoenix.model.entity.billing.billingRun.BillingSummaryData;
import bg.energo.phoenix.model.entity.customer.Customer;
import bg.energo.phoenix.model.entity.customer.CustomerDetails;
import bg.energo.phoenix.model.entity.nomenclature.product.Currency;
import bg.energo.phoenix.model.enums.billing.billings.ContractOrderType;
import bg.energo.phoenix.model.enums.contract.InterestRate.InterestRateStatus;
import bg.energo.phoenix.model.enums.contract.products.ProductContractStatus;
import bg.energo.phoenix.model.enums.customer.CustomerStatus;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.response.billing.billingRun.manualInvoice.*;
import bg.energo.phoenix.model.response.contract.InterestRate.InterestRateShortResponse;
import bg.energo.phoenix.model.response.contract.biling.BillingGroupListingResponse;
import bg.energo.phoenix.model.response.customer.CustomerCommunicationDataResponse;
import bg.energo.phoenix.model.response.customer.CustomerDetailsShortResponse;
import bg.energo.phoenix.model.response.nomenclature.customer.BankResponse;
import bg.energo.phoenix.model.response.nomenclature.product.VatRateResponse;
import bg.energo.phoenix.model.response.shared.ShortResponse;
import bg.energo.phoenix.repository.billing.billingRun.BillingDetailedDataRepository;
import bg.energo.phoenix.repository.billing.billingRun.BillingRunBillingGroupRepository;
import bg.energo.phoenix.repository.billing.billingRun.BillingSummaryDataRepository;
import bg.energo.phoenix.repository.contract.billing.ContractBillingGroupRepository;
import bg.energo.phoenix.repository.contract.order.goods.GoodsOrderRepository;
import bg.energo.phoenix.repository.contract.order.service.ServiceOrderRepository;
import bg.energo.phoenix.repository.contract.product.ProductContractDetailsRepository;
import bg.energo.phoenix.repository.contract.product.ProductContractRepository;
import bg.energo.phoenix.repository.contract.service.ServiceContractDetailsRepository;
import bg.energo.phoenix.repository.contract.service.ServiceContractsRepository;
import bg.energo.phoenix.repository.customer.CustomerDetailsRepository;
import bg.energo.phoenix.repository.customer.CustomerRepository;
import bg.energo.phoenix.repository.customer.communicationData.CustomerCommunicationsRepository;
import bg.energo.phoenix.repository.interestRate.InterestRateRepository;
import bg.energo.phoenix.repository.nomenclature.customer.BankRepository;
import bg.energo.phoenix.repository.nomenclature.product.CurrencyRepository;
import bg.energo.phoenix.repository.nomenclature.product.VatRateRepository;
import bg.energo.phoenix.util.contract.CommunicationContactPurposeProperties;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ManualInvoiceMapperService {

    private final CustomerDetailsRepository customerDetailsRepository;
    private final CustomerRepository customerRepository;
    private final InterestRateRepository interestRateRepository;
    private final VatRateRepository vatRateRepository;
    private final BankRepository bankRepository;
    private final GoodsOrderRepository goodsOrderRepository;
    private final ServiceOrderRepository serviceOrderRepository;
    private final ProductContractRepository productContractRepository;
    private final ProductContractDetailsRepository productContractDetailsRepository;
    private final ServiceContractsRepository serviceContractsRepository;
    private final ServiceContractDetailsRepository serviceContractDetailsRepository;
    private final BillingRunBillingGroupRepository billingRunBillingGroupRepository;
    private final ContractBillingGroupRepository billingGroupRepository;
    private final CustomerCommunicationsRepository customerCommunicationsRepository;
    private final CommunicationContactPurposeProperties communicationContactPurposeProperties;
    private final BillingSummaryDataRepository billingSummaryDataRepository;
    private final CurrencyRepository currencyRepository;
    private final BillingDetailedDataRepository billingDetailedDataRepository;


    protected ManualInvoiceBasicDataParametersResponse mapBasicDataParameters(BillingRun billingRun) {
        ManualInvoiceBasicDataParametersResponse basicDataParametersResponse = new ManualInvoiceBasicDataParametersResponse();
        basicDataParametersResponse.setBasisForIssuing(billingRun.getBasisForIssuing());
        basicDataParametersResponse.setNumberOfIncomeAccount(billingRun.getNumberOfIncomeAccount());
        basicDataParametersResponse.setNumberOfIncomeAccountManual(billingRun.getNumberOfIncomeAccount() != null);
        basicDataParametersResponse.setCostCenterControllingOrder(billingRun.getCostCenterControllingOrder());
        basicDataParametersResponse.setCostCenterControllingOrderManual(billingRun.getCostCenterControllingOrder() != null);
        if (billingRun.getVatRateId() != null || (billingRun.getGlobalVatRate() != null && billingRun.getGlobalVatRate().equals(Boolean.TRUE))) {
            basicDataParametersResponse.setVatRateManual(true);
            if (billingRun.getVatRateId() != null) {
                basicDataParametersResponse.setVatRate(getVatRate(billingRun.getVatRateId()));
            }
        }
        basicDataParametersResponse.setGlobalVatRate(billingRun.getGlobalVatRate());
        if (billingRun.getInterestRateId() != null) {
            basicDataParametersResponse.setApplicableInterestRateManual(true);
            basicDataParametersResponse.setApplicableInterestRate(getInterestRate(billingRun.getInterestRateId()));
        }
        basicDataParametersResponse.setDirectDebit(billingRun.getDirectDebit());
        basicDataParametersResponse.setDirectDebitManual(billingRun.getDirectDebit() != null && billingRun.getDirectDebit().equals(Boolean.TRUE));
        if (billingRun.getBankId() != null) {
            basicDataParametersResponse.setBank(getBank(billingRun.getBankId()));
        }
        basicDataParametersResponse.setIban(billingRun.getIban());
        basicDataParametersResponse.setPrefixType(billingRun.getPrefixType());
        basicDataParametersResponse.setCustomerDetails(getCustomerDetailsResponse(billingRun.getCustomerDetailId()));
        mapContractOrderParameters(basicDataParametersResponse, billingRun);
        if (Objects.equals(basicDataParametersResponse.getContractOrderType(), ContractOrderType.PRODUCT_CONTRACT)) {
            basicDataParametersResponse.setBillingGroupResponse(getBillingGroupResponse(billingRun.getId(), billingRun.getProductContractId()));
        }
        basicDataParametersResponse.setInvoiceCommunicationData(getCommunicationData(billingRun, basicDataParametersResponse));
        return basicDataParametersResponse;
    }

    public ManualInvoiceSummaryDataParametersResponse mapSummaryDataParameters(BillingRun billingRun) {
        ManualInvoiceSummaryDataParametersResponse summaryDataParametersResponse = new ManualInvoiceSummaryDataParametersResponse();
        List<BillingSummaryData> billingSummaryData = billingSummaryDataRepository.findByBillingId(billingRun.getId());
        summaryDataParametersResponse.setManualInvoiceType(billingRun.getManualInvoiceType());
        List<SummaryDataRowParametersViewResponse> summaryDataRowParametersList = new ArrayList<>();
        for (BillingSummaryData summaryDataRow : billingSummaryData) {
            SummaryDataRowParametersViewResponse dataRowParametersResponse = new SummaryDataRowParametersViewResponse();
            dataRowParametersResponse.setId(summaryDataRow.getId());
            dataRowParametersResponse.setPriceComponentOrPriceComponentGroupOrItem(summaryDataRow.getPriceComponentOrPriceComponentGroups());
            dataRowParametersResponse.setTotalVolumes(summaryDataRow.getTotalVolumes());
            dataRowParametersResponse.setUnitOfMeasuresForTotalVolumes(summaryDataRow.getMeasuresUnitForTotalVolumes());
            dataRowParametersResponse.setUnitPrice(summaryDataRow.getUnitPrice());
            dataRowParametersResponse.setUnitOfMeasureForUnitPrice(summaryDataRow.getMeasureUnitForUnitPrice());
            dataRowParametersResponse.setValue(summaryDataRow.getValue());
            if (summaryDataRow.getValueCurrencyId() != null) {
                dataRowParametersResponse.setCurrency(getCurrencyResponse(summaryDataRow.getValueCurrencyId()));
            }
            dataRowParametersResponse.setIncomeAccount(summaryDataRow.getIncomeAccount());
            dataRowParametersResponse.setCostCenter(summaryDataRow.getCostCenter());
            if (summaryDataRow.getVatRateId() != null) {
                dataRowParametersResponse.setVatRate(getVatRate(summaryDataRow.getVatRateId()));
            }
            if (summaryDataRow.getGlobalVatRate() != null) {
                dataRowParametersResponse.setGlobalVatRate(summaryDataRow.getGlobalVatRate());
            }
            summaryDataRowParametersList.add(dataRowParametersResponse);
        }
        summaryDataParametersResponse.setSummaryDataRowParametersList(summaryDataRowParametersList);
        return summaryDataParametersResponse;
    }

    public ManualInvoiceDetailedDataParametersResponse mapDetailedDataParameters(BillingRun billingRun) {
        ManualInvoiceDetailedDataParametersResponse detailedDataParametersResponse = new ManualInvoiceDetailedDataParametersResponse();
        List<DetailedDataRowParametersViewResponse> detailedDataRowParametersList = new ArrayList<>();
        List<BillingDetailedData> detailedDataRows = billingDetailedDataRepository.findByBillingId(billingRun.getId());
        for (BillingDetailedData detailedDataRow : detailedDataRows) {
            DetailedDataRowParametersViewResponse parametersResponse = new DetailedDataRowParametersViewResponse();
            parametersResponse.setId(detailedDataRow.getId());
            parametersResponse.setPriceComponent(detailedDataRow.getPriceComponent());
            parametersResponse.setPointOfDelivery(detailedDataRow.getPod());
            parametersResponse.setPeriodFrom(detailedDataRow.getPeriodFrom());
            parametersResponse.setPeriodTo(detailedDataRow.getPeriodTo());
            parametersResponse.setMeter(detailedDataRow.getMeter());
            parametersResponse.setNewMeterReading(detailedDataRow.getNewMeterReading());
            parametersResponse.setOldMeterReading(detailedDataRow.getOldMeterReading());
            parametersResponse.setDifferences(detailedDataRow.getDifferences());
            parametersResponse.setMultiplier(detailedDataRow.getMultiplier());
            parametersResponse.setCorrection(detailedDataRow.getCorrection());
            parametersResponse.setDeducted(detailedDataRow.getDeducted());
            parametersResponse.setTotalVolumes(detailedDataRow.getTotalVolumes());
            parametersResponse.setUnitOfMeasureForTotalVolumes(detailedDataRow.getMeasuresUnitForTotalVolumes());
            parametersResponse.setUnitPrice(detailedDataRow.getUnitPrice());
            parametersResponse.setUnitOfMeasureForUnitPrice(detailedDataRow.getMeasureUnitForUnitPrice());
            parametersResponse.setCurrentValue(detailedDataRow.getValue());
            if (detailedDataRow.getValueCurrencyId() != null) {
                parametersResponse.setCurrency(getCurrencyResponse(detailedDataRow.getValueCurrencyId()));
            }
            parametersResponse.setIncomeAccount(detailedDataRow.getIncomeAccount());
            parametersResponse.setCostCenter(detailedDataRow.getCostCenter());
            if (detailedDataRow.getVatRateId() != null) {
                parametersResponse.setVatRate(getVatRate(detailedDataRow.getVatRateId()));
            }
            if (detailedDataRow.getGlobalVatRate() != null) {
                parametersResponse.setGlobalVatRate(detailedDataRow.getGlobalVatRate());
            }
            detailedDataRowParametersList.add(parametersResponse);
        }
        detailedDataParametersResponse.setDetailedDataRowParametersList(detailedDataRowParametersList);
        return detailedDataParametersResponse;
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

    private void mapContractOrderParameters(ManualInvoiceBasicDataParametersResponse basicDataParametersResponse, BillingRun billingRun) {
        if (billingRun.getServiceContractId() != null) {
            basicDataParametersResponse.setContractOrderType(ContractOrderType.SERVICE_CONTRACT);
        } else if (billingRun.getProductContractId() != null) {
            basicDataParametersResponse.setContractOrderType(ContractOrderType.PRODUCT_CONTRACT);
        } else if (billingRun.getGoodsOrderId() != null) {
            basicDataParametersResponse.setContractOrderType(ContractOrderType.GOODS_ORDER);
        } else if (billingRun.getServiceOrderId() != null) {
            basicDataParametersResponse.setContractOrderType(ContractOrderType.SERVICE_ORDER);
        }
        ContractOrderShortResponse shortResponse;
        if (basicDataParametersResponse.getContractOrderType() != null) {
            switch (basicDataParametersResponse.getContractOrderType()) {
                case SERVICE_CONTRACT ->
                        shortResponse = serviceContractsRepository.findByIdAndStatus(billingRun.getServiceContractId(), EntityStatus.ACTIVE)
                                .orElseThrow(() -> new DomainEntityNotFoundException("Service contract not found by given id: %s".formatted(billingRun.getServiceContractId())));
                case PRODUCT_CONTRACT ->
                        shortResponse = productContractRepository.findByIdAndStatus(billingRun.getProductContractId(), ProductContractStatus.ACTIVE)
                                .orElseThrow(() -> new DomainEntityNotFoundException("Product contract not found by given id: %s".formatted(billingRun.getProductContractId())));
                case SERVICE_ORDER ->
                        shortResponse = serviceOrderRepository.findByIdAndStatus(billingRun.getServiceOrderId(), EntityStatus.ACTIVE)
                                .orElseThrow(() -> new DomainEntityNotFoundException("Service order not found by given id: %s".formatted(billingRun.getServiceOrderId())));
                case GOODS_ORDER ->
                        shortResponse = goodsOrderRepository.findByIdAndStatus(billingRun.getGoodsOrderId(), EntityStatus.ACTIVE)
                                .orElseThrow(() -> new DomainEntityNotFoundException("Goods order not found by given id: %s".formatted(billingRun.getGoodsOrderId())));
                default -> shortResponse = null;
            }
            basicDataParametersResponse.setContractOrder(shortResponse);
        }
    }

    private BillingGroupListingResponse getBillingGroupResponse(Long id, Long productContractId) {
        BillingRunBillingGroup billingRunBillingGroup = billingRunBillingGroupRepository.findByBillingRunIdAndStatus(id, EntityStatus.ACTIVE)
                .orElseThrow(() -> new DomainEntityNotFoundException("Billing run billing group not found by billing run id: %s".formatted(id)));
        return billingGroupRepository.findByIdAndContractIdAndStatus(billingRunBillingGroup.getBillingGroupId(), productContractId, EntityStatus.ACTIVE)
                .orElseThrow(() -> new DomainEntityNotFoundException("Contract billing group by id %s not found for given contract with id: %s".formatted(billingRunBillingGroup.getBillingGroupId(), productContractId)));
    }

    private CustomerCommunicationDataResponse getCommunicationData(BillingRun billingRun, ManualInvoiceBasicDataParametersResponse basicDataParametersResponse) {
        CustomerCommunicationDataResponse response = null;
        if (basicDataParametersResponse.getContractOrderType() == null) {
            response = customerCommunicationsRepository.findByIdAndCustomerDetailsIdAndPurpose(billingRun.getCustomerCommunicationId(), billingRun.getCustomerDetailId(), communicationContactPurposeProperties.getBillingCommunicationId())
                    .orElseThrow(() -> new DomainEntityNotFoundException("Customer communication with billing purpose not found for customer details with id %s by given id: %s".formatted(billingRun.getCustomerDetailId(), billingRun.getCustomerCommunicationId())));
        } else {
            switch (basicDataParametersResponse.getContractOrderType()) {
                case PRODUCT_CONTRACT ->
                        response = productContractDetailsRepository.findCommunicationDataByIdAndContractIdAndCustomerDetailId(
                                        billingRun.getCustomerCommunicationId(), billingRun.getProductContractId(), billingRun.getCustomerDetailId())
                                .orElseThrow(() -> new DomainEntityNotFoundException("Communication data not found for given product contract with id: %s".formatted(billingRun.getProductContractId())));
                case SERVICE_CONTRACT ->
                        response = serviceContractDetailsRepository.findCommunicationDataByIdAndContractIdAndCustomerDetailId(
                                        billingRun.getCustomerCommunicationId(), billingRun.getServiceContractId(), billingRun.getCustomerDetailId())
                                .orElseThrow(() -> new DomainEntityNotFoundException("Communication data not found for given service contract with id: %s".formatted(billingRun.getServiceContractId())));
                case GOODS_ORDER ->
                        response = goodsOrderRepository.findCommunicationDataByIdAndOrderIdAndCustomerDetailId(
                                        billingRun.getCustomerCommunicationId(), billingRun.getGoodsOrderId(), billingRun.getCustomerDetailId())
                                .orElseThrow(() -> new DomainEntityNotFoundException("Communication data not found for given goods order with id: %s".formatted(billingRun.getGoodsOrderId())));
                case SERVICE_ORDER ->
                        response = serviceOrderRepository.findCommunicationDataByIdAndOrderIdAndCustomerDetailId(
                                        billingRun.getCustomerCommunicationId(), billingRun.getServiceOrderId(), billingRun.getCustomerDetailId())
                                .orElseThrow(() -> new DomainEntityNotFoundException("Communication data not found for given service order with id: %s".formatted(billingRun.getServiceOrderId())));
            }
        }
        return response;
    }

    private ShortResponse getCurrencyResponse(Long valueCurrencyId) {
        Currency currency = currencyRepository.findByIdAndStatus(valueCurrencyId, List.of(NomenclatureItemStatus.ACTIVE, NomenclatureItemStatus.INACTIVE,NomenclatureItemStatus.DELETED))
                .orElseThrow(() -> new DomainEntityNotFoundException("currency not found with given id: %s".formatted(valueCurrencyId)));
        return new ShortResponse(currency.getId(), currency.getName());
    }

}
