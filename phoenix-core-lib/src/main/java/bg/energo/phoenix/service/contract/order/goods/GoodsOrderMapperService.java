package bg.energo.phoenix.service.contract.order.goods;

import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.contract.InterestRate.InterestRate;
import bg.energo.phoenix.model.entity.contract.order.goods.GoodsOrder;
import bg.energo.phoenix.model.entity.contract.order.goods.GoodsOrderProxies;
import bg.energo.phoenix.model.entity.contract.order.goods.GoodsOrderProxyFiles;
import bg.energo.phoenix.model.entity.contract.order.goods.GoodsOrderProxyManagers;
import bg.energo.phoenix.model.entity.customer.AccountManager;
import bg.energo.phoenix.model.entity.customer.Customer;
import bg.energo.phoenix.model.entity.customer.CustomerDetails;
import bg.energo.phoenix.model.entity.customer.Manager;
import bg.energo.phoenix.model.entity.customer.communication.CustomerCommunications;
import bg.energo.phoenix.model.entity.nomenclature.contract.Campaign;
import bg.energo.phoenix.model.entity.nomenclature.customer.Bank;
import bg.energo.phoenix.model.entity.nomenclature.product.Currency;
import bg.energo.phoenix.model.entity.nomenclature.product.VatRate;
import bg.energo.phoenix.model.entity.nomenclature.product.goods.GoodsUnits;
import bg.energo.phoenix.model.entity.product.goods.GoodsDetails;
import bg.energo.phoenix.model.enums.billing.invoice.InvoiceStatus;
import bg.energo.phoenix.model.enums.contract.InterestRate.InterestRateStatus;
import bg.energo.phoenix.model.enums.contract.OrderType;
import bg.energo.phoenix.model.enums.contract.products.ContractSubObjectStatus;
import bg.energo.phoenix.model.enums.customer.CustomerStatus;
import bg.energo.phoenix.model.enums.customer.Status;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.request.contract.order.goods.GoodsOrderGoodsParametersTableItemResponse;
import bg.energo.phoenix.model.request.contract.relatedEntities.RelatedEntityType;
import bg.energo.phoenix.model.response.contract.order.goods.*;
import bg.energo.phoenix.model.response.customer.CustomerCommunicationDataResponse;
import bg.energo.phoenix.model.response.customer.CustomerDetailsShortResponse;
import bg.energo.phoenix.model.response.nomenclature.customer.BankResponse;
import bg.energo.phoenix.model.response.nomenclature.goods.GoodsUnitsShortResponse;
import bg.energo.phoenix.model.response.nomenclature.product.VatRateResponse;
import bg.energo.phoenix.model.response.nomenclature.product.currency.CurrencyShortResponse;
import bg.energo.phoenix.permissions.PermissionContextEnum;
import bg.energo.phoenix.repository.billing.invoice.InvoiceRepository;
import bg.energo.phoenix.repository.contract.order.goods.GoodsOrderGoodsRepository;
import bg.energo.phoenix.repository.contract.order.goods.GoodsOrderProxyFilesRepository;
import bg.energo.phoenix.repository.contract.order.goods.GoodsOrderProxyManagersRepository;
import bg.energo.phoenix.repository.contract.order.goods.GoodsOrderProxyRepository;
import bg.energo.phoenix.repository.customer.AccountManagerRepository;
import bg.energo.phoenix.repository.customer.CustomerDetailsRepository;
import bg.energo.phoenix.repository.customer.CustomerRepository;
import bg.energo.phoenix.repository.customer.ManagerRepository;
import bg.energo.phoenix.repository.customer.communicationData.CustomerCommunicationsRepository;
import bg.energo.phoenix.repository.interestRate.InterestRateRepository;
import bg.energo.phoenix.repository.nomenclature.contract.CampaignRepository;
import bg.energo.phoenix.repository.nomenclature.customer.BankRepository;
import bg.energo.phoenix.repository.nomenclature.product.CurrencyRepository;
import bg.energo.phoenix.repository.nomenclature.product.VatRateRepository;
import bg.energo.phoenix.repository.nomenclature.product.goods.GoodsUnitsRepository;
import bg.energo.phoenix.repository.product.goods.GoodsDetailsRepository;
import bg.energo.phoenix.security.PermissionService;
import bg.energo.phoenix.service.contract.relatedEntities.RelatedContractsAndOrdersService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static bg.energo.phoenix.permissions.PermissionEnum.GOODS_ORDER_VIEW;
import static bg.energo.phoenix.permissions.PermissionEnum.GOODS_ORDER_VIEW_DELETED;

