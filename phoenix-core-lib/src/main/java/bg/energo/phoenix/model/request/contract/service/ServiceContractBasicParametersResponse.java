package bg.energo.phoenix.model.request.contract.service;

import bg.energo.phoenix.model.enums.contract.service.ContractVersionStatus;
import bg.energo.phoenix.model.enums.contract.service.ServiceContractContractType;
import bg.energo.phoenix.model.enums.contract.service.ServiceContractDetailStatus;
import bg.energo.phoenix.model.enums.contract.service.ServiceContractDetailsSubStatus;
import bg.energo.phoenix.model.enums.customer.CustomerType;
import bg.energo.phoenix.model.response.contract.ContractFileResponse;
import bg.energo.phoenix.model.response.contract.relatedEntities.RelatedEntityResponse;
import bg.energo.phoenix.model.response.contract.serviceContract.ServiceContractContractVersionTypesResponse;
import bg.energo.phoenix.model.response.customer.CustomerCommunicationDataResponse;
import bg.energo.phoenix.model.response.nomenclature.product.currency.CurrencyResponse;
import bg.energo.phoenix.model.response.proxy.ProxyResponse;
import bg.energo.phoenix.model.response.shared.FileWithStatusesResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ServiceContractBasicParametersResponse {

    private Long id;
    private Long serviceId;
    private Long serviceDetailId;
    private Long serviceVersionId;
    private String serviceName;
    private Long versionId;
    private String contractNumber;
    private String additionalSuffix;
    private LocalDate creationDate;
    private ServiceContractDetailStatus contractStatus;
    private LocalDate contractStatusModifyDate;
    private ServiceContractContractType type;
    private ServiceContractDetailsSubStatus subStatus;
    private LocalDate statusModifyDate;
    private Boolean hasUntilAmount;
    private BigDecimal contractTermUntilTheAmountValue;
    private LocalDate signInDate;
    private LocalDate entryIntoForceDate;
    private LocalDate terminationDate;
    private LocalDate contractTermEndDate;
    //private LocalDate activationDate;
    private LocalDate perpetuityDate;
    private BigDecimal contractTermUntilAmountIsReached;
    private Boolean contractTermUntilAmountIsReachedCheckbox;
    //private Long currencyId;
    private CurrencyResponse currency;
    private Long customerId;
    private Long customerDetailId;
    private Long customerVersionId;
    private CustomerType customerType;
    private Boolean bussinessActivity;
    private Long communicationDataForBilling;
    private Long communicationDataForContract;
    private String customerName;
    private ContractVersionStatus contractVersionStatus;
    private LocalDate contractInitialTermStartDate;
    private List<ServiceContractContractVersionTypesResponse> versionTypes;

    private CustomerCommunicationDataResponse billingCommunicationData;
    private CustomerCommunicationDataResponse contractCommunicationData;

    private List<ProxyResponse> proxyResponse;
    private List<ContractFileResponse> contractFiles;
    private List<FileWithStatusesResponse> additionalDocuments;

    private List<RelatedEntityResponse> relatedEntities;

}
