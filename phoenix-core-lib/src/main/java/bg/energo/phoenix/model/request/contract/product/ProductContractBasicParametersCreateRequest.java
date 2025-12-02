package bg.energo.phoenix.model.request.contract.product;

import bg.energo.phoenix.model.customAnotations.DuplicatedValuesValidator;
import bg.energo.phoenix.model.enums.contract.express.ProductContractVersionStatus;
import bg.energo.phoenix.model.enums.contract.products.ContractDetailType;
import bg.energo.phoenix.model.enums.contract.products.ContractDetailsStatus;
import bg.energo.phoenix.model.enums.contract.products.ContractDetailsSubStatus;
import bg.energo.phoenix.model.request.contract.ProxyEditRequest;
import bg.energo.phoenix.model.request.contract.relatedEntities.RelatedEntityRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;


@Data
public class ProductContractBasicParametersCreateRequest {

    private ContractDetailsStatus status;
    private ContractDetailsSubStatus subStatus;
    private LocalDate statusModifyDate;
    @NotNull(message = "basicParameters.productId-productId is mandatory;")
    private Long productId;
    @NotNull(message = "basicParameters.productVersionId-productVersionId is mandatory;")
    private Long productVersionId;
    @NotNull(message = "basicParameters.type-type is mandatory;")
    private ContractDetailType type;

    private boolean hasUntilAmount;
    private boolean hasUntilVolume;
    private boolean procurementLaw;
    @DecimalMin(value = "0.01", message = "basicParameters.untilAmount-[untilAmount] should be more than 0.01;")
    @DecimalMax(value = "99999999.99", message = "basicParameters.untilAmount-[untilAmount] should be less than 99999999.99;")
    private BigDecimal untilAmount;
    private BigDecimal untilVolume;
    private Long untilAmountCurrencyId;

    private LocalDate signingDate;
    private LocalDate entryInForceDate;
    private LocalDate startOfInitialTerm;


    @NotNull(message = "basicParameters.customerId-customerId is mandatory;")
    private Long customerId;
    @NotNull(message = "basicParameters.customerVersionId-customerVersionId is mandatory;")
    private Long customerVersionId;

    @NotNull(message = "basicParameters.communicationDataBillingId-communicationDataBillingId is mandatory;")
    private Long communicationDataBillingId;
    @NotNull(message = "basicParameters.communicationDataContractId-communicationDataContractId is mandatory;")
    private Long communicationDataContractId;

    private Long customerNewDetailsId;

    @DuplicatedValuesValidator(fieldPath = "basicParameters.files")
    private List<Long> files;

    @DuplicatedValuesValidator(fieldPath = "basicParameters.documents")
    private List<Long> documents;

    @NotNull(message = "basicParameters.versionStatus-versionStatus is mandatory;")
    private ProductContractVersionStatus versionStatus;
    @NotEmpty(message = "basicParameters.versionTypeIds-versionTypeIds can not be empty;")
    private Set<Long> versionTypeIds;

    private List<@Valid ProxyEditRequest> proxy;

    private List<@Valid RelatedEntityRequest> relatedEntities;

}