@Service
@RequiredArgsConstructor
public class GoodsOrderMapperService {
    private final GoodsOrderActivityService goodsOrderActivityService;
    private final RelatedContractsAndOrdersService relatedContractsAndOrdersService;
    private final GoodsOrderGoodsRepository goodsOrderGoodsRepository;
    private final PermissionService permissionService;
    private final BankRepository bankRepository;
    private final InterestRateRepository interestRateRepository;
    private final CampaignRepository campaignRepository;
    private final CustomerCommunicationsRepository communicationsRepository;
    private final VatRateRepository vatRateRepository;
    private final GoodsOrderProxyManagersRepository goodsOrderProxyManagersRepository;
    private final GoodsOrderProxyFilesRepository goodsOrderProxyFilesRepository;
    private final GoodsOrderProxyRepository goodsOrderProxyRepository;
    private final CustomerDetailsRepository customerDetailsRepository;
    private final CustomerRepository customerRepository;
    private final GoodsOrderBasicParametersService goodsOrderBasicParametersService;
    private final GoodsDetailsRepository goodsDetailsRepository;
    private final GoodsUnitsRepository goodsUnitsRepository;
    private final CurrencyRepository currencyRepository;
    private final AccountManagerRepository accountManagerRepository;
    private final ManagerRepository managerRepository;
    private final InvoiceRepository invoiceRepository;


    public GoodsOrderProxyResponse mapProxyResponse(GoodsOrderProxies proxy, List<GoodsOrderProxyFiles> proxyFiles, List<GoodsOrderProxyManagers> proxyManagers) {
        return GoodsOrderProxyResponse.builder()
                .id(proxy.getId())
                .proxyForeignEntityPerson(proxy.getProxyForeignEntityPerson())
                .proxyName(proxy.getProxyName())
                .proxyCustomerIdentifier(proxy.getProxyPersonalIdentifier())
                .proxyEmail(proxy.getProxyEmail())
                .proxyPhone(proxy.getProxyMobilePhone())
                .proxyPowerOfAttorneyNumber(proxy.getProxyAttorneyPowerNumber())
                .proxyData(proxy.getProxyDate())
                .proxyValidTill(proxy.getProxyValidTill())
                .notaryPublic(proxy.getProxyNotaryPublic())
                .registrationNumber(proxy.getProxyRegistrationNumber())
                .areaOfOperation(proxy.getProxyOperationArea())
                .authorizedProxyForeignEntityPerson(proxy.getProxyByProxyForeignEntityPerson())
                .proxyAuthorizedByProxy(proxy.getProxyByProxyName())
                .authorizedProxyCustomerIdentifier(proxy.getProxyByProxyPersonalIdentifier())
                .authorizedProxyEmail(proxy.getProxyByProxyEmail())
                .authorizedProxyPhone(proxy.getProxyByProxyMobilePhone())
                .authorizedProxyPowerOfAttorneyNumber(proxy.getProxyByProxyAttorneyPowerNumber())
                .authorizedProxyData(proxy.getProxyByProxyDate())
                .authorizedProxyValidTill(proxy.getProxyByProxyValidTill())
                .authorizedProxyNotaryPublic(proxy.getProxyByProxyNotaryPublic())
                .authorizedProxyRegistrationNumber(proxy.getProxyByProxyRegistrationNumber())
                .authorizedProxyAreaOfOperation(proxy.getProxyByProxyOperationArea())
                .status(getProxyStatus(proxy.getStatus()))
                .proxyFiles(mapProxyFiles(proxyFiles))
                .proxyManagers(mapProxyManagers(proxyManagers))
                .build();
    }

    public ContractSubObjectStatus getProxyStatus(EntityStatus status) {
        if (status.equals(EntityStatus.ACTIVE)) {
            return ContractSubObjectStatus.ACTIVE;
        }
        if (status.equals(EntityStatus.DELETED)) {
            return ContractSubObjectStatus.DELETED;
        }
        return null;
    }

