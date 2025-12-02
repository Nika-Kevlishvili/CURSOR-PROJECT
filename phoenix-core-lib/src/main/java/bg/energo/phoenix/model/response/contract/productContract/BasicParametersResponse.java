package bg.energo.phoenix.model.response.contract.productContract;

import bg.energo.phoenix.model.enums.contract.express.ProductContractVersionStatus;
import bg.energo.phoenix.model.enums.contract.products.ContractDetailType;
import bg.energo.phoenix.model.enums.contract.products.ContractDetailsStatus;
import bg.energo.phoenix.model.enums.contract.products.ContractDetailsSubStatus;
import bg.energo.phoenix.model.enums.customer.CustomerType;
import bg.energo.phoenix.model.response.contract.ContractFileResponse;
import bg.energo.phoenix.model.response.contract.relatedEntities.RelatedEntityResponse;
import bg.energo.phoenix.model.response.customer.CustomerCommunicationDataResponse;
import bg.energo.phoenix.model.response.nomenclature.contract.ContractVersionTypesResponse;
import bg.energo.phoenix.model.response.nomenclature.product.currency.CurrencyResponse;
import bg.energo.phoenix.model.response.proxy.ProxyResponse;
import bg.energo.phoenix.model.response.shared.FileWithStatusesResponse;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class BasicParametersResponse {
    private Long id;
    private Integer versionId;
    private String productName;
    private Long productId;
    private Long productVersionId;
    private Long productDetailId;
    private String contractNumber;
    private LocalDate creationDate;

    private ContractDetailsStatus status;
    private ContractDetailsSubStatus subStatus;
    private LocalDate statusModifyDate;

    private ContractDetailType type;

    private Boolean hasUntilAmount;
    private Boolean hasUntilVolume;
    private Boolean procurementLaw;

    private BigDecimal untilAmount;
    private BigDecimal untilVolume;
    private CurrencyResponse untilAmountCurrency;

    private LocalDate signingDate;
    private LocalDate entryInForceDate;
    private LocalDate startOfInitialTerm;
    private LocalDate contractTermEndDate;
    private LocalDate activationDate;
    private LocalDate supplyActivationDate;
    private LocalDate perpetuityDate;
    private LocalDate terminationDate;

    private Long customerId;
    private CustomerType customerType;
    private Long customerVersionId;
    private Long customerDetailId;
    private Boolean businessActivity;
    private String customerName;
    private String contractSuffix;

    private CustomerCommunicationDataResponse billingCommunicationData;
    private CustomerCommunicationDataResponse contractCommunicationData;
    private List<ProxyResponse> proxy;

    private List<RelatedEntityResponse> relatedEntities;

    private ProductContractVersionStatus versionStatus;
    private List<ContractVersionTypesResponse> versionTypesResponse;

    private List<ContractFileResponse> files;
    private List<FileWithStatusesResponse> documents;
    private List<ProductContractResignResponse> resignedFrom;
    private List<ProductContractResignResponse> resignedTo;
}