    public List<GoodsOrderProxyFilesResponse> mapProxyFiles(List<GoodsOrderProxyFiles> proxyFiles) {
        List<GoodsOrderProxyFilesResponse> responses = new ArrayList<>();
        for (GoodsOrderProxyFiles item : proxyFiles) {
            GoodsOrderProxyFilesResponse proxyFileResponse = new GoodsOrderProxyFilesResponse();
            proxyFileResponse.setId(item.getId());
            proxyFileResponse.setName(item.getName());
            proxyFileResponse.setFileUrl(item.getFileUrl());
            proxyFileResponse.setGoodsOrderProxyId(item.getOrderProxyId());
            proxyFileResponse.setStatus(item.getStatus());
            responses.add(proxyFileResponse);
        }
        return responses;
    }

    public List<GoodsOrderProxyManagersResponse> mapProxyManagers(List<GoodsOrderProxyManagers> proxyManagers) {
        List<GoodsOrderProxyManagersResponse> responses = new ArrayList<>();
        for (GoodsOrderProxyManagers item : proxyManagers) {
            GoodsOrderProxyManagersResponse proxyManagersResponse = new GoodsOrderProxyManagersResponse();
            proxyManagersResponse.setId(item.getId());
            proxyManagersResponse.setManagerName(getManagerName(item.getCustomerManagerId()));
            proxyManagersResponse.setGoodsOrderProxyId(item.getOrderProxyId());
            proxyManagersResponse.setCustomerManagerId(item.getCustomerManagerId());
            proxyManagersResponse.setStatus(item.getStatus());
            responses.add(proxyManagersResponse);
        }
        return responses;
    }

    private String getManagerName(Long customerManagerId) {
        Optional<Manager> accountManagerOptional = managerRepository.findById(customerManagerId);
        if (accountManagerOptional.isPresent()) {
            Manager manager = accountManagerOptional.get();
            return manager.getName();
        }
        return null;
    }

    public GoodsOrderBasicParametersResponse mapGoodsOrderToBasicParametersResponse(GoodsOrder goodsOrder) {
        GoodsOrderBasicParametersResponse basicParametersResponse = new GoodsOrderBasicParametersResponse();

        basicParametersResponse.setId(goodsOrder.getId());
        basicParametersResponse.setCreateDate(goodsOrder.getCreateDate());
        basicParametersResponse.setOrderNumber(goodsOrder.getOrderNumber());
        basicParametersResponse.setStatus(goodsOrder.getStatus());
        basicParametersResponse.setDirectDebit(goodsOrder.getDirectDebit());
        Bank bank = getBank(goodsOrder.getBankId(), List.of(NomenclatureItemStatus.ACTIVE, NomenclatureItemStatus.INACTIVE));
        basicParametersResponse.setBank(bank == null ? null : new BankResponse(bank));
        basicParametersResponse.setIban(goodsOrder.getIban());
        InterestRate interestRate = getInterestRate(goodsOrder.getApplicableInterestRateId());
        basicParametersResponse.setApplicableInterestRateId(interestRate == null ? null : interestRate.getId());
        basicParametersResponse.setApplicableInterestRateName(interestRate == null ? null : interestRate.getName());
        Campaign campaign = getCampaign(goodsOrder.getCampaignId(), List.of(NomenclatureItemStatus.ACTIVE, NomenclatureItemStatus.INACTIVE));
        basicParametersResponse.setCampaignId(campaign == null ? null : campaign.getId());
        basicParametersResponse.setCampaignName(campaign == null ? null : campaign.getName());
        basicParametersResponse.setPrepaymentTermInCalendarDays(goodsOrder.getPrepaymentTermInCalendarDays());
        basicParametersResponse.setCustomer(getCustomerResponse(goodsOrder.getCustomerDetailId()));
        basicParametersResponse.setBillingCommunicationData(getCommunicationDataForBilling(goodsOrder.getCustomerCommunicationIdForBilling()));
        basicParametersResponse.setNoInterestOnOverdueDebts(goodsOrder.getNoInterestOnOverdueDebts());
        basicParametersResponse.setIncomeAccountNumber(goodsOrder.getIncomeAccountNumber());
        basicParametersResponse.setCostCenterControllingOrder(goodsOrder.getCostCenterControllingOrder());
        basicParametersResponse.setOrderStatus(goodsOrder.getOrderStatus());
        basicParametersResponse.setStatusModifyDate(goodsOrder.getStatusModifyDate());
        AccountManager employee = getEmployeeResponse(goodsOrder);
        basicParametersResponse.setEmployeeId(employee.getId());
        basicParametersResponse.setEmployeeName("%s (%s)".formatted(employee.getDisplayName(), employee.getUserName()));
        basicParametersResponse.setOrderInvoiceStatus(goodsOrder.getOrderInvoiceStatus());

        basicParametersResponse.setProxyResponse(getProxyResponse(goodsOrder));

        // NOTE: id field in the following objects represents the account manager id (in internal intermediaries and assisting employees)
        // and external intermediary id (in external intermediaries), and not a db record id.
        basicParametersResponse.setInternalIntermediaries(goodsOrderBasicParametersService.getInternalIntermediaries(goodsOrder.getId()));
        basicParametersResponse.setExternalIntermediaries(goodsOrderBasicParametersService.getExternalIntermediaries(goodsOrder.getId()));
        basicParametersResponse.setAssistingEmployees(goodsOrderBasicParametersService.getAssistingEmployees(goodsOrder.getId()));

        basicParametersResponse.setActivities(goodsOrderActivityService.getActivitiesByConnectedObjectId(goodsOrder.getId()));
        basicParametersResponse.setRelatedEntities(relatedContractsAndOrdersService.getRelatedEntities(goodsOrder.getId(), RelatedEntityType.GOODS_ORDER));
        basicParametersResponse.setPaymentTerms(goodsOrderBasicParametersService.getGoodsOrderPaymentTerms(goodsOrder.getId()));
        basicParametersResponse.setTasks(goodsOrderBasicParametersService.getTasks(goodsOrder.getId()));
        basicParametersResponse.setInvoiceTemplateResponse(goodsOrderBasicParametersService.getTemplate(goodsOrder.getInvoiceTemplateId()));
        basicParametersResponse.setEmailTemplateResponse(goodsOrderBasicParametersService.getTemplate(goodsOrder.getEmailTemplateId()));
        basicParametersResponse.setInvoice(invoiceRepository.findOrderInvoices(goodsOrder.getId(), InvoiceStatus.REAL, OrderType.GOODS_ORDER.name()).stream().findFirst().orElse(null));
        return basicParametersResponse;
    }

    private AccountManager getEmployeeResponse(GoodsOrder goodsOrder) {
        return accountManagerRepository
                .findById(goodsOrder.getEmployeeId())
                .orElseThrow(() -> new DomainEntityNotFoundException("Unable to find employee with ID %s".formatted(goodsOrder.getEmployeeId())));
    }


    private String getCustomerName(Customer customer, CustomerDetails customerDetails) {
        String legalFormName = customerDetailsRepository.getLegalFormName(customerDetails.getId());
        return String.format("%s (%s%s%s%s)", customer.getIdentifier(), customerDetails.getName(),
                customerDetails.getMiddleName() != null ? " " + customerDetails.getMiddleName() : "",
                customerDetails.getLastName() != null ? " " + customerDetails.getLastName() : "",
                StringUtils.isNotEmpty(legalFormName) ? " " + legalFormName : "");
    }

    private VatRate getVatRate(Long vatRateId) {
        Optional<VatRate> vatRateOptional = vatRateRepository.findByIdAndStatus(vatRateId, List.of(NomenclatureItemStatus.ACTIVE, NomenclatureItemStatus.INACTIVE));
        return vatRateOptional.orElse(new VatRate());
    }

    private CustomerCommunicationDataResponse getCommunicationDataForBilling(Long customerCommunicationIdForBilling) {
        Optional<CustomerCommunications> billingComsOptional = communicationsRepository.findByIdAndStatuses(customerCommunicationIdForBilling, List.of(Status.ACTIVE));
        if (billingComsOptional.isPresent()) {
            CustomerCommunications billingComs = billingComsOptional.get();
            CustomerCommunicationDataResponse response = new CustomerCommunicationDataResponse(billingComs.getId(), billingComs.getContactTypeName(), billingComs.getCreateDate());
            response.setConcatPurposes(customerRepository.getConcatPurposeFromCustomerCommunicationData(billingComs.getId()));
            return response;
        }
        return null;
    }

    private CustomerDetails getCustomerDetails(Long customerDetailId) {
        return customerDetailsRepository.findById(customerDetailId)
                .orElseThrow(() -> new DomainEntityNotFoundException("Can't find customerDetails with id:%s".formatted(customerDetailId)));
    }

    private Customer getCustomer(Long customerDetailId, List<CustomerStatus> statuses) {
        return customerRepository.findByCustomerDetailIdAndStatusIn(customerDetailId, statuses)
                .orElseThrow(() -> new DomainEntityNotFoundException("Can't find Customer with customerDetailId: %s;".formatted(customerDetailId)));
    }

    private Campaign getCampaign(Long campaignId, List<NomenclatureItemStatus> statuses) {
        if (campaignId != null) {
            return campaignRepository.findByIdAndStatusIn(campaignId, statuses)
                    .orElseThrow(() -> new DomainEntityNotFoundException("Can't find active campaign:%s;".formatted(campaignId)));
        } else return null;
    }

    private InterestRate getInterestRate(Long id) {
        if (id != null) {
            return interestRateRepository.findByIdAndStatusIn(id, List.of(InterestRateStatus.ACTIVE))
                    .orElseThrow(() -> new DomainEntityNotFoundException("Can't find active interestRate:%s;".formatted(id)));
        } else return null;
    }

    private Bank getBank(Long bankId, List<NomenclatureItemStatus> statuses) {
        if (bankId != null) {
            return bankRepository.findByIdAndStatus(bankId, statuses)
                    .orElseThrow(() -> new DomainEntityNotFoundException("Can't find active bank:%s;".formatted(bankId)));
        } else return null;
    }

    private List<GoodsOrderProxyResponse> getProxyResponse(GoodsOrder goodsOrder) {
        List<GoodsOrderProxies> proxyList = goodsOrderProxyRepository.findByOrderIdAndStatusIn(goodsOrder.getId(), List.of(EntityStatus.ACTIVE));
        if (proxyList.isEmpty()) {
            return null;
        }
        List<GoodsOrderProxyResponse> proxyResponses = new ArrayList<>();
        for (GoodsOrderProxies proxy : proxyList) {
            List<GoodsOrderProxyFiles> proxyFiles = goodsOrderProxyFilesRepository.findByOrderProxyIdAndStatusIn(proxy.getId(), List.of(EntityStatus.ACTIVE));
            List<GoodsOrderProxyManagers> proxyManagers = goodsOrderProxyManagersRepository.findByOrderProxyIdAndStatus(proxy.getId(), EntityStatus.ACTIVE);
            GoodsOrderProxyResponse proxyResponse = mapProxyResponse(proxy, proxyFiles, proxyManagers);
            proxyResponses.add(proxyResponse);
        }
        return proxyResponses;
    }

    public GoodsOrderGoodsParametersResponse mapGoodsOrderToGoodsParameterResponse(GoodsOrder goodsOrder) {
        GoodsOrderGoodsParametersResponse goodsParametersResponse = new GoodsOrderGoodsParametersResponse();

        goodsParametersResponse.setNumberOfIncomeAccount(goodsOrder.getIncomeAccountNumber());
        goodsParametersResponse.setCostCenterOrControllingOrder(goodsOrder.getCostCenterControllingOrder());
        goodsParametersResponse.setIsGlobalVatRate(goodsOrder.getGlobalVatRate());
        goodsParametersResponse.setVatRate(new VatRateResponse(getVatRate(goodsOrder.getVatRateId())));

        goodsParametersResponse.setGoods(
                goodsOrderGoodsRepository
                        .findAllByOrderId(goodsOrder.getId())
                        .stream()
                        .map(goodsOrderGoods -> {
                            Long goodsDetailsId = goodsOrderGoods.getGoodsDetailsId();
                            if (goodsDetailsId != null) {
                                Optional<GoodsDetails> goodsDetailsOptional = goodsDetailsRepository
                                        .findById(goodsDetailsId);
                                if (goodsDetailsOptional.isPresent()) {
                                    GoodsDetails goodsDetails = goodsDetailsOptional.get();

                                    GoodsUnits goodsUnits = goodsDetails.getGoodsUnits();
                                    Currency currency = goodsDetails.getCurrency();

                                    return new GoodsOrderGoodsParametersTableItemResponse(
                                            goodsOrderGoods.getId(),
                                            goodsDetailsId,
                                            goodsDetails.getName(),
                                            goodsDetails.getOtherSystemConnectionCode(),
                                            goodsUnits == null ? null : new GoodsUnitsShortResponse(goodsUnits),
                                            goodsOrderGoods.getQuantity(),
                                            goodsDetails.getPrice(),
                                            currency == null ? null : new CurrencyShortResponse(currency),
                                            goodsDetails.getIncomeAccountNumbers(),
                                            goodsDetails.getControllingOrderId()
                                    );
                                } else {
                                    return new GoodsOrderGoodsParametersTableItemResponse(
                                            goodsOrderGoods.getId(),
                                            goodsDetailsId,
                                            null,
                                            null,
                                            null,
                                            goodsOrderGoods.getQuantity(),
                                            null,
                                            null,
                                            null,
                                            null
                                    );
                                }
                            } else {
                                return new GoodsOrderGoodsParametersTableItemResponse(
                                        goodsOrderGoods.getId(),
                                        goodsDetailsId,
                                        goodsOrderGoods.getName(),
                                        goodsOrderGoods.getOtherSystemConnectionCode(),
                                        fetchGoodsUnitAndMapToResponse(goodsOrderGoods.getGoodsUnitsId()),
                                        goodsOrderGoods.getQuantity(),
                                        goodsOrderGoods.getPrice(),
                                        fetchCurrencyAndMapToResponse(goodsOrderGoods.getCurrencyId()),
                                        goodsOrderGoods.getIncomeAccountNumber(),
                                        goodsOrderGoods.getCostCenterControllingOrder()
                                );
                            }
                        })
                        .toList()
        );

        return goodsParametersResponse;
    }

    private CustomerDetailsShortResponse getCustomerResponse(Long customerDetailId) {
        Customer customer = getCustomer(customerDetailId, List.of(CustomerStatus.ACTIVE));
        CustomerDetails customerDetails = getCustomerDetails(customerDetailId);
        return new CustomerDetailsShortResponse(customer.getId(), customerDetails.getId(), getCustomerName(customer, customerDetails), customer.getCustomerType(), customerDetails.getBusinessActivity());
    }

    private GoodsUnitsShortResponse fetchGoodsUnitAndMapToResponse(Long goodsUnitId) {
        Optional<GoodsUnits> goodsUnitsOptional = goodsUnitsRepository
                .findByIdAndStatus(goodsUnitId, List.of(NomenclatureItemStatus.ACTIVE, NomenclatureItemStatus.INACTIVE));
        return goodsUnitsOptional.map(GoodsUnitsShortResponse::new).orElse(null);
    }

    private CurrencyShortResponse fetchCurrencyAndMapToResponse(Long currencyId) {
        Optional<Currency> currencyOptional = currencyRepository
                .findByIdAndStatus(currencyId, List.of(NomenclatureItemStatus.ACTIVE, NomenclatureItemStatus.INACTIVE));
        return currencyOptional.map(CurrencyShortResponse::new).orElse(null);
    }

    public List<EntityStatus> getStatuses() {
        List<EntityStatus> statuses = new ArrayList<>();
        if (permissionService.permissionContextContainsPermissions(PermissionContextEnum.GOODS_ORDERS, List.of(GOODS_ORDER_VIEW_DELETED))) {
            statuses.add(EntityStatus.DELETED);
        }
        if (permissionService.permissionContextContainsPermissions(PermissionContextEnum.GOODS_ORDERS, List.of(GOODS_ORDER_VIEW))) {
            statuses.add(EntityStatus.ACTIVE);
        }
        return statuses;
    }
}
